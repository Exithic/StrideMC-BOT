package Paqlio.me;

import Paqlio.me.Addons.DiscordChatBridge;
import Paqlio.me.Addons.SyncManager;
import Paqlio.me.CMD.DiscordLinkCommand;
import Paqlio.me.Configurations.Constants;
import Paqlio.me.Listeners.*;
import Paqlio.me.ListenersAdapters.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public final class BOT extends JavaPlugin {

    private static JDA jda;
    private static BOT instance;
    private static final Logger LOGGER = Logger.getLogger("StrideMC-Bot");

    private SyncManager syncManager;
    private ConsoleSystem consoleSystem;

    public static JDA getJda() { return jda; }
    public static BOT getInstance() { return instance; }
    public SyncManager getSyncManager() { return syncManager; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        Constants.loadFromConfig();

        var rawToken = System.getenv("DISCORD_TOKEN");
        if (rawToken == null || rawToken.isBlank()) rawToken = getConfig().getString("bot.token");
        if (rawToken == null || rawToken.isBlank() || rawToken.contains("TUTAJ_WKLEJ")) {
            LOGGER.severe("✖ Brak poprawnego tokena! Ustaw DISCORD_TOKEN w env lub bot.token w config.yml.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        final var token = rawToken.trim();

        // 1. Managery i Komendy Bukkit
        syncManager = new SyncManager();
        var discordCmd = getCommand("dclink");
        if (discordCmd != null) discordCmd.setExecutor(new DiscordLinkCommand(syncManager));

        // 2. Eventy Bukkita + Bridge
        var pm = Bukkit.getPluginManager();
        pm.registerEvents(new Chat(), this);
        pm.registerEvents(new PlayerJoin(), this);
        pm.registerEvents(new PlayerQuit(), this);

        var bridge = new Paqlio.me.Addons.BridgeListener();
        getServer().getMessenger().registerIncomingPluginChannel(this, "stridemc:bridge", bridge);

        // 3. Start JDA (Async)
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                LOGGER.info("⏳ Łączenie z API Discorda...");

                jda = JDABuilder.createLight(token,
                                GatewayIntent.GUILD_MESSAGES,
                                GatewayIntent.MESSAGE_CONTENT,
                                GatewayIntent.GUILD_MEMBERS,
                                GatewayIntent.GUILD_VOICE_STATES)
                        .setActivity(Activity.listening(getConfig().getString("bot.status", "❤️ STRIDEMC")))
                        .build();

                jda.awaitReady();
                LOGGER.info("✅ JDA połączone!");

                // ============================================================
                // 4. REJESTRACJA I ODŚWIEŻANIE KOMEND SLASH (MENU DISCORDA)
                // ============================================================
                jda.updateCommands().addCommands(
                        Commands.slash("link", "Połącz swoje konto Minecraft z Discordem!"),
                        Commands.slash("user", "Sprawdź informacje o profilu użytkownika")
                                .addOption(OptionType.USER, "użytkownik", "Wybierz gracza", true),
                        Commands.slash("mc", "Sprawdź status serwera Minecraft"),
                        Commands.slash("ticket", "Postaw panel zgłoszeń (Admin)"),
                        Commands.slash("regulamin", "Wyślij panel regulaminu (Admin)"),
                        Commands.slash("clear", "Usuń wiadomości na kanale (Admin)")
                                .addOption(OptionType.INTEGER, "ilość", "Liczba wiadomości (1-100)", true),

                        // NOWA KOMENDA DO GIVEAWAYÓW
                        Commands.slash("giveaway", "Stwórz losowanie z nagrodą (Admin)")
                                .addOption(OptionType.INTEGER, "czas", "Czas trwania w minutach", true)
                                .addOption(OptionType.STRING, "nagroda", "Co jest do wygrania?", true),

                        // NOWE KOMENDY
                        Commands.slash("online", "Sprawdź kto jest online na serwerze"),
                        Commands.slash("broadcast", "Wyślij ogłoszenie na czat MC (Admin)")
                                .addOption(OptionType.STRING, "wiadomość", "Treść ogłoszenia", true),
                        Commands.slash("reactionrole", "Stwórz panel reakcyjny (Admin)")
                                .addOption(OptionType.CHANNEL, "kanał", "Kanał dla panelu", true)
                                .addOption(OptionType.ROLE, "rola", "Rola do nadania", true)
                                .addOption(OptionType.STRING, "emoji", "Emoji (np. ✅)", true)
                                .addOption(OptionType.STRING, "nazwa", "Nazwa przycisku", true),
                        Commands.slash("help", "Pokaż listę komend bota")
                ).queue();

                // 5. Systemy i Listenery
                jda.addEventListener(new ButtonsInteractions());
                jda.addEventListener(new DiscordSyncListener(syncManager));

                consoleSystem = new ConsoleSystem();
                consoleSystem.start();
                jda.addEventListener(consoleSystem);


                registerListenerAdapters();

                DiscordChatBridge.sendServerStartMessage();

                // Auto-update status co 5 minut
                Bukkit.getScheduler().runTaskTimer(BOT.getInstance(), () -> {
                    var j = BOT.getJda();
                    if (j == null) return;
                    var online = Bukkit.getOnlinePlayers().size();
                    j.getPresence().setActivity(Activity.listening("❤️ " + online + " graczy | " + getConfig().getString("bot.status", "StrideMC")));
                }, 200L, 6000L);

            } catch (Exception e) {
                LOGGER.severe("✖ Błąd startu bota: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onDisable() {
        if (syncManager != null) syncManager.saveDataSync();
        if (consoleSystem != null) consoleSystem.stop();
        if (jda != null) {
            DiscordChatBridge.sendServerStopMessage();
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            jda.shutdown();
            try {
                if (!jda.awaitShutdown(5, TimeUnit.SECONDS)) jda.shutdownNow();
            } catch (InterruptedException e) {
                jda.shutdownNow();
            }
        }
    }

    private void registerListenerAdapters() {
        var adapters = new ListenerAdapter[]{
                new ChatMC(syncManager),
                new Clear(),
                new GiveawaySystem(),
                new HelpCommand(),
                new MemberJoin(),
                new OnlineCommand(),
                new BroadcastCommand(),
                new ReactionRoleCommand(),
                new Regulamin(),
                new Server(),
                new Ticket(),
                new User()
        };
        for (var a : adapters) jda.addEventListener(a);
        LOGGER.info("✅ Zarejestrowano " + adapters.length + " adapterów.");
    }
}
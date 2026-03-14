package Paqlio.me;

import Paqlio.me.Addons.DiscordChatBridge;
import Paqlio.me.Listeners.Chat;
import Paqlio.me.Listeners.PlayerJoin;
import Paqlio.me.Listeners.PlayerQuit;
import Paqlio.me.ListenersAdapters.ButtonsInteractions;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.reflect.Modifier;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public final class BOT extends JavaPlugin {
    private static JDA jda;
    private static final Logger LOGGER = Logger.getLogger("StrideMC-Bot");
    private ButtonsInteractions buttonsInteractions;

    public static JDA getJda() {
        return jda;
    }

    @Override
    public void onEnable() {
        // 1. Inicjalizacja konfiguracji
        saveDefaultConfig();
        var config = getConfig();

        // 2. Pobieranie i czyszczenie tokena (fix błędu 0xd3)
        var token = config.getString("bot.token");
        if (token == null || token.isEmpty() || token.equalsIgnoreCase("TUTAJ_WKLEJ_TOKEN")) {
            LOGGER.severe("Nie można uruchomić bota: Podaj poprawny token w config.yml!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        try {
            // 3. Budowanie JDA
            jda = JDABuilder.createLight(token.trim(),
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.MESSAGE_CONTENT,
                            GatewayIntent.GUILD_MEMBERS,
                            GatewayIntent.GUILD_VOICE_STATES)
                    .setActivity(Activity.listening(config.getString("bot.status", "StrideMC")))
                    .build();

            // 4. Rejestracja ButtonsInteractions (wymaga instancji pluginu do configu)
            buttonsInteractions = new ButtonsInteractions(this);
            jda.addEventListener(buttonsInteractions);

            // 5. Automatyczna rejestracja pozostałych adapterów
            registerListenerAdapters();

            // 6. Rejestracja komend Slash
            registerSlashCommands();

            // 7. Rejestracja eventów Bukkita
            var pm = Bukkit.getPluginManager();
            pm.registerEvents(new Chat(), this);
            pm.registerEvents(new PlayerJoin(), this);
            pm.registerEvents(new PlayerQuit(), this);

            LOGGER.info("StrideMC-BOT został pomyślnie uruchomiony!");
            DiscordChatBridge.sendServerStartMessage();

        } catch (Exception e) {
            LOGGER.severe("Wystąpił krytyczny błąd podczas startu JDA: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void registerSlashCommands() {
        if (jda == null) return;

        jda.updateCommands().addCommands(
                Commands.slash("mc", "Wyświetla informacje o serwerze Minecraft"),
                Commands.slash("ticket", "Tworzy nowy ticket"),
                Commands.slash("regulamin", "Wyświetla regulamin serwera"),
                Commands.slash("user", "Wyświetla informacje o użytkowniku")
                        .addOption(OptionType.USER, "użytkownik", "Wybierz użytkownika", true),
                Commands.slash("clear", "Usuwa określoną liczbę wiadomości")
                        .addOption(OptionType.INTEGER, "ilość", "Ile wiadomości usunąć?", true)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE))
        ).queue();
    }

    @Override
    public void onDisable() {
        LOGGER.info("Zamykanie systemów bota...");

        if (jda != null) {
            DiscordChatBridge.sendServerStopMessage();
            jda.shutdown();
            try {
                if (!jda.awaitShutdown(10, TimeUnit.SECONDS)) {
                    jda.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                jda.shutdownNow();
            }
        }

        if (buttonsInteractions != null && buttonsInteractions.getScheduler() != null) {
            var scheduler = buttonsInteractions.getScheduler();
            scheduler.shutdownNow();
        }

        LOGGER.info("StrideMC-BOT został pomyślnie wyłączony.");
    }

    private void registerListenerAdapters() {
        var packageName = "Paqlio.me.ListenersAdapters";
        try {
            var reflections = new Reflections(packageName, Scanners.SubTypes);
            var classes = reflections.getSubTypesOf(ListenerAdapter.class);

            var count = 0;
            for (var clazz : classes) {
                // Pomijamy klasy abstrakcyjne, interfejsy i te już dodane ręcznie
                if (Modifier.isAbstract(clazz.getModifiers()) || clazz.isInterface() || clazz.equals(ButtonsInteractions.class)) {
                    continue;
                }

                try {
                    var instance = clazz.getDeclaredConstructor().newInstance();
                    jda.addEventListener(instance);
                    count++;
                    LOGGER.info("Zautomatyzowana rejestracja: " + clazz.getSimpleName());
                } catch (Exception e) {
                    LOGGER.warning("Błąd podczas tworzenia instancji " + clazz.getSimpleName() + ": " + e.getMessage());
                }
            }
            LOGGER.info("Pomyślnie zarejestrowano " + count + " adapterów JDA.");
        } catch (Exception e) {
            LOGGER.severe("Błąd podczas skanowania pakietu adapterów: " + e.getMessage());
        }
    }
}
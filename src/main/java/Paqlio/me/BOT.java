package Paqlio.me;

import Paqlio.me.Addons.DiscordChatBridge;
import Paqlio.me.Listeners.Chat;
import Paqlio.me.Listeners.PlayerJoin;
import Paqlio.me.Listeners.PlayerQuit;
import Paqlio.me.ListenersAdapters.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class BOT extends JavaPlugin {
    private static JDA jda;
    private static final Logger LOGGER = Logger.getLogger("StrideMC-Bot");
    private ButtonsInteractions buttonsInteractionsInstance;

    public static JDA getJda() {
        return jda;
    }

    @Override
    public void onEnable() {
        try {
            jda = JDABuilder.createLight("MTMzNTAwMDQ3NTIyODk2NjkxNg.Gjo9GT.P6UjfNwDIqvLWxzjaJQ3txpuLDgU9agHkrbrw4",
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.MESSAGE_CONTENT,
                            GatewayIntent.GUILD_MEMBERS,
                            GatewayIntent.GUILD_VOICE_STATES)
                    .setActivity(Activity.listening("❤️ STRIDEMC"))
                    .addEventListeners(new ListenerAdapter() {
                        @Override
                        public void onReady(@NotNull ReadyEvent event) {
                            LOGGER.info("JDA jest gotowe!");
                            DiscordChatBridge.sendServerStartMessage();
                        }
                    })
                    .build();

            buttonsInteractionsInstance = new ButtonsInteractions();
            jda.addEventListener(buttonsInteractionsInstance);
            registerListenerAdapters();

            jda.updateCommands()
                    .addCommands(Commands.slash("mc", "Wyświetla informacje o serwerze Minecraft"))
                    .addCommands(Commands.slash("ticket", "Tworzy ticket"))
                    .addCommands(Commands.slash("regulamin", "Wyświetla regulamin"))
                    .addCommands(Commands.slash("user", "Wyświetla informacje o użytkowniku")
                            .addOption(OptionType.USER, "użytkownik", "Użytkownik", true))
                    .addCommands(Commands.slash("clear", "Czyści wiadomości")
                            .addOption(OptionType.INTEGER, "ilość", "Ilość wiadomości do usunięcia", true)
                            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE)))
                    .queue();

            var pm = Bukkit.getPluginManager();
            jda.addEventListener(
                    new Ticket(),
                    new MemberJoin(),
                    new Regulamin(),
                    new ChatMC(),
                    new User(),
                    new Server(),
                    new Clear()
            );

            pm.registerEvents(new Chat(), this);
            pm.registerEvents(new PlayerJoin(), this);
            pm.registerEvents(new PlayerQuit(), this);

            LOGGER.info("StrideMC-BOT został włączony!");
        } catch (Exception e) {
            LOGGER.severe("Błąd podczas uruchamiania bota: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDisable() {
        LOGGER.info("🛑 Wyłączanie bota JDA...");

        if (jda != null && jda.getStatus() == JDA.Status.CONNECTED) {
            DiscordChatBridge.sendServerStopMessage();

            // Daj chwilę na wysłanie wiadomości
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (jda != null) {
            jda.shutdown();
            try {
                jda.awaitShutdown(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.severe("Wątek wyłączania JDA został przerwany.");
            }
        }

        if (buttonsInteractionsInstance != null &&
                buttonsInteractionsInstance.getScheduler() != null &&
                !buttonsInteractionsInstance.getScheduler().isShutdown()) {

            LOGGER.info("Wyłączanie schedulera ButtonsInteractions...");
            buttonsInteractionsInstance.getScheduler().shutdownNow();
            try {
                if (!buttonsInteractionsInstance.getScheduler().awaitTermination(5, TimeUnit.SECONDS)) {
                    LOGGER.warning("Scheduler ButtonsInteractions nie zakończył działania w czasie.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.severe("Wątek wyłączania schedulera został przerwany.");
            }
        }

        LOGGER.info("StrideMC-BOT został wyłączony!");
    }

    private void registerListenerAdapters() {
        var packageName = "Paqlio.me.ListenersAdapters";
        var classLoader = getClass().getClassLoader();

        try {
            LOGGER.info("Skanowanie pakietu " + packageName + "...");
            var reflections = new Reflections(packageName, new SubTypesScanner(false), classLoader);
            var listenerClasses = reflections.getSubTypesOf(ListenerAdapter.class)
                    .stream()
                    .filter(clazz -> !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers()))
                    .filter(clazz -> !clazz.equals(ButtonsInteractions.class))
                    .collect(Collectors.toSet());

            if (listenerClasses.isEmpty()) {
                LOGGER.warning("Nie znaleziono dodatkowych adapterów JDA w pakiecie: " + packageName);
                return;
            }

            var registeredCount = 0;
            for (var listenerClass : listenerClasses) {
                try {
                    var instance = listenerClass.getDeclaredConstructor().newInstance();
                    jda.addEventListener(instance);
                    registeredCount++;
                    LOGGER.info("Zarejestrowano: " + listenerClass.getSimpleName());
                } catch (NoSuchMethodException e) {
                    LOGGER.warning(listenerClass.getSimpleName() + " nie ma konstruktora bezargumentowego");
                } catch (InstantiationException | IllegalAccessException e) {
                    LOGGER.warning("Nie można utworzyć instancji: " + listenerClass.getSimpleName());
                } catch (InvocationTargetException e) {
                    LOGGER.warning("Błąd w konstruktorze: " + listenerClass.getSimpleName());
                    e.getTargetException().printStackTrace();
                }
            }
            LOGGER.info("Zarejestrowano " + registeredCount + " adapterów z pakietu " + packageName);
        } catch (Exception e) {
            LOGGER.warning("Błąd podczas rejestracji adapterów: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
package Paqlio.me;

import Paqlio.me.Listeners.Chat;
import Paqlio.me.Listeners.PlayerJoin;
import Paqlio.me.Listeners.PlayerQuit;
import Paqlio.me.ListenersAdapters.*; // Import all your ListenerAdapters
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
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

    // Deklaracja instancji ButtonsInteractions jako pola klasy BOT
    private ButtonsInteractions buttonsInteractionsInstance;

    public static JDA getJda() {
        return jda;
    }

    @Override
    public void onEnable() {
        try {
            jda = JDABuilder.createLight("MTMzNTAwMDQ3NTIyODk2NjkxNg.Gix5Ox.65xJUBexvcu2dzufaJH3pMSg6DJUXGCScf1V2E", GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_VOICE_STATES)
                    .setActivity(Activity.playing("🪴 "))
                    .build();

            buttonsInteractionsInstance = new ButtonsInteractions();
            jda.addEventListener(buttonsInteractionsInstance); // Rejestrujemy ją bezpośrednio
            registerListenerAdapters();
            jda.updateCommands()
                    .addCommands(Commands.slash("mc", "Wyświetla informacje o serwerze Minecraft"))
                    .addCommands(Commands.slash("ticket", "Tworzy ticket"))
                    .addCommands(Commands.slash("regulamin", "Wyświetla regulamin"))
                    .addCommands(Commands.slash("user", "Wyświetla informacje o użytkowniku").addOption(OptionType.USER, "użytkownik", "Użytkownik", true))
                    .queue();

            var pm = Bukkit.getPluginManager();
            jda.addEventListener(new Ticket(), new MemberJoin(), new Regulamin(), new ChatMC(), new User(), new Server());
            pm.registerEvents(new Chat(), this);
            pm.registerEvents(new PlayerJoin(), this);
            pm.registerEvents(new PlayerQuit(), this);

            LOGGER.info("StrideMC-BOT został włączony!");
        } catch (Exception e) {
            LOGGER.severe("Błąd podczas uruchamiania bota: " + e.getMessage());
            throw new RuntimeException(e); // Propaguj błąd, aby plugin się nie włączył niepoprawnie
        }
    }


    @Override
    public void onDisable() {
        LOGGER.info("🛑 Wyłączanie bota JDA...");
        if (jda != null) {
            jda.shutdown(); // Shut down JDA first
            try {
                // Wait a bit for JDA to fully disconnect its WebSocket
                // Await the shutdown completion
                jda.awaitShutdown(10, TimeUnit.SECONDS); // Give JDA up to 10 seconds to shut down gracefully
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.severe("Wątek wyłączania JDA został przerwany.");
            } catch (Exception e) {
                LOGGER.severe("Błąd podczas oczekiwania na zamknięcie JDA: " + e.getMessage());
            }
        }

        // Now, shut down your custom scheduler
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
        final String packageName = "Paqlio.me.ListenersAdapters";
        final ClassLoader classLoader = getClass().getClassLoader();

        try {
            LOGGER.info("Rozpoczęto skanowanie pakietu " + packageName + " w poszukiwaniu adapterów nasłuchujących...");
            Reflections reflections = new Reflections(packageName, new SubTypesScanner(false), classLoader);
            Set<Class<? extends ListenerAdapter>> listenerClasses =
                    reflections.getSubTypesOf(ListenerAdapter.class)
                            .stream()
                            .filter(clazz -> !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers()))
                            .filter(clazz -> !clazz.equals(ButtonsInteractions.class))
                            .collect(Collectors.toSet());

            if (listenerClasses.isEmpty()) {
                LOGGER.warning("Nie znaleziono żadnych dodatkowych adapterów JDA w pakiecie: " + packageName);
                return;
            }
            int registeredCount = 0;
            for (Class<? extends ListenerAdapter> listenerClass : listenerClasses) {
                try {
                    ListenerAdapter instance = listenerClass.getDeclaredConstructor().newInstance();
                    jda.addEventListener(instance);
                    registeredCount++;
                    LOGGER.info("Zarejestrowano adapter: " + listenerClass.getSimpleName());
                } catch (NoSuchMethodException e) {
                    LOGGER.warning("Klasa " + listenerClass.getSimpleName() + " nie ma konstruktora bezargumentowego: " + e.getMessage());
                } catch (InstantiationException | IllegalAccessException e) {
                    LOGGER.warning("Nie można utworzyć instancji klasy " + listenerClass.getSimpleName() + ": " + e.getMessage());
                } catch (InvocationTargetException e) {
                    LOGGER.warning("Błąd w konstruktorze klasy " + listenerClass.getSimpleName() + ": " + e.getTargetException().getMessage());
                    e.getTargetException().printStackTrace(); // Wydrukuj pełny stos błędu konstruktora
                }
            }
            LOGGER.info("Zarejestrowano " + registeredCount + " dodatkowych adapterów nasłuchujących z pakietu " + packageName);
        } catch (Exception e) {
            LOGGER.warning("Krytyczny błąd podczas rejestracji adapterów za pomocą Reflections: " + e.getMessage());
            e.printStackTrace(); // Wydrukuj pełny stos błędu refleksji
        }
    }
}
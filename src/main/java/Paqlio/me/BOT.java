package Paqlio.me;

import Paqlio.me.Listeners.Chat;
import Paqlio.me.Listeners.PlayerJoin;
import Paqlio.me.Listeners.PlayerQuit;
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
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class BOT extends JavaPlugin {
    private static JDA jda;
    private static final Logger LOGGER = Logger.getLogger("StrideMC-Bot");

    public static JDA getJda() {
        return jda;
    }

    @Override
    public void onEnable() {
        try {
            jda = JDABuilder.createLight("MTMzNTAwMDQ3NTIyODk2NjkxNg.Gix5Ox.65xJUBexvcu2dzufaJH3pMSg6DJUXGCScf1V2E", GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_VOICE_STATES)
                    .setActivity(Activity.playing("🪴 "))
                    .build();
            registerListenerAdapters();
            jda.updateCommands()
                    .addCommands(Commands.slash("mc", "Wyświetla informacje o serwerze Minecraft"))
                    .addCommands(Commands.slash("ticket", "Tworzy ticket"))
                    .addCommands(Commands.slash("regulamin", "Wyświetla regulamin"))
                    .addCommands(Commands.slash("user", "Wyświetla informacje o użytkowniku").addOption(OptionType.USER, "użytkownik", "Użytkownik", true))
                    .queue();

            var pm = Bukkit.getPluginManager();
            jda.addEventListener(new Ticket(), new ButtonsInteractions(), new MemberJoin(), new Regulamin(),new ChatMC(),new User(),new Server());
            pm.registerEvents(new Chat(), this);
            pm.registerEvents(new PlayerJoin(),this);
            pm.registerEvents(new PlayerQuit(),this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDisable() {
        if (jda != null) {
            LOGGER.info("🛑 Wyłączanie bota...");
            jda.shutdown();
        }
    }
    private void registerListenerAdapters() {
        final String packageName = "paqlio.me.listeners"; // Zalecana mała litera w nazwach pakietów
        final ClassLoader classLoader = getClass().getClassLoader();

        try {
            LOGGER.info("Rozpoczęto skanowanie pakietu {} w poszukiwaniu adapterów nasłuchujących...");
            Reflections reflections = new Reflections(packageName, new SubTypesScanner(false), classLoader);
            Set<Class<? extends ListenerAdapter>> listenerClasses =
                    reflections.getSubTypesOf(ListenerAdapter.class)
                            .stream()
                            .filter(clazz -> !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers()))
                            .collect(Collectors.toSet());
            if (listenerClasses.isEmpty()) {
                LOGGER.warning("Nie znaleziono żadnych adapterów w pakiecie: " + packageName);
                return;
            }
            int registeredCount = 0;
            for (Class<? extends ListenerAdapter> listenerClass : listenerClasses) {
                try {
                    ListenerAdapter instance = listenerClass.getDeclaredConstructor().newInstance();
                    jda.addEventListener(instance);
                    registeredCount++;
                    LOGGER.info("Zarejestrowano adapter: {}");
                } catch (NoSuchMethodException e) {
                    LOGGER.warning("Klasa {} nie ma konstruktora bezargumentowego");
                } catch (InstantiationException | IllegalAccessException e) {
                    LOGGER.warning("Nie można utworzyć instancji klasy {}: {}");
                } catch (InvocationTargetException e) {
                    LOGGER.warning("Błąd w konstruktorze klasy {}: {}");
                }
            }
            LOGGER.info("Zarejestrowano {} adapterów nasłuchujących z pakietu {}");
        } catch (Exception e) {
            LOGGER.warning("Krytyczny błąd podczas rejestracji adapter: " + e.getMessage());
        }
    }
}

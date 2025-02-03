package Paqlio.me;

import Paqlio.me.Listeners.Chat;
import Paqlio.me.Listeners.PlayerJoin;
import Paqlio.me.Listeners.PlayerQuit;
import Paqlio.me.ListenersAdapters.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

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

            jda.updateCommands()
                    .addCommands(Commands.slash("ticket", "Tworzy ticket"))
                    .addCommands(Commands.slash("regulamin", "Wyświetla regulamin"))
                    .queue();
            jda.addEventListener(new Ticket(), new ButtonsInteractions(), new MemberJoin(), new Regulamin(),new ChatMC());
            var pm  = Bukkit.getPluginManager();
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
}

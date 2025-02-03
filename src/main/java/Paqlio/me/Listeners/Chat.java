package Paqlio.me.Listeners;

import Paqlio.me.BOT;
import Paqlio.me.Configurations.Constants;
import me.clip.placeholderapi.PlaceholderAPI;
import net.dv8tion.jda.api.EmbedBuilder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.awt.*;
import java.util.Objects;

/**
 * @author Paqlio
 * @since 03.02.2025- 19:47
 **/
public class Chat implements Listener {
    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event){
        var player = event.getPlayer();
        var message = event.getMessage();
        var channel = BOT.getJda().getTextChannelById(Constants.MinecraftChannel);
        assert channel != null;
        var eb = new EmbedBuilder()
                .setColor(new Color(81, 81, 81))
                .setAuthor(PlaceholderAPI.setPlaceholders(player,"%luckperms_primary_group_name% ") + " "  + player.getName() + ": " + message,"https://StrideMC.pl/", "http://cravatar.eu/avatar/"+ player.getName() +"/128.png");
        channel.sendMessageEmbeds(eb.build()).queue();
    }
}

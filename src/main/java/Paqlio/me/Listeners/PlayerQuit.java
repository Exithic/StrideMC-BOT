package Paqlio.me.Listeners;

import Paqlio.me.BOT;
import Paqlio.me.Configurations.Constants;
import me.clip.placeholderapi.PlaceholderAPI;
import net.dv8tion.jda.api.EmbedBuilder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.awt.*;
import java.util.Objects;

/**
 * @author Paqlio
 * @since 03.02.2025- 19:51
 **/
public class PlayerQuit implements Listener {
    @EventHandler
    public void onQuit(PlayerQuitEvent event){
        var player = event.getPlayer();
        var channel = BOT.getJda().getTextChannelById(Constants.MinecraftChannel);
        var eb = new EmbedBuilder()
                .setColor(new Color(255, 0, 0))
                .setAuthor(PlaceholderAPI.setPlaceholders(player,"%luckperms_primary_group_name%") +   " "  + player.getName(),"https://stridemc.pl/", "http://cravatar.eu/avatar/"+ player.getName() +"/128.png");
        assert channel != null;
        channel.sendMessageEmbeds(eb.build()).queue();
    }
}

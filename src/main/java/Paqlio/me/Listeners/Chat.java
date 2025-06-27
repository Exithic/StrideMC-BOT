package Paqlio.me.Listeners;

import Paqlio.me.BOT;
import Paqlio.me.Configurations.Constants;
import me.clip.placeholderapi.PlaceholderAPI;
import net.dv8tion.jda.api.EmbedBuilder;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.awt.*;

public class Chat implements Listener {
    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        var player = event.getPlayer();
        var message = event.getMessage();

        // Jeśli gracz wpisuje coś w innym pluginie, blokujemy wysyłanie wiadomości na Discorda
        if (player.isConversing()) return;

        var channel = BOT.getJda().getTextChannelById(Constants.MinecraftChannel);
        if (channel == null) return;

        var group = PlaceholderAPI.setPlaceholders(player, "%luckperms_primary_group_name%");
        var avatarUrl = "http://cravatar.eu/avatar/" + player.getName() + "/128.png";

        var eb = new EmbedBuilder()
                .setColor(new Color(81, 81, 81))
                .setAuthor(group + " " + player.getName() + ": " + message, "https://StrideMC.pl/", avatarUrl);
        channel.sendMessageEmbeds(eb.build()).queue();
    }
}

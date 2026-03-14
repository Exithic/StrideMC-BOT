package Paqlio.me.Listeners;

import Paqlio.me.BOT;
import Paqlio.me.Configurations.Constants;
import me.clip.placeholderapi.PlaceholderAPI;
import net.dv8tion.jda.api.EmbedBuilder;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.time.Instant;

/**
 * @author Paqlio
 * @since 03.02.2025 - 19:51
 **/
public class PlayerJoin implements Listener {

    private static final Color JOIN_COLOR = new Color(80, 255, 44);

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        var player = event.getPlayer();
        var jda = BOT.getJda();

        if (jda == null) return;

        var channel = jda.getTextChannelById(Constants.MinecraftChannel);
        if (channel == null) return;

        // 1. Pobieranie rangi przez PAPI (z fallbackiem na "Gracz")
        var group = PlaceholderAPI.setPlaceholders(player, "%luckperms_primary_group_name%");
        if (group.equals("%luckperms_primary_group_name%")) group = "Gracz";

        // 2. Statystyki serwera
        var onlineCount = Bukkit.getOnlinePlayers().size();

        // 3. Budowanie estetycznego powiadomienia
        var embed = new EmbedBuilder()
                .setColor(JOIN_COLOR)
                .setAuthor(
                        "➕ " + group.toUpperCase() + " | " + player.getName() + " dołączył do gry!",
                        Constants.link,
                        "https://mc-heads.net/avatar/" + player.getName() + "/128.png"
                )
                .setFooter("Graczy online: " + onlineCount, null)
                .setTimestamp(Instant.now());

        channel.sendMessageEmbeds(embed.build()).queue();
    }
}
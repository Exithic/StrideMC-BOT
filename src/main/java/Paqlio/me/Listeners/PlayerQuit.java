package Paqlio.me.Listeners;

import Paqlio.me.BOT;
import Paqlio.me.Configurations.Constants;
import me.clip.placeholderapi.PlaceholderAPI;
import net.dv8tion.jda.api.EmbedBuilder;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.time.Instant;

/**
 * @author Paqlio
 * @since 03.02.2025 - 19:51
 **/
public class PlayerQuit implements Listener {

    private static final Color QUIT_COLOR = new Color(255, 0, 0);

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        var player = event.getPlayer();
        var jda = BOT.getJda();

        // Bezpieczne sprawdzanie JDA
        if (jda == null) return;

        var channel = jda.getTextChannelById(Constants.MinecraftChannel);
        if (channel == null) return;

        // 1. Pobieranie rangi przez PAPI (fallback na "Gracz")
        var group = PlaceholderAPI.setPlaceholders(player, "%luckperms_primary_group_name%");
        if (group.equals("%luckperms_primary_group_name%")) group = "Gracz";

        // 2. Statystyki serwera (liczba osób PO wyjściu gracza)
        var onlineCount = Bukkit.getOnlinePlayers().size() - 1;
        var finalCount = Math.max(0, onlineCount); // Zabezpieczenie przed ujemną liczbą

        // 3. Budowanie estetycznego powiadomienia
        var embed = new EmbedBuilder()
                .setColor(QUIT_COLOR)
                .setAuthor(
                        "➖ " + group.toUpperCase() + " | " + player.getName() + " opuścił grę",
                        Constants.link,
                        "https://mc-heads.net/avatar/" + player.getName() + "/128.png"
                )
                .setFooter("Pozostało graczy: " + finalCount, null)
                .setTimestamp(Instant.now());

        channel.sendMessageEmbeds(embed.build()).queue();
    }
}
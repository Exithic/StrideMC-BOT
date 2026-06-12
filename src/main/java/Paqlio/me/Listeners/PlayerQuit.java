package Paqlio.me.Listeners;

import Paqlio.me.BOT;
import Paqlio.me.Configurations.Constants;
import Paqlio.me.Configurations.EmbedHelper;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerQuit implements Listener {

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        var player = event.getPlayer();
        var jda = BOT.getJda();
        if (jda == null) return;

        var channel = jda.getTextChannelById(Constants.MINECRAFT_CHANNEL);
        if (channel == null) return;

        var finalCount = Math.max(0, Bukkit.getOnlinePlayers().size() - 1);
        var embed = EmbedHelper.error("")
                .setAuthor("➖ " + player.getName() + " opuścił serwer", null,
                        "https://mc-heads.net/avatar/" + player.getName() + "/128.png")
                .setFooter("Graczy online: " + finalCount, null);

        Bukkit.getScheduler().runTaskAsynchronously(BOT.getInstance(), () ->
                channel.sendMessageEmbeds(embed.build()).queue(null, e -> {}));
    }
}

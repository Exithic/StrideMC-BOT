package Paqlio.me.Listeners;

import Paqlio.me.BOT;
import Paqlio.me.Configurations.Constants;
import Paqlio.me.Configurations.EmbedHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerJoin implements Listener {

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        var player = event.getPlayer();
        var jda = BOT.getJda();
        if (jda == null) return;

        var channel = jda.getTextChannelById(Constants.MINECRAFT_CHANNEL);
        if (channel == null) return;

        var embed = EmbedHelper.success("")
                .setAuthor("➕ " + player.getName() + " dołączył do gry", null,
                        "https://mc-heads.net/avatar/" + player.getName() + "/128.png")
                .setFooter("Graczy online: " + Bukkit.getOnlinePlayers().size(), null);

        Bukkit.getScheduler().runTaskAsynchronously(BOT.getInstance(), () ->
                channel.sendMessageEmbeds(embed.build()).queue(null, e -> {}));
    }
}

package Paqlio.me.ListenersAdapters;

import Paqlio.me.BOT;
import Paqlio.me.Configurations.EmbedHelper;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;

import java.lang.management.ManagementFactory;

public class OnlineCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("online")) return;

        event.deferReply().queue();

        Bukkit.getScheduler().runTask(BOT.getInstance(), () -> {
            var players = Bukkit.getOnlinePlayers();
            var sb = new StringBuilder();
            players.forEach(p -> sb.append("`").append(p.getName()).append("` "));

            var avgPing = players.stream().mapToInt(org.bukkit.entity.Player::getPing).average().orElse(0);
            var tps = ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class).getSystemLoadAverage();
            var tpsStr = tps < 0 ? "?" : String.format("%.1f", Math.min(20, tps));

            var embed = EmbedHelper.info("🎮 Online")
                    .addField("👥 Gracze", "**" + players.size() + "**", true)
                    .addField("📶 Ping", "**" + String.format("%.0f ms", avgPing) + "**", true)
                    .addField("⚡ TPS", "**" + tpsStr + "**", true)
                    .setDescription(players.isEmpty() ? "*Brak graczy online*" : sb.toString());

            event.getHook().sendMessageEmbeds(embed.build()).queue();
        });
    }
}

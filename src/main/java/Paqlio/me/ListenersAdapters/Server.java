package Paqlio.me.ListenersAdapters;

import Paqlio.me.Configurations.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;

/**
 * @author Paqlio
 * @since 03.04.2025- 17:31
 **/
public class Server extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("mc")) return;
        var server = event.getGuild();
        assert server != null;
        var eb = new EmbedBuilder()
                .setTitle("⚠️ Informacje o serwerze")
                .addField("`\uD83C\uDF10` IP", "stridemc.pl" , false)
                .addField("`⚡` Wersja", Bukkit.getBukkitVersion() , false)
                .addField("`\uD83C\uDF0D` Graczy Online", Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers(), false)
                .addField("`\uD83D\uDD25` Typ Serwera", "Java Edition", false)
                .addField("`\uD83C\uDFF9` Typ Silnika", Bukkit.getServer().getName(), false)
                .setFooter(server.getName(), server.getIconUrl())
                .setColor(Constants.defaultcolor);
        event.replyEmbeds(eb.build()).queue();
    }
}

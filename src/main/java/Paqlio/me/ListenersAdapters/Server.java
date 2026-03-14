package Paqlio.me.ListenersAdapters;

import Paqlio.me.Configurations.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.stream.Collectors;

public class Server extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("mc")) return;

        var guild = event.getGuild();
        if (guild == null) return;

        // 1. Podstawowe dane z Bukkit
        var version = Bukkit.getBukkitVersion().split("-")[0];
        var onlineCount = Bukkit.getOnlinePlayers().size();
        var maxPlayers = Bukkit.getMaxPlayers();

        // 2. Pobieranie administracji online (Gracze z OP lub permisją)
        var staffOnline = Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.isOp() || p.hasPermission("stridemc.staff"))
                .map(p -> "• " + p.getName())
                .collect(Collectors.joining("\n"));

        if (staffOnline.isEmpty()) staffOnline = "• *Brak administracji online*";

        // 3. Obliczanie pingu (średni TPS lub ping serwera)
        // W nowszych wersjach Paper/Spigot można pobrać ping bezpośrednio
        var tps = Bukkit.getTPS()[0]; // Pobiera średni TPS z ostatniej minuty
        var formattedTps = String.format("%.2f", tps);
        var statusEmoji = (tps >= 18.0) ? "🟢 Stabilny" : (tps >= 15.0) ? "🟡 Obciążony" : "🔴 Problemy";

        var embed = new EmbedBuilder()
                .setAuthor("Status Serwera Minecraft", null, guild.getIconUrl())
                .setTitle("`🎮`〢 StrideMC.pl")
                .setThumbnail(guild.getIconUrl())
                .setColor(Constants.defaultcolor)
                .setDescription("""
                        Aktualne statystyki i stan techniczny serwera.
                        Dołącz do nas i twórz historię!
                        """)
                .addField("`🌐` Adres IP", "```stridemc.pl```", false)
                .addField("`📈` Gracze", "`%d / %d`".formatted(onlineCount, maxPlayers), true)
                .addField("`⚡` Wersja", "`%s`".formatted(version), true)
                .addField("`📶` Stan Silnika", "`%s (%s)`".formatted(statusEmoji, formattedTps), true)
                .addField("`🛡️` Administracja Online:", staffOnline, false)
                .setFooter("Zażądano przez: " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl())
                .setTimestamp(Instant.now());

        event.replyEmbeds(embed.build()).queue();
    }
}
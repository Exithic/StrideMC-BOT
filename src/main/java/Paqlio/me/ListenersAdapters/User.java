package Paqlio.me.ListenersAdapters;

import Paqlio.me.Configurations.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;

/**
 * @author Paqlio
 * @since 29.03.2025 - 22:38
 **/
public class User extends ListenerAdapter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy");

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("user")) return;

        var targetMember = event.getOption("użytkownik").getAsMember();
        var guild = event.getGuild();

        if (targetMember == null || guild == null) {
            event.reply("❌ Nie udało się znaleźć tego użytkownika.").setEphemeral(true).queue();
            return;
        }

        // 1. Logika sprawdzania rangi (bezpieczna - bierze najwyższą lub "Brak")
        var highestRole = targetMember.getRoles().stream()
                .max(Comparator.comparingInt(Role::getPosition))
                .map(Role::getAsMention)
                .orElse("`Brak` ");

        // 2. Integracja z Minecraft (sprawdzanie po nicku z Discorda)
        var mcPlayer = Bukkit.getPlayer(targetMember.getEffectiveName());
        var mcStatus = (mcPlayer != null && mcPlayer.isOnline())
                ? "🟢 **Online** (`" + (int) mcPlayer.getHealth() + " HP`)"
                : "🔴 **Offline**";

        var embed = new EmbedBuilder()
                .setColor(Constants.defaultcolor)
                .setAuthor("Informacje o profilu", null, targetMember.getEffectiveAvatarUrl())
                .setThumbnail(targetMember.getEffectiveAvatarUrl())
                .setDescription("""
                        ### `👤`〢 Profil: %s
                        Statystyki i informacje o użytkowniku serwera **%s**.
                        """.formatted(targetMember.getAsMention(), guild.getName()))

                .addField("`🆔` ID Konta", "`" + targetMember.getId() + "`", true)
                .addField("`🛡️` Najwyższa Ranga", highestRole, true)
                .addField("`🤖` Typ konta", (targetMember.getUser().isBot() ? "✅ Bot" : "👤 Gracz"), true)

                .addField("`🎮` Status Minecraft", mcStatus, false)

                .addField("`📅` Data utworzenia", "`" + targetMember.getTimeCreated().format(DATE_FORMATTER) + "`", true)
                .addField("`📥` Data dołączenia", "`" + targetMember.getTimeJoined().format(DATE_FORMATTER) + "`", true)

                .setFooter("Zażądano przez: " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl())
                .setTimestamp(java.time.Instant.now());

        event.replyEmbeds(embed.build()).queue();
    }
}
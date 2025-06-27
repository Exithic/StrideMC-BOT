package Paqlio.me.ListenersAdapters;

import Paqlio.me.Configurations.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;

/**
 * @author Paqlio
 * @since 29.03.2025- 22:38
 **/
public class User extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("user")) return;
        var user = event.getOption("użytkownik").getAsMember();
        var server = event.getGuild();
        assert server != null;
        var eb = new EmbedBuilder()
                .setColor(new Color(0x2F3136))
                .setAuthor("`⚠️` Informacje o użytkowniku", null, user.getEffectiveAvatarUrl())
                .addField("● Nick Użytkownika", user.getAsMention(), true)
                .addField("● ID Konta", user.getId(), true)
                .addField("● Czy jest Botem?", (user.getUser().isBot() ? "✅ Tak" : "❌ Nie"), false)
                .addField("● Ranga", user.getRoles().getFirst().getAsMention(), true)
                .addField("● Konto Utworzono", user.getTimeCreated().format(DateTimeFormatter.ofPattern("HH:mm dd-MM-yyyy")), true)
                .addField("● Data Dołączenia", user.getTimeJoined().format(DateTimeFormatter.ofPattern("HH:mm dd-MM-yyyy")), true)
                .setThumbnail(user.getEffectiveAvatarUrl())
                .setColor(Constants.defaultcolor)
                .setFooter(server.getName(), server.getIconUrl());
        event.replyEmbeds(eb.build()).queue();
    }
}

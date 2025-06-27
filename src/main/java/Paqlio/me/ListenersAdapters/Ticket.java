package Paqlio.me.ListenersAdapters;

import Paqlio.me.Configurations.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Objects;

public class Ticket extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("ticket")) return;

        var guild = event.getGuild();
        var channel = event.getChannel();
        if (guild == null) return;

        var eb = new EmbedBuilder()
                .setColor(Constants.defaultcolor)
                .setAuthor("📨 StrideMC - Ticket System", Constants.link, guild.getIconUrl())
                .setDescription(">  ## `⚡`〢 INFORMACJA  \n" +
                        "> \n > `✨`   Hej! Masz problem albo potrzebujesz pomocy? Stwórz zgłoszenie i napisz, o co chodzi — zrobimy wszystko, żeby pomóc jak najszybciej!\n" +
                        "> \n" +
                        "> **`⚠️` Zgłoszenia dla żartów mogą skończyć się wyrzuceniem lub banem. **\n" +
                        "> Jeśli wystawiłeś zgłoszenie przez pomyłkę, po prostu je zamknij.\n")
                .setThumbnail(guild.getIconUrl())
                .setImage(Constants.img);

        channel.sendMessageEmbeds(eb.build())
                .addActionRow(Button.secondary("ticket", "📨〢Otwórz Ticket"))
                .queue();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!event.getComponentId().equals("ticket")) return;
        var guild = event.getGuild();
        var user = event.getUser();

        if (guild == null) return;
        var category = guild.getCategoryById("1388106421656096768");
        if (category == null) return;

        if (category.getTextChannels().stream().anyMatch(tc -> tc.getName().contains(user.getName()))) {
            event.reply("> `⚠️` **Posiadasz już otwarty ticket!**").setEphemeral(true).queue();
            return;
        }

        var subject = TextInput.create("subject", "Temat:", TextInputStyle.SHORT)
                .setPlaceholder("Napisz nazwe trybu np. Survival lub Discord")
                .setMinLength(5)
                .setMaxLength(20)
                .build();

        var body = TextInput.create("body", "Opis", TextInputStyle.PARAGRAPH)
                .setPlaceholder("⚠️ W czym możemy Ci pomóc? Opisz swój problem lub pytanie.")
                .setMinLength(5)
                .setMaxLength(300)
                .build();

        var modal = Modal.create("ticket_modal", "StrideMC - Ticket System 1.0")
                .addComponents(ActionRow.of(subject), ActionRow.of(body))
                .build();

        event.replyModal(modal).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (!event.getModalId().equals("ticket_modal")) return;

        var guild = event.getGuild();
        var user = event.getUser();
        if (guild == null) return;

        var category = guild.getCategoryById("1388106421656096768");
        if (category == null) return;

        var subject = Objects.requireNonNull(event.getValue("subject")).getAsString();
        var body = Objects.requireNonNull(event.getValue("body")).getAsString();

        category.createTextChannel("ticket〢" + user.getName())
                .addMemberPermissionOverride(user.getIdLong(), EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null)
                .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .queue(textChannel -> {
                    var formatDate = new java.text.SimpleDateFormat("HH:mm dd/MM/yyyy");
                    var date = java.util.Date.from(event.getTimeCreated().toInstant());
                    formatDate.setTimeZone(java.util.TimeZone.getTimeZone("Europe/Warsaw"));
                    var eb = new EmbedBuilder()
                            .setColor(Constants.defaultcolor)
                            .setAuthor(Constants.name, Constants.link, guild.getIconUrl())
                            .setDescription("> ### `📨`〢TICKET \n " +
                                    "> \n" +
                                    "> `👤` ***Użytkownik:*** " + user.getAsMention() + "\n" +
                                    "> `🕒` ***Data: ***" + formatDate.format(date)  + "\n" +
                                    "> `📋`*** Temat: ***```" + subject + "```\n" +
                                    "> `📨`*** Treść:***\n > ```" + body + "```\n" )
                            .setThumbnail(guild.getIconUrl())
                            .setImage(Constants.img)
                            .setTimestamp(event.getTimeCreated())
                            .setFooter(guild.getName(), guild.getIconUrl());
                    var support = guild.getRoleById("1388111975657111642"); // ID roli wsparcia, zmień na odpowiednią rolę
                    textChannel.sendMessage(user.getAsMention())// Ping użytkownika
                            .addEmbeds(eb.build())
                            .addActionRow(
                                    Button.danger("off", "❌〢Usuń Ticket"),
                                    Button.link(Constants.link, "🌐〢Strona"))
                            .queue(message -> {
                                if (support != null) {
                                    textChannel.sendMessage("`📨` " + support.getAsMention()).queue();
                                }
                            });

                    event.replyEmbeds(
                            new EmbedBuilder()
                                    .setColor(Color.GREEN)
                                    .setTitle("📨 Ticket utworzony!")
                                    .setDescription("Twój kanał ticketa został utworzony: " + textChannel.getAsMention())
                                    .build()
                    ).addActionRow(
                            Button.link("https://discord.com/channels/" + guild.getId() + "/" + textChannel.getId(), "📂 Przejdź do ticketa")
                    ).setEphemeral(true).queue();
                });
    }
}

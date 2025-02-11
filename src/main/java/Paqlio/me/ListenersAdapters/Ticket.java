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
                .setAuthor("Tickets System", Constants.link, guild.getIconUrl())
                .setDescription(">  ## 🧩〢 INFORMACJA  \n" +
                        "> \n > ✨  Hej! Masz problem?! Szukasz pomocy? Stwórz zgłoszenie! Napisz czego potrzebujesz, a postaramy się pomóc najszybciej jak to możliwe!\n" +
                        "> \n" +
                        "> **⚠️ Tworzenie zgłoszeń dla żartów spowoduje wyrzucenie / zbanowanie ⚠️**\n" +
                        "> W przypadku przypadkowego wystawienia biletu, prosimy go od razu zamknąć.\n")
                .setThumbnail(guild.getIconUrl())
                .setImage(Constants.img);

        channel.sendMessageEmbeds(eb.build())
                .addActionRow(Button.secondary("ticket", "🎫〢Stwórz Ticket"))
                .queue();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!event.getComponentId().equals("ticket")) return;

        var guild = event.getGuild();
        var user = event.getUser();
        if (guild == null) return;

        var category = guild.getCategoryById("1334998734291075132");
        if (category == null) return;

        if (category.getTextChannels().stream().anyMatch(tc -> tc.getName().contains(user.getId()))) {
            event.reply("Posiadasz już otwarty ticket!").setEphemeral(true).queue();
            return;
        }

        var subject = TextInput.create("subject", "Tryb:", TextInputStyle.SHORT)
                .setPlaceholder("Napisz nazwe trybu np. Survival")
                .setMinLength(5)
                .setMaxLength(20)
                .build();

        var body = TextInput.create("body", "Opis", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Opisz swój problem")
                .setMinLength(5)
                .setMaxLength(300)
                .build();

        var modal = Modal.create("ticket_modal", "Tickets System")
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

        var category = guild.getCategoryById("1334998734291075132");
        if (category == null) return;

        var subject = Objects.requireNonNull(event.getValue("subject")).getAsString();
        var body = Objects.requireNonNull(event.getValue("body")).getAsString();

        category.createTextChannel("ticket〢" + user.getName())
                .addMemberPermissionOverride(user.getIdLong(), EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null)
                .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .queue(textChannel -> {
                    var eb = new EmbedBuilder()
                            .setColor(Constants.defaultcolor)
                            .setAuthor(Constants.name, Constants.link, guild.getIconUrl())
                            .setDescription("> ### ⚖️〢TICKET \n " +
                                    "> \n > Opis zgłoszenia poniżej. Postaramy się odpowiedzieć jak najszybciej.\n" +
                                    "> \n" +
                                    "> ### 📝 Temat: `" + subject + "`\n" +
                                    "> 📨 Treść:\n```" + body + "```")
                            .setThumbnail(guild.getIconUrl())
                            .setImage(Constants.img)
                            .setFooter(guild.getName(), guild.getIconUrl());

                    textChannel.sendMessage(user.getAsMention()) // Ping użytkownika
                            .addEmbeds(eb.build())
                            .addActionRow(
                                    Button.danger("off", "❌〢Usuń Ticket"),
                                    Button.link(Constants.link, "🌐〢Strona"))
                            .queue();

                    event.reply("Twój ticket został utworzony: " + textChannel.getAsMention())
                            .setEphemeral(true)
                            .queue();
                });
    }
}

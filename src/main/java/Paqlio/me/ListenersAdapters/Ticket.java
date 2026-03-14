package Paqlio.me.ListenersAdapters;

import Paqlio.me.Configurations.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Objects;

public class Ticket extends ListenerAdapter {

    private static final String CATEGORY_ID = "1388106421656096768";
    private static final String SUPPORT_ROLE_ID = "1388111975657111642";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("ticket")) return;

        var guild = event.getGuild();
        if (guild == null) return;

        var eb = new EmbedBuilder()
                .setColor(Constants.defaultcolor)
                .setAuthor("📨 StrideMC - Ticket System", Constants.link, guild.getIconUrl())
                .setThumbnail(guild.getIconUrl())
                .setImage(Constants.img)
                .setDescription("""
                        ## `⚡`〢 CENTRUM POMOCY
                        
                        `✨` Witaj! Masz problem lub potrzebujesz pomocy? Stwórz zgłoszenie, a nasz zespół dołoży wszelkich starań, aby Ci pomóc!
                        
                        **`⚠️` Pamiętaj:**
                        > • Zgłoszenia dla żartów są karane.
                        > • Opisz swój problem jak najdokładniej.
                        > • Cierpliwie czekaj na odpowiedź administracji.
                        """);

        event.reply("> Pomyślnie wysłano wiadomość systemową.").setEphemeral(true).queue();
        event.getChannel().sendMessageEmbeds(eb.build())
                .addActionRow(Button.secondary("ticket", "📨〢Otwórz zgłoszenie"))
                .queue();
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!event.getComponentId().equals("ticket")) return;

        var guild = event.getGuild();
        var user = event.getUser();
        if (guild == null) return;

        var category = guild.getCategoryById(CATEGORY_ID);
        if (category == null) return;

        // Sprawdzanie czy użytkownik ma już otwarty kanał
        var exists = category.getTextChannels().stream()
                .anyMatch(tc -> tc.getName().contains(user.getName().toLowerCase()));

        if (exists) {
            event.reply("> `⚠️` **Posiadasz już otwarte zgłoszenie!**")
                    .setEphemeral(true).queue();
            return;
        }

        var subject = TextInput.create("subject", "Temat zgłoszenia:", TextInputStyle.SHORT)
                .setPlaceholder("np. Błąd na Survival, Skarga, Inne")
                .setMinLength(4)
                .setMaxLength(30)
                .build();

        var body = TextInput.create("body", "Opisz swój problem:", TextInputStyle.PARAGRAPH)
                .setPlaceholder("W czym możemy Ci pomóc? Opisz sytuację dokładnie.")
                .setMinLength(10)
                .setMaxLength(500)
                .build();

        var modal = Modal.create("ticket_modal", "Zgłoszenie - StrideMC")
                .addComponents(ActionRow.of(subject), ActionRow.of(body))
                .build();

        event.replyModal(modal).queue();
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (!event.getModalId().equals("ticket_modal")) return;

        var guild = event.getGuild();
        var user = event.getUser();
        if (guild == null) return;

        var category = guild.getCategoryById(CATEGORY_ID);
        var subject = Objects.requireNonNull(event.getValue("subject")).getAsString();
        var body = Objects.requireNonNull(event.getValue("body")).getAsString();

        event.deferReply(true).queue(hook -> {
            if (category == null) {
                hook.editOriginal("❌ Błąd: Nie znaleziono kategorii zgłoszeń.").queue();
                return;
            }

            category.createTextChannel("ticket〢" + user.getName())
                    .setTopic("Opened by: " + user.getId()) // Ważne dla archiwizacji logów!
                    .addMemberPermissionOverride(user.getIdLong(), EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null)
                    .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                    .queue(channel -> {

                        var ticketEmbed = new EmbedBuilder()
                                .setColor(Constants.defaultcolor)
                                .setAuthor(Constants.name + " - Zgłoszenie", Constants.link, guild.getIconUrl())
                                .setThumbnail(guild.getIconUrl())
                                .setImage(Constants.img)
                                .setTimestamp(event.getTimeCreated())
                                .setDescription("""
                                        ### `📨`〢 INFORMACJE O ZGŁOSZENIU
                                        
                                        `👤` **Użytkownik:** %s
                                        `🕒` **Data:** %s
                                        
                                        `📋` **Temat:**
                                        ```%s```
                                        `📨` **Treść:**
                                        ```%s```
                                        """.formatted(user.getAsMention(), event.getTimeCreated().format(DATE_FORMATTER), subject, body))
                                .setFooter(guild.getName(), guild.getIconUrl());

                        channel.sendMessage(user.getAsMention()).addEmbeds(ticketEmbed.build())
                                .addActionRow(
                                        Button.danger("off", "❌ Zamknij Ticket"),
                                        Button.link(Constants.link, "🌐 Strona WWW")
                                ).queue();

                        var supportRole = guild.getRoleById(SUPPORT_ROLE_ID);
                        if (supportRole != null) {
                            channel.sendMessage(supportRole.getAsMention() + " **Nowe zgłoszenie!**").queue();
                        }

                        hook.editOriginalEmbeds(new EmbedBuilder()
                                .setColor(Color.GREEN)
                                .setTitle("✅ Zgłoszenie utworzone!")
                                .setDescription("Twoje zgłoszenie znajduje się tutaj: " + channel.getAsMention())
                                .build()
                        ).setActionRow(Button.link("https://discord.com/channels/%s/%s".formatted(guild.getId(), channel.getId()), "📂 Przejdź do kanału")).queue();
                    });
        });
    }
}
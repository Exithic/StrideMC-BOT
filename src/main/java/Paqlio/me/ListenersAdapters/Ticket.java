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
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Objects;

public class Ticket extends ListenerAdapter {

    // Rekomendacja: Warto to przenieść do Configu Bukkita w wolnej chwili!
    private static final String CATEGORY_ID = "1388106421656096768";
    private static final String SUPPORT_ROLE_ID = "1388111975657111642";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("ticket")) return;

        var guild = event.getGuild();
        if (guild == null) return;

        // Ograniczenie - tylko admin powinien móc wysłać ten panel
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("> `❌` Brak uprawnień do postawienia panelu.").setEphemeral(true).queue();
            return;
        }

        var eb = new EmbedBuilder()
                .setColor(Constants.DEFAULT_COLOR)
                .setAuthor("📨 StrideMC - Ticket System", Constants.LINK, guild.getIconUrl())
                .setThumbnail(guild.getIconUrl())
                .setImage(Constants.IMG)
                .setDescription("""
                        ## `⚡`〢 CENTRUM POMOCY
                        
                        `✨` Witaj! Masz problem lub potrzebujesz pomocy? Stwórz zgłoszenie, a nasz zespół dołoży wszelkich starań, aby Ci pomóc!
                        
                        **`⚠️` Pamiętaj:**
                        > • Zgłoszenia dla żartów są karane.
                        > • Opisz swój problem jak najdokładniej.
                        > • Cierpliwie czekaj na odpowiedź administracji.
                        """);

        event.reply("> `✅` Pomyślnie postawiono panel zgłoszeń.").setEphemeral(true).queue();
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
        if (category == null) {
            event.reply("> `❌` Błąd techniczny: Kategoria ticketów nie istnieje.").setEphemeral(true).queue();
            return;
        }

        // POPRAWKA: Niezawodne sprawdzanie limitu ticketów po ID zapisanym w temacie kanału
        var exists = category.getTextChannels().stream()
                .anyMatch(tc -> tc.getTopic() != null && tc.getTopic().contains(user.getId()));

        if (exists) {
            event.reply("> `⚠️` **Posiadasz już otwarte zgłoszenie!** Zamknij poprzednie, aby otworzyć nowe.")
                    .setEphemeral(true).queue();
            return;
        }

        var subject = TextInput.create("subject", "Temat zgłoszenia:", TextInputStyle.SHORT)
                .setPlaceholder("np. Błąd na serwerze, Skarga, Donacja")
                .setMinLength(4)
                .setMaxLength(50)
                .build();

        var body = TextInput.create("body", "Opisz swój problem:", TextInputStyle.PARAGRAPH)
                .setPlaceholder("W czym możemy Ci pomóc? Bądź konkretny.")
                .setMinLength(10)
                .setMaxLength(1000) // Warto dać więcej niż 500, niektórzy piszą wypracowania!
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
                hook.editOriginal("> `❌` Błąd: Nie znaleziono kategorii zgłoszeń.").queue();
                return;
            }

            // POPRAWKA: Czyszczenie nicku z niedozwolonych znaków Discorda (spacje, emoji)
            var safeName = user.getName().replaceAll("[^a-zA-Z0-9_-]", "").toLowerCase();
            if (safeName.isBlank()) safeName = user.getId();

            // Tworzenie kanału z poprawnymi uprawnieniami (dodano dodawanie plików i linków)
            category.createTextChannel("ticket〢" + safeName)
                    .setTopic("Opened by: " + user.getId()) // GWARANTUJE POPRAWNE ZNALEZIENIE TICKETU
                    .addMemberPermissionOverride(user.getIdLong(),
                            EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_EXT_EMOJI),
                            null)
                    .addRolePermissionOverride(Long.parseLong(SUPPORT_ROLE_ID),
                            EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND),
                            null)
                    .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                    .queue(channel -> {

                        var ticketEmbed = new EmbedBuilder()
                                .setColor(Constants.DEFAULT_COLOR)
                                .setAuthor(Constants.NAME + " - Zgłoszenie", Constants.LINK, guild.getIconUrl())
                                .setThumbnail(guild.getIconUrl())
                                .setImage(Constants.IMG)
                                .setTimestamp(event.getTimeCreated())
                                .setDescription("""
                                        ### `📨`〢 INFORMACJE O ZGŁOSZENIU
                                        
                                        `👤` **Użytkownik:** %s
                                        `🕒` **Data:** %s
                                        
                                        `📋` **Temat:**
                                        ```%s```
                                        `💬` **Treść:**
                                        ```%s```
                                        """.formatted(user.getAsMention(), event.getTimeCreated().format(DATE_FORMATTER), subject, body))
                                .setFooter(guild.getName(), guild.getIconUrl());

                        // Pingujemy twórcę zgłoszenia i od razu rangę Support w jednej wiadomości
                        var pings = user.getAsMention() + " <@&" + SUPPORT_ROLE_ID + ">";

                        channel.sendMessage(pings).addEmbeds(ticketEmbed.build())
                                .addActionRow(
                                        Button.danger("off", "❌ Zamknij Ticket"),
                                        Button.link(Constants.LINK, "🌐 Strona WWW")
                                ).queue();

                        // Powiadomienie gracza, że się udało
                        hook.editOriginalEmbeds(new EmbedBuilder()
                                .setColor(Color.GREEN)
                                .setTitle("✅ Zgłoszenie utworzone!")
                                .setDescription("Twój ticket został pomyślnie otwarty tutaj: " + channel.getAsMention())
                                .build()
                        ).setActionRow(Button.link("https://discord.com/channels/%s/%s".formatted(guild.getId(), channel.getId()), "📂 Przejdź do zgłoszenia")).queue();
                    }, error -> hook.editOriginal("> `❌` Wystąpił błąd podczas tworzenia kanału: " + error.getMessage()).queue());
        });
    }
}
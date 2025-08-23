package Paqlio.me.ListenersAdapters;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Clear extends ListenerAdapter {

    private static final Logger LOGGER = Logger.getLogger("ClearCommand");


    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("clear")) {
            return;
        }

        event.deferReply(true).queue();

        if (event.getChannelType() != ChannelType.TEXT) {
            event.getHook().sendMessage("Komenda `clear` może być używana tylko na kanałach tekstowych.").queue();
            return;
        }

        if (!Objects.requireNonNull(event.getGuild()).getSelfMember().hasPermission(event.getGuildChannel(), Permission.MESSAGE_MANAGE)) {
            event.getHook().sendMessage("Bot nie ma uprawnień do zarządzania wiadomościami w tym kanale!").queue();
            LOGGER.warning("Bot nie ma uprawnień MESSAGE_MANAGE w kanale " + event.getGuildChannel().getName() + " do wykonania komendy clear.");
            return;
        }
        if (!Objects.requireNonNull(event.getMember()).hasPermission(Permission.MESSAGE_MANAGE)) {
            event.getHook().sendMessage("Nie masz uprawnień do zarządzania wiadomościami!").queue();
            LOGGER.warning("Użytkownik " + event.getUser().getAsTag() + " próbował użyć komendy clear bez uprawnień MESSAGE_MANAGE.");
            return;
        }

        OptionMapping amountOption = event.getOption("ilość");
        if (amountOption == null) {
            event.getHook().sendMessage("Musisz podać ilość wiadomości do usunięcia!").queue();
            return;
        }

        int amount = (int) amountOption.getAsLong();

        if (amount < 1 || amount > 100) {
            event.getHook().sendMessage("Ilość wiadomości do usunięcia musi być w zakresie od 1 do 100.").queue();
            return;
        }

        event.getChannel().getHistory().retrievePast(amount).queue(
                messages -> {
                    List<Message> deletableMessages = messages.stream()
                            .filter(m -> m.getTimeCreated().isAfter(OffsetDateTime.now().minusDays(14)))
                            .toList();

                    int deletedCount = deletableMessages.size();
                    int skippedCount = amount - deletedCount;

                    if (deletableMessages.isEmpty()) {
                        event.getHook().sendMessage("Nie znaleziono wiadomości do usunięcia (lub wszystkie są starsze niż 14 dni).").queue();
                        return;
                    }

                    event.getGuildChannel().deleteMessages(deletableMessages).queue(
                            success -> {
                                String responseMessage = "`✅` Pomyślnie usunięto **" + deletedCount + "** wiadomość(i)!";
                                if (skippedCount > 0) {
                                    responseMessage += " (Pominięto **" + skippedCount + "** wiadomości starszych niż 14 dni).";
                                }

                                event.getHook().sendMessage(responseMessage).queue(
                                        msg -> msg.delete().queueAfter(5, TimeUnit.SECONDS),
                                        error -> LOGGER.severe("Nie udało się wysłać efemerycznej odpowiedzi o usunięciu wiadomości: " + error.getMessage())
                                );

                                LOGGER.info("Użytkownik " + event.getUser().getAsTag() + " usunął " + deletedCount + " wiadomości w kanale #" + event.getGuildChannel().getName());
                            },
                            failure -> {
                                if (failure instanceof InsufficientPermissionException) {
                                    event.getHook().sendMessage("Bot nie ma uprawnień do usuwania wiadomości w tym kanale!").queue();
                                } else {
                                    event.getHook().sendMessage("Wystąpił błąd podczas usuwania wiadomości: " + failure.getMessage()).queue();
                                }
                                LOGGER.severe("Błąd podczas usuwania wiadomości przez " + event.getUser().getAsTag() + " w kanale " + event.getGuildChannel().getName() + ": " + failure.getMessage());
                            }
                    );
                },
                failure -> {
                    event.getHook().sendMessage("Wystąpił błąd podczas pobierania historii wiadomości: " + failure.getMessage()).queue();
                    LOGGER.severe("Błąd podczas pobierania historii wiadomości dla komendy clear: " + failure.getMessage());
                }
        );
    }
}
package Paqlio.me.ListenersAdapters;

import Paqlio.me.BOT;
import Paqlio.me.Configurations.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;

public class Clear extends ListenerAdapter {

    private final String logChannelId = BOT.getInstance().getConfig().getString("bot.log-channel", "1388505635251028070");

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("clear")) return;

        var guild = event.getGuild();
        var member = event.getMember();
        var messageChannel = event.getChannel(); // Do pobierania historii
        var guildChannel = event.getGuildChannel(); // Do usuwania (wymaga kanału serwerowego)

        if (guild == null || member == null || guildChannel == null) return;

        // 1. Wstępna weryfikacja uprawnień
        if (!member.hasPermission(Permission.MESSAGE_MANAGE)) {
            event.reply("> `❌` Nie masz uprawnień do zarządzania wiadomościami!").setEphemeral(true).queue();
            return;
        }

        if (!guild.getSelfMember().hasPermission(guildChannel, Permission.MESSAGE_MANAGE)) {
            event.reply("> `❌` Bot nie posiada uprawnień `MESSAGE_MANAGE` na tym kanale!").setEphemeral(true).queue();
            return;
        }

        var amountOption = event.getOption("ilość");
        if (amountOption == null) {
            event.reply("> `⚠️` Musisz podać ilość wiadomości do usunięcia!").setEphemeral(true).queue();
            return;
        }

        int amount = amountOption.getAsInt();
        if (amount < 1 || amount > 100) {
            event.reply("> `⚠️` Ilość wiadomości musi mieścić się w przedziale **1 - 100**.").setEphemeral(true).queue();
            return;
        }

        // Odpowiadamy efemerycznie (niewidoczne dla innych)
        event.deferReply(true).queue();

        messageChannel.getHistory().retrievePast(amount).queue(messages -> {
            var twoWeeksAgo = OffsetDateTime.now().minusDays(14);

            // Filtrowanie wiadomości
            var deletableMessages = messages.stream()
                    .filter(m -> m.getTimeCreated().isAfter(twoWeeksAgo))
                    .toList();

            if (deletableMessages.isEmpty()) {
                event.getHook().sendMessage("> `⚠️` Nie znaleziono wiadomości młodszych niż 14 dni.").queue();
                return;
            }

            // 2. Bezpieczne usuwanie wiadomości
            if (deletableMessages.size() == 1) {
                deletableMessages.get(0).delete().queue(
                        success -> handleSuccess(event, 1),
                        error -> handleError(event, error)
                );
            } else {
                // TUTAJ JEST POPRAWKA: Używamy guildChannel zamiast ogólnego messageChannel
                guildChannel.deleteMessages(deletableMessages).queue(
                        success -> handleSuccess(event, deletableMessages.size()),
                        error -> handleError(event, error)
                );
            }

        }, error -> event.getHook().sendMessage("> `❌` Błąd podczas pobierania historii kanału: " + error.getMessage()).queue());
    }

    // --- METODY POMOCNICZE ---

    private void handleSuccess(SlashCommandInteractionEvent event, int count) {
        event.getHook().sendMessage("`✅` Pomyślnie usunięto **" + count + "** wiadomości.").queue();
        sendLog(event, count);
    }

    private void handleError(SlashCommandInteractionEvent event, Throwable error) {
        event.getHook().sendMessage("> `❌` Błąd API podczas usuwania: " + error.getMessage()).queue();
    }

    private void sendLog(SlashCommandInteractionEvent event, int count) {
        var guild = event.getGuild();
        if (guild == null) return;

        var logChannel = guild.getTextChannelById(logChannelId);
        if (logChannel == null) return;

        var logEmbed = new EmbedBuilder()
                .setTitle("`🧹`〢 Czyszczenie czatu")
                .setColor(Constants.DEFAULT_COLOR)
                .addField("> `👤` Wykonawca:", event.getUser().getAsMention(), true)
                .addField("> `💬` Kanał:", event.getChannel().getAsMention(), true)
                .addField("> `🗑️` Ilość usuniętych:", "```" + count + " wiadomości```", false)
                .setTimestamp(OffsetDateTime.now())
                .setFooter(guild.getName(), guild.getIconUrl());

        logChannel.sendMessageEmbeds(logEmbed.build()).queue();
    }
}
package Paqlio.me.ListenersAdapters;

import Paqlio.me.BOT;
import Paqlio.me.Configurations.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

public class Clear extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("clear")) return;

        var guild = event.getGuild();
        var member = event.getMember();
        var channel = event.getGuildChannel();

        if (guild == null || member == null) return;

        // 1. Wstępna weryfikacja uprawnień
        if (!member.hasPermission(Permission.MESSAGE_MANAGE)) {
            event.reply("> `❌` Nie masz uprawnień do zarządzania wiadomościami!").setEphemeral(true).queue();
            return;
        }

        if (!guild.getSelfMember().hasPermission(channel, Permission.MESSAGE_MANAGE)) {
            event.reply("> `❌` Bot nie posiada uprawnień `MESSAGE_MANAGE` na tym kanale!").setEphemeral(true).queue();
            return;
        }

        var amountOption = event.getOption("ilość");
        if (amountOption == null) return;

        int amount = amountOption.getAsInt();
        if (amount < 1 || amount > 100) {
            event.reply("> `⚠️` Ilość wiadomości musi mieścić się w przedziale **1 - 100**.").setEphemeral(true).queue();
            return;
        }

        // Odpowiadamy efemerycznie, aby nie śmiecić na czacie
        event.deferReply(true).queue();

        event.getChannel().getHistory().retrievePast(amount).queue(messages -> {
            var twoWeeksAgo = OffsetDateTime.now().minusDays(14);

            // Filtrowanie wiadomości (Discord nie pozwala usuwać grupowo starszych niż 14 dni)
            var deletableMessages = messages.stream()
                    .filter(m -> m.getTimeCreated().isAfter(twoWeeksAgo))
                    .toList();

            if (deletableMessages.isEmpty()) {
                event.getHook().sendMessage("> `❌` Nie znaleziono wiadomości młodszych niż 14 dni.").queue();
                return;
            }

            // 2. Usuwanie wiadomości
            channel.deleteMessages(deletableMessages).queue(success -> {
                var deletedCount = deletableMessages.size();

                // Informacja dla użytkownika
                event.getHook().sendMessage("`✅` Pomyślnie usunięto **" + deletedCount + "** wiadomości.")
                        .delay(5, TimeUnit.SECONDS)
                        .flatMap(m -> m.delete()) // Automatyczne usuwanie potwierdzenia
                        .queue();

                // 3. Logowanie do kanału logów (opcjonalne, ale bardzo przydatne)
                sendLog(event, deletedCount);

            }, error -> event.getHook().sendMessage("> `❌` Błąd podczas usuwania: " + error.getMessage()).queue());
        });
    }

    private void sendLog(SlashCommandInteractionEvent event, int count) {
        // Pobieramy kanał logów z Twoich stałych (ID z ticketów)
        var logChannel = event.getGuild().getTextChannelById("1388505635251028070");
        if (logChannel == null) return;

        var logEmbed = new EmbedBuilder()
                .setTitle("`🧹`〢 Czyszczenie czatu")
                .setColor(Constants.defaultcolor)
                .addField("> Wykonawca:", event.getUser().getAsMention(), true)
                .addField("> Kanał:", event.getGuildChannel().getAsMention(), true)
                .addField("> Ilość:", "```" + count + " wiadomości```", false)
                .setTimestamp(OffsetDateTime.now())
                .setFooter(event.getGuild().getName(), event.getGuild().getIconUrl());

        logChannel.sendMessageEmbeds(logEmbed.build()).queue();
    }
}
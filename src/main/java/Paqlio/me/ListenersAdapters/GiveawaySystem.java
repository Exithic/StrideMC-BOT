package Paqlio.me.ListenersAdapters;

import Paqlio.me.BOT;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.bukkit.Bukkit;

import java.awt.Color;
import java.time.Instant;
import java.util.*;

public class GiveawaySystem extends ListenerAdapter {

    // Mapa przechowująca ID Wiadomości -> Lista ID użytkowników, którzy kliknęli przycisk
    private final Map<String, Set<String>> activeGiveaways = new HashMap<>();

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("giveaway")) return;

        // Tylko administracja może tworzyć losowania
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("> `❌` Nie masz uprawnień do tworzenia losowań!").setEphemeral(true).queue();
            return;
        }

        int minutes = event.getOption("czas").getAsInt();
        String prize = event.getOption("nagroda").getAsString();

        // Obliczamy czas zakończenia (w formacie Unix, dla odliczania na żywo)
        long endUnixTime = Instant.now().getEpochSecond() + (minutes * 60L);

        var embed = new EmbedBuilder()
                .setTitle("`🎉` NOWE LOSOWANIE")
                .setDescription("""
                        **Do wygrania:** %s
                        
                        Kliknij przycisk poniżej, aby wziąć udział!
                        Koniec: <t:%d:R> (<t:%d:f>)
                        """.formatted(prize, endUnixTime, endUnixTime))
                .setColor(new Color(155, 89, 182)) // Ładny fioletowy kolor (Amethyst)
                .setFooter("Zorganizowane przez: " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl())
                .build();

        // Przycisk do dołączania
        var joinButton = Button.success("giveaway_join", "🎉 Weź udział");

        // Wysyłamy wiadomość na kanał
        event.replyEmbeds(embed).addActionRow(joinButton).queue(hook -> {
            // Pobieramy wygenerowaną wiadomość, aby móc zapisywać kliknięcia pod jej ID
            hook.retrieveOriginal().queue(message -> {
                activeGiveaways.put(message.getId(), new HashSet<>());

                // Uruchamiamy odliczanie (bez blokowania serwera)
                long ticksDelay = minutes * 60L * 20L;
                Bukkit.getScheduler().runTaskLaterAsynchronously(BOT.getInstance(), () -> {
                    endGiveaway(message.getChannel(), message.getId(), prize);
                }, ticksDelay);
            });
        });
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!event.getComponentId().equals("giveaway_join")) return;

        String messageId = event.getMessageId();

        // Sprawdzamy czy losowanie nadal trwa
        if (!activeGiveaways.containsKey(messageId)) {
            event.reply("> `❌` To losowanie już się zakończyło!").setEphemeral(true).queue();
            return;
        }

        Set<String> participants = activeGiveaways.get(messageId);
        String userId = event.getUser().getId();

        // Zabezpieczenie przed wielokrotnym dołączeniem
        if (participants.contains(userId)) {
            event.reply("> `❔` Już bierzesz udział w tym losowaniu!").setEphemeral(true).queue();
        } else {
            participants.add(userId);
            event.reply("> `✅` Pomyślnie dołączyłeś do losowania! Powodzenia!").setEphemeral(true).queue();
        }
    }

    private void endGiveaway(MessageChannel channel, String messageId, String prize) {
        // Pobieramy i od razu usuwamy losowanie z aktywnych
        Set<String> participants = activeGiveaways.remove(messageId);

        if (participants == null || participants.isEmpty()) {
            channel.sendMessage("`😢` Nikt nie wziął udziału w losowaniu o **" + prize + "**...").queue();
            return;
        }

        // Losujemy zwycięzcę
        List<String> list = new ArrayList<>(participants);
        String winnerId = list.get(new Random().nextInt(list.size()));

        // Wysyłamy ogłoszenie z pingiem zwycięzcy
        channel.sendMessage("`🎊` Gratulacje <@" + winnerId + ">! Wygrałeś(aś) **" + prize + "**!\nSkontaktuj się z administracją po odbiór nagrody.").queue();
    }
}
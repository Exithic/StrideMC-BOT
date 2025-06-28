package Paqlio.me.Addons;

import Paqlio.me.BOT;
import Paqlio.me.Configurations.Constants;
import me.clip.placeholderapi.PlaceholderAPI;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bukkit.entity.Player;

import java.awt.*;

/**
 * @author RocketLunchi
 * @since 28.06.2025- 17:03
 **/
public class DiscordChatBridge {
    private static final int MAX_MESSAGE_LENGTH = 256; // Limit dla autora embeda w Discordzie (lub inna wartość)

        public static void sendMessageToDiscord(Player player, String message) {
            // Sprawdzamy, czy JDA jest zainicjowane
            if (BOT.getJda() == null) {
                System.out.println("JDA nie jest zainicjowane. Nie można wysłać wiadomości na Discord.");
                return;
            }

            // Sprawdzamy, czy gracz wpisuje coś w innym pluginie
            if (player.isConversing()) {
                return; // Nie wysyłamy wiadomości, jeśli gracz jest w trybie konwersacji z innym pluginem
            }

            // Pobieramy kanał tekstowy Discorda
            TextChannel channel = BOT.getJda().getTextChannelById(Constants.MinecraftChannel);
            if (channel == null) {
                System.out.println("Nie znaleziono kanału Discord o ID: " + Constants.MinecraftChannel);
                return;
            }

            String group = PlaceholderAPI.setPlaceholders(player, "%luckperms_primary_group_name%");
            if (group == null || group.isEmpty()) {
                group = "Gracz"; // Domyślna wartość, jeśli placeholder nie zwróci niczego
            }

            // Adres URL avatara
            String avatarUrl = "http://cravatar.eu/avatar/" + player.getName() + "/128.png";

            // Tworzymy tekst dla autora embeda, upewniając się, że nie przekracza limitu
            String authorText = group + " " + player.getName() + ": " + message;
            if (authorText.length() > MAX_MESSAGE_LENGTH) {
                authorText = authorText.substring(0, MAX_MESSAGE_LENGTH - 3) + "..."; // Skróć i dodaj '...'
            }

            EmbedBuilder eb = new EmbedBuilder()
                    .setColor(new Color(81, 81, 81))
                    .setAuthor(authorText, null, avatarUrl);

            // Wysyłamy embeda na Discord
            channel.sendMessageEmbeds(eb.build()).queue(
                    success -> {
                        // Opcjonalnie: loguj sukces
                    },
                    failure -> {
                        System.out.println("Nie udało się wysłać wiadomości na Discord: " + failure.getMessage());
                    }
            );
        }
}

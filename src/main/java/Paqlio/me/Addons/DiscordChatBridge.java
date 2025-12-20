package Paqlio.me.Addons;

import Paqlio.me.BOT;
import Paqlio.me.Configurations.Constants;
import me.clip.placeholderapi.PlaceholderAPI;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bukkit.entity.Player;

import java.awt.*;
import java.time.Instant;

/**
 * Mostek komunikacji między Minecraft a Discord
 * @author RocketLunchi, Paqlio
 */
public class DiscordChatBridge {

    private static final int MAX_LENGTH = 256;
    private static final String AVATAR_URL = "https://cravatar.eu/avatar/%s/128.png";

    // Kolory
    private static final Color PLAYER_COLOR = new Color(128, 128, 128);  // Szary dla wiadomości graczy
    private static final Color SERVER_ON_COLOR = new Color(0, 255, 0);   // Zielony dla startu
    private static final Color SERVER_OFF_COLOR = new Color(255, 0, 0);  // Czerwony dla wyłączenia

    public static void sendMessageToDiscord(Player player, String message) {
        if (player.isConversing()) return;

        var channel = getChannel();
        if (channel == null) return;

        var group = PlaceholderAPI.setPlaceholders(player, "%luckperms_primary_group_name%");
        if (group == null || group.isEmpty()) group = "Gracz";

        var embed = new EmbedBuilder()
                .setColor(PLAYER_COLOR)
                .setAuthor(player.getName(), null, String.format(AVATAR_URL, player.getName()))
                .setDescription("**" + group + "** » " + message)
                .setTimestamp(Instant.now())
                .build();

        channel.sendMessageEmbeds(embed).queue(null, e ->
                System.err.println("Discord error: " + e.getMessage())
        );
    }

    public static void sendServerStartMessage() {
        sendServerMessage("🟢 Serwer został uruchomiony!", SERVER_ON_COLOR);
    }

    public static void sendServerStopMessage() {
        sendServerMessage("🔴 Serwer został wyłączony!", SERVER_OFF_COLOR);
    }

    public static void sendServerMessage(String message) {
        sendServerMessage(message, SERVER_ON_COLOR);
    }

    private static void sendServerMessage(String message, Color color) {
        var channel = getChannel();
        if (channel == null) return;

        var embed = new EmbedBuilder()
                .setColor(color)
                .setDescription(message)
                .setTimestamp(Instant.now())
                .build();

        channel.sendMessageEmbeds(embed).queue(null, e ->
                System.err.println("Discord error: " + e.getMessage())
        );
    }

    private static TextChannel getChannel() {
        var jda = BOT.getJda();
        if (jda == null) {
            System.err.println("JDA not initialized");
            return null;
        }

        var channel = jda.getTextChannelById(Constants.MinecraftChannel);
        if (channel == null) {
            System.err.println("Channel not found: " + Constants.MinecraftChannel);
        }
        return channel;
    }
}
package Paqlio.me.Addons;

import Paqlio.me.BOT;
import Paqlio.me.Configurations.Constants;
import me.clip.placeholderapi.PlaceholderAPI;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.time.Instant;

/**
 * Mostek komunikacji między Minecraft a Discord
 * @author Paqlio
 */
public class DiscordChatBridge {

    // Nowocześniejszy render głów gracza
    private static final String AVATAR_URL = "https://mc-heads.net/avatar/%s/128.png";

    // Kolory
    private static final Color SERVER_ON_COLOR = new Color(46, 204, 113);   // Soczysty zielony
    private static final Color SERVER_OFF_COLOR = new Color(231, 76, 60);   // Soczysty czerwony
    private static final Color CHAT_COLOR = new Color(52, 152, 219);        // Niebieski dla czatu

    /**
     * Wysyła wiadomość gracza na Discord w formie estetycznego paska
     */
    public static void sendMessageToDiscord(Player player, String message) {
        var channel = getChannel();
        if (channel == null || message.isBlank()) return;

        // Ograniczenie długości wiadomości (zabezpieczenie przed spamerami)
        var safeMessage = message.length() > 250 ? message.substring(0, 247) + "..." : message;

        // Pobieranie rangi z LuckPerms
        var group = PlaceholderAPI.setPlaceholders(player, "%luckperms_primary_group_name%");
        if (group.isEmpty() || group.contains("%")) group = "Gracz";

        var embed = new EmbedBuilder()
                .setColor(CHAT_COLOR)
                .setAuthor(player.getName(), null, String.format(AVATAR_URL, player.getName()))
                .setDescription("`" + group.toUpperCase() + "` **»** " + safeMessage)
                .build();

        channel.sendMessageEmbeds(embed).queue();
    }

    /**
     * Wiadomość o starcie serwera
     */
    public static void sendServerStartMessage() {
        sendServerStatus("🚀 SERWER ZOSTAŁ URUCHOMIONY", "Serwer StrideMC jest już dostępny dla graczy!", SERVER_ON_COLOR);
    }

    /**
     * Wiadomość o zatrzymaniu serwera
     */
    public static void sendServerStopMessage() {
        sendServerStatus("🛑 SERWER ZOSTAŁ WYŁĄCZONY", "Trwają prace konserwacyjne lub serwer został zatrzymany.", SERVER_OFF_COLOR);
    }

    /**
     * Pomocnicza metoda do statusów serwera
     */
    private static void sendServerStatus(String title, String description, Color color) {
        var channel = getChannel();
        if (channel == null) return;

        var embed = new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(color)
                .setTimestamp(Instant.now())
                .build();

        channel.sendMessageEmbeds(embed).queue();
    }

    /**
     * Pobiera kanał z JDA w bezpieczny sposób
     */
    @Nullable
    private static TextChannel getChannel() {
        var jda = BOT.getJda();
        if (jda == null) return null;

        // Używamy Constants.MinecraftChannel (upewnij się, że to poprawne ID w formie String)
        return jda.getTextChannelById(Constants.MinecraftChannel);
    }
}
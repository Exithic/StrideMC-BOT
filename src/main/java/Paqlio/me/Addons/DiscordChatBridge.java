package Paqlio.me.Addons;

import Paqlio.me.BOT;
import Paqlio.me.Configurations.Constants;
import Paqlio.me.Configurations.EmbedHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import org.bukkit.entity.Player;

public class DiscordChatBridge {

    public static void sendMessageToDiscord(Player player, String message) {
        var channel = getChannel();
        if (channel == null || message.isBlank()) return;

        var safeMessage = message.length() > 250 ? message.substring(0, 247) + "..." : message;

        var embed = EmbedHelper.info("")
                .setAuthor(player.getName(), null, "https://mc-heads.net/avatar/" + player.getName() + "/128.png")
                .setDescription("`" + player.getName() + "` **»** " + safeMessage);

        channel.sendMessageEmbeds(embed.build()).queue();
    }

    public static void sendServerStartMessage() {
        var channel = getChannel();
        if (channel == null) return;

        var embed = EmbedHelper.success("🚀 SERWER URUCHOMIONY")
                .setDescription("Serwer **" + Constants.NAME + "** jest już otwarty dla graczy!");

        channel.sendMessageEmbeds(embed.build()).queue(
                s -> BOT.getInstance().getLogger().info("START wysłany"),
                e -> BOT.getInstance().getLogger().warning("Błąd START: " + e.getMessage()));
    }

    public static void sendServerStopMessage() {
        var channel = getChannel();
        if (channel == null) return;

        var embed = EmbedHelper.error("🛑 SERWER WYŁĄCZONY")
                .setDescription("Serwer **" + Constants.NAME + "** został zatrzymany. Trwają prace techniczne.");

        try {
            channel.sendMessageEmbeds(embed.build()).complete();
        } catch (Exception ignored) {}
    }

    private static net.dv8tion.jda.api.entities.channel.concrete.TextChannel getChannel() {
        var jda = BOT.getJda();
        if (jda == null) return null;
        return jda.getTextChannelById(Constants.MINECRAFT_CHANNEL);
    }
}

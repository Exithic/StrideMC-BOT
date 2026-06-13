package Paqlio.me.Addons;

import Paqlio.me.BOT;
import Paqlio.me.Configurations.EmbedHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BridgeListener implements PluginMessageListener {

    public static final Map<UUID, Long> mutedPlayers = new ConcurrentHashMap<>();

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!"stridemc:bridge".equals(channel)) return;

        try {
            var in = com.google.common.io.ByteStreams.newDataInput(message);
            var source = in.readUTF();
            var action = in.readUTF();
            var data = in.readUTF();

            switch (action.toUpperCase()) {
                case "BAN" -> {
                    var pd = data.split("\\|");
                    sendToChannel("ban", EmbedHelper.error("⛓ BAN")
                            .setThumbnail("https://mc-heads.net/avatar/" + (pd.length > 0 ? pd[0] : "") + "/128.png")
                            .setDescription("""
                                    `Gracz` **%s**
                                    `Administrator` **%s**
                                    `Powód` **%s**
                                    `Wygasa` **%s**
                                    """.formatted(pd.length > 0 ? pd[0] : "?", pd.length > 1 ? pd[1] : "?",
                                    pd.length > 2 ? pd[2] : "?", pd.length > 3 ? pd[3] : "?")));
                }
                case "UNBAN" -> {
                    sendToChannel("ban", EmbedHelper.success("✅ UNBAN").setDescription("`Gracz` **" + data + "**"));
                }
                case "REPORT" -> {
                    var parts = data.split("\\|", 3);
                    sendToChannel("report", EmbedHelper.warn("🚨 NOWE ZGŁOSZENIE")
                            .setThumbnail("https://mc-heads.net/avatar/" + (parts.length > 0 ? parts[0] : "") + "/128.png")
                            .setDescription("""
                                    `Gracz` **%s**
                                    `Zgłosił` **%s**
                                    `Powód` **%s**
                                    """.formatted(
                                    parts.length > 0 ? parts[0] : "?",
                                    parts.length > 1 ? parts[1] : "?",
                                    parts.length > 2 ? parts[2] : "?"
                            )));
                }
                case "MUTE" -> {
                    var p = data.split("\\|", 2);
                    if (p.length == 2) mutedPlayers.put(UUID.fromString(p[0]), Long.parseLong(p[1]));
                }
                case "UNMUTE" -> mutedPlayers.remove(UUID.fromString(data));
            }
        } catch (Exception e) {
            BOT.getInstance().getLogger().warning("[Bridge] " + e.getMessage());
        }
    }

    private void sendToChannel(String key, EmbedBuilder embed) {
        var cfg = BOT.getInstance().getConfig();
        var channelId = cfg.getString("discord.channels." + key);
        if (channelId == null || channelId.isEmpty()) return;

        var jda = BOT.getJda();
        if (jda == null) return;

        var channel = jda.getTextChannelById(channelId);
        if (channel != null) channel.sendMessageEmbeds(embed.build()).queue();
    }
}

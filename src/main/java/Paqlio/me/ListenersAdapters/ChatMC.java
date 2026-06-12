package Paqlio.me.ListenersAdapters;

import Paqlio.me.Addons.BridgeListener;
import Paqlio.me.Addons.SyncManager;
import Paqlio.me.Configurations.Constants;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class ChatMC extends ListenerAdapter {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final TextColor DISCORD_BLURPLE = TextColor.color(88, 101, 242);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final SyncManager syncManager;

    public ChatMC(SyncManager syncManager) {
        this.syncManager = syncManager;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || event.isWebhookMessage()) return;
        if (!event.getChannel().getId().equals(Constants.MINECRAFT_CHANNEL)) return;

        var discordId = event.getAuthor().getId();
        if (syncManager.isDiscordLinked(discordId)) {
            var uuid = syncManager.getPlayerUUID(discordId);
            if (uuid != null && BridgeListener.mutedPlayers.containsKey(uuid)) return;
        }

        var content = event.getMessage().getContentDisplay();
        var hasAttachments = !event.getMessage().getAttachments().isEmpty();
        if (content.isBlank() && !hasAttachments) return;
        if (hasAttachments) content += (content.isBlank() ? "" : " ") + "📎";

        var authorName = event.getMember() != null ? event.getMember().getEffectiveName() : event.getAuthor().getName();
        var time = event.getMessage().getTimeCreated().format(TIME_FMT);

        var tag = MM.deserialize("<dark_gray>[</dark_gray><color:#5865F2>Discord</color><dark_gray>]</dark_gray>")
                .hoverEvent(HoverEvent.showText(MM.deserialize(
                        "<color:#5865F2><bold>Discord</bold></color>\n<gray>" + time + "</gray>")))
                .clickEvent(ClickEvent.openUrl(Constants.LINK));

        var name1 = MM.deserialize("<gray>" + authorName + "</gray>")
                .hoverEvent(HoverEvent.showText(MM.deserialize("<white>" + event.getAuthor().getName() + "</white>")));

        var msg = hasAttachments
                ? Component.text(content.replace("📎", "")).color(NamedTextColor.WHITE)
                        .append(MM.deserialize(" <aqua><i>📎</i></aqua>"))
                : Component.text(content).color(NamedTextColor.WHITE);

        var finalMsg = Component.empty()
                .append(tag).append(Component.space())
                .append(name1)
                .append(MM.deserialize("<dark_gray>:</dark_gray> "))
                .append(msg);

        Bukkit.getServer().sendMessage(finalMsg);
    }
}

package Paqlio.me.ListenersAdapters;

import Paqlio.me.Configurations.Constants;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ChatMC extends ListenerAdapter {
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (event.getChannel().getId().equals(Constants.MinecraftChannel)) {
            var message = event.getMessage();
            var author = event.getAuthor();
            Bukkit.getOnlinePlayers().forEach(player -> {
                if (!player.isConversing()) {
                    player.sendMessage("§7(§bDiscord§7) §f" + author.getName() + "§7: §f" + message.getContentDisplay());
                }
            });
        }
    }
}

package Paqlio.me.ListenersAdapters;

import Paqlio.me.Configurations.Constants;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;

/**
 * @author Paqlio
 * @since 03.02.2025- 20:19
 **/
public class ChatMC extends ListenerAdapter {
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (event.getChannel().getId().equals(Constants.MinecraftChannel)) {
            var message = event.getMessage();
            var author = event.getAuthor();
            //minecraft chat
            Bukkit.broadcastMessage("§7[§bDiscord§7] §f" + author.getName() + "§7: §f" + message.getContentDisplay());
        }
    }
}

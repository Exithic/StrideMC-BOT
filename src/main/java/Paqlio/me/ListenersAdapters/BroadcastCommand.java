package Paqlio.me.ListenersAdapters;

import Paqlio.me.BOT;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;

public class BroadcastCommand extends ListenerAdapter {

    private final MiniMessage mm = MiniMessage.miniMessage();

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("broadcast")) return;
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("❌ Brak uprawnień!").setEphemeral(true).queue();
            return;
        }

        var msg = event.getOption("wiadomość").getAsString();

        Bukkit.getScheduler().runTask(BOT.getInstance(), () -> {
            Bukkit.broadcast(mm.deserialize(
                    "<dark_gray>[<red>🔴</red>]</dark_gray> <white>" + msg));
        });

        event.reply("✅ Wysłano: " + msg).setEphemeral(true).queue();
    }
}

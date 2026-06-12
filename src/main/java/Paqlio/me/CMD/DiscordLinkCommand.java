package Paqlio.me.CMD;

import Paqlio.me.Addons.SyncManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DiscordLinkCommand implements CommandExecutor {

    private final SyncManager syncManager;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public DiscordLinkCommand(SyncManager syncManager) {
        this.syncManager = syncManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Ta komenda jest dostepna tylko dla graczy w grze!");
            return true;
        }

        // Zabezpieczenie przed multikontami i ponownym łączeniem
        if (syncManager.isLinked(player.getUniqueId())) {
            player.sendMessage(mm.deserialize("<red>Twoje konto Minecraft jest już połączone z Discordem!"));
            return true;
        }

        String code = syncManager.generateCode(player);

        player.sendMessage(mm.deserialize("""
            <gradient:#5865F2:#ffffff><bold>DISCORD SYNC</bold></gradient>
            <gray>Twój unikalny kod weryfikacyjny to: <gold><bold>%s</bold>
            <gray>Wpisz go na Discordzie używając komendy <white>/link</white>.
            <dark_gray>Kod jest ważny do momentu użycia lub restartu serwera.
            """.formatted(code)));

        return true;
    }
}
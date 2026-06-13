package Paqlio.me.ListenersAdapters;

import Paqlio.me.BOT;
import Paqlio.me.Configurations.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.bukkit.Bukkit;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ReactionRoleCommand extends ListenerAdapter {

    private final Map<String, String> pendingRoles = new HashMap<>();

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("reactionrole")) return;
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("❌ Brak uprawnień!").setEphemeral(true).queue();
            return;
        }

        var channel = event.getOption("kanał").getAsChannel().asTextChannel();
        var role = event.getOption("rola").getAsRole();
        var emoji = event.getOption("emoji").getAsString();
        var label = event.getOption("nazwa").getAsString();

        var embed = new EmbedBuilder()
                .setColor(Constants.DEFAULT_COLOR)
                .setTitle("🎭 " + label)
                .setDescription("Kliknij przycisk, aby otrzymać rolę " + role.getAsMention())
                .build();

        var buttonId = "rr_" + System.currentTimeMillis();
        var button = Button.secondary(buttonId, label).withEmoji(Emoji.fromUnicode(emoji));

        pendingRoles.put(buttonId, role.getId());
        Bukkit.getScheduler().runTaskLaterAsynchronously(BOT.getInstance(), () -> pendingRoles.remove(buttonId), TimeUnit.HOURS.toSeconds(24) * 20);

        channel.sendMessageEmbeds(embed).setActionRow(button).queue(
                m -> event.reply("✅ Utworzono panel roli!").setEphemeral(true).queue(),
                e -> event.reply("❌ Błąd: " + e.getMessage()).setEphemeral(true).queue()
        );
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        var id = event.getComponentId();
        if (!id.startsWith("rr_")) return;
        var roleId = pendingRoles.get(id);
        if (roleId == null) { event.reply("❌ Ten panel wygasł.").setEphemeral(true).queue(); return; }

        var role = event.getGuild().getRoleById(roleId);
        if (role == null) { event.reply("❌ Rola nie istnieje.").setEphemeral(true).queue(); return; }

        var member = event.getMember();
        if (member.getRoles().contains(role)) {
            event.getGuild().removeRoleFromMember(member, role).queue();
            event.reply("✅ Zdjęto rolę " + role.getAsMention()).setEphemeral(true).queue();
        } else {
            event.getGuild().addRoleToMember(member, role).queue();
            event.reply("✅ Nadano rolę " + role.getAsMention()).setEphemeral(true).queue();
        }
    }
}

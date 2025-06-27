package Paqlio.me.ListenersAdapters;

import Paqlio.me.Configurations.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ButtonsInteractions extends ListenerAdapter {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        switch (event.getComponentId()) {
            case "off" -> event.replyEmbeds(createEmbed(
                            "Potwierdzenie usunięcia",
                            "Czy na pewno chcesz usunąć ticket?",
                            "Jeśli tak, kliknij w przycisk poniżej"
                    ).build())
                    .addActionRow(Button.success("off1", "✅〢Potwierdzam"))
                    .queue();
            case "off1" -> {
                event.reply("Ticket zostanie usunięty za **10** sekund!").queue(response -> {
                    response.retrieveOriginal().queue(message -> startCountdown(message, (TextChannel) event.getChannel()));
                });
            }
            case "accept" -> {
                var guild = event.getGuild();
                var member = event.getMember();
                var rang = guild != null ? guild.getRoleById("1336036742213537842") : null;
                if (guild == null || member == null || rang == null) return;
                event.reply("Zaakceptowałeś regulamin!").setEphemeral(true).queue();
                guild.addRoleToMember(member, rang).queue();
            }
        }
    }

    private void startCountdown(Message message, TextChannel channel) {
        scheduler.scheduleAtFixedRate(new Runnable() {
            int secondsLeft = 10;

            @Override
            public void run() {
                if (secondsLeft > 0) {
                    message.editMessage("Ticket zostanie usunięty za `⌚`**" + secondsLeft + "** sekund!").queue(null, error -> {});
                    secondsLeft--;
                } else {
                    message.delete().queue(null, error -> {}); // Ignoruje błąd, jeśli wiadomość już nie istnieje
                    if (channel != null) channel.delete().queue(null, error -> {});
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private EmbedBuilder createEmbed(String title, String... fields) {
        var embed = new EmbedBuilder().setColor(Constants.defaultcolor).setAuthor(Constants.name, Constants.link);
        if (title != null) embed.setTitle(title);
        for (var field : fields) embed.addField("", field, false);
        embed.setImage(Constants.img);
        return embed;
    }
}

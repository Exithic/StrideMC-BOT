package Paqlio.me.ListenersAdapters;

import Paqlio.me.Configurations.EmbedHelper;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class HelpCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("help")) return;

        var embed = EmbedHelper.info("📋 Komendy")
                .setDescription("""
                        **🎮 Ogólne**
                        `help` — Lista komend
                        `online` — Kto jest online
                        `mc` — Status serwera
                        `user` — Info o użytkowniku

                        **🔗 Weryfikacja**
                        `link` — Połącz konto MC z Discordem

                        **⚙️ Administracja**
                        `broadcast` — Ogłoszenie na czat MC
                        `clear` — Usuń wiadomości
                        `giveaway` — Losowanie
                        `reactionrole` — Panel ról
                        `regulamin` — Panel regulaminu
                        `ticket` — System ticketów
                        """);

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }
}

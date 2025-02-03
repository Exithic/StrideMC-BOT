package Paqlio.me.ListenersAdapters;

import Paqlio.me.Configurations.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;

/**
 * @author Paqlio
 * @since 03.02.2025- 18:53
 **/
public class Regulamin  extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("regulamin")) return;
        var guild = event.getGuild();
        var channel = event.getChannel();
        if (guild == null) return;
        var eb = new EmbedBuilder()
                .setColor(Constants.defaultcolor)
                .setAuthor("👋 Regulamin Discord", Constants.link, guild.getIconUrl())
                .setDescription("> ### Poniższa lista obejmuje kanały tekstowe oraz głosowe! \n" +
                        "> **1.** Zakaz reklamowania,\n" +
                        "> **2.** Zakaz spamu, obojętnie w jakiej formie.\n" +
                        "> **3.** Zakaz oznaczania administracji bez wyraźnego powodu, czyli powodu, który jest nic nie wnoszący, możliwy do rozwiązania w strefie pomocy lub może być wyjaśniony pomiędzy graczem a administratorem w rozmowie prywatnej.\n" +
                        "> **4.** Zakaz rażącego utrudniania rozmowy.\n" +
                        "> **5.** Zakaz tworzenie multi-kont.\n" +
                        "> **6.** Zakaz podszywania się pod osoby publiczne.\n" +
                        "> **7.** Zakaz obrażania innych użytkowników.\n" +
                        "> **8.** Zakaz publikacji treści NSFW.\n" +
                        "> **9.** Zakaz pisania na tematy niezwiązane ze strefą.\n" +
                        "> **10.** Zakaz korzystania z bindów, soundboardów na kanałach głosowych.\n" +
                        "> **11.** Zakaz nagrywania innych osób bez ich zgody.\n" +
                        "> **12.** Zakaz udostępniania zdjęć innych osób.\n" +
                        "> **13.** Zakaz rozpowszechniania prywatnych danych.\n" +
                        "> **14.** Próba obejścia regulaminu będzie skutkowała trwałym wykluczeniem z serwera.\n" +
                        "> **15.** Nieznajomość regulaminu nie zwalnia z odpowiedzialności.\n")
                .setTimestamp(event.getTimeCreated())
                .setThumbnail(guild.getIconUrl());
        channel.sendMessageEmbeds(eb.build())
                .addActionRow(Button.success("accept", "✅〢Akceptuje"))
                .queue();
    }
}

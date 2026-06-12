package Paqlio.me.ListenersAdapters;

import Paqlio.me.Configurations.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;

/**
 * @author Paqlio
 * Zoptymalizowane i rozbudowane o weryfikację
 **/
public class Regulamin extends ListenerAdapter {

    // Wpisz tutaj ID roli, którą gracz dostanie po kliknięciu "Akceptuję"
    private static final String VERIFIED_ROLE_ID = "1380000000000000000";

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("regulamin")) return;

        var guild = event.getGuild();
        var member = event.getMember();
        var channel = event.getChannel();

        if (guild == null || member == null) return;

        // 1. Zabezpieczenie: Tylko admin może wysłać regulamin
        if (!member.hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("> `❌` Tylko administratorzy mogą używać tej komendy!").setEphemeral(true).queue();
            return;
        }

        // 2. Budowanie nowoczesnego Embedu (Użycie Text Blocków)
        var eb = new EmbedBuilder()
                .setColor(Constants.DEFAULT_COLOR)
                .setAuthor("👋 Regulamin Społeczności", Constants.LINK, guild.getIconUrl())
                .setDescription("""
                        > ### `📜` Poniższa lista obejmuje kanały tekstowe oraz głosowe!
                        
                        > **1.** Zakaz reklamowania.
                        > **2.** Zakaz spamu, obojętnie w jakiej formie.
                        > **3.** Zakaz oznaczania administracji bez wyraźnego powodu.
                        > **4.** Zakaz rażącego utrudniania rozmowy.
                        > **5.** Zakaz tworzenie multi-kont na Discordzie serwera.
                        > **6.** Zakaz podszywania się pod osoby publiczne lub administrację.
                        > **7.** Zakaz obrażania i wyzywania innych użytkowników.
                        > **8.** Zakaz publikacji treści NSFW, drastycznych lub niezgodnych z prawem.
                        > **9.** Zakaz pisania na tematy niezwiązane z danym kanałem (Off-topic).
                        > **10.** Zakaz korzystania z soundboardów na kanałach publicznych.
                        > **11.** Zakaz nagrywania innych osób bez ich wyraźnej zgody.
                        > **12.** Zakaz udostępniania zdjęć innych osób (Doxing).
                        > **13.** Zakaz rozpowszechniania prywatnych danych.
                        > **14.** Próba obejścia regulaminu będzie skutkowała trwałym banem.
                        > **15.** Nieznajomość regulaminu nie zwalnia z odpowiedzialności.
                        
                        `💡` **Kliknij zielony przycisk poniżej, aby zaakceptować regulamin i uzyskać dostęp do serwera!**
                        """)
                .setTimestamp(OffsetDateTime.now())
                .setThumbnail(guild.getIconUrl());

        // 3. WYSYŁKA REGULAMINU NA KANAŁ
        channel.sendMessageEmbeds(eb.build())
                .addActionRow(Button.success("accept_rules", "✅〢 Akceptuję Regulamin"))
                .queue();

        // 4. POTWIERDZENIE DLA ADMINA (Ukryte, zapobiega błędowi "Aplikacja nie odpowiada")
        event.reply("`✅` Pomyślnie wysłano wiadomość z regulaminem!").setEphemeral(true).queue();
    }


    // ==========================================
    // OBSŁUGA KLIKNIĘCIA PRZYCISKU WERYFIKACJI
    // ==========================================
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!event.getComponentId().equals("accept_rules")) return;

        var guild = event.getGuild();
        var member = event.getMember();
        if (guild == null || member == null) return;

        var role = guild.getRoleById(VERIFIED_ROLE_ID);

        if (role == null) {
            event.reply("> `❌` Błąd konfiguracji! Rola zweryfikowanego nie została znaleziona. Zgłoś to administracji.")
                    .setEphemeral(true).queue();
            return;
        }

        // Sprawdzenie, czy gracz już ma tę rolę
        if (member.getRoles().contains(role)) {
            event.reply("> `⚠️` Już wcześniej zaakceptowałeś regulamin!").setEphemeral(true).queue();
            return;
        }

        // Nadanie roli graczowi
        guild.addRoleToMember(member, role).queue(
                success -> {
                    // Wysłanie pięknego, ukrytego powiadomienia do gracza
                    event.reply("> `🎉` **Dziękujemy!** Pomyślnie zaakceptowano regulamin. Uzyskałeś dostęp do serwera!")
                            .setEphemeral(true).queue();
                },
                error -> {
                    // Gdyby bot miał rangę niżej niż rola, którą ma nadać
                    event.reply("> `❌` Wystąpił błąd podczas nadawania roli. Bot może nie mieć odpowiednich uprawnień.")
                            .setEphemeral(true).queue();
                }
        );
    }
}
package Paqlio.me.ListenersAdapters;

import Paqlio.me.Configurations.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

public class MemberJoin extends ListenerAdapter {

    // ID kanałów (Najlepiej przenieść je do klasy Constants w przyszłości)
    private static final String VOICE_COUNTER_ID = "1387897312092880966";
    private static final String WELCOME_CHANNEL_ID = "1383916471645638818";
    private static final String RULES_CHANNEL_ID = "1384254060575854602";

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        var member = event.getMember();
        var guild = event.getGuild();
        var memberCount = guild.getMemberCount();

        // 1. Aktualizacja licznika osób na kanale głosowym
        var voiceChannel = guild.getVoiceChannelById(VOICE_COUNTER_ID);
        if (voiceChannel != null) {
            voiceChannel.getManager()
                    .setName("📊 ┊ Osób: " + memberCount)
                    .queue();
        }

        // 2. Wysłanie powitania na kanał tekstowy
        var welcomeChannel = guild.getTextChannelById(WELCOME_CHANNEL_ID);
        if (welcomeChannel != null) {

            var embed = new EmbedBuilder()
                    .setAuthor("Nowy użytkownik na serwerze!", null, guild.getIconUrl())
                    .setTitle("Witaj w społeczności " + guild.getName() + "!")
                    .setColor(Constants.defaultcolor)
                    .setThumbnail(member.getEffectiveAvatarUrl())
                    .setDescription("""
                        ### `👋`〢 Witaj, %s!
                        Bardzo cieszymy się, że do nas dołączyłeś. Pamiętaj, aby zapoznać się z zasadami panującymi na serwerze.
                        
                        `📌` **Zacznij od:**
                        > Przeczytaj regulamin: <#%s>
                        > Sprawdź naszą stronę: [Kliknij tutaj](%s)
                        
                        `📈` **Statystyki:**
                        > Jesteś naszym **%d** użytkownikiem!
                        """.formatted(member.getAsMention(), RULES_CHANNEL_ID, Constants.link, memberCount))
                    .setFooter("Data dołączenia • " + guild.getName(), guild.getIconUrl())
                    .setTimestamp(Instant.now()) // Pokazuje dokładny czas wejścia
                    .build();

            welcomeChannel.sendMessageEmbeds(embed)
                    .addActionRow(
                            Button.link(Constants.link, "🌐 Strona WWW"),
                            Button.secondary("rules_info", "🛡️ Zasady serwera").asDisabled() // Przycisk ozdobny/info
                    )
                    .queue();
        }
    }
}
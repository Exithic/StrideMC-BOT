package Paqlio.me.ListenersAdapters;

import Paqlio.me.BOT;
import Paqlio.me.Configurations.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

public class MemberJoin extends ListenerAdapter {

    // W przyszłości warto przenieść to do config.yml!
    private static final String VOICE_COUNTER_ID = "1387897312092880966";
    private static final String WELCOME_CHANNEL_ID = "1383916471645638818";
    private static final String RULES_CHANNEL_ID = "1384254060575854602";
    private static final String AUTO_ROLE_ID = "1380000000000000000"; // ZMIEŃ NA ID ROLI STARTOWEJ (np. Gracz)

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        var member = event.getMember();
        var guild = event.getGuild();
        var memberCount = guild.getMemberCount();

        // ==========================================
        // 1. NADAWANIE ROLI STARTOWEJ (Auto-Role)
        // ==========================================
        var autoRole = guild.getRoleById(AUTO_ROLE_ID);
        if (autoRole != null) {
            guild.addRoleToMember(member, autoRole).queue(
                    success -> {},
                    error -> BOT.getInstance().getLogger().warning("Nie mogę nadać roli nowemu graczowi. Sprawdź uprawnienia bota!")
            );
        }

        // ==========================================
        // 2. AKTUALIZACJA LICZNIKA (Z zabezpieczeniem API)
        // ==========================================
        var voiceChannel = guild.getVoiceChannelById(VOICE_COUNTER_ID);
        if (voiceChannel != null) {
            // Discord pozwala na zmianę nazwy kanału tylko 2 RAZY NA 10 MINUT.
            // Pusta lambda w "error" sprawia, że jeśli limit zostanie przekroczony, konsola nie wybuchnie.
            voiceChannel.getManager()
                    .setName("📊 ┊ Osób: " + memberCount)
                    .queue(success -> {}, error -> {});
        }

        // ==========================================
        // 3. WYSYŁANIE WIADOMOŚCI POWITALNEJ
        // ==========================================
        var welcomeChannel = guild.getTextChannelById(WELCOME_CHANNEL_ID);
        if (welcomeChannel != null) {

            var embed = new EmbedBuilder()
                    .setAuthor("Nowy członek społeczności!", null, guild.getIconUrl())
                    .setTitle("Witaj na " + guild.getName() + "!")
                    .setColor(Constants.DEFAULT_COLOR)
                    .setThumbnail(member.getEffectiveAvatarUrl())
                    // TIP: Tutaj możesz dodać banner powitalny, np. .setImage("link_do_gifa")
                    .setDescription("""
                        ### `👋`〢 Cześć, %s!
                        Bardzo cieszymy się, że do nas dołączyłeś. Jesteśmy serwerem Minecraft, który stawia na jakość i społeczność.
                        
                        `📌` **Zacznij od:**
                        > Przeczytaj regulamin: <#%s>
                        > Sprawdź naszą stronę: [Kliknij tutaj](%s)
                        
                        `📈` **Statystyki serwera:**
                        > Jesteś naszym **%d** użytkownikiem! Baw się dobrze.
                        """.formatted(member.getAsMention(), RULES_CHANNEL_ID, Constants.LINK, memberCount))
                    .setFooter("Dołączył(a) do nas", guild.getIconUrl())
                    .setTimestamp(Instant.now())
                    .build();

            // Dynamiczny przycisk do kanału z regulaminem (Link do URL kanału Discord)
            var rulesUrl = "https://discord.com/channels/" + guild.getId() + "/" + RULES_CHANNEL_ID;

            welcomeChannel.sendMessage(member.getAsMention()) // Pingujemy gracza (żeby zauważył powitanie)
                    .addEmbeds(embed)
                    .addActionRow(
                            Button.link(Constants.LINK, "🌐 Strona WWW"),
                            Button.link(rulesUrl, "🛡️ Regulamin") // Prawdziwy link do kanału zamiast wyłączonego przycisku
                    )
                    .queue();
        }

        // ==========================================
        // 4. WIADOMOŚĆ PRYWATNA (Opcjonalnie)
        // ==========================================
        member.getUser().openPrivateChannel().queue(privateChannel -> {
            var pmEmbed = new EmbedBuilder()
                    .setTitle("Witaj na StrideMC!")
                    .setColor(Constants.DEFAULT_COLOR)
                    .setDescription("Cześć! Dziękujemy za dołączenie do naszego serwera Discord. Gdybyś potrzebował pomocy, załóż Ticket na odpowiednim kanale.\n\nŻyczymy miłej gry! `❤️`")
                    .build();

            // Wysyłamy, ale ignorujemy błąd, jeśli użytkownik ma zablokowane wiadomości prywatne
            privateChannel.sendMessageEmbeds(pmEmbed).queue(success -> {}, error -> {});
        }, error -> {}); // Ignorujemy, jeśli nie da się otworzyć DM
    }
}
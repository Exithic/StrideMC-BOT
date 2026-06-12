package Paqlio.me.ListenersAdapters;

import Paqlio.me.BOT;
import Paqlio.me.Configurations.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;

/**
 * @author Paqlio
 * Zoptymalizowane - Pełna synchronizacja z wątkiem Bukkit
 **/
public class User extends ListenerAdapter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy");

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("user")) return;

        // Bezpieczne pobieranie opcji
        var option = event.getOption("użytkownik");
        if (option == null) {
            event.reply("> `❌` Nie podano użytkownika!").setEphemeral(true).queue();
            return;
        }

        var targetMember = option.getAsMember();
        var guild = event.getGuild();

        if (targetMember == null || guild == null) {
            event.reply("> `❌` Nie udało się znaleźć tego użytkownika na serwerze.").setEphemeral(true).queue();
            return;
        }

        // 1. Zabezpieczamy czas odpowiedzi Discorda (Odroczenie)
        event.deferReply().queue();

        // 2. Zbieranie danych z Discorda (Możemy to robić asynchronicznie)
        var highestRole = targetMember.getRoles().stream()
                .max(Comparator.comparingInt(Role::getPosition))
                .map(Role::getAsMention)
                .orElse("`Brak`");

        var effectiveName = targetMember.getEffectiveName();

        // 3. SYNCHRONIZACJA Z BUKKITEM (Zapobiega błędom i crashom silnika)
        Bukkit.getScheduler().runTask(BOT.getInstance(), () -> {
            try {
                var mcPlayer = Bukkit.getPlayer(effectiveName);

                // Rozszerzony status Minecraft
                String mcStatus;
                if (mcPlayer != null && mcPlayer.isOnline()) {
                    int ping = mcPlayer.getPing();
                    int hp = (int) mcPlayer.getHealth();
                    mcStatus = "🟢 **Online** (`" + hp + " HP`, `" + ping + " ms`)";
                } else {
                    // Dodano adnotację, bo nick na DC często różni się od tego w MC
                    mcStatus = "🔴 **Offline** *(lub inny nick w grze)*";
                }

                // 4. Budowanie interfejsu (Embed)
                var embed = new EmbedBuilder()
                        .setColor(Constants.DEFAULT_COLOR)
                        .setAuthor("Informacje o profilu", null, targetMember.getEffectiveAvatarUrl())
                        .setThumbnail(targetMember.getEffectiveAvatarUrl())
                        .setDescription("""
                                ### `👤`〢 Profil: %s
                                Statystyki i informacje o użytkowniku serwera **%s**.
                                """.formatted(targetMember.getAsMention(), guild.getName()))

                        .addField("`🆔` ID Konta", "`" + targetMember.getId() + "`", true)
                        .addField("`🛡️` Najwyższa Ranga", highestRole, true)
                        .addField("`🤖` Typ konta", (targetMember.getUser().isBot() ? "`✅` Bot" : "`👤` Gracz"), true)

                        .addField("`🎮` Status Minecraft", mcStatus, false)

                        .addField("`📅` Data utworzenia", "`" + targetMember.getTimeCreated().format(DATE_FORMATTER) + "`", true)
                        .addField("`📥` Data dołączenia", "`" + targetMember.getTimeJoined().format(DATE_FORMATTER) + "`", true)

                        .setFooter("Zażądano przez: " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl())
                        .setTimestamp(java.time.Instant.now());

                // 5. Wysyłka gotowego Embedu przez Hooka (bo zrobiliśmy deferReply!)
                event.getHook().sendMessageEmbeds(embed.build()).queue();

            } catch (Exception e) {
                event.getHook().sendMessage("> `❌` Wystąpił błąd podczas pobierania danych z serwera Minecraft.").queue();
                e.printStackTrace();
            }
        });
    }
}
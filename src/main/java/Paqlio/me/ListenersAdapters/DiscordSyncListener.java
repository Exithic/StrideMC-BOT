package Paqlio.me.ListenersAdapters;

import Paqlio.me.Addons.SyncManager;
import Paqlio.me.BOT;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;

import java.awt.Color;
import java.time.Instant;

public class DiscordSyncListener extends ListenerAdapter {
    private final SyncManager syncManager;

    public DiscordSyncListener(SyncManager syncManager) {
        this.syncManager = syncManager;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("link")) return;

        // Sprawdzamy czy użytkownik na Discordzie przypadkiem już nie weryfikował konta
        if (syncManager.isDiscordLinked(event.getUser().getId())) {
            event.reply("> `❌` Twoje konto Discord jest już połączone z kontem Minecraft!").setEphemeral(true).queue();
            return;
        }

        TextInput codeInput = TextInput.create("sync_code", "Kod z gry:", TextInputStyle.SHORT)
                .setPlaceholder("Wpisz kod wygenerowany komendą /discord")
                .setMinLength(6)
                .setMaxLength(6)
                .build();

        Modal modal = Modal.create("sync_modal", "Weryfikacja Konta - StrideMC")
                .addComponents(ActionRow.of(codeInput))
                .build();

        event.replyModal(modal).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (!event.getModalId().equals("sync_modal")) return;

        String code = event.getValue("sync_code").getAsString();
        var uuid = syncManager.getPlayerByCode(code);

        if (uuid == null) {
            event.reply("> `❌` Podany kod jest nieprawidłowy lub wygasł!").setEphemeral(true).queue();
            return;
        }

        // Blokada: Upewniamy się, czy ktoś w międzyczasie nie przypisał tego UUID
        if (syncManager.isLinked(uuid)) {
            event.reply("> `❌` To konto Minecraft jest już powiązane z innym kontem Discord!").setEphemeral(true).queue();
            return;
        }

        // Zapisanie do pliku JSON
        syncManager.link(uuid, event.getUser().getId());

        // 1. Opcjonalne: Nadanie Roli (Zmień na ID swojej roli 'Zweryfikowany' lub usuń tę linię)
        var role = event.getGuild().getRoleById("1495723874564964432");
        if (role != null) event.getGuild().addRoleToMember(event.getUser(), role).queue();

        var mcName = Bukkit.getOfflinePlayer(uuid).getName();
        if (mcName != null && event.getMember() != null) {
            // 2. Zmiana Nicku (Bot musi być wyżej w hierarchii ról i mieć uprawnienie Zmiany Pseudonimów!)
            event.getGuild().modifyNickname(event.getMember(), mcName).queue(
                    success -> {},
                    error -> BOT.getInstance().getLogger().warning("Nie udało się zmienić nicku graczowi " + mcName)
            );
        }

        event.reply("> `✅` Pomyślnie połączono Twoje konto! Twój nick został zaktualizowany.").setEphemeral(true).queue();

        // 3. Powiadomienie DM dla gracza
        var embedDM = new EmbedBuilder()
                .setTitle("`🎉` Konto zostało zweryfikowane!")
                .setColor(new Color(88, 101, 242)) // Blurple (Kolor Discorda)
                .setDescription("Witaj **" + mcName + "**!\nTwoje konto Minecraft zostało pomyślnie połączone z naszym serwerem Discord.\n\nOdebrałeś(aś) nagrodę w grze. Życzymy miłej gry na **StrideMC**!")
                .setTimestamp(Instant.now())
                .build();

        event.getUser().openPrivateChannel().queue(privateChannel -> {
            privateChannel.sendMessageEmbeds(embedDM).queue(null, error -> {});
        });

        // 4. Interakcja na Serwerze (Nagrody i Powiadomienie)
        var player = Bukkit.getPlayer(uuid);
        if (player != null) {

            player.showTitle(net.kyori.adventure.title.Title.title(
                    MiniMessage.miniMessage().deserialize("<gradient:#5865F2:#ffffff><bold>DISCORD</bold></gradient>"),
                    MiniMessage.miniMessage().deserialize("<gray>Pomyślnie zweryfikowano konto!")
            ));

            Bukkit.broadcast(MiniMessage.miniMessage().deserialize(
                    "\n<gradient:#5865F2:#ffffff><bold>DISCORD SYNC</bold></gradient>\n" +
                            "<gray>Gracz <white><bold>" + player.getName() + "</bold></white> połączył konto z Discordem i odebrał nagrodę!\n" +
                            "<gray>Ty też możesz to zrobić wpisując <white>/discord</white>!\n"
            ));

            // Nadanie nagród przez konsolę
            Bukkit.getScheduler().runTask(BOT.getInstance(), () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "eco give " + player.getName() + " 1000");
//                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "give " + player.getName() + " diamond 1");
            });
        }
    }
}
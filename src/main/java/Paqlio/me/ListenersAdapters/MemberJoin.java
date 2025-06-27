package Paqlio.me.ListenersAdapters;

import Paqlio.me.Configurations.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class MemberJoin extends ListenerAdapter {

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        var member = event.getMember();
        var guild = event.getGuild();

        var channelmembers = guild.getVoiceChannelById("1387897312092880966");
        if (channelmembers != null) {
            channelmembers.getManager()
                    .setName("⚡ ┊ Ilość osób: " + guild.getMemberCount())
                    .queue();
        } else {
            System.err.println("Nie znaleziono kanału głosowego o ID: 1387897312092880966");
        }

        var channel = guild.getTextChannelById("1383916471645638818");
        if (channel != null) {
            var eb = new EmbedBuilder()
                    .setTitle("👋 〢 NOWY UŻYTKOWNIK")
                    .setColor(Constants.defaultcolor)
                    .setThumbnail(member.getEffectiveAvatarUrl())
                    .setDescription("> ✨ Witaj " + member.getAsMention() + " na serwerze \n"
                            + "> Regulamin znajdziesz tutaj 👋 <#1384254060575854602> \n "
                            + "> Jesteś ``" + guild.getMemberCount() + "`` użytkownikiem na naszym discordzie!")
                    .setTimestamp(member.getTimeJoined())
                    .setFooter(guild.getName(), guild.getIconUrl())
                    .build();

            channel.sendMessageEmbeds(eb)
                    .addActionRow(Button.link(Constants.link, "🌐〢Strona"))
                    .queue();
        } else {
            System.err.println("Nie znaleziono kanału tekstowego o ID: 1383916471645638818");
        }

        System.out.println("New member joined: " + member.getUser().getAsTag());
    }
}

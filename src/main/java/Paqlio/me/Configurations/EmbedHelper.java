package Paqlio.me.Configurations;

import Paqlio.me.BOT;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;
import java.time.Instant;

public final class EmbedHelper {
    private EmbedHelper() {}

    public static final Color COLOR_INFO = new Color(52, 152, 219);
    public static final Color COLOR_SUCCESS = new Color(46, 204, 113);
    public static final Color COLOR_WARN = new Color(241, 196, 15);
    public static final Color COLOR_ERROR = new Color(231, 76, 60);
    public static final Color COLOR_PURPLE = new Color(155, 89, 182);

    public static EmbedBuilder base(String title, Color color) {
        var eb = new EmbedBuilder()
                .setColor(color != null ? color : COLOR_INFO)
                .setFooter(Constants.NAME + " • " + java.time.LocalDate.now().getYear())
                .setTimestamp(Instant.now());
        if (title != null && !title.isEmpty()) eb.setTitle(title);
        return eb;
    }

    public static EmbedBuilder success(String title) { return base(title, COLOR_SUCCESS); }
    public static EmbedBuilder error(String title) { return base(title, COLOR_ERROR); }
    public static EmbedBuilder info(String title) { return base(title, COLOR_INFO); }
    public static EmbedBuilder warn(String title) { return base(title, COLOR_WARN); }
}

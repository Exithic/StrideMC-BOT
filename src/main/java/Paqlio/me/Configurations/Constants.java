package Paqlio.me.Configurations;

import Paqlio.me.BOT;

import java.awt.*;

public class Constants {
    private Constants() {}

    public static final String NAME = "StrideMC";
    public static final Color DEFAULT_COLOR = new Color(14, 179, 229);
    public static final String LINK = "https://www.stridemc.pl/";
    public static final String IMG = "https://media.discordapp.net/attachments/1384282577304817734/1450816989051228262/bn2.png";

    public static String MINECRAFT_CHANNEL = "1384253969127178301";
    public static String CONSOLE_CHANNEL = "1495553473176014950";
    public static String BAN_CHANNEL = "";
    public static String REPORT_CHANNEL = "";

    public static void loadFromConfig() {
        var cfg = BOT.getInstance().getConfig();
        MINECRAFT_CHANNEL = cfg.getString("discord.channels.minecraft", MINECRAFT_CHANNEL);
        CONSOLE_CHANNEL = cfg.getString("discord.channels.console", CONSOLE_CHANNEL);
        BAN_CHANNEL = cfg.getString("discord.channels.ban", BAN_CHANNEL);
        REPORT_CHANNEL = cfg.getString("discord.channels.report", REPORT_CHANNEL);
    }
}

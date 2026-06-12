package Paqlio.me.ListenersAdapters;

import Paqlio.me.BOT;
import Paqlio.me.Configurations.Constants;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class ConsoleSystem extends ListenerAdapter {

    private final String consoleChannelId;
    private final ConsoleAppender appender;
    private int taskId = -1;

    public ConsoleSystem() {
        this.consoleChannelId = Constants.CONSOLE_CHANNEL;
        this.appender = new ConsoleAppender();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || event.isWebhookMessage()) return;
        if (!event.getChannel().getId().equals(consoleChannelId)) return;

        var raw = event.getMessage().getContentRaw();
        if (!raw.startsWith("/")) return; // tylko komendy

        var command = raw.substring(1);
        BOT.getInstance().getLogger().info("[RCON] " + event.getAuthor().getEffectiveName() + ": /" + command);

        event.getMessage().addReaction(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("⏳")).queue();

        Bukkit.getScheduler().runTask(BOT.getInstance(), () -> {
            try {
                var success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                event.getMessage().removeReaction(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("⏳")).queue();
                event.getMessage().addReaction(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode(success ? "✅" : "❔")).queue();
            } catch (Exception e) {
                event.getMessage().removeReaction(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("⏳")).queue();
                event.getMessage().addReaction(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("❌")).queue();
            }
        });
    }

    public void start() {
        Logger rootLogger = (Logger) LogManager.getRootLogger();
        rootLogger.addAppender(appender);
        appender.start();

        taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(BOT.getInstance(), () -> {
            List<String> toSend = appender.getAndClearBuffer();
            if (toSend.isEmpty()) return;

            var jda = BOT.getJda();
            if (jda == null) return;

            TextChannel channel = jda.getTextChannelById(consoleChannelId);
            if (channel == null) return;

            StringBuilder sb = new StringBuilder("```\n");
            for (String line : toSend) {
                if (sb.length() + line.length() > 1900) {
                    sb.append("```");
                    channel.sendMessage(sb.toString()).queue();
                    sb = new StringBuilder("```\n");
                }
                sb.append(line).append("\n");
            }
            if (sb.length() > 4) {
                sb.append("```");
                channel.sendMessage(sb.toString()).queue();
            }
            }, 40L, 40L).getTaskId(); // Co 2 sekundy wyrzuca logi

        // Czyszczenie starych logów co 6h
        Bukkit.getScheduler().runTaskTimerAsynchronously(BOT.getInstance(), () -> {
            var jda = BOT.getJda();
            if (jda == null) return;
            var channel = jda.getTextChannelById(consoleChannelId);
            if (channel == null) return;
            var cutoff = System.currentTimeMillis() - 86400000L; // 24h temu
            channel.getIterableHistory().takeAsync(50).thenAccept(msgs -> {
                var delay = new long[]{0};
                msgs.forEach(m -> {
                    if (m.getTimeCreated().toInstant().toEpochMilli() < cutoff) {
                        delay[0] += 200; // 200ms między deletami
                        m.delete().queueAfter(delay[0], java.util.concurrent.TimeUnit.MILLISECONDS, null, __ -> {});
                    }
                });
            });
        }, 200L, 432000L); // co 6h (432000 ticks = 6h * 60m * 60s * 20t)
    }

    public void stop() {
        Logger rootLogger = (Logger) LogManager.getRootLogger();
        rootLogger.removeAppender(appender);
        appender.stop();
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
    }

    private static class ConsoleAppender extends AbstractAppender {
        private final List<String> buffer = new ArrayList<>();
        private final ReentrantLock lock = new ReentrantLock();

        public ConsoleAppender() {
            super("DiscordConsoleStream", null, null, false, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            String message = event.getMessage().getFormattedMessage().replaceAll("\u001B\\[[;\\d]*m", "");
            lock.lock();
            try {
                buffer.add("[" + event.getLevel().name() + "] " + message);
            } finally {
                lock.unlock();
            }
        }

        public List<String> getAndClearBuffer() {
            List<String> copy = new ArrayList<>();
            lock.lock();
            try {
                if (!buffer.isEmpty()) {
                    copy.addAll(buffer);
                    buffer.clear();
                }
            } finally {
                lock.unlock();
            }
            return copy;
        }
    }
}
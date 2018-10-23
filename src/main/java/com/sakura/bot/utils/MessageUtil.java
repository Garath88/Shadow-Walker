package com.sakura.bot.utils;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.sakura.bot.configuration.Config;
import com.sakura.bot.quiz.Response;

import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Message.Attachment;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public final class MessageUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageUtil.class);

    private MessageUtil() {
    }

    public static void sendMessage(User user, Message message) {
        user.openPrivateChannel()
            .queue(pc -> {
                if (GuildUtil.userIsInGuild(pc.getUser())) {
                    MessageUtil.sendMessage(pc, message);
                }
            });
    }

    public static void sendMessage(User user, String message) {
        user.openPrivateChannel()
            .queue(pc -> {
                if (GuildUtil.userIsInGuild(pc.getUser())) {
                    MessageUtil.sendMessage(pc, message);
                }
            });
    }

    private static void sendMessage(MessageChannel channel, Message message) {
        if (!message.getContentRaw().isEmpty()) {
            channel.sendMessage(message)
                .queue(success -> {
                }, fail -> {
                });
        }
        List<Message.Attachment> attachments = message.getAttachments();
        if (!attachments.isEmpty()) {
            sendAttachments(attachments, channel);
        }
    }

    private static void sendAttachments(List<Attachment> attachments, MessageChannel textChannel) {
        attachments.forEach(attachment -> {
            try {
                textChannel.sendFile(attachment.getInputStream(), attachment.getFileName())
                    .queue();
            } catch (IOException e) {
                LOGGER.error("Failed to add attachment", e);
            }
        });
    }

    private static void sendMessage(MessageChannel channel, String message) {
        channel.sendMessage(message)
            .queue(success -> {
            }, fail -> {
            });
    }

    public static void sendMessageAfter(User user, String message, long delayInSeconds) {
        user.openPrivateChannel()
            .queue(pc -> {
                if (GuildUtil.userIsInGuild(pc.getUser())) {
                    MessageUtil.sendMessageAfter(pc, message, delayInSeconds);
                }
            });
    }

    private static void sendMessageAfter(MessageChannel channel, String message, long delayInSeconds) {
        channel.sendMessage(message)
            .queueAfter(delayInSeconds, TimeUnit.SECONDS, success -> {
            }, fail -> {
            });
    }

    public static void waitForResponse(User user, Guild guild,
        EventWaiter waiter, Response checkResponse, int timeoutMinutes) {
        waiter.waitForEvent(MessageReceivedEvent.class,
            // make sure it's by the same user, and in the same channel
            e -> e.getAuthor().equals(user) && e.getChannel().getType().equals(ChannelType.PRIVATE),
            // respond, inserting the name they listed into the response
            e -> checkResponse.apply(guild, e, waiter),
            timeoutMinutes, TimeUnit.MINUTES, () -> MessageUtil.sendMessage(user, String.format("- Sorry you were too slow %s :frowning: \n"
                    + "- Please try again by typing the **%s" + "member** command.",
                user.getAsMention(), Config.PREFIX)));
    }
}

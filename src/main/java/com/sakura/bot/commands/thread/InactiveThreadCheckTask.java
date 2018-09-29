package com.sakura.bot.commands.thread;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import com.sakura.bot.tasks.Task;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

public final class InactiveThreadCheckTask extends Task {
    private static final int NO_CONTENT_EXPIRE_AFTER_MIN = 5;
    private static final int EXPIRE_AFTER_MIN = 24 * 60;
    private static final int WARNING_BEFORE_MIN = 8 * 60;
    private static final int FINAL_WARNING_BEFORE_MIN = 60;
    private final TextChannel thread;
    private boolean hasWarned;

    InactiveThreadCheckTask(TextChannel thread) {
        super((long)(NO_CONTENT_EXPIRE_AFTER_MIN / 1.5), (long)(NO_CONTENT_EXPIRE_AFTER_MIN / 1.5));
        this.thread = thread;
    }

    private InactiveThreadCheckTask(TextChannel thread,
        long loopTime, long delay) {
        super(loopTime, delay);
        this.thread = thread;
        hasWarned = true;
    }

    @Override
    public void execute() {
        checkIfChannelIsInactive();
    }

    private void checkIfChannelIsInactive() {
        long timeLeft = getExpirationTimeInMinutes();
        if (timeLeft <= 0) {
            if (!hasWarned) {
                sendInactivityMsg(FINAL_WARNING_BEFORE_MIN);
                scheduleDelete(FINAL_WARNING_BEFORE_MIN, 0);
            } else {
                deleteChannel();
            }
        } else if (timeLeft <= FINAL_WARNING_BEFORE_MIN) {
            sendInactivityMsg(getExpirationTimeInMinutes());
            scheduleDelete(timeLeft, 0);
        } else if (timeLeft <= WARNING_BEFORE_MIN) {
            sendInactivityMsg(getExpirationTimeInMinutes());
            scheduleDelete(timeLeft, FINAL_WARNING_BEFORE_MIN);
        }
    }

    private void sendInactivityMsg(long expirationTime) {
        String message = createInactivityMessage(expirationTime);
        thread.sendMessage(message)
            .queue();
    }

    private void scheduleDelete(long timeLeft, long warningTime) {
        reScheduleTask(new InactiveThreadCheckTask(thread,
            1, timeLeft - warningTime));
    }

    private List<Message> getLatestMessages() {
        return thread.getIterableHistory().stream()
            .limit(5)
            .collect(Collectors.toList());
    }

    private long getExpirationTimeInMinutes() {
        List<Message> latestMessages = getLatestMessages();
        OffsetDateTime creationTime = getLatestMessageTime(latestMessages);
        OffsetDateTime currentTime = OffsetDateTime.now(ZoneOffset.UTC);
        if (creationTime != null) {
            return currentTime.until(creationTime.plusMinutes(EXPIRE_AFTER_MIN),
                ChronoUnit.MINUTES);
        } else {
            return currentTime.until(thread.getCreationTime()
                    .plusMinutes(NO_CONTENT_EXPIRE_AFTER_MIN),
                ChronoUnit.MINUTES);
        }
    }

    private OffsetDateTime getLatestMessageTime(List<Message> latestMessages) {
        Message latestUserMsg = getLatestUserMessage(latestMessages);
        if (latestUserMsg != null) {
            return latestUserMsg.getCreationTime();
        }
        return null;
    }

    private Message getLatestUserMessage(List<Message> messages) {
        List<Message> latestUserMsgs = messages.stream()
            .filter(msg -> !msg.getAuthor().isBot())
            .collect(Collectors.toList());
        if (!latestUserMsgs.isEmpty()) {
            return latestUserMsgs.get(0);
        }
        return null;
    }

    void deleteChannel() {
        InactiveThreadChecker.getTaskListContainer()
            .cancelTask(this);
        thread.delete()
            .queue();
    }

    private String createInactivityMessage(long expirationTime) {
        long hours = expirationTime / 60;
        long minutes = expirationTime % 60;
        return String.format("This thread has been marked as **inactive** "
            + "and will be deleted in **%dh:%02dm** if no activity occurs!", hours, minutes);
    }

    private void reScheduleTask(Task newTask) {
        InactiveThreadChecker.getTaskListContainer()
            .reScheduleTask(this,
                newTask);
    }

    public TextChannel getThread() {
        return thread;
    }
}
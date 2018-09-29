package com.sakura.bot.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.ISnowflake;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;

/*TODO: TEST JOOQ INSTEAD OF THIS*/

public final class ThreadDbTable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadDbTable.class);
    private static final String DB_NAME = "threads";
    private static final String QUERY_RESULT_ERROR = "Failed to close or get result from query";

    private ThreadDbTable() {
    }

    public static void addThread(User user, Channel thread) {
        String sql = String.format("INSERT INTO `sakurabot`.`%s` "
                + "(`user`,`user_id`, `name`, `id`) "
                + "VALUES ('%s', '%s', '%s', '%s')",
            DB_NAME, user.getName(), user.getId(), thread.getName(), thread.getId());
        executeQuery(sql);
    }

    private static void executeQuery(String sql) {
        try {
            ResultSet result = MariaDbConnector.executeSql(sql);
            if (result != null) {
                result.close();
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to close resource from query", e);
        }
    }

    private static List<Long> getAllChannelIds() {
        String sql = String.format("SELECT `id` FROM %s", DB_NAME);
        ResultSet result = MariaDbConnector.executeSql(sql);
        List<Long> channelIds = new ArrayList<>();
        if (result != null) {
            try {
                while (result.next()) {
                    channelIds.add(result.getLong("id"));
                }
                result.close();
                return channelIds;
            } catch (SQLException e) {
                LOGGER.error(QUERY_RESULT_ERROR, e);
            }
        }
        return Collections.emptyList();
    }

    public static ThreadInfo getThreadInfoFromUser(User user) {
        String sql = String.format(
            "SELECT `name`, `id` FROM %s WHERE `user_id` = %s", DB_NAME, user.getId());
        ResultSet result = MariaDbConnector.executeSql(sql);
        List<String> outprint = new ArrayList<>();
        List<Long> channelIds = new ArrayList<>();
        int count = 0;
        if (result != null) {
            try {
                while (result.next()) {
                    count++;
                    outprint.add((String.format("`[%d]`  **%-2s**",
                        count, result.getString("name"))));
                    channelIds.add(result.getLong("id"));
                }
                result.close();
                return new ThreadInfo(outprint.stream()
                    .collect(Collectors.joining("\n")), outprint, channelIds);
            } catch (SQLException e) {
                LOGGER.error(QUERY_RESULT_ERROR, e);
            }
        }
        return new ThreadInfo("No created channels found!",
            Collections.emptyList(), Collections.emptyList());
    }

    public static void deleteChannel(Long id) {
        String sql = String.format(
            "DELETE FROM %s WHERE `id` = %s", DB_NAME, id);
        executeQuery(sql);
    }

    public static void checkForThreadDbInconsistency(List<TextChannel> threads) {
        List<Long> allThreadsById = threads.stream()
            .map(ISnowflake::getIdLong)
            .collect(Collectors.toList());
        List<Long> deletedChannels = ThreadDbTable.getAllChannelIds().stream()
            .filter(chanId -> !allThreadsById
                .contains(chanId))
            .collect(Collectors.toList());
        deletedChannels.forEach(ThreadDbTable::deleteChannel);
        threads.forEach(ThreadDbTable::checkStoredLatestMessage);
    }

    private static void checkStoredLatestMessage(TextChannel thread) {
        long messageId = getLatestMsgId(thread.getIdLong());
        try {
            thread.getMessageById(messageId)
                .complete();
        } catch (ErrorResponseException e) {
            updateLatestMsgInDbIfDeleted(messageId, thread);
        }
    }

    public static void updateLatestMsgInDbIfDeleted(long deletedMsgId, TextChannel textChan) {
        if (deletedMsgId == ThreadDbTable.getLatestMsgId(textChan.getIdLong())) {
            updateToPreviousMsg(deletedMsgId, textChan);
        }
    }

    private static void updateToPreviousMsg(long deletedMsgId, TextChannel textChan) {
        long textChanId = textChan.getIdLong();
        textChan.getHistoryBefore(deletedMsgId, 1).queue(history -> {
            List<Message> messages = history.getRetrievedHistory();
            if (!messages.isEmpty()) {
                ThreadDbTable.storeLatestMsgId(
                    messages.get(0).getIdLong(), textChanId);
            } else {
                ThreadDbTable.storeLatestMsgId(0, textChanId);
            }
        });
    }

    public static void storeLatestMsgId(long messageId, long threadId) {
        String sql = String.format("UPDATE `sakurabot`.`%s` "
                + "SET latest_message_id = %s WHERE id = %s ",
            DB_NAME, messageId, threadId);
        executeQuery(sql);
    }

    public static long getLatestMsgId(long threadId) {
        String sql = String.format(
            "SELECT latest_message_id FROM sakurabot.threads WHERE id = %s",
            threadId);
        ResultSet result = MariaDbConnector.executeSql(sql);
        long latestMessageId = 0;
        if (result != null) {
            try {
                while (result.next()) {
                    latestMessageId = result.getLong("latest_message_id");
                }
                result.close();
                return latestMessageId;
            } catch (SQLException e) {
                LOGGER.error(QUERY_RESULT_ERROR, e);
            }
        }
        return latestMessageId;
    }

    public static int getPostCount(TextChannel thread) {
        long threadId = thread.getIdLong();
        return getPostCount(threadId);
    }

    private static int getPostCount(long threadId) {
        String sql = String.format(
            "SELECT post_count FROM sakurabot.threads WHERE id = %s",
            threadId);
        ResultSet result = MariaDbConnector.executeSql(sql);
        int postCount = 0;
        if (result != null) {
            try {
                while (result.next()) {
                    postCount = result.getInt("post_count");
                }
                result.close();
                return postCount;
            } catch (SQLException e) {
                LOGGER.error(QUERY_RESULT_ERROR, e);
            }
        }
        return postCount;
    }

    public static void storePostCount(int postCount, long threadId) {
        String sql = String.format("UPDATE `sakurabot`.`%s` "
                + "SET post_count = %s WHERE id = %s ",
            DB_NAME, postCount, threadId);
        executeQuery(sql);
    }
}

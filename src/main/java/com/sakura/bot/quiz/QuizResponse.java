package com.sakura.bot.quiz;

import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.sakura.bot.utils.EmojiUtil;
import com.sakura.bot.utils.MessageUtil;
import com.sakura.bot.utils.RoleUtil;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class QuizResponse implements Response {
    private CommandClient client;
    private static final String RETRY_MSG = "- You can try again by typing the **+member** command";

    public QuizResponse(CommandClient client) {
        this.client = client;
    }

    @Override
    public void apply(Guild guild, MessageReceivedEvent e, EventWaiter waiter) {
        User user = e.getAuthor();
        String response = e.getMessage().getContentRaw().toLowerCase();
        if (response.contains("asagi")) {
            MessageUtil.sendMessageToUser(user, EmojiUtil.getCustomEmoji(e.getJDA(), "sakura"));
            MessageUtil.sendMessageToUser(user, "- Correct");
            RoleUtil.addRole(guild, user, QuizQuestion.RULES_ROLE);
            RoleUtil.removeRole(guild, user, QuizQuestion.QUIZ_ROLE);
            RulesQuestion.perform(user, guild, waiter, client);
        } else if (response.equals("sakura") || response.equals("sakura igawa")) {
            MessageUtil.sendMessageToUser(user,
                "- Mee?! Why would I fight myself? You silly goose! :smile:\n"
                    + RETRY_MSG);
        } else {
            MessageUtil.sendMessageToUser(user,
                String.format("- Aww.. wrong answer %s \n"
                        + RETRY_MSG,
                    EmojiUtil.getCustomEmoji(e.getJDA(), "feelsbadman")));
            MessageUtil.sendMessageToUser(guild.getOwner().getUser(),
                String.format("%s#%s: %s%n%s"
                    , user.getName(), user.getDiscriminator(), response, user.getAsMention()));
        }
    }
}

package com.sakura.bot.commands.say;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.sakura.bot.utils.ArgumentChecker;
import com.sakura.bot.utils.MessageUtil;
import com.sakura.bot.utils.PrivateChannelWrapper;
import com.sakura.bot.utils.UserUtil;

import net.dv8tion.jda.core.entities.Message.Attachment;
import net.dv8tion.jda.core.entities.User;

public class SakuraDMCommand extends Command {

    public SakuraDMCommand() {
        this.name = "sakura_dm";
        this.help = "say something with Sakura in a DM to a user.";
        this.arguments = "<text> followed by separator '|' <user id>";
        this.guildOnly = false;
        this.ownerCommand = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        try {
            String args = event.getArgs();
            validateArguments(args);
            String[] items = args.split("\\|");
            User user = UserUtil.findUser(items[1], event);
            List<Attachment> attachments = event.getMessage().getAttachments();
            user.openPrivateChannel().queue(
                PrivateChannelWrapper.userIsInGuild(pc ->
                {
                    MessageUtil.sendAttachmentsToChannel(attachments, pc);
                    MessageUtil.sendMessageToChannel(items[0], pc, true);
                }),
                fail -> {
                });
        } catch (IllegalArgumentException e) {
            event.replyWarning(e.getMessage());
        }
    }

    private void validateArguments(String args) {
        ArgumentChecker.checkArgsByPipe(args, 2);
        String[] items = args.split("\\|");
        String userId = items[1].replaceAll("\\s+", "");
        Preconditions.checkArgument(StringUtils.isNumeric(userId),
            String.format("Invalid user id \"%s\", id must be numeric", userId));
    }
}

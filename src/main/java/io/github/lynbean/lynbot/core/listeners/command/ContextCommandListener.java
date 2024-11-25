package io.github.lynbean.lynbot.core.listeners.command;

import java.util.List;

import javax.annotation.Nonnull;

import io.github.lynbean.lynbot.core.command.processor.CommandProcessor;
import io.github.lynbean.lynbot.core.thread.ThreadController;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ContextCommandListener extends ListenerAdapter {
    private final List<String> prefixes;
    private final CommandProcessor commandProcessor;

    public ContextCommandListener(CommandProcessor commandProcessor, List<String> prefixes) {
        this.commandProcessor = commandProcessor;
        this.prefixes = prefixes;
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw();
        String selfUserMention = event.getJDA().getSelfUser().getAsMention();

        for (int i = 0; i <= prefixes.size(); i++) {
            if (message.startsWith(selfUserMention)) {
                message = message.substring(selfUserMention.length())
                    .trim();
                break;
            }

            if (i == prefixes.size())
                return;

            if (!message.toLowerCase().startsWith(prefixes.get(i).toLowerCase()))
                continue;

            message = message.substring(prefixes.get(i).length())
                .trim();

            break;
        }

        final String messageWithoutPrefix = message;
        ThreadController.executeCommand(() -> commandProcessor.execute(event, messageWithoutPrefix));
    }
}

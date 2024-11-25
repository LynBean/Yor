package io.github.lynbean.lynbot.common.command;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import io.github.lynbean.lynbot.common.command.meta.ContextCommandMeta;
import io.github.lynbean.lynbot.core.BotCore;
import lombok.Setter;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public abstract class ContextCommand {
    protected @Setter BotCore core;
    protected final @Nonnull String name;
    protected final Map<String, ContextCommand> subCommands = new HashMap<>();

    public ContextCommand() {
        var clazz = getClass();

        if (!clazz.isAnnotationPresent(ContextCommandMeta.class))
            throw new IllegalArgumentException("Command annotation is missing");

        ContextCommandMeta meta = clazz.getAnnotation(ContextCommandMeta.class);
        this.name = meta.name();
    }

    /**
     * Handles the command event.
     * @param event
     * @param messageWithArgsOnly The message without the prefix and the command name.
     */
    protected abstract void process(MessageReceivedEvent event, String messageWithArgsOnly);

    /**
     * Executes the command.
     * @param event
     * @param messageWithoutPrefix The message without the prefix.
     */
    public void execute(MessageReceivedEvent event, String messageWithoutPrefix) {
        String[] args = messageWithoutPrefix.split(" ");
        ContextCommand subCommand = null;

        if (args.length > 1)
            subCommand = subCommands.get(args[1]);

        if (subCommand != null) {
            String messageWithArgsOnly = messageWithoutPrefix.substring(
                messageWithoutPrefix.indexOf(args[2])
            );
            subCommand.process(event, messageWithArgsOnly);
            return;
        }

        String messageWithArgsOnly = messageWithoutPrefix;

        if (args.length > 1)
            messageWithArgsOnly = messageWithoutPrefix.substring(
                messageWithoutPrefix.indexOf(args[1])
            );

        process(event, messageWithArgsOnly);
    }

    /**
     * Registers a sub command.
     */
    public void addSubCommand(ContextCommand command) {
        subCommands.put(command.getName(), command);
    }

    /**
     * Returns the name of the command.
     * @return
     */
    public String getName() {
        return name;
    }
}

package io.github.lynbean.lynbot.core.command;

import java.util.List;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.lynbean.lynbot.common.command.ContextCommand;
import io.github.lynbean.lynbot.common.command.SlashCommand;
import io.github.lynbean.lynbot.core.BotCore;
import io.github.lynbean.lynbot.core.command.manager.CommandManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class CommandRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(CommandRegistry.class);
    public final CommandManager manager;

    public CommandRegistry(CommandManager manager) {
        this.manager = manager;
    }

    /**
     * A shortcut method to get BotCore instance.
     * It is equivalent to {@code manager.getCore()}.
     * @return BotCore instance
     */
    public BotCore getCore() {
        return manager.getCore();
    }

    public void execute(MessageReceivedEvent event, String contentWithoutPrefix) {
        String commandName = contentWithoutPrefix.split(" ")[0];
        ContextCommand command = manager.getContextCommands().get(commandName);

        if (command != null) {
            LOG.info("\u001B[32m<{} - {}> -> {}\u001B[0m", event.getAuthor().getName(), event.getAuthor().getId(), commandName);
            command.execute(event, contentWithoutPrefix);
        }
    }

    public void execute(SlashCommandInteractionEvent event) {
        SlashCommand command = manager.getSlashCommands().get(event.getName());

        if (command != null) {
            LOG.info("\u001B[32m<{} - {}> -> {}\u001B[0m", event.getUser().getName(), event.getUser().getId(), event.getCommandString());
            command.execute(event);
        }
    }

    public void autoComplete(CommandAutoCompleteInteractionEvent event) {
        SlashCommand command = manager.getSlashCommands().get(event.getName());

        if (command != null)
            command.autoComplete(event);
    }

    /**
     * Registers the slash command to the command manager.
     * @param clazz
     */
    public void registerGlobalSlashCommand(@Nonnull Class<? extends SlashCommand> clazz) {
        manager.registerGlobalSlashCommand(clazz);
    }

    /**
     * Registers the slash command to the command manager.
     * This command will only be registered in development guild.
     * @param clazz
     */
    public void registerGuildSlashCommand(@Nonnull Class<? extends SlashCommand> clazz, @Nonnull String guildId) {
        manager.registerGuildSlashCommand(clazz, guildId);
    }

    /**
     * Registers the context command to the command manager.
     * @param clazz
     */
    public void registerContextCommand(@Nonnull Class<? extends ContextCommand> clazz) {
        manager.registerContextCommand(clazz);
    }

    /**
     * Registers the event listeners to all shards.
     * @param listener
     */
    public void registerEventListeners(@Nonnull Object... listener) {
        List<JDA> jdas = BotCore.getInstances()
            .stream()
            .map(core -> core.getMainShardManager().getShards())
            .flatMap(List::stream)
            .toList();

        for (JDA jda : jdas) {
            jda.addEventListener(listener);
        }
    }
}

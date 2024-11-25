package io.github.lynbean.lynbot.core.listeners.command;

import javax.annotation.Nonnull;

import io.github.lynbean.lynbot.core.command.processor.CommandProcessor;
import io.github.lynbean.lynbot.core.thread.ThreadController;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class SlashCommandInteractionListener extends ListenerAdapter {
    private final CommandProcessor commandProcessor;

    public SlashCommandInteractionListener(CommandProcessor commandProcessor) {
        this.commandProcessor = commandProcessor;
    }

    @Override
    public void onSlashCommandInteraction(@Nonnull SlashCommandInteractionEvent event) {
        ThreadController.executeCommand(() -> commandProcessor.execute(event));
    }

    @Override
    public void onCommandAutoCompleteInteraction(@Nonnull CommandAutoCompleteInteractionEvent event) {
        ThreadController.executeCommand(() -> commandProcessor.commandAutoComplete(event));
    }
}

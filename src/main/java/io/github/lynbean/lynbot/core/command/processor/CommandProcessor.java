package io.github.lynbean.lynbot.core.command.processor;

import io.github.lynbean.lynbot.core.command.CommandRegistry;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class CommandProcessor {
    public final CommandRegistry registry;

    public CommandProcessor(CommandRegistry registry) {
        this.registry = registry;
    }

    public void execute(MessageReceivedEvent event, String messageWithoutPrefix) {
        registry.execute(event, messageWithoutPrefix);
    }

    public void execute(SlashCommandInteractionEvent event) {
        registry.execute(event);
    }

    public void commandAutoComplete(CommandAutoCompleteInteractionEvent event) {
        registry.autoComplete(event);
    }
}

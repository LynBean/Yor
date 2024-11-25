package io.github.lynbean.lynbot.common.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import io.github.lynbean.lynbot.common.command.meta.SlashCommandMeta;
import io.github.lynbean.lynbot.core.BotCore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public abstract class SlashCommand {
    private @Getter @Setter BotCore core;
    private @Getter @Nonnull final String name;
    private @Getter @Nonnull final String description;
    private @Getter final boolean isDeferReply;
    private @Getter final boolean isEphemeral;
    private @Getter final boolean isNSFW;
    private @Getter final boolean isGuildOnly;
    private @Getter final boolean isAdminOnly;
    private @Getter final Permission[] requiredPermissions;
    private @Getter @Nonnull final List<OptionData> options = new ArrayList<>();
    private final Map<String, SlashCommand> subCommands = new HashMap<>();
    private final List<SubcommandData> subCommandDatas = new ArrayList<>();

    public SlashCommand() {
        var clazz = getClass();

        // SlashCommand MetaData must be present
        if (!clazz.isAnnotationPresent(SlashCommandMeta.class))
            throw new IllegalArgumentException("Command annotation is missing");

        SlashCommandMeta meta = clazz.getAnnotation(SlashCommandMeta.class);
        this.name = meta.name();
        this.description = meta.description();
        this.isDeferReply = meta.deferReply();
        this.isEphemeral = meta.ephemeral();
        this.isNSFW = meta.isNSFW();
        this.isGuildOnly = meta.isGuildOnly();
        this.isAdminOnly = meta.isAdminOnly();
        this.requiredPermissions = meta.permissions();

        if (meta.options().length > 0) {
            List<OptionData> tempOptions = new ArrayList<>();

            List.of(meta.options())
                .stream()
                .forEach(
                    option -> {
                        OptionData data = new OptionData(
                            option.type(), option.name(), option.description(), option.required()
                        );

                        if (data.getType() == OptionType.INTEGER)
                            data.setMinValue(option.minIntValues())
                                .setMaxValue(option.maxIntValues());

                        if (data.getType() == OptionType.NUMBER)
                            data.setMinValue(option.minDoubleValues())
                                .setMaxValue(option.maxDoubleValues());

                        if (option.choices().length > 0 && data.getType().canSupportChoices()) {
                            for (var choice : option.choices()) {
                                if (data.getType() == OptionType.INTEGER)
                                    data.addChoice(choice.description(), Integer.parseInt(choice.value()));
                                else if (data.getType() == OptionType.NUMBER)
                                    data.addChoice(choice.description(), Double.parseDouble(choice.value()));
                                else
                                    data.addChoice(choice.description(), choice.value());
                            }
                        }

                        tempOptions.add(data);
                    }
                );

            this.options.addAll(editOptions(tempOptions));
        }
    }

    /**
     * Handles the slash command event.
     * @param event
     */
    protected abstract void process(SlashCommandInteractionEvent event);

    /**
     * Override this method to edit the option data before it is added to the command.
     * This method is created for convenience and is not required.
     * At most cases, you can use the {@link SlashCommandMeta} annotation to add options.
     * @param data The option data to edit.
     * @return The edited option data.
     */
    protected List<OptionData> editOptions(List<OptionData> options) {
        return options;
    };

    /**
     * Handles the auto complete event.
     * @param event
     */
    public void autoComplete(CommandAutoCompleteInteractionEvent event) {};

    /**
     * Executes the command.
     * @param event
     */
    public void execute(SlashCommandInteractionEvent event) {
        if (isDeferReply())
            event.deferReply().queue();

        SlashCommand subCommand = subCommands.get(event.getSubcommandName());

        if (subCommand != null) {
            subCommand.process(event);
            return;
        }

        process(event);
    }

    /**
     * Registers a sub command.
     */
    public void addSubCommand(SlashCommand command) {
        SubcommandData data = new SubcommandData(command.getName(), command.getDescription());

        if (!command.getOptions().isEmpty())
            data.addOptions(command.getOptions());

        subCommands.put(command.getName(), command);
        subCommandDatas.add(data);
    }

    /**
     * Returns an unmodifiable map of all registered sub commands.
     */
    public Map<String, SlashCommand> getSubCommands() {
        return Collections.unmodifiableMap(subCommands);
    }

    /**
     * Returns an unmodifiable list of all registered sub commands.
     */
    public List<SubcommandData> getSubCommandDatas() {
        return Collections.unmodifiableList(subCommandDatas);
    }
}

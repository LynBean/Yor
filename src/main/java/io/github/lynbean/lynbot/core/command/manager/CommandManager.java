package io.github.lynbean.lynbot.core.command.manager;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import io.github.lynbean.lynbot.common.command.ContextCommand;
import io.github.lynbean.lynbot.common.command.SlashCommand;
import io.github.lynbean.lynbot.core.BotCore;
import lombok.Getter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class CommandManager {
    private final Map<String, ContextCommand> contextCommands = new HashMap<>();
    private final Map<String, SlashCommand> slashCommands = new HashMap<>();
    private final List<CommandData> globalSlashCommandDatas = new ArrayList<>();
    private final Map<String, List<CommandData>> guildSlashCommandDatas = new HashMap<>();

    private @Getter final BotCore core;

    public CommandManager(BotCore core) {
        this.core = core;
    }

    /**
     * Returns an unmodifiable map of all registered context commands.
     */
    public Map<String, ContextCommand> getContextCommands() {
        return Collections.unmodifiableMap(contextCommands);
    }

    /**
     * Returns an unmodifiable map of all registered slash commands.
     */
    public Map<String, SlashCommand> getSlashCommands() {
        return Collections.unmodifiableMap(slashCommands);
    }

    /**
     * Returns an unmodifiable list of all registered slash commands.
     */
    public List<CommandData> getGlobalSlashCommandDatas() {
        return Collections.unmodifiableList(globalSlashCommandDatas);
    }

    public Map<String, List<CommandData>> getGuildSlashCommandData() {
        return Collections.unmodifiableMap(guildSlashCommandDatas);
    }

    /**
     * Instantiates a new instance of the given class.
     */
    @Nonnull
    private <T> T instantiate(Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Registers a context command.
     */
    public <T extends ContextCommand> T registerContextCommand(@Nonnull Class<T> clazz) {
        return registerContextCommand(instantiate(clazz));
    }

    private <T extends ContextCommand> T registerContextCommand(@Nonnull T command) {
        if (contextCommands.putIfAbsent(command.getName(), command) != null) {
            throw new IllegalArgumentException("Context Command with name " + command.getName() + " already exists");
        }

        command.setCore(core);
        registerSubContextCommands(command);
        contextCommands.put(command.getName(), command);
        return command;
    }

    private void registerSubContextCommands(ContextCommand command) {
        for (var inner : command.getClass().getDeclaredClasses()) {
            if (!Modifier.isStatic(inner.getModifiers())) continue;
            if (!ContextCommand.class.isAssignableFrom(inner)) continue;
            if (inner.isLocalClass() || inner.isAnonymousClass()) continue;
            if (Modifier.isAbstract(inner.getModifiers())) continue;

            var subCommand = (ContextCommand) instantiate(inner);
            command.addSubCommand(subCommand);
        }
    }

    /**
     * Registers a slash command.
     */
    public <T extends SlashCommand> T registerGlobalSlashCommand(@Nonnull Class<T> clazz) {
        T command = registerSlashCommand(instantiate(clazz));
        CommandData commandData = registerSlashCommandData(command);
        slashCommands.put(command.getName(), command);
        globalSlashCommandDatas.add(commandData);
        return command;
    }

    /**
     * Registers a slash command.
     * This command will only be registered in development guild.
     */
    public <T extends SlashCommand> T registerGuildSlashCommand(@Nonnull Class<T> clazz, @Nonnull String guildId) {
        T command = registerSlashCommand(instantiate(clazz));
        CommandData commandData = registerSlashCommandData(command);
        slashCommands.put(command.getName(), command);

        guildSlashCommandDatas.putIfAbsent(guildId, new ArrayList<>());
        guildSlashCommandDatas.get(guildId)
            .add(commandData);

        return command;
    }

    private <T extends SlashCommand> T registerSlashCommand(@Nonnull T command) {
        if (slashCommands.putIfAbsent(command.getName(), command) != null) {
            throw new IllegalArgumentException("Slash Command with name " + command.getName() + " already exists");
        }

        command.setCore(core);
        registerSubSlashCommands(command);

        return command;
    }

    private <T extends SlashCommand> CommandData registerSlashCommandData(@Nonnull T command) {
        CommandData commandData;

        if (command.getSubCommands().isEmpty())
            commandData = Commands.slash(command.getName(), command.getDescription())
                .addOptions(command.getOptions());

        else
            commandData = Commands.slash(command.getName(), command.getDescription())
                .addSubcommands(command.getSubCommandDatas());

        commandData
            .setDefaultPermissions(command.isAdminOnly() == false ? DefaultMemberPermissions.ENABLED : DefaultMemberPermissions.DISABLED)
            .setGuildOnly(command.isGuildOnly())
            .setNSFW(command.isNSFW());

        if (command.getRequiredPermissions().length > 0)
            commandData.setDefaultPermissions(DefaultMemberPermissions.enabledFor(command.getRequiredPermissions()));

        return commandData;
    }

    private void registerSubSlashCommands(SlashCommand command) {
        for (var inner : command.getClass().getDeclaredClasses()) {
            if (!Modifier.isStatic(inner.getModifiers())) continue;
            if (!SlashCommand.class.isAssignableFrom(inner)) continue;
            if (inner.isLocalClass() || inner.isAnonymousClass()) continue;
            if (Modifier.isAbstract(inner.getModifiers())) continue;

            SlashCommand subCommand = (SlashCommand) instantiate(inner);
            subCommand.setCore(core);
            command.addSubCommand(subCommand);
        }
    }

    public void unregisterAll() {
        contextCommands.clear();
        slashCommands.clear();
        globalSlashCommandDatas.clear();
        guildSlashCommandDatas.clear();
    }
}

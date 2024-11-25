package io.github.lynbean.lynbot.core;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

import io.github.lynbean.lynbot.core.command.CommandRegistry;
import io.github.lynbean.lynbot.core.command.manager.CommandManager;
import io.github.lynbean.lynbot.core.command.processor.CommandProcessor;
import io.github.lynbean.lynbot.core.listeners.command.ContextCommandListener;
import io.github.lynbean.lynbot.core.listeners.command.SlashCommandInteractionListener;
import io.github.lynbean.lynbot.core.listeners.waiter.EventWaiter;
import io.github.lynbean.lynbot.loader.BotPaths;
import io.github.lynbean.lynbot.loader.CogsLoader;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class BotCore {

    private final static EventBus eventBus = new EventBus();
    private final static Logger LOG = LoggerFactory.getLogger(BotCore.class);

    private final BotCoreBuilder builder;

    private final CommandManager manager = new CommandManager(this);
    private final CommandRegistry registry = new CommandRegistry(manager);
    private final CommandProcessor processor = new CommandProcessor(registry);
    private final static List<BotCore> instances = new ArrayList<BotCore>();

    private final List<String> subBotTokens;
    private final String mainBotToken;

    private final List<String> prefixes;
    private final int shardsTotal;

    private final ShardManager mainShardManager;
    private final List<ShardManager> subShardManagers;

    private final static EventWaiter eventWaiter = new EventWaiter();

    private final List<CacheFlag> cacheFlags;
    private final ChunkingFilter chunkingFilter;
    private final List<GatewayIntent> gateways;
    private final MemberCachePolicy memberCachePolicy;

    public static class BotCoreBuilder {
        private List<String> subBotTokens = new ArrayList<>();
        private String mainBotToken;

        private List<String> prefixes = new ArrayList<>();
        private int shardsTotal = -1;

        private List<CacheFlag> cacheFlags = new ArrayList<>();
        private ChunkingFilter chunkingFilter = ChunkingFilter.NONE;
        private List<GatewayIntent> gateways = new ArrayList<>();
        private MemberCachePolicy memberCachePolicy = MemberCachePolicy.NONE;

        public BotCoreBuilder(String mainBotToken) {
            this.mainBotToken = mainBotToken;
        }

        public BotCoreBuilder appendSubBotTokens(List<String> tokens) {
            this.subBotTokens.addAll(tokens);
            return this;
        }

        public BotCoreBuilder setSubBotTokens(List<String> tokens) {
            this.subBotTokens = tokens;
            return this;
        }

        public BotCoreBuilder appendPrefixes(List<String> prefixes) {
            this.prefixes.addAll(prefixes);
            return this;
        }

        public BotCoreBuilder setPrefixes(List<String> prefixes) {
            this.prefixes = prefixes;
            return this;
        }

        public BotCoreBuilder setShardsTotal(int shardsTotal) {
            this.shardsTotal = shardsTotal;
            return this;
        }

        public BotCoreBuilder appendCacheFlags(List<CacheFlag> cacheFlags) {
            this.cacheFlags.addAll(cacheFlags);
            return this;
        }

        public BotCoreBuilder setCacheFlags(List<CacheFlag> cacheFlags) {
            this.cacheFlags = cacheFlags;
            return this;
        }

        public BotCoreBuilder setChunkingFilter(ChunkingFilter chunkingFilter) {
            this.chunkingFilter = chunkingFilter;
            return this;
        }

        public BotCoreBuilder appendGatewayIntents(List<GatewayIntent> gateways) {
            this.gateways.addAll(gateways);
            return this;
        }

        public BotCoreBuilder setGatewayIntents(List<GatewayIntent> gateways) {
            this.gateways = gateways;
            return this;
        }

        public BotCoreBuilder setMemberCachePolicy(MemberCachePolicy memberCachePolicy) {
            this.memberCachePolicy = memberCachePolicy;
            return this;
        }

        public BotCore build() {
            return new BotCore(this);
        }
    }

    private BotCore(BotCoreBuilder builder) {
        this.builder = builder;
        this.mainBotToken = builder.mainBotToken;
        this.subBotTokens = builder.subBotTokens;
        this.prefixes = builder.prefixes;
        this.shardsTotal = builder.shardsTotal;
        this.cacheFlags = builder.cacheFlags;
        this.chunkingFilter = builder.chunkingFilter;
        this.gateways = builder.gateways;
        this.memberCachePolicy = builder.memberCachePolicy;

        LOG.info("\u001B[31mBuilding Shard Managers for " + (subBotTokens.size() + 1) + " bots...\u001B[0m");
        this.mainShardManager = buildShardManager(mainBotToken, true);
        this.subShardManagers = subBotTokens.stream()
            .map(token -> buildShardManager(token))
            .collect(Collectors.toList());

        instances.add(this);
        login();
    }

    private ShardManager buildShardManager(String token) {
        return buildShardManager(token, false);
    }

    private ShardManager buildShardManager(String token, boolean enableFunctionality) {
        DefaultShardManagerBuilder smb = DefaultShardManagerBuilder.create(token, gateways);
        smb.setShardsTotal(shardsTotal);

        if (enableFunctionality) {
            smb.enableCache(cacheFlags)
                .setChunkingFilter(chunkingFilter)
                .setMemberCachePolicy(memberCachePolicy)
                .addEventListeners(eventWaiter)
                .addEventListenerProviders(
                    List.of(
                        shardId -> new SlashCommandInteractionListener(processor),
                        shardId -> new ContextCommandListener(processor, prefixes)
                    )
                );
        }

        return smb.build(false);
    }

    private void login() {
        LOG.info("\u001B[31mStarting shards...\u001B[0m");
        // getShardManagers().forEach(ShardManager::login);
        // getShardManagers().forEach(shardManager -> {
        //     SelfUser botUser = shardManager.getShards().get(0).getSelfUser();
        //     LOG.info(
        //         "\u001B[33m" + botUser.getName() + "#" + botUser.getDiscriminator() + " - " + botUser.getId() + " is ready!" +
        //         " (" + String.valueOf(shardManager.getShards().size()) + " shards)" +
        //         " (" + String.valueOf(shardManager.getGuilds().size()) + " guilds)" +
        //         " (" + String.valueOf(shardManager.getUsers().size()) + " users)" +
        //         " (GatewayPing " + String.valueOf(shardManager.getAverageGatewayPing()) + "ms)" +
        //         "\u001B[0m"
        //     );
        // });

        LOG.info("\u001B[31mShards started. Registering commands...\u001B[0m");
        BotPaths.setup();
        List<Class<?>> cogs = CogsLoader.getCogs();

        for (Class<?> cog : cogs) {
            try {
                Object instance = cog.getDeclaredConstructor().newInstance();
                eventBus.register(instance);
                LOG.info("\u001B[33mRegistering cog " + cog.getName() + "\u001B[0m");
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                LOG.error("\u001B[31mFailed to register cog " + cog.getName() + "\u001B[0m");
            }
        }

        try {
            eventBus.post(registry);
        } catch (Exception e) {
            LOG.error("\u001B[31mFailed to register commands\u001B[0m", e);
        }

        LOG.info("\u001B[31mCommands registered. Syncing commands to Discord...\u001B[0m");
        List<CommandData> globalSlashCommandDatas = manager.getGlobalSlashCommandDatas();
        Map<String, List<CommandData>> guildSlashCommandDatas = manager.getGuildSlashCommandData();
        for (JDA mainBotJda : mainShardManager.getShards()) {
            mainBotJda.updateCommands()
                .addCommands(globalSlashCommandDatas)
                .queue();

            LOG.info(
                "\u001B[33mSynced {} global commands for shard {}\u001B[0m",
                globalSlashCommandDatas.size(), mainBotJda.getShardInfo().getShardId()
            );

            guildSlashCommandDatas.forEach(
                (guildId, slashCommandDatas) -> {
                    mainBotJda.getGuildById(guildId)
                        .updateCommands()
                        .addCommands(slashCommandDatas)
                        .queue();

                    LOG.info("\u001B[33mSynced {} guild commands for shard {} in guild {}\u001B[0m",
                        slashCommandDatas.size(), mainBotJda.getShardInfo().getShardId(), guildId
                    );
                }
            );
        }

        LOG.info("\u001B[31mCommands synced. Ready to go!\u001B[0m");
    }

    public BotCoreBuilder toBuilder() {
        return builder;
    }

    public List<ShardManager> getShardManagers() {
        List<ShardManager> shardManagers = new ArrayList<>();
        shardManagers.add(mainShardManager);
        shardManagers.addAll(subShardManagers);
        return shardManagers;
    }

    public ShardManager getMainShardManager() {
        return mainShardManager;
    }

    public List<ShardManager> getSubShardManagers() {
        return subShardManagers;
    }

    public static List<BotCore> getInstances() {
        return instances;
    }

    public List<String> getSubBotTokens() {
        return subBotTokens;
    }

    public String getMainBotToken() {
        return mainBotToken;
    }

    public List<String> getPrefixes() {
        return prefixes;
    }

    public int getShardsTotal() {
        return shardsTotal;
    }

    public static EventWaiter getEventWaiter() {
        return eventWaiter;
    }
}

package io.github.lynbean.lynbot;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;

import io.github.lynbean.lynbot.core.BotCore;
import io.github.lynbean.lynbot.core.BotCore.BotCoreBuilder;
import io.github.lynbean.lynbot.database.HoconManager;
import io.github.lynbean.lynbot.database.MongoManager;
import lombok.Getter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class Bot {
    private static final Logger LOG = LoggerFactory.getLogger(Bot.class);
    private static final String botConfigPath = "data/LynBot.conf";

    private @Getter static MongoManager mongoManager;
    private static HoconManager hoconManager;

    private static final List<GatewayIntent> GATEWAYS = List.of(
        GatewayIntent.DIRECT_MESSAGES,
        GatewayIntent.DIRECT_MESSAGE_REACTIONS,
        GatewayIntent.DIRECT_MESSAGE_TYPING,
        GatewayIntent.GUILD_EMOJIS_AND_STICKERS,
        GatewayIntent.GUILD_INVITES,
        GatewayIntent.GUILD_MEMBERS,
        GatewayIntent.GUILD_MESSAGES,
        GatewayIntent.GUILD_MESSAGE_REACTIONS,
        GatewayIntent.GUILD_MESSAGE_TYPING,
        GatewayIntent.GUILD_PRESENCES,
        GatewayIntent.GUILD_VOICE_STATES,
        GatewayIntent.GUILD_WEBHOOKS,
        GatewayIntent.MESSAGE_CONTENT,
        GatewayIntent.SCHEDULED_EVENTS
    );

    private static final List<CacheFlag> CACHE_FLAGS = List.of(
        CacheFlag.ACTIVITY,
        CacheFlag.CLIENT_STATUS,
        CacheFlag.EMOJI,
        CacheFlag.FORUM_TAGS,
        CacheFlag.MEMBER_OVERRIDES,
        CacheFlag.ONLINE_STATUS,
        CacheFlag.ROLE_TAGS,
        CacheFlag.SCHEDULED_EVENTS,
        CacheFlag.STICKER,
        CacheFlag.VOICE_STATE
    );

    public static void main(String[] args) {
        createConfigFile();
        hoconManager = new HoconManager(botConfigPath);
        mongoManager = new MongoManager(
            getConfig().getString("database.mongodb.url"),
            getConfig().getString("database.mongodb.database_name")
        );

        mongoManager.addCodecPackages("io.github.lynbean.lynbot.database.pojo");

        new BotCoreBuilder(getConfig().getString("bot.token"))
            .setPrefixes(getConfig().getStringList("bot.prefixes"))
            .appendSubBotTokens(getConfig().getStringList("bot.sub_tokens"))
            .appendGatewayIntents(GATEWAYS)
            .appendCacheFlags(CACHE_FLAGS)
            .setChunkingFilter(ChunkingFilter.ALL)
            .setMemberCachePolicy(MemberCachePolicy.ALL)
            .build();
    }

    private static void createConfigFile() {
        File file = new File(botConfigPath);
        file.getParentFile().mkdirs();
        if (file.exists())
            return;

        LOG.warn("\u001B[31mLynBot.conf not found. Creating one...\u001B[0m");

        try (InputStream in = Bot.class.getClassLoader().getResourceAsStream("reference.conf")) {
            Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            LOG.warn("\u001B[31mLynBot.conf created. Please edit the file and restart the bot.\u001B[0m");
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Config getConfig() {
        return hoconManager.getConfig();
    }
}

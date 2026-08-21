package dev.itsdavlooo.coralduels.bootstrap;

import dev.itsdavlooo.coralduels.service.config.ConfigService;
import dev.itsdavlooo.coralduels.service.message.MessageService;
import dev.itsdavlooo.coralduels.service.database.DatabaseService;
import dev.itsdavlooo.coralduels.service.world.DuelWorldManager;
import dev.itsdavlooo.coralduels.domain.kit.KitManager;
import dev.itsdavlooo.coralduels.domain.player.PlayerStateManager;
import dev.itsdavlooo.coralduels.domain.arena.ArenaManager;
import dev.itsdavlooo.coralduels.domain.reward.RewardManager;
import dev.itsdavlooo.coralduels.domain.statistic.StatisticRepository;

public final class ModuleRegistry {

    private static ConfigService configService;
    private static MessageService messageService;
    private static DatabaseService databaseService;
    private static DuelWorldManager worldManager;
    private static KitManager kitManager;
    private static PlayerStateManager playerStateManager;
    private static ArenaManager arenaManager;
    private static RewardManager rewardManager;
    private static StatisticRepository statisticRepository;

    private ModuleRegistry() {}

    public static void register(ConfigService cs, MessageService ms, DatabaseService ds,
                                DuelWorldManager wm, KitManager km, PlayerStateManager psm,
                                ArenaManager am, RewardManager rm, StatisticRepository sr) {
        configService = cs;
        messageService = ms;
        databaseService = ds;
        worldManager = wm;
        kitManager = km;
        playerStateManager = psm;
        arenaManager = am;
        rewardManager = rm;
        statisticRepository = sr;
    }

    public static ConfigService config() { return configService; }
    public static MessageService messages() { return messageService; }
    public static DatabaseService database() { return databaseService; }
    public static DuelWorldManager world() { return worldManager; }
    public static KitManager kits() { return kitManager; }
    public static PlayerStateManager playerStates() { return playerStateManager; }
    public static ArenaManager arenas() { return arenaManager; }
    public static RewardManager rewards() { return rewardManager; }
    public static StatisticRepository statistics() { return statisticRepository; }
}
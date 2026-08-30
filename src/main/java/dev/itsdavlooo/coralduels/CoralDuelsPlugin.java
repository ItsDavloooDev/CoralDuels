package dev.itsdavlooo.coralduels;

import dev.itsdavlooo.coralduels.bootstrap.ModuleRegistry;
import dev.itsdavlooo.coralduels.command.AdminCommand;
import dev.itsdavlooo.coralduels.command.DuelCommand;
import dev.itsdavlooo.coralduels.command.KitCommand;
import dev.itsdavlooo.coralduels.listener.DuelListener;
import dev.itsdavlooo.coralduels.listener.PlayerListener;
import dev.itsdavlooo.coralduels.listener.WorldListener;
import dev.itsdavlooo.coralduels.service.config.ConfigService;
import dev.itsdavlooo.coralduels.service.database.DatabaseService;
import dev.itsdavlooo.coralduels.service.database.repository.MySQLStatisticRepository;
import dev.itsdavlooo.coralduels.service.database.repository.StatisticRepository;
import dev.itsdavlooo.coralduels.service.message.MessageService;
import dev.itsdavlooo.coralduels.service.world.DuelWorldManager;
import dev.itsdavlooo.coralduels.domain.duel.DuelManager;
import dev.itsdavlooo.coralduels.domain.duel.RequestManager;
import dev.itsdavlooo.coralduels.domain.duel.SessionManager;
import dev.itsdavlooo.coralduels.domain.kit.KitManager;
import dev.itsdavlooo.coralduels.domain.player.PlayerStateManager;
import dev.itsdavlooo.coralduels.domain.arena.ArenaManager;
import dev.itsdavlooo.coralduels.domain.reward.RewardManager;
import dev.itsdavlooo.coralduels.domain.statistic.StatisticManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class CoralDuelsPlugin extends JavaPlugin {

    private static CoralDuelsPlugin instance;

    private ConfigService configService;
    private MessageService messageService;
    private DatabaseService databaseService;
    private DuelWorldManager worldManager;
    private KitManager kitManager;
    private PlayerStateManager playerStateManager;
    private ArenaManager arenaManager;
    private RewardManager rewardManager;
    private StatisticManager statisticManager;
    private RequestManager requestManager;
    private SessionManager sessionManager;
    private DuelManager duelManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("kits.yml", false);
        saveResource("rewards.yml", false);
        saveResource("arenas.yml", false);
        saveResource("leaderboard.yml", false);

        configService = new ConfigService(this);
        messageService = new MessageService(configService);
        databaseService = new DatabaseService(this, configService);
        worldManager = new DuelWorldManager(this, configService);
        kitManager = new KitManager(configService);
        playerStateManager = new PlayerStateManager(this);
        arenaManager = new ArenaManager(configService, this);
        rewardManager = new RewardManager(configService, this);
        StatisticRepository statisticRepository = new MySQLStatisticRepository(databaseService);
        statisticManager = new StatisticManager(statisticRepository);
        requestManager = new RequestManager();
        sessionManager = new SessionManager();
        duelManager = new DuelManager(requestManager, sessionManager,
                playerStateManager, rewardManager, statisticManager, arenaManager);

        ModuleRegistry.register(configService, messageService, databaseService,
                worldManager, kitManager, playerStateManager,
                arenaManager, rewardManager, null);

        worldManager.initialize();

        registerCommands();
        registerListeners();

        getLogger().info("CoralDuels enabled successfully");
    }

    @Override
    public void onDisable() {
        if (duelManager != null) {
            duelManager.stopCleanupTask();
        }
        if (databaseService != null) {
            databaseService.close();
        }
        if (worldManager != null) {
            worldManager.cleanup();
        }
        instance = null;
        getLogger().info("CoralDuels disabled");
    }

    private void registerCommands() {
        getCommand("duel").setExecutor(new DuelCommand(messageService, duelManager, requestManager,
                kitManager, playerStateManager));
        getCommand("dueladmin").setExecutor(new AdminCommand(messageService));
        getCommand("kit").setExecutor(new KitCommand(messageService));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new DuelListener(), this);
        getServer().getPluginManager().registerEvents(new WorldListener(), this);
    }

    public static CoralDuelsPlugin getInstance() {
        return instance;
    }

    public ConfigService getConfigService() { return configService; }
    public MessageService getMessageService() { return messageService; }
    public DatabaseService getDatabaseService() { return databaseService; }
    public DuelWorldManager getWorldManager() { return worldManager; }
    public KitManager getKitManager() { return kitManager; }
    public PlayerStateManager getPlayerStateManager() { return playerStateManager; }
    public ArenaManager getArenaManager() { return arenaManager; }
    public RewardManager getRewardManager() { return rewardManager; }
    public StatisticManager getStatisticManager() { return statisticManager; }
    public DuelManager getDuelManager() { return duelManager; }
    public RequestManager getRequestManager() { return requestManager; }
    public SessionManager getSessionManager() { return sessionManager; }
}
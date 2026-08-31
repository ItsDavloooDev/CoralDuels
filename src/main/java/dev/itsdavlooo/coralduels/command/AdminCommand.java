package dev.itsdavlooo.coralduels.command;

import dev.itsdavlooo.coralduels.CoralDuelsPlugin;
import dev.itsdavlooo.coralduels.domain.duel.DuelManager;
import dev.itsdavlooo.coralduels.domain.kit.KitManager;
import dev.itsdavlooo.coralduels.domain.arena.ArenaManager;
import dev.itsdavlooo.coralduels.domain.reward.RewardManager;
import dev.itsdavlooo.coralduels.service.config.ConfigService;
import dev.itsdavlooo.coralduels.service.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public final class AdminCommand implements CommandExecutor {

    private final MessageService messages;
    private final ConfigService configService;
    private final KitManager kitManager;
    private final ArenaManager arenaManager;
    private final RewardManager rewardManager;
    private final DuelManager duelManager;

    public AdminCommand(MessageService messages) {
        this.messages = messages;
        CoralDuelsPlugin plugin = CoralDuelsPlugin.getInstance();
        this.configService = plugin.getConfigService();
        this.kitManager = plugin.getKitManager();
        this.arenaManager = plugin.getArenaManager();
        this.rewardManager = plugin.getRewardManager();
        this.duelManager = plugin.getDuelManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "no-permission");
            return true;
        }

        if (!player.hasPermission("coralduels.admin")) {
            messages.send(player, "no-permission");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        return switch (subCommand) {
            case "reload" -> handleReload(player, args);
            case "arena" -> handleArena(player, args);
            case "kit" -> handleKit(player, args);
            case "forceend" -> handleForceEnd(player, args);
            case "stats" -> handleStats(player, args);
            default -> {
                sendHelp(player);
                yield true;
            }
        };
    }

    private void sendHelp(Player player) {
        messages.send(player, "admin-help-header");
        messages.send(player, "admin-help-reload");
        messages.send(player, "admin-help-arena");
        messages.send(player, "admin-help-kit");
        messages.send(player, "admin-help-forceend");
        messages.send(player, "admin-help-stats");
    }

    private boolean handleReload(Player player, String[] args) {
        if (args.length == 1) {
            configService.reload();
            kitManager.reload();
            arenaManager.reload();
            rewardManager.reload();
            duelManager.getRequestManager().clear();
            duelManager.getSessionManager().clear();
            duelManager.restartCleanupTask();
            messages.send(player, "reloaded");
        } else {
            String target = args[1].toLowerCase();
            switch (target) {
                case "config" -> {
                    configService.reload("config.yml");
                    messages.send(player, "reloaded-config");
                }
                case "kits" -> {
                    configService.reload("kits.yml");
                    kitManager.reload();
                    messages.send(player, "reloaded-kits");
                }
                case "arenas" -> {
                    configService.reload("arenas.yml");
                    arenaManager.reload();
                    messages.send(player, "reloaded-arenas");
                }
                case "rewards" -> {
                    configService.reload("rewards.yml");
                    rewardManager.reload();
                    messages.send(player, "reloaded-rewards");
                }
                case "messages" -> {
                    configService.reload("messages.yml");
                    messages.send(player, "reloaded-messages");
                }
                default -> messages.send(player, "admin-reload-unknown", Map.of("target", target));
            }
        }
        return true;
    }

    private boolean handleArena(Player player, String[] args) {
        if (args.length < 2) {
            messages.send(player, "admin-arena-usage");
            return true;
        }
        String action = args[1].toLowerCase();
        if (action.equals("list")) {
            arenaManager.getAllArenas().forEach((name, arena) -> 
                player.sendMessage("§e" + name + " §7- §f" + arena.getName() + " §7(" + (arena.isEnabled() ? "§aabilitata" : "§cdisabilitata") + "§7)"));
        }
        return true;
    }

    private boolean handleKit(Player player, String[] args) {
        if (args.length < 2) {
            messages.send(player, "admin-kit-usage");
            return true;
        }
        String action = args[1].toLowerCase();
        if (action.equals("list")) {
            kitManager.getAllKits().forEach((name, kit) -> 
                player.sendMessage("§e" + name + " §7- §f" + kit.getDisplayName() + " §7(" + kit.getPermission() + ")"));
        }
        return true;
    }

    private boolean handleForceEnd(Player player, String[] args) {
        if (args.length < 2) {
            messages.send(player, "admin-forceend-usage");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            messages.send(player, "player-not-found", Map.of("player", args[1]));
            return true;
        }
        if (!duelManager.getPlayerStateManager().isInDuel(target.getUniqueId())) {
            messages.send(player, "admin-not-in-duel", Map.of("player", target.getName()));
            return true;
        }
        duelManager.cancelDuel(target.getUniqueId());
        messages.send(player, "admin-forceend-success", Map.of("player", target.getName()));
        return true;
    }

    private boolean handleStats(Player player, String[] args) {
        if (args.length < 2) {
            messages.send(player, "admin-stats-usage");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            messages.send(player, "player-not-found", Map.of("player", args[1]));
            return true;
        }
        duelManager.getStatisticManager().showStats(player, target.getUniqueId());
        return true;
    }
}
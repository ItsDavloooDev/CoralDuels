package dev.itsdavlooo.coralduels.command;

import dev.itsdavlooo.coralduels.domain.duel.DuelManager;
import dev.itsdavlooo.coralduels.domain.player.PlayerStateManager;
import dev.itsdavlooo.coralduels.service.gui.DuelGuiManager;
import dev.itsdavlooo.coralduels.service.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public final class DuelCommand implements CommandExecutor {

    private final MessageService messages;
    private final DuelManager duelManager;
    private final PlayerStateManager playerStateManager;
    private final DuelGuiManager guiManager;

    public DuelCommand(MessageService messages, DuelManager duelManager,
                       PlayerStateManager playerStateManager, DuelGuiManager guiManager) {
        this.messages = messages;
        this.duelManager = duelManager;
        this.playerStateManager = playerStateManager;
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            messages.send(player, "duel-usage");
            return true;
        }

        String subCommand = args[0].toLowerCase();
        return switch (subCommand) {
            case "challenge", "send" -> handleChallenge(player, args);
            case "accept" -> {
                guiManager.acceptRequest(player);
                yield true;
            }
            case "deny" -> {
                guiManager.denyRequest(player);
                yield true;
            }
            case "leave" -> handleLeave(player);
            case "stats" -> handleStats(player, args);
            case "top", "leaderboard" -> handleLeaderboard(player, args);
            default -> challengePlayer(player, args[0], args.length >= 2 ? args[1] : null);
        };
    }

    private boolean handleChallenge(Player player, String[] args) {
        if (args.length < 2) {
            messages.send(player, "duel-usage");
            return true;
        }
        return challengePlayer(player, args[1], args.length >= 3 ? args[2] : null);
    }

    private boolean challengePlayer(Player player, String targetName, String kitName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            messages.send(player, "player-not-found", Map.of("player", targetName));
            return true;
        }
        if (kitName == null) {
            guiManager.openKitSelection(player, target);
            return true;
        }
        guiManager.sendChallenge(player, target, kitName);
        return true;
    }

    private boolean handleLeave(Player player) {
        if (!playerStateManager.isInDuel(player.getUniqueId())) {
            messages.send(player, "duel-leave");
            return true;
        }
        duelManager.cancelDuel(player.getUniqueId());
        messages.send(player, "duel-leave");
        return true;
    }

    private boolean handleStats(Player player, String[] args) {
        UUID targetUuid = player.getUniqueId();
        if (args.length >= 2) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                messages.send(player, "player-not-found", Map.of("player", args[1]));
                return true;
            }
            targetUuid = target.getUniqueId();
        }
        duelManager.getStatisticManager().showStats(player, targetUuid);
        return true;
    }

    private boolean handleLeaderboard(Player player, String[] args) {
        String category = args.length >= 2 ? args[1].toLowerCase() : "elo";
        duelManager.getStatisticManager().showLeaderboard(player, category);
        return true;
    }
}

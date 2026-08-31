package dev.itsdavlooo.coralduels.command;

import dev.itsdavlooo.coralduels.domain.duel.DuelManager;
import dev.itsdavlooo.coralduels.domain.duel.DuelRequest;
import dev.itsdavlooo.coralduels.domain.duel.RequestManager;
import dev.itsdavlooo.coralduels.domain.kit.Kit;
import dev.itsdavlooo.coralduels.domain.kit.KitManager;
import dev.itsdavlooo.coralduels.domain.player.PlayerStateManager;
import dev.itsdavlooo.coralduels.service.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class DuelCommand implements CommandExecutor {

    private final MessageService messages;
    private final DuelManager duelManager;
    private final RequestManager requestManager;
    private final KitManager kitManager;
    private final PlayerStateManager playerStateManager;

    public DuelCommand(MessageService messages, DuelManager duelManager, RequestManager requestManager,
                       KitManager kitManager, PlayerStateManager playerStateManager) {
        this.messages = messages;
        this.duelManager = duelManager;
        this.requestManager = requestManager;
        this.kitManager = kitManager;
        this.playerStateManager = playerStateManager;
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
            case "accept" -> handleAccept(player);
            case "deny" -> handleDeny(player);
            case "leave" -> handleLeave(player);
            case "stats" -> handleStats(player, args);
            case "top", "leaderboard" -> handleLeaderboard(player, args);
            default -> challengePlayer(player, args[0], args.length >= 2 ? args[1] : "classic");
        };
    }

    private boolean handleChallenge(Player player, String[] args) {
        if (args.length < 2) {
            messages.send(player, "duel-usage");
            return true;
        }
        return challengePlayer(player, args[1], args.length >= 3 ? args[2] : "classic");
    }

    private boolean challengePlayer(Player player, String targetName, String kitName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            messages.send(player, "player-not-found", Map.of("player", targetName));
            return true;
        }

        if (target.equals(player)) {
            messages.send(player, "self-challenge");
            return true;
        }

        if (playerStateManager.isInDuel(player.getUniqueId())) {
            messages.send(player, "you-in-duel");
            return true;
        }

        if (playerStateManager.isInDuel(target.getUniqueId())) {
            messages.send(player, "player-in-duel", Map.of("player", target.getName()));
            return true;
        }

        Optional<Kit> kitOpt = kitManager.getKit(kitName);
        if (kitOpt.isEmpty()) {
            messages.send(player, "kit-not-found", Map.of("kit", kitName));
            return true;
        }

        Kit kit = kitOpt.get();
        if (!kitManager.canUse(player, kit)) {
            messages.send(player, "kit-no-permission", Map.of("kit", kit.getDisplayName()));
            return true;
        }

        if (requestManager.hasPendingRequest(target.getUniqueId())) {
            messages.send(player, "request-expired");
            return true;
        }

        DuelRequest request = duelManager.getRequestManager().addRequest(player.getUniqueId(), target.getUniqueId(), kit);

        messages.send(player, "request-sent", Map.of("target", target.getName()));
        messages.send(target, "request-received", Map.of("challenger", player.getName(), "kit", kit.getDisplayName()));
        return true;
    }

    private boolean handleAccept(Player player) {
        Optional<DuelRequest> requestOpt = requestManager.getRequestByTarget(player.getUniqueId());
        if (requestOpt.isEmpty()) {
            messages.send(player, "request-expired");
            return true;
        }

        DuelRequest request = requestOpt.get();
        Player challenger = Bukkit.getPlayer(request.challenger());
        if (challenger == null || !challenger.isOnline()) {
            requestManager.removeRequest(request.id());
            messages.send(player, "player-offline", Map.of("player", request.challenger().toString()));
            return true;
        }

        if (playerStateManager.isInDuel(player.getUniqueId()) || playerStateManager.isInDuel(challenger.getUniqueId())) {
            requestManager.removeRequest(request.id());
            messages.send(player, "you-in-duel");
            return true;
        }

        requestManager.removeRequest(request.id());
        messages.send(player, "request-accepted", Map.of("challenger", challenger.getName()));
        messages.send(challenger, "target-accepted", Map.of("target", player.getName()));

        duelManager.startDuel(request);
        return true;
    }

    private boolean handleDeny(Player player) {
        Optional<DuelRequest> requestOpt = requestManager.getRequestByTarget(player.getUniqueId());
        if (requestOpt.isEmpty()) {
            messages.send(player, "request-expired");
            return true;
        }

        DuelRequest request = requestOpt.get();
        Player challenger = Bukkit.getPlayer(request.challenger());
        if (challenger != null && challenger.isOnline()) {
            messages.send(challenger, "target-denied", Map.of("target", player.getName()));
        }
        messages.send(player, "request-denied", Map.of("challenger", request.challenger().toString()));
        requestManager.removeRequest(request.id());
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
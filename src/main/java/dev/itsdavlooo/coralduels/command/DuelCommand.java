package dev.itsdavlooo.coralduels.command;

import dev.itsdavlooo.coralduels.CoralDuelsPlugin;
import dev.itsdavlooo.coralduels.service.message.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class DuelCommand implements CommandExecutor {

    private final MessageService messages;

    public DuelCommand(MessageService messages) {
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            messages.send(player, "self-challenge");
            return true;
        }

        String subCommand = args[0].toLowerCase();
        return switch (subCommand) {
            case "send" -> handleChallenge(player, args);
            case "accept" -> handleAccept(player);
            case "deny" -> handleDeny(player);
            case "leave" -> handleLeave(player);
            case "stats" -> handleStats(player, args);
            case "leaderboard" -> handleLeaderboard(player, args);
            default -> {
                messages.send(player, "self-challenge");
                yield true;
            }
        };
    }

    private boolean handleChallenge(Player player, String[] args) {
        messages.send(player, "self-challenge");
        return true;
    }

    private boolean handleAccept(Player player) {
        messages.send(player, "request-expired");
        return true;
    }

    private boolean handleDeny(Player player) {
        messages.send(player, "request-expired");
        return true;
    }

    private boolean handleLeave(Player player) {
        messages.send(player, "duel-leave");
        return true;
    }

    private boolean handleStats(Player player, String[] args) {
        messages.send(player, "stats-header");
        return true;
    }

    private boolean handleLeaderboard(Player player, String[] args) {
        messages.send(player, "leaderboard-header");
        return true;
    }
}
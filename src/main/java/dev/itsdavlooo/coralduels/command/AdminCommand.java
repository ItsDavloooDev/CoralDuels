package dev.itsdavlooo.coralduels.command;

import dev.itsdavlooo.coralduels.service.message.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class AdminCommand implements CommandExecutor {

    private final MessageService messages;

    public AdminCommand(MessageService messages) {
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "no-permission");
            return true;
        }

        if (!player.hasPermission("coralduels.admin.reload")) {
            messages.send(player, "no-permission");
            return true;
        }

        if (args.length == 0) {
            messages.send(player, "reloaded");
            return true;
        }

        String subCommand = args[0].toLowerCase();
        return switch (subCommand) {
            case "reload" -> handleReload(player);
            case "arena" -> handleArena(player, args);
            case "kit" -> handleKit(player, args);
            case "forceend" -> handleForceEnd(player, args);
            default -> {
                messages.send(player, "reloaded");
                yield true;
            }
        };
    }

    private boolean handleReload(Player player) {
        messages.send(player, "reloaded");
        return true;
    }

    private boolean handleArena(Player player, String[] args) {
        messages.send(player, "reloaded");
        return true;
    }

    private boolean handleKit(Player player, String[] args) {
        messages.send(player, "reloaded");
        return true;
    }

    private boolean handleForceEnd(Player player, String[] args) {
        messages.send(player, "reloaded");
        return true;
    }
}
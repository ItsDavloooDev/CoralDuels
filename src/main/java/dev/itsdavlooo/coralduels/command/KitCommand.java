package dev.itsdavlooo.coralduels.command;

import dev.itsdavlooo.coralduels.service.message.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class KitCommand implements CommandExecutor {

    private final MessageService messages;

    public KitCommand(MessageService messages) {
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            messages.send(player, "kit-not-found");
            return true;
        }

        String subCommand = args[0].toLowerCase();
        return switch (subCommand) {
            case "list" -> handleList(player);
            case "select" -> handleSelect(player, args);
            case "preview" -> handlePreview(player, args);
            default -> {
                messages.send(player, "kit-not-found");
                yield true;
            }
        };
    }

    private boolean handleList(Player player) {
        messages.send(player, "kit-not-found");
        return true;
    }

    private boolean handleSelect(Player player, String[] args) {
        messages.send(player, "kit-not-found");
        return true;
    }

    private boolean handlePreview(Player player, String[] args) {
        messages.send(player, "kit-not-found");
        return true;
    }
}
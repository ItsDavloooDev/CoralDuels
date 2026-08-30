package dev.itsdavlooo.coralduels.command;

import dev.itsdavlooo.coralduels.CoralDuelsPlugin;
import dev.itsdavlooo.coralduels.domain.kit.Kit;
import dev.itsdavlooo.coralduels.domain.kit.KitManager;
import dev.itsdavlooo.coralduels.service.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Optional;

public final class KitCommand implements CommandExecutor {

    private final MessageService messages;
    private final KitManager kitManager;

    public KitCommand(MessageService messages) {
        this.messages = messages;
        this.kitManager = CoralDuelsPlugin.getInstance().getKitManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            handleList(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        return switch (subCommand) {
            case "list" -> handleList(player);
            case "select" -> handleSelect(player, args);
            case "preview" -> handlePreview(player, args);
            default -> {
                messages.send(player, "kit-not-found", Map.of("kit", args[0]));
                yield true;
            }
        };
    }

    private boolean handleList(Player player) {
        messages.send(player, "kit-list-header");
        kitManager.getAllKits().forEach((name, kit) -> {
            boolean canUse = kitManager.canUse(player, kit);
            String status = canUse ? "§a[Disponibile]" : "§c[Bloccato]";
            player.sendMessage(" §e" + name + " §7- §f" + kit.getDisplayName() + " " + status);
        });
        return true;
    }

    private boolean handleSelect(Player player, String[] args) {
        if (args.length < 2) {
            messages.send(player, "kit-select-usage");
            return true;
        }
        String kitName = args[1].toLowerCase();
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
        kitManager.giveKit(player, kit);
        messages.send(player, "kit-selected", Map.of("kit", kit.getDisplayName()));
        return true;
    }

    private boolean handlePreview(Player player, String[] args) {
        if (args.length < 2) {
            messages.send(player, "kit-preview-usage");
            return true;
        }
        String kitName = args[1].toLowerCase();
        Optional<Kit> kitOpt = kitManager.getKit(kitName);
        if (kitOpt.isEmpty()) {
            messages.send(player, "kit-not-found", Map.of("kit", kitName));
            return true;
        }
        Kit kit = kitOpt.get();
        messages.send(player, "kit-preview-header", Map.of("kit", kit.getDisplayName()));
        kit.getItems().forEach((slot, item) -> {
            if (item.getType() != Material.AIR) {
                player.sendMessage(" §7Slot " + slot + ": §f" + item.getType().name() + " §7x" + item.getAmount());
            }
        });
        for (int i = 0; i < kit.getArmor().length; i++) {
            ItemStack piece = kit.getArmor()[i];
            if (piece.getType() != Material.AIR) {
                String slotName = switch (i) {
                    case 0 -> "Boots";
                    case 1 -> "Leggings";
                    case 2 -> "Chestplate";
                    case 3 -> "Helmet";
                    default -> "Unknown";
                };
                player.sendMessage(" §7" + slotName + ": §f" + piece.getType().name());
            }
        }
        if (!kit.getEffects().isEmpty()) {
            player.sendMessage(" §7Effetti:");
            kit.getEffects().forEach(e -> player.sendMessage(" §7- " + e.type().getName() + " " + (e.amplifier() + 1) + " (" + e.duration() + " ticks)"));
        }
        return true;
    }
}
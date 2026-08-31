package dev.itsdavlooo.coralduels.service.gui;

import dev.itsdavlooo.coralduels.CoralDuelsPlugin;
import dev.itsdavlooo.coralduels.domain.duel.DuelManager;
import dev.itsdavlooo.coralduels.domain.duel.DuelRequest;
import dev.itsdavlooo.coralduels.domain.duel.RequestManager;
import dev.itsdavlooo.coralduels.domain.kit.Kit;
import dev.itsdavlooo.coralduels.domain.kit.KitManager;
import dev.itsdavlooo.coralduels.domain.player.PlayerStateManager;
import dev.itsdavlooo.coralduels.service.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DuelGuiManager {

    public static final String KIT_SELECT_TITLE = "§8Seleziona un Kit";
    public static final String REQUEST_TITLE_PREFIX = "§8Sfida da ";

    private static final int ACCEPT_SLOT = 2;
    private static final int DENY_SLOT = 6;

    private final MessageService messages;
    private final DuelManager duelManager;
    private final RequestManager requestManager;
    private final KitManager kitManager;
    private final PlayerStateManager playerStateManager;
    private final int requestTimeoutSeconds;

    private final Map<UUID, UUID> kitSelectionTargets = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> kitSelectionKits = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> requestGuis = new ConcurrentHashMap<>();

    public DuelGuiManager(MessageService messages, DuelManager duelManager, RequestManager requestManager,
                          KitManager kitManager, PlayerStateManager playerStateManager) {
        this.messages = messages;
        this.duelManager = duelManager;
        this.requestManager = requestManager;
        this.kitManager = kitManager;
        this.playerStateManager = playerStateManager;
        this.requestTimeoutSeconds = CoralDuelsPlugin.getInstance().getConfigService()
                .getConfig().getInt("timers.request-timeout", 30);
    }

    public void openKitSelection(Player player, Player target) {
        List<String> ordered = new ArrayList<>(kitManager.getAllKits().keySet());
        Collections.sort(ordered);
        kitSelectionTargets.put(player.getUniqueId(), target.getUniqueId());
        kitSelectionKits.put(player.getUniqueId(), ordered);

        int size = Math.max(9, (int) Math.ceil(ordered.size() / 9.0) * 9);
        Inventory inv = Bukkit.createInventory(null, size, KIT_SELECT_TITLE);
        for (int i = 0; i < ordered.size(); i++) {
            Optional<Kit> kitOpt = kitManager.getKit(ordered.get(i));
            if (kitOpt.isEmpty()) continue;
            Kit kit = kitOpt.get();
            ItemStack icon = kit.getIcon().clone();
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add("");
                if (kitManager.canUse(player, kit)) {
                    lore.add("§eClicca per sfidare con questo kit");
                } else {
                    lore.add("§cNon hai il permesso per questo kit");
                }
                meta.setLore(lore);
                icon.setItemMeta(meta);
            }
            inv.setItem(i, icon);
        }
        player.openInventory(inv);
    }

    public void handleKitSelectionClick(Player player, int slot) {
        List<String> kits = kitSelectionKits.get(player.getUniqueId());
        if (kits == null || slot < 0 || slot >= kits.size()) return;
        UUID targetUuid = kitSelectionTargets.remove(player.getUniqueId());
        kitSelectionKits.remove(player.getUniqueId());
        player.closeInventory();
        if (targetUuid == null) return;

        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null) {
            messages.send(player, "player-not-found", Map.of("player", targetUuid.toString()));
            return;
        }
        sendChallenge(player, target, kits.get(slot));
    }

    public void openRequestGui(DuelRequest request) {
        Player target = Bukkit.getPlayer(request.target());
        if (target == null) return;
        Player challenger = Bukkit.getPlayer(request.challenger());
        requestGuis.put(target.getUniqueId(), request.id());

        Inventory inv = Bukkit.createInventory(null, 9,
                REQUEST_TITLE_PREFIX + (challenger != null ? challenger.getName() : "?"));
        inv.setItem(ACCEPT_SLOT, buildItem(Material.GREEN_DYE, "§aAccetta il duello", "§7Clicca per accettare la sfida"));
        inv.setItem(DENY_SLOT, buildItem(Material.RED_DYE, "§cRifiuta il duello", "§7Clicca per rifiutare la sfida"));
        target.openInventory(inv);
    }

    public void handleRequestGuiClick(Player player, int slot) {
        if (slot != ACCEPT_SLOT && slot != DENY_SLOT) return;
        UUID requestId = requestGuis.remove(player.getUniqueId());
        player.closeInventory();
        if (requestId == null) return;
        if (requestManager.getRequest(requestId).isEmpty()) {
            messages.send(player, "request-expired");
            return;
        }
        if (slot == ACCEPT_SLOT) {
            acceptRequest(player);
        } else {
            denyRequest(player);
        }
    }

    public void onInventoryClose(Player player) {
        kitSelectionTargets.remove(player.getUniqueId());
        kitSelectionKits.remove(player.getUniqueId());
        requestGuis.remove(player.getUniqueId());
    }

    public void cleanup(Player player) {
        onInventoryClose(player);
    }

    public boolean sendChallenge(Player player, Player target, String kitName) {
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
        Optional<DuelRequest> existingOpt = requestManager.getRequestByTarget(target.getUniqueId());
        if (existingOpt.isPresent()) {
            DuelRequest existing = existingOpt.get();
            if (existing.isExpired(requestTimeoutSeconds)) {
                requestManager.removeRequest(existing.id());
            } else {
                messages.send(player, "target-has-request", Map.of("player", target.getName()));
                return true;
            }
        }
        DuelRequest request = requestManager.addRequest(player.getUniqueId(), target.getUniqueId(), kit);
        messages.send(player, "request-sent", Map.of("target", target.getName()));
        messages.send(target, "request-received", Map.of("challenger", player.getName(), "kit", kit.getDisplayName()));
        openRequestGui(request);
        return true;
    }

    public void acceptRequest(Player player) {
        Optional<DuelRequest> requestOpt = requestManager.getRequestByTarget(player.getUniqueId());
        if (requestOpt.isEmpty()) {
            messages.send(player, "request-expired");
            return;
        }
        DuelRequest request = requestOpt.get();
        if (request.isExpired(requestTimeoutSeconds)) {
            requestManager.removeRequest(request.id());
            messages.send(player, "request-expired");
            return;
        }
        Player challenger = Bukkit.getPlayer(request.challenger());
        if (challenger == null || !challenger.isOnline()) {
            requestManager.removeRequest(request.id());
            messages.send(player, "player-offline", Map.of("player", request.challenger().toString()));
            return;
        }
        if (playerStateManager.isInDuel(player.getUniqueId()) || playerStateManager.isInDuel(challenger.getUniqueId())) {
            requestManager.removeRequest(request.id());
            messages.send(player, "you-in-duel");
            return;
        }
        requestManager.removeRequest(request.id());
        messages.send(player, "request-accepted", Map.of("challenger", challenger.getName()));
        messages.send(challenger, "target-accepted", Map.of("target", player.getName()));
        duelManager.startDuel(request);
    }

    public void denyRequest(Player player) {
        Optional<DuelRequest> requestOpt = requestManager.getRequestByTarget(player.getUniqueId());
        if (requestOpt.isEmpty()) {
            messages.send(player, "request-expired");
            return;
        }
        DuelRequest request = requestOpt.get();
        Player challenger = Bukkit.getPlayer(request.challenger());
        if (challenger != null && challenger.isOnline()) {
            messages.send(challenger, "target-denied", Map.of("target", player.getName()));
        }
        messages.send(player, "request-denied",
                Map.of("challenger", challenger != null ? challenger.getName() : request.challenger().toString()));
        requestManager.removeRequest(request.id());
    }

    private ItemStack buildItem(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}

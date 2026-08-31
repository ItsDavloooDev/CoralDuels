package dev.itsdavlooo.coralduels.listener;

import dev.itsdavlooo.coralduels.CoralDuelsPlugin;
import dev.itsdavlooo.coralduels.service.gui.DuelGuiManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

public final class DuelGuiListener implements Listener {

    private final DuelGuiManager guiManager;

    public DuelGuiListener() {
        this.guiManager = CoralDuelsPlugin.getInstance().getDuelGuiManager();
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory clicked = event.getClickedInventory();
        if (clicked == null) return;

        if (event.getView().getTitle().equals(DuelGuiManager.KIT_SELECT_TITLE)) {
            event.setCancelled(true);
            guiManager.handleKitSelectionClick(player, event.getSlot());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        guiManager.onInventoryClose(player);
    }
}

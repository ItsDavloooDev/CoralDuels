package dev.itsdavlooo.coralduels.listener;

import dev.itsdavlooo.coralduels.service.gui.LeaderboardGuiManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class LeaderboardGuiListener implements Listener {

    private final LeaderboardGuiManager guiManager;

    public LeaderboardGuiListener(LeaderboardGuiManager guiManager) {
        this.guiManager = guiManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        String title = event.getView().getTitle();
        if (title.equals(LeaderboardGuiManager.MAIN_TITLE)) {
            event.setCancelled(true);
            guiManager.handleMainClick(player, event.getSlot());
        } else if (title.startsWith(LeaderboardGuiManager.CATEGORY_TITLE_PREFIX)) {
            event.setCancelled(true);
            guiManager.handleCategoryClick(player, event.getSlot());
        }
    }
}

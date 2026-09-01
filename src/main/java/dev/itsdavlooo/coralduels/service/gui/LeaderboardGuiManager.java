package dev.itsdavlooo.coralduels.service.gui;

import dev.itsdavlooo.coralduels.CoralDuelsPlugin;
import dev.itsdavlooo.coralduels.domain.statistic.LeaderboardEntry;
import dev.itsdavlooo.coralduels.domain.statistic.StatisticManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

public final class LeaderboardGuiManager {

    public static final String MAIN_TITLE = "§8⛁ §lClassifiche";
    public static final String CATEGORY_TITLE_PREFIX = "§8⛁ §lTop ";

    private static final int MAIN_SIZE = 27;
    private static final int CATEGORY_PAGE_SIZE = 36;
    private static final int RANKING_LIMIT = 10;
    private static final int MAIN_CATEGORY_START = 10;
    private static final int BACK_SLOT = 31;
    private static final int HEAD_SLOT_ROW_2_START = 19;

    private record Category(String key, String displayName, Material material, String color, Material pane) {}

    private static final List<Category> CATEGORIES = List.of(
            new Category("elo", "ELO", Material.DIAMOND, "§b", Material.LIGHT_BLUE_STAINED_GLASS_PANE),
            new Category("wins", "Vittorie", Material.GOLD_INGOT, "§6", Material.YELLOW_STAINED_GLASS_PANE),
            new Category("losses", "Sconfitte", Material.REDSTONE, "§c", Material.RED_STAINED_GLASS_PANE),
            new Category("played", "Giocate", Material.CLOCK, "§e", Material.LIME_STAINED_GLASS_PANE),
            new Category("streak", "Striscia", Material.BLAZE_POWDER, "§d", Material.MAGENTA_STAINED_GLASS_PANE),
            new Category("kills", "Uccisioni", Material.IRON_SWORD, "§a", Material.GREEN_STAINED_GLASS_PANE),
            new Category("deaths", "Morti", Material.SKELETON_SKULL, "§7", Material.GRAY_STAINED_GLASS_PANE)
    );

    private final StatisticManager statisticManager;

    public LeaderboardGuiManager(StatisticManager statisticManager) {
        this.statisticManager = statisticManager;
    }

    public void openMain(Player player) {
        Inventory inv = Bukkit.createInventory(null, MAIN_SIZE, MAIN_TITLE);

        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, pane(Material.BLACK_STAINED_GLASS_PANE));
        }

        for (int i = 0; i < CATEGORIES.size(); i++) {
            inv.setItem(i + 1, pane(CATEGORIES.get(i).pane()));
        }

        ItemStack title = new ItemStack(Material.COMPASS);
        ItemMeta titleMeta = title.getItemMeta();
        if (titleMeta != null) {
            titleMeta.setDisplayName("§b⛁ §lClassifiche");
            titleMeta.setLore(List.of(
                    "§7Scegli una categoria per vedere",
                    "§7i migliori duellanti del server"
            ));
            title.setItemMeta(titleMeta);
        }
        inv.setItem(4, title);

        for (int i = 0; i < CATEGORIES.size(); i++) {
            inv.setItem(MAIN_CATEGORY_START + i, categoryItem(CATEGORIES.get(i)));
        }

        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§7Come funziona");
            infoMeta.setLore(List.of(
                    "§7Clicca su una categoria per",
                    "§7aprire la classifica corrispondente"
            ));
            info.setItemMeta(infoMeta);
        }
        inv.setItem(22, info);

        player.openInventory(inv);
    }

    public void openCategory(Player player, String key) {
        Category category = getCategory(key);
        if (category == null) {
            return;
        }
        String title = CATEGORY_TITLE_PREFIX + category.displayName();
        CoralDuelsPlugin plugin = CoralDuelsPlugin.getInstance();
        statisticManager.getLeaderboard(category.key(), RANKING_LIMIT)
                .thenAcceptAsync(entries -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    Inventory inv = Bukkit.createInventory(null, CATEGORY_PAGE_SIZE, title);

                    for (int i = 0; i < inv.getSize(); i++) {
                        inv.setItem(i, pane(Material.BLACK_STAINED_GLASS_PANE));
                    }

                    for (int i = 0; i < 9; i++) {
                        inv.setItem(i, pane(category.pane()));
                        inv.setItem(i + 27, pane(category.pane()));
                    }
                    inv.setItem(9, pane(category.pane()));
                    inv.setItem(17, pane(category.pane()));
                    inv.setItem(18, pane(category.pane()));
                    inv.setItem(26, pane(category.pane()));

                    ItemStack header = new ItemStack(category.material());
                    ItemMeta headerMeta = header.getItemMeta();
                    if (headerMeta != null) {
                        headerMeta.setDisplayName(category.color() + "§l" + category.displayName());
                        headerMeta.setLore(List.of("§7I 10 migliori giocatori per " + category.displayName().toLowerCase()));
                        header.setItemMeta(headerMeta);
                    }
                    inv.setItem(4, header);

                    if (entries.isEmpty()) {
                        inv.setItem(13, emptyItem());
                    } else {
                        int slot = 0;
                        for (LeaderboardEntry entry : entries) {
                            if (slot >= RANKING_LIMIT) {
                                break;
                            }
                            int targetSlot;
                            if (slot < 7) {
                                targetSlot = 10 + slot;
                            } else {
                                targetSlot = HEAD_SLOT_ROW_2_START + (slot - 7);
                            }
                            inv.setItem(targetSlot, playerHead(entry, category));
                            slot++;
                        }
                    }

                    inv.setItem(BACK_SLOT, backItem());
                    player.openInventory(inv);
                }, runnable -> plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> runnable.run()));
    }

    public void handleMainClick(Player player, int slot) {
        int index = slot - MAIN_CATEGORY_START;
        if (index >= 0 && index < CATEGORIES.size()) {
            openCategory(player, CATEGORIES.get(index).key());
        }
    }

    public void handleCategoryClick(Player player, int slot) {
        if (slot == BACK_SLOT) {
            openMain(player);
        }
    }

    private Category getCategory(String key) {
        return CATEGORIES.stream()
                .filter(c -> c.key().equalsIgnoreCase(key))
                .findFirst()
                .orElse(null);
    }

    private ItemStack categoryItem(Category category) {
        ItemStack item = new ItemStack(category.material());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(category.color() + "§l" + category.displayName());
            meta.setLore(List.of(
                    "§7Clicca per vedere la classifica",
                    "§7per " + category.displayName().toLowerCase()
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack playerHead(LeaderboardEntry entry, Category category) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(entry.username()));
            meta.setDisplayName(positionColor(entry.position()) + "#" + entry.position() + " §f" + entry.username());
            meta.setLore(List.of(statLine(category, entry)));
            head.setItemMeta(meta);
        }
        return head;
    }

    private String statLine(Category category, LeaderboardEntry entry) {
        String valueColor = category.color().equals("§8") ? "§f" : category.color();
        return switch (category.key()) {
            case "elo" -> "§7ELO: " + valueColor + entry.elo();
            case "wins" -> "§7Vittorie: " + valueColor + entry.wins();
            case "losses" -> "§7Sconfitte: " + valueColor + entry.losses();
            case "played" -> "§7Giocate: " + valueColor + entry.played();
            case "streak" -> "§7Striscia: " + valueColor + entry.streak();
            case "kills" -> "§7Uccisioni: " + valueColor + entry.kills();
            case "deaths" -> "§7Morti: " + valueColor + entry.deaths();
            default -> "§7ELO: " + valueColor + entry.elo();
        };
    }

    private String positionColor(int position) {
        return switch (position) {
            case 1 -> "§6";
            case 2 -> "§7";
            case 3 -> "§c";
            default -> "§8";
        };
    }

    private ItemStack emptyItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cNessun dato disponibile");
            meta.setLore(List.of("§7Gioca qualche duello per", "§7comparire in questa classifica"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack backItem() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c← Torna alle classifiche");
            meta.setLore(List.of("§7Torna al menu principale"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack pane(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        return pane;
    }
}

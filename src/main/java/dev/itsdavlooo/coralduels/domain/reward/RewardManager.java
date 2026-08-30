package dev.itsdavlooo.coralduels.domain.reward;

import dev.itsdavlooo.coralduels.CoralDuelsPlugin;
import dev.itsdavlooo.coralduels.service.config.ConfigService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

public final class RewardManager {

    private final ConfigService config;
    private final CoralDuelsPlugin plugin;
    private final Map<String, List<Reward>> rewards = new HashMap<>();
    private final Random random = new Random();
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%(\\w+)%");
    private static final java.util.Set<String> ALLOWED_PLACEHOLDERS = java.util.Set.of("player", "uuid", "kit_id");

    public RewardManager(ConfigService config, CoralDuelsPlugin plugin) {
        this.config = config;
        this.plugin = plugin;
        loadRewards();
    }

    private void loadRewards() {
        rewards.clear();
        ConfigurationSection section = config.getRewards().getConfigurationSection("rewards");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                if (!key.equals("enabled")) {
                    List<?> list = section.getList(key);
                    if (list != null) {
                        rewards.put(key, parseRewards(list));
                    }
                }
            }
        }
    }

    private List<Reward> parseRewards(List<?> list) {
        if (list == null) return List.of();
        return list.stream()
                .filter(o -> o instanceof Map)
                .map(o -> (Map<String, Object>) o)
                .map(map -> {
                    RewardType type = RewardType.valueOf(((String) map.getOrDefault("type", "COMMAND")).toUpperCase());
                    int weight = ((Number) map.getOrDefault("weight", 100)).intValue();
                    String value = (String) map.getOrDefault("value", "");
                    String message = (String) map.getOrDefault("message", "");
                    boolean silent = (Boolean) map.getOrDefault("silent", false);
                    return new Reward(type, weight, value, message, silent);
                })
                .toList();
    }

    public void executeRewards(Player player, String category) {
        List<Reward> categoryRewards = rewards.get(category);
        if (categoryRewards == null || categoryRewards.isEmpty()) return;

        int totalWeight = categoryRewards.stream().mapToInt(Reward::weight).sum();
        int roll = random.nextInt(totalWeight);
        int current = 0;

        for (Reward reward : categoryRewards) {
            current += reward.weight();
            if (roll < current) {
                executeReward(player, reward);
                break;
            }
        }
    }

    public void executeWinRewards(Player player, String kitName) {
        executeRewards(player, "win");
        executeRewards(player, "kit." + kitName.toLowerCase());
    }

    public void executeLossRewards(Player player) {
        executeRewards(player, "loss");
    }

    public void executeDrawRewards(Player player) {
        executeRewards(player, "draw");
    }

    public void giveRewards(Player player, String kitName) {
        executeRewards(player, "win");
        executeRewards(player, "kit." + kitName.toLowerCase());
    }

    private void executeReward(Player player, Reward reward) {
        String parsedValue = sanitizeValue(reward.value(), player);
        String parsedMessage = sanitizeValue(reward.message(), player);

        switch (reward.type()) {
            case COMMAND -> plugin.getServer().getGlobalRegionScheduler().run(plugin, task ->
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), parsedValue));
            case ITEM -> giveItemReward(player, parsedValue);
            case MONEY -> giveMoneyReward(player, parsedValue);
            case EXPERIENCE -> player.giveExp(Integer.parseInt(parsedValue));
            case PERMISSION -> givePermissionReward(player, parsedValue);
        }

        if (!reward.silent() && !parsedMessage.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', parsedMessage));
        }
    }

    private String sanitizeValue(String value, Player player) {
        return PLACEHOLDER_PATTERN.matcher(value).replaceAll(match -> {
            String placeholder = match.group(1);
            return ALLOWED_PLACEHOLDERS.contains(placeholder) ? switch (placeholder) {
                case "player" -> player.getName();
                case "uuid" -> player.getUniqueId().toString();
                case "kit_id" -> "";
                default -> "";
            } : match.group(0);
        });
    }

    private void giveItemReward(Player player, String value) {
        String[] parts = value.split(":");
        Material material = Material.getMaterial(parts[0].toUpperCase());
        if (material == null) return;
        int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
        ItemStack item = new ItemStack(material, Math.min(amount, 64));
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover.values().iterator().next());
        }
    }

    private void giveMoneyReward(Player player, String value) {
        String cmd = "eco give " + player.getName() + " " + value;
        plugin.getServer().getGlobalRegionScheduler().run(plugin, task ->
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd));
    }

    private void givePermissionReward(Player player, String value) {
        String[] parts = value.split(":");
        if (parts.length >= 2) {
            String cmd = "lp user " + player.getName() + " permission settemp " + parts[0] + " " + parts[1];
            plugin.getServer().getGlobalRegionScheduler().run(plugin, task ->
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd));
        }
    }

    public void reload() {
        loadRewards();
    }
}
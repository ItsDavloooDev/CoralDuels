package dev.itsdavlooo.coralduels.domain.reward;

import dev.itsdavlooo.coralduels.CoralDuelsPlugin;
import dev.itsdavlooo.coralduels.service.config.ConfigService;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Random;

public final class RewardManager {

    private final ConfigService config;
    private final CoralDuelsPlugin plugin;
    private final Map<String, List<Reward>> rewards = new HashMap<>();
    private final Random random = new Random();

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
                .map(o -> (Map<?, ?>) o)
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

    private void executeReward(Player player, Reward reward) {
        String parsedValue = reward.value()
                .replace("%player%", player.getName())
                .replace("%uuid%", player.getUniqueId().toString());

        switch (reward.type()) {
            case COMMAND -> plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), parsedValue);
            case ITEM -> {
            }
            case MONEY -> {
            }
            case EXPERIENCE -> player.giveExp(Integer.parseInt(parsedValue));
            case PERMISSION -> {
            }
        }

        if (!reward.silent() && !reward.message().isEmpty()) {
            player.sendMessage(net.kyori.adventure.text.Component.text(reward.message())
                    .replaceText(b -> b.match('&').replacement("§")));
        }
    }

    public void reload() {
        loadRewards();
    }
}
package dev.itsdavlooo.coralduels.service.message;

import dev.itsdavlooo.coralduels.CoralDuelsPlugin;
import dev.itsdavlooo.coralduels.service.config.ConfigService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class MessageService {

    private final ConfigService config;
    private final Pattern placeholderPattern = Pattern.compile("%(\\w+)%");

    public MessageService(ConfigService config) {
        this.config = config;
    }

    public String get(String key) {
        String msg = config.getMessages().getString(key, "Missing message: " + key);
        return colorize(msg);
    }

    public String get(String key, Map<String, String> placeholders) {
        String msg = get(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            msg = msg.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return msg;
    }

    public void send(CommandSender sender, String key) {
        sender.sendMessage(get(key));
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(get(key, placeholders));
    }

    public void send(Player player, String key) {
        player.sendMessage(get(key));
    }

    public void send(Player player, String key, Map<String, String> placeholders) {
        player.sendMessage(get(key, placeholders));
    }

    public void broadcast(String key) {
        broadcast(key, Map.of());
    }

    public void broadcast(String key, Map<String, String> placeholders) {
        String msg = get(key, placeholders);
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(msg));
    }

    public List<String> getList(String key) {
        return config.getMessages().getStringList(key).stream()
                .map(this::colorize)
                .toList();
    }

    public String getPrefix() {
        return get("prefix");
    }

    private String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
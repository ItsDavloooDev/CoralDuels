package dev.itsdavlooo.coralduels.command;

import dev.itsdavlooo.coralduels.service.gui.LeaderboardGuiManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class LeaderboardCommand implements CommandExecutor {

    private final LeaderboardGuiManager guiManager;

    public LeaderboardCommand(LeaderboardGuiManager guiManager) {
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cSolo i giocatori possono usare questo comando.");
            return true;
        }
        guiManager.openMain(player);
        return true;
    }
}

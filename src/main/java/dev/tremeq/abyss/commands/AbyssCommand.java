package dev.tremeq.abyss.commands;

import dev.tremeq.abyss.Abyss;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Komenda do otwierania GUI Otchłani
 */
public class AbyssCommand implements CommandExecutor, TabCompleter {
    private final Abyss plugin;

    public AbyssCommand(Abyss plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Sprawdź, czy to gracz
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.only-players"));
            return true;
        }

        // Sprawdź permisje
        if (!player.hasPermission("devotchlan.use")) {
            player.sendMessage(plugin.getMessageManager().getMessage("commands.no-permission"));
            return true;
        }

        // Sprawdź czy okno czasowe jest otwarte
        if (!plugin.isAbyssWindowOpen()) {
            player.sendMessage(plugin.getMessageManager().getMessage("commands.window-closed"));
            return true;
        }

        // Otwórz GUI
        plugin.getAbyssGUI().openGUI(player);
        player.sendMessage(plugin.getMessageManager().getMessage("commands.gui-opened"));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return new ArrayList<>();
    }
}

package dev.tremeq.abyss.commands;

import dev.tremeq.abyss.Abyss;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

/**
 * Komenda do przeładowania konfiguracji pluginu
 */
public class AbyssReloadCommand implements CommandExecutor, TabCompleter {
    private final Abyss plugin;

    public AbyssReloadCommand(Abyss plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Sprawdź permisje
        if (!sender.hasPermission("devotchlan.reload")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.no-permission"));
            return true;
        }

        try {
            // Przeładuj konfigurację
            plugin.reloadConfig();
            plugin.getMessageManager().loadMessages();

            sender.sendMessage(plugin.getMessageManager().getMessage("commands.reload-success"));
            plugin.getLogger().info("Konfiguracja została przeładowana przez " + sender.getName());
        } catch (Exception e) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.reload-error"));
            plugin.getLogger().severe("Błąd podczas przeładowywania konfiguracji: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return new ArrayList<>();
    }
}

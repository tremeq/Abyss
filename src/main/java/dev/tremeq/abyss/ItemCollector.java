package dev.tremeq.abyss;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Zbiera przedmioty z ziemi i dodaje je do magazynu Otchłani
 */
public class ItemCollector extends BukkitRunnable {
    private final Abyss plugin;

    public ItemCollector(Abyss plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        collectItemsNow();
    }

    /**
     * Natychmiast zbiera itemy z ziemi (może być wywołane bezpośrednio)
     */
    public void collectItemsNow() {
        if (!plugin.getConfig().getBoolean("item-collection.enabled", true)) {
            return;
        }

        int collectedCount = 0;
        List<ItemStack> collectedItems = new ArrayList<>();
        List<String> worldBlacklist = plugin.getConfig().getStringList("item-collection.world-blacklist");

        // Iteruj przez wszystkie światy
        for (World world : Bukkit.getWorlds()) {
            // Sprawdź, czy świat jest na blackliście
            if (worldBlacklist.contains(world.getName())) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("Pomijam świat z blacklisty: " + world.getName());
                }
                continue;
            }

            // Zbierz wszystkie entity Item
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Item item) {
                    ItemStack itemStack = item.getItemStack();

                    if (itemStack != null && !itemStack.getType().isAir()) {
                        // Dodaj do listy zebranych itemów
                        collectedItems.add(itemStack.clone());
                        collectedCount += itemStack.getAmount();

                        // Usuń item z ziemi
                        entity.remove();
                    }
                }
            }
        }

        // Jeśli coś zebrano, dodaj do magazynu
        if (!collectedItems.isEmpty()) {
            plugin.getAbyssManager().addItems(collectedItems);

            // Odśwież GUI dla wszystkich graczy
            plugin.getAbyssGUI().refreshAllViewers();

            // Powiadom graczy, jeśli włączone
            if (plugin.getConfig().getBoolean("item-collection.notify-players", true)) {
                notifyPlayers(collectedCount);
            }

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("Zebrano " + collectedItems.size() + " stosów przedmiotów (" + collectedCount + " itemów)");
            }
        }
    }

    /**
     * Powiadamia wszystkich graczy online o zebranych itemach
     */
    private void notifyPlayers(int amount) {
        String message = plugin.getMessageManager().getMessage("item-collection.items-collected",
                "amount", String.valueOf(amount));

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
    }

    /**
     * Uruchamia task zbierania itemów
     */
    public void start() {
        int interval = plugin.getConfig().getInt("item-collection.interval", 60);
        // Konwertuj sekundy na ticki (20 ticków = 1 sekunda)
        long intervalTicks = interval * 20L;

        // Uruchom task co określony interwał
        this.runTaskTimer(plugin, intervalTicks, intervalTicks);

        plugin.getLogger().info("ItemCollector uruchomiony (interwał: " + interval + " sekund)");
    }
}

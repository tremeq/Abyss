package dev.tremeq.abyss;

import dev.tremeq.abyss.commands.AbyssCommand;
import dev.tremeq.abyss.commands.AbyssReloadCommand;
import dev.tremeq.abyss.listeners.InventoryListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * Główna klasa pluginu Abyss / Otchłań
 * @author tremeq
 */
public class Abyss extends JavaPlugin {
    private MessageManager messageManager;
    private AbyssManager abyssManager;
    private AbyssGUI abyssGUI;
    private ItemCollector itemCollector;
    private BukkitTask autoOpenTask;
    private BukkitTask autoCloseTask;

    @Override
    public void onEnable() {
        // Logo
        getLogger().info("==================================");
        getLogger().info("  Abyss / Otchłań");
        getLogger().info("  Autor: tremeq");
        getLogger().info("  Wersja: " + getDescription().getVersion());
        getLogger().info("==================================");

        // Załaduj konfigurację
        saveDefaultConfig();

        // Inicjalizuj menedżery
        this.messageManager = new MessageManager(this);
        this.abyssManager = new AbyssManager(this);
        this.abyssGUI = new AbyssGUI(this);

        // Zarejestruj komendy
        registerCommands();

        // Zarejestruj listenery
        registerListeners();

        // Uruchom ItemCollector
        if (getConfig().getBoolean("item-collection.enabled", true)) {
            this.itemCollector = new ItemCollector(this);
            this.itemCollector.start();
        }

        // Uruchom auto-otwieranie GUI (domyślnie wyłączone - GUI otwiera się przez komendy)
        if (getConfig().getBoolean("auto-open.enabled", false)) {
            startAutoOpen();
        }

        getLogger().info("Plugin został pomyślnie załadowany!");
    }

    @Override
    public void onDisable() {
        // Anuluj taski
        if (autoOpenTask != null) {
            autoOpenTask.cancel();
        }
        if (autoCloseTask != null) {
            autoCloseTask.cancel();
        }
        if (itemCollector != null) {
            itemCollector.cancel();
        }

        // Zamknij wszystkie GUI
        if (abyssGUI != null) {
            abyssGUI.closeAllGUIs();
        }

        getLogger().info("Plugin został wyłączony!");
    }

    /**
     * Rejestruje komendy
     */
    private void registerCommands() {
        getCommand("abyss").setExecutor(new AbyssCommand(this));
        getCommand("otchlan").setExecutor(new AbyssCommand(this));
        getCommand("abyssreload").setExecutor(new AbyssReloadCommand(this));
    }

    /**
     * Rejestruje listenery
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
    }

    /**
     * Uruchamia automatyczne otwieranie GUI
     */
    private void startAutoOpen() {
        int interval = getConfig().getInt("auto-open.interval", 300);
        int duration = getConfig().getInt("auto-open.duration", 10);

        // Konwertuj sekundy na ticki
        long intervalTicks = interval * 20L;
        long durationTicks = duration * 20L;

        autoOpenTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Otwórz GUI dla wszystkich graczy online
                for (Player player : Bukkit.getOnlinePlayers()) {
                    abyssGUI.openGUI(player);
                    player.sendMessage(messageManager.getMessage("gui.auto-opened"));
                }

                // Zaplanuj auto-zamknięcie
                scheduleAutoClose(durationTicks);
            }
        }.runTaskTimer(this, intervalTicks, intervalTicks);

        getLogger().info("Auto-otwieranie GUI uruchomione (interwał: " + interval + "s, czas otwarcia: " + duration + "s)");
    }

    /**
     * Planuje automatyczne zamknięcie GUI
     */
    private void scheduleAutoClose(long durationTicks) {
        if (autoCloseTask != null) {
            autoCloseTask.cancel();
        }

        // Odliczanie przed zamknięciem
        autoCloseTask = new BukkitRunnable() {
            int secondsLeft = (int) (durationTicks / 20);

            @Override
            public void run() {
                if (secondsLeft <= 0) {
                    // Zamknij GUI dla wszystkich
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (abyssGUI.hasOpenGUI(player)) {
                            abyssGUI.closeGUI(player);
                        }
                    }
                    cancel();
                } else if (secondsLeft <= 5) {
                    // Powiadom graczy o zamknięciu w ostatnich 5 sekundach
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (abyssGUI.hasOpenGUI(player)) {
                            player.sendMessage(messageManager.getMessage("gui.auto-closing",
                                    "seconds", String.valueOf(secondsLeft)));
                        }
                    }
                    secondsLeft--;
                } else {
                    secondsLeft--;
                }
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    /**
     * Przeładowuje plugin (używane przez komendę reload)
     */
    @Override
    public void reloadConfig() {
        super.reloadConfig();

        // Przeładuj menedżer wiadomości
        if (messageManager != null) {
            messageManager.loadMessages();
        }

        // Restart auto-otwierania jeśli włączone
        if (autoOpenTask != null) {
            autoOpenTask.cancel();
        }

        if (getConfig().getBoolean("auto-open.enabled", false)) {
            startAutoOpen();
        }

        // Restart ItemCollector jeśli włączony
        if (itemCollector != null) {
            itemCollector.cancel();
        }

        if (getConfig().getBoolean("item-collection.enabled", true)) {
            this.itemCollector = new ItemCollector(this);
            this.itemCollector.start();
        }
    }

    // Gettery
    public MessageManager getMessageManager() {
        return messageManager;
    }

    public AbyssManager getAbyssManager() {
        return abyssManager;
    }

    public AbyssGUI getAbyssGUI() {
        return abyssGUI;
    }
}

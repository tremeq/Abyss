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
    private boolean abyssWindowOpen = false;

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

        // Uruchom system okien czasowych (domyślnie wyłączony)
        if (getConfig().getBoolean("auto-open.enabled", false)) {
            startAutoOpen();
        } else {
            // Jeśli auto-open wyłączony, okno jest zawsze otwarte
            abyssWindowOpen = true;
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
        // Użyj jednej instancji dla obu komend (optymalizacja pamięci)
        AbyssCommand abyssCommand = new AbyssCommand(this);
        getCommand("abyss").setExecutor(abyssCommand);
        getCommand("otchlan").setExecutor(abyssCommand);

        getCommand("abyssreload").setExecutor(new AbyssReloadCommand(this));
    }

    /**
     * Rejestruje listenery
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
    }

    /**
     * Uruchamia system okien czasowych
     */
    private void startAutoOpen() {
        int interval = getConfig().getInt("auto-open.interval", 300);
        int duration = getConfig().getInt("auto-open.duration", 30);

        // Konwertuj sekundy na ticki
        long intervalTicks = interval * 20L;
        long durationTicks = duration * 20L;

        autoOpenTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Otwórz okno czasowe
                abyssWindowOpen = true;

                // Powiadom wszystkich graczy że okno jest otwarte
                String message = messageManager.getMessage("window.opened", "duration", String.valueOf(duration));
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(message);
                }

                getLogger().info("Okno Otchłani OTWARTE - gracze mogą używać komendy przez " + duration + " sekund");

                // Zaplanuj zamknięcie okna
                scheduleWindowClose(durationTicks);
            }
        }.runTaskTimer(this, intervalTicks, intervalTicks);

        getLogger().info("System okien czasowych uruchomiony (interwał: " + interval + "s, czas otwarcia: " + duration + "s)");
    }

    /**
     * Planuje zamknięcie okna czasowego
     */
    private void scheduleWindowClose(long durationTicks) {
        if (autoCloseTask != null) {
            autoCloseTask.cancel();
        }

        // Odliczanie przed zamknięciem okna
        autoCloseTask = new BukkitRunnable() {
            int secondsLeft = (int) (durationTicks / 20);

            @Override
            public void run() {
                if (secondsLeft <= 0) {
                    // Zamknij okno czasowe
                    abyssWindowOpen = false;

                    // Zamknij wszystkie otwarte GUI
                    abyssGUI.closeAllGUIs();

                    // Powiadom graczy
                    String message = messageManager.getMessage("window.closed");
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendMessage(message);
                    }

                    getLogger().info("Okno Otchłani ZAMKNIĘTE - komendy niedostępne");
                    cancel();
                } else if (secondsLeft <= 10 && secondsLeft % 5 == 0) {
                    // Powiadom graczy co 5 sekund w ostatnich 10 sekundach
                    String message = messageManager.getMessage("window.closing", "seconds", String.valueOf(secondsLeft));
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendMessage(message);
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

    /**
     * Sprawdza czy okno czasowe jest otwarte
     */
    public boolean isAbyssWindowOpen() {
        return abyssWindowOpen;
    }
}

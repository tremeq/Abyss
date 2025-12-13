package dev.tremeq.abyss;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Zarządza GUI Otchłani z paginacją i synchronizacją między graczami
 */
public class AbyssGUI implements InventoryHolder {
    private final Abyss plugin;
    private final Map<UUID, Integer> playerPages;
    private final Map<UUID, Inventory> playerInventories;
    private final Map<UUID, BukkitTask> playerCloseTasks;
    private final int size;
    private final int itemsPerPage;

    public AbyssGUI(Abyss plugin) {
        this.plugin = plugin;
        this.playerPages = new HashMap<>();
        this.playerInventories = new HashMap<>();
        this.playerCloseTasks = new HashMap<>();
        this.size = plugin.getConfig().getInt("gui.size", 54);
        // Ostatnia linia (9 slotów) to nawigacja
        this.itemsPerPage = size - 9;
    }

    /**
     * Otwiera GUI dla gracza
     */
    public void openGUI(Player player) {
        openGUI(player, 0);
    }

    /**
     * Otwiera GUI dla gracza na określonej stronie
     */
    public void openGUI(Player player, int page) {
        // Ustaw bieżącą stronę gracza
        playerPages.put(player.getUniqueId(), page);

        // Stwórz lub zaktualizuj inventory
        Inventory inventory = createInventory(player, page);
        playerInventories.put(player.getUniqueId(), inventory);

        // Otwórz GUI
        player.openInventory(inventory);

        // Zaplanuj automatyczne zamknięcie po określonym czasie
        scheduleAutoCloseForPlayer(player);

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("Otwarto GUI dla gracza " + player.getName() + " na stronie " + page);
        }
    }

    /**
     * Planuje automatyczne zamknięcie GUI dla konkretnego gracza
     */
    private void scheduleAutoCloseForPlayer(Player player) {
        // Pobierz czas otwarcia z configu (w sekundach)
        int duration = plugin.getConfig().getInt("auto-open.duration", 10);

        // Anuluj poprzedni task jeśli istnieje
        BukkitTask existingTask = playerCloseTasks.get(player.getUniqueId());
        if (existingTask != null) {
            existingTask.cancel();
        }

        // Stwórz nowy task zamykania
        BukkitTask closeTask = new BukkitRunnable() {
            int secondsLeft = duration;

            @Override
            public void run() {
                if (!player.isOnline() || !hasOpenGUI(player)) {
                    cancel();
                    playerCloseTasks.remove(player.getUniqueId());
                    return;
                }

                if (secondsLeft <= 0) {
                    // Zamknij GUI
                    closeGUI(player);
                    cancel();
                    playerCloseTasks.remove(player.getUniqueId());
                } else if (secondsLeft <= 5) {
                    // Powiadom gracza o zamknięciu w ostatnich 5 sekundach
                    player.sendMessage(plugin.getMessageManager().getMessage("gui.auto-closing",
                            "seconds", String.valueOf(secondsLeft)));
                    secondsLeft--;
                } else {
                    secondsLeft--;
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        playerCloseTasks.put(player.getUniqueId(), closeTask);
    }

    /**
     * Tworzy inventory dla gracza
     */
    private Inventory createInventory(Player player, int page) {
        // Pobierz tytuł z config.yml (nie z messages!)
        String title = plugin.getConfig().getString("gui.title", "&8&lOtchłań");
        title = plugin.getMessageManager().colorize(title);

        Inventory inventory = Bukkit.createInventory(this, size, title);

        // Wypełnij przedmiotami z magazynu
        fillWithItems(inventory, page);

        // Dodaj przyciski nawigacji
        addNavigationButtons(inventory, page);

        return inventory;
    }

    /**
     * Wypełnia inventory przedmiotami z magazynu
     */
    private void fillWithItems(Inventory inventory, int page) {
        AbyssManager manager = plugin.getAbyssManager();
        List<ItemStack> pageItems = manager.getItemsForPage(page, itemsPerPage);

        // Wypełnij sloty przedmiotami (bez ostatniej linii)
        for (int i = 0; i < pageItems.size() && i < itemsPerPage; i++) {
            ItemStack item = pageItems.get(i);
            if (item != null && !item.getType().isAir()) {
                inventory.setItem(i, item);
            }
        }
    }

    /**
     * Dodaje przyciski nawigacji do ostatniej linii GUI
     */
    private void addNavigationButtons(Inventory inventory, int page) {
        AbyssManager manager = plugin.getAbyssManager();
        int totalPages = manager.getTotalPages(itemsPerPage);
        int itemCount = manager.getItemCount();

        // Slot początku ostatniej linii
        int navStart = size - 9;

        // Wypełnij dolny pasek szkłem, jeśli włączone
        if (plugin.getConfig().getBoolean("navigation.glass-filler.enabled", true)) {
            fillNavigationBarWithGlass(inventory, navStart);
        }

        // Przycisk "Poprzednia strona"
        if (plugin.getConfig().getBoolean("navigation.previous-page.enabled", true) && page > 0) {
            int prevSlot = navStart + plugin.getConfig().getInt("navigation.previous-page.slot", 0);
            ItemStack prevButton = createNavigationButton("previous-page", page > 0);
            inventory.setItem(prevSlot, prevButton);
        }

        // Przycisk "Info"
        if (plugin.getConfig().getBoolean("navigation.info.enabled", true)) {
            int infoSlot = navStart + plugin.getConfig().getInt("navigation.info.slot", 3);
            ItemStack infoButton = createInfoButton(page, totalPages, itemCount);
            inventory.setItem(infoSlot, infoButton);
        }

        // Przycisk "Następna strona"
        if (plugin.getConfig().getBoolean("navigation.next-page.enabled", true) && page < totalPages - 1) {
            int nextSlot = navStart + plugin.getConfig().getInt("navigation.next-page.slot", 8);
            ItemStack nextButton = createNavigationButton("next-page", page < totalPages - 1);
            inventory.setItem(nextSlot, nextButton);
        }

        // Przycisk "Zamknij"
        if (plugin.getConfig().getBoolean("navigation.close.enabled", true)) {
            int closeSlot = navStart + plugin.getConfig().getInt("navigation.close.slot", 5);
            ItemStack closeButton = createNavigationButton("close", true);
            inventory.setItem(closeSlot, closeButton);
        }
    }

    /**
     * Wypełnia dolny pasek nawigacji szkłem
     */
    private void fillNavigationBarWithGlass(Inventory inventory, int navStart) {
        String material = plugin.getConfig().getString("navigation.glass-filler.material", "GRAY_STAINED_GLASS_PANE");
        String name = plugin.getConfig().getString("navigation.glass-filler.name", " ");

        ItemStack glass = new ItemStack(Material.valueOf(material));
        ItemMeta meta = glass.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text(plugin.getMessageManager().colorize(name)));
            glass.setItemMeta(meta);
        }

        // Wypełnij wszystkie 9 slotów dolnego paska
        for (int i = 0; i < 9; i++) {
            inventory.setItem(navStart + i, glass);
        }
    }

    /**
     * Tworzy przycisk nawigacji
     */
    private ItemStack createNavigationButton(String type, boolean enabled) {
        String materialPath = "navigation." + type + ".material";
        String material = plugin.getConfig().getString(materialPath, "ARROW");

        ItemStack button = new ItemStack(Material.valueOf(material));
        ItemMeta meta = button.getItemMeta();

        if (meta != null) {
            // Nazwa przycisku
            String namePath = plugin.getConfig().getString("navigation." + type + ".name-key");
            String name = plugin.getMessageManager().getMessage(namePath);
            meta.displayName(Component.text(name));

            // Lore przycisku
            String lorePath = plugin.getConfig().getString("navigation." + type + ".lore-key");
            List<String> lore = plugin.getMessageManager().getMessageList(lorePath);
            List<Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                loreComponents.add(Component.text(line));
            }
            meta.lore(loreComponents);

            button.setItemMeta(meta);
        }

        return button;
    }

    /**
     * Tworzy przycisk informacyjny
     */
    private ItemStack createInfoButton(int currentPage, int totalPages, int itemCount) {
        String material = plugin.getConfig().getString("navigation.info.material", "BOOK");
        ItemStack button = new ItemStack(Material.valueOf(material));
        ItemMeta meta = button.getItemMeta();

        if (meta != null) {
            // Nazwa
            String namePath = plugin.getConfig().getString("navigation.info.name-key");
            String name = plugin.getMessageManager().getMessage(namePath);
            meta.displayName(Component.text(name));

            // Lore z podstawieniami
            String lorePath = plugin.getConfig().getString("navigation.info.lore-key");
            List<String> lore = plugin.getMessageManager().getMessageList(lorePath,
                    "current", String.valueOf(currentPage + 1),
                    "total", String.valueOf(totalPages),
                    "items", String.valueOf(itemCount));

            List<Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                loreComponents.add(Component.text(line));
            }
            meta.lore(loreComponents);

            button.setItemMeta(meta);
        }

        return button;
    }

    /**
     * Odświeża GUI dla wszystkich graczy, którzy je mają otwarte
     */
    public void refreshAllViewers() {
        // Skopiuj keySet aby uniknąć ConcurrentModificationException
        Set<UUID> viewers = new HashSet<>(playerInventories.keySet());

        for (UUID uuid : viewers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                Integer page = playerPages.getOrDefault(uuid, 0);
                refreshGUI(player, page);
            } else {
                // Usuń offline graczy
                playerPages.remove(uuid);
                playerInventories.remove(uuid);
            }
        }
    }

    /**
     * Odświeża GUI dla konkretnego gracza
     */
    public void refreshGUI(Player player, int page) {
        Inventory current = player.getOpenInventory().getTopInventory();

        // Sprawdź, czy gracz ma otwarte nasze GUI
        if (current.getHolder() instanceof AbyssGUI) {
            Inventory newInventory = createInventory(player, page);

            // Aktualizuj zawartość obecnego inventory
            current.clear();
            current.setContents(newInventory.getContents());

            playerPages.put(player.getUniqueId(), page);
            playerInventories.put(player.getUniqueId(), current);
        }
    }

    /**
     * Zmienia stronę dla gracza
     */
    public void changePage(Player player, int newPage) {
        openGUI(player, newPage);
    }

    /**
     * Pobiera bieżącą stronę gracza
     */
    public int getCurrentPage(Player player) {
        return playerPages.getOrDefault(player.getUniqueId(), 0);
    }

    /**
     * Sprawdza, czy slot jest w obszarze nawigacji
     */
    public boolean isNavigationSlot(int slot) {
        return slot >= (size - 9);
    }

    /**
     * Sprawdza, czy slot jest przyciskiem nawigacji
     */
    public boolean isNavigationButton(int slot) {
        int navStart = size - 9;
        int relativeSlot = slot - navStart;

        return (plugin.getConfig().getBoolean("navigation.previous-page.enabled", true) &&
                relativeSlot == plugin.getConfig().getInt("navigation.previous-page.slot", 0)) ||
               (plugin.getConfig().getBoolean("navigation.next-page.enabled", true) &&
                relativeSlot == plugin.getConfig().getInt("navigation.next-page.slot", 8)) ||
               (plugin.getConfig().getBoolean("navigation.close.enabled", true) &&
                relativeSlot == plugin.getConfig().getInt("navigation.close.slot", 5)) ||
               (plugin.getConfig().getBoolean("navigation.info.enabled", true) &&
                relativeSlot == plugin.getConfig().getInt("navigation.info.slot", 3));
    }

    /**
     * Obsługuje kliknięcie w przycisk nawigacji
     */
    public void handleNavigationClick(Player player, int slot) {
        int navStart = size - 9;
        int relativeSlot = slot - navStart;
        int currentPage = getCurrentPage(player);
        AbyssManager manager = plugin.getAbyssManager();
        int totalPages = manager.getTotalPages(itemsPerPage);

        if (relativeSlot == plugin.getConfig().getInt("navigation.previous-page.slot", 0)) {
            // Poprzednia strona
            if (currentPage > 0) {
                changePage(player, currentPage - 1);
            }
        } else if (relativeSlot == plugin.getConfig().getInt("navigation.next-page.slot", 8)) {
            // Następna strona
            if (currentPage < totalPages - 1) {
                changePage(player, currentPage + 1);
            }
        } else if (relativeSlot == plugin.getConfig().getInt("navigation.close.slot", 5)) {
            // Zamknij
            closeGUI(player);
        }
        // Przycisk info - nic nie robi przy kliknięciu
    }

    /**
     * Zamyka GUI dla gracza
     */
    public void closeGUI(Player player) {
        player.closeInventory();
        playerPages.remove(player.getUniqueId());
        playerInventories.remove(player.getUniqueId());

        // Anuluj task auto-close jeśli istnieje
        BukkitTask closeTask = playerCloseTasks.remove(player.getUniqueId());
        if (closeTask != null) {
            closeTask.cancel();
        }
    }

    /**
     * Zamyka GUI dla wszystkich graczy
     */
    public void closeAllGUIs() {
        Set<UUID> viewers = new HashSet<>(playerInventories.keySet());

        for (UUID uuid : viewers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                closeGUI(player);
            }
        }

        // Anuluj wszystkie pozostałe taski
        for (BukkitTask task : playerCloseTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }

        playerPages.clear();
        playerInventories.clear();
        playerCloseTasks.clear();
    }

    /**
     * Sprawdza, czy gracz ma otwarte GUI
     */
    public boolean hasOpenGUI(Player player) {
        return playerInventories.containsKey(player.getUniqueId());
    }

    /**
     * Pobiera ilość itemów na stronę
     */
    public int getItemsPerPage() {
        return itemsPerPage;
    }

    @Override
    public Inventory getInventory() {
        // Zwraca pusty inventory - każdy gracz ma swój własny
        return Bukkit.createInventory(this, size);
    }
}

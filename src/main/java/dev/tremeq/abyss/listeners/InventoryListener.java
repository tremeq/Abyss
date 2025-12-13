package dev.tremeq.abyss.listeners;

import dev.tremeq.abyss.Abyss;
import dev.tremeq.abyss.AbyssGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Obsługuje eventy związane z GUI Otchłani
 */
public class InventoryListener implements Listener {
    private final Abyss plugin;

    public InventoryListener(Abyss plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        // Sprawdź, czy to nasze GUI
        if (!(event.getInventory().getHolder() instanceof AbyssGUI)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        AbyssGUI gui = plugin.getAbyssGUI();
        int slot = event.getRawSlot();

        // Jeśli kliknięto poza GUI (w ekwipunku gracza)
        if (slot >= event.getInventory().getSize()) {
            // Gracz może wrzucać itemy do GUI z własnego ekwipunku
            if (event.getClick().isShiftClick()) {
                event.setCancelled(true);
                ItemStack clicked = event.getCurrentItem();

                if (clicked != null && !clicked.getType().isAir()) {
                    // Sklonuj item aby zachować wszystkie NBT i metadata (custom itemy)
                    ItemStack clonedItem = clicked.clone();

                    // Dodaj item do magazynu
                    plugin.getAbyssManager().addItem(clonedItem);

                    // Usuń item z ekwipunku gracza
                    event.setCurrentItem(null);

                    // Odśwież GUI dla wszystkich
                    gui.refreshAllViewers();

                    // Wyślij wiadomość
                    player.sendMessage(plugin.getMessageManager().getMessage("gui.item-added"));
                }
            }
            return;
        }

        // Kliknięto w GUI
        int currentPage = gui.getCurrentPage(player);

        // Sprawdź, czy to przycisk nawigacji
        if (gui.isNavigationButton(slot)) {
            event.setCancelled(true);
            gui.handleNavigationClick(player, slot);
            return;
        }

        // Sprawdź, czy to obszar nawigacji (ale nie przycisk)
        if (gui.isNavigationSlot(slot)) {
            event.setCancelled(true);
            return;
        }

        // Kliknięto w item w magazynie
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        // Oblicz globalny indeks
        int globalIndex = plugin.getAbyssManager().getGlobalIndex(currentPage, slot, gui.getItemsPerPage());

        // Sprawdź typ kliknięcia
        switch (event.getClick()) {
            case LEFT:
            case RIGHT:
                // Zabieranie itemów z magazynu
                if (clickedItem != null && !clickedItem.getType().isAir()) {
                    // Atomowo pobierz i usuń item z magazynu (zapobiega race conditions)
                    ItemStack itemFromStorage = plugin.getAbyssManager().takeItem(globalIndex);

                    if (itemFromStorage != null) {
                        // Item już sklonowany w takeItem(), możemy go bezpiecznie użyć
                        // Spróbuj dodać item do ekwipunku gracza
                        var remaining = player.getInventory().addItem(itemFromStorage);

                        // Sprawdź czy udało się dodać wszystkie itemy
                        if (remaining.isEmpty()) {
                            // Wszystko OK, item już usunięty z magazynu przez takeItem()
                            // Odśwież GUI dla wszystkich
                            gui.refreshAllViewers();

                            // Wyślij wiadomość
                            player.sendMessage(plugin.getMessageManager().getMessage("gui.item-taken"));
                        } else {
                            // Ekwipunek pełny - musimy zwrócić item do magazynu
                            plugin.getAbyssManager().addItem(itemFromStorage);

                            // Odśwież GUI aby pokazać przywrócony item
                            gui.refreshAllViewers();

                            player.sendMessage(plugin.getMessageManager().getMessage("errors.inventory-full"));
                        }
                    }
                }
                // Wkładanie itemów do magazynu
                else if (cursorItem != null && !cursorItem.getType().isAir()) {
                    // Sklonuj item aby zachować wszystkie NBT i metadata (custom itemy)
                    ItemStack clonedCursorItem = cursorItem.clone();

                    // Dodaj item do magazynu
                    plugin.getAbyssManager().addItem(clonedCursorItem);

                    // Usuń item z kursora
                    event.setCursor(null);

                    // Odśwież GUI dla wszystkich
                    gui.refreshAllViewers();

                    // Wyślij wiadomość
                    player.sendMessage(plugin.getMessageManager().getMessage("gui.item-added"));
                }
                break;

            case SHIFT_LEFT:
            case SHIFT_RIGHT:
                // Shift-click - zabieranie itemów
                if (clickedItem != null && !clickedItem.getType().isAir()) {
                    // Atomowo pobierz i usuń item z magazynu (zapobiega race conditions)
                    ItemStack itemFromStorage = plugin.getAbyssManager().takeItem(globalIndex);

                    if (itemFromStorage != null) {
                        // Item już sklonowany w takeItem(), możemy go bezpiecznie użyć
                        // Spróbuj dodać item do ekwipunku gracza
                        var remaining = player.getInventory().addItem(itemFromStorage);

                        // Sprawdź czy udało się dodać wszystkie itemy
                        if (remaining.isEmpty()) {
                            // Wszystko OK, item już usunięty z magazynu przez takeItem()
                            // Odśwież GUI dla wszystkich
                            gui.refreshAllViewers();

                            // Wyślij wiadomość
                            player.sendMessage(plugin.getMessageManager().getMessage("gui.item-taken"));
                        } else {
                            // Ekwipunek pełny - musimy zwrócić item do magazynu
                            plugin.getAbyssManager().addItem(itemFromStorage);

                            // Odśwież GUI aby pokazać przywrócony item
                            gui.refreshAllViewers();

                            player.sendMessage(plugin.getMessageManager().getMessage("errors.inventory-full"));
                        }
                    }
                }
                break;

            case NUMBER_KEY:
            case DROP:
            case CONTROL_DROP:
            case SWAP_OFFHAND:
            case DOUBLE_CLICK:
                // Zablokuj te akcje - mogłyby powodować problemy z custom itemami
                // lub nieprzewidywalne zachowanie
                break;

            case MIDDLE:
                // Creative mode - sklonuj item
                if (player.getGameMode() == org.bukkit.GameMode.CREATIVE && clickedItem != null && !clickedItem.getType().isAir()) {
                    ItemStack itemFromStorage = plugin.getAbyssManager().getItem(globalIndex);
                    if (itemFromStorage != null) {
                        // Daj graczowi sklonowany item
                        event.setCursor(itemFromStorage.clone());
                    }
                }
                break;

            default:
                // Zablokuj wszystkie inne nieobsługiwane typy kliknięć
                break;
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        // Sprawdź, czy to nasze GUI
        if (!(event.getInventory().getHolder() instanceof AbyssGUI)) {
            return;
        }

        // Sprawdź, czy drag obejmuje sloty GUI
        for (int slot : event.getRawSlots()) {
            if (slot < event.getInventory().getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Sprawdź, czy to nasze GUI
        if (!(event.getInventory().getHolder() instanceof AbyssGUI)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        AbyssGUI gui = plugin.getAbyssGUI();

        // Usuń gracza z mapy
        // GUI już to robi, ale dla pewności
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("Gracz " + player.getName() + " zamknął GUI");
        }
    }
}

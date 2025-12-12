package dev.tremeq.abyss;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Menedżer globalnego magazynu Otchłani
 * Zarządza wszystkimi przedmiotami w publicznej "skrzyni"
 */
public class AbyssManager {
    private final Abyss plugin;
    private final List<ItemStack> storage;

    public AbyssManager(Abyss plugin) {
        this.plugin = plugin;
        this.storage = Collections.synchronizedList(new ArrayList<>());
    }

    /**
     * Dodaje przedmiot do magazynu Otchłani
     */
    public void addItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }

        synchronized (storage) {
            storage.add(item.clone());
        }

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("Dodano przedmiot do Otchłani: " + item.getType() + " x" + item.getAmount());
        }
    }

    /**
     * Dodaje wiele przedmiotów do magazynu
     */
    public void addItems(List<ItemStack> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        synchronized (storage) {
            for (ItemStack item : items) {
                if (item != null && !item.getType().isAir()) {
                    storage.add(item.clone());
                }
            }
        }

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("Dodano " + items.size() + " przedmiotów do Otchłani");
        }
    }

    /**
     * Usuwa przedmiot z magazynu
     */
    public boolean removeItem(ItemStack item) {
        synchronized (storage) {
            return storage.remove(item);
        }
    }

    /**
     * Usuwa przedmiot z określonego indeksu
     */
    public ItemStack removeItem(int index) {
        synchronized (storage) {
            if (index >= 0 && index < storage.size()) {
                return storage.remove(index);
            }
        }
        return null;
    }

    /**
     * Pobiera przedmiot z określonego indeksu (bez usuwania)
     */
    public ItemStack getItem(int index) {
        synchronized (storage) {
            if (index >= 0 && index < storage.size()) {
                return storage.get(index);
            }
        }
        return null;
    }

    /**
     * Ustawia przedmiot na określonym indeksie
     */
    public void setItem(int index, ItemStack item) {
        synchronized (storage) {
            if (index >= 0 && index < storage.size()) {
                if (item == null || item.getType().isAir()) {
                    storage.remove(index);
                } else {
                    storage.set(index, item.clone());
                }
            } else if (item != null && !item.getType().isAir()) {
                storage.add(item.clone());
            }
        }
    }

    /**
     * Pobiera wszystkie przedmioty
     */
    public List<ItemStack> getAllItems() {
        synchronized (storage) {
            return new ArrayList<>(storage);
        }
    }

    /**
     * Pobiera ilość przedmiotów w magazynie
     */
    public int getItemCount() {
        synchronized (storage) {
            return storage.size();
        }
    }

    /**
     * Pobiera przedmioty dla konkretnej strony
     */
    public List<ItemStack> getItemsForPage(int page, int itemsPerPage) {
        synchronized (storage) {
            int startIndex = page * itemsPerPage;
            int endIndex = Math.min(startIndex + itemsPerPage, storage.size());

            if (startIndex >= storage.size()) {
                return new ArrayList<>();
            }

            return new ArrayList<>(storage.subList(startIndex, endIndex));
        }
    }

    /**
     * Oblicza całkowitą liczbę stron
     */
    public int getTotalPages(int itemsPerPage) {
        synchronized (storage) {
            if (storage.isEmpty()) {
                return 1;
            }
            return (int) Math.ceil((double) storage.size() / itemsPerPage);
        }
    }

    /**
     * Czyści cały magazyn
     */
    public void clear() {
        synchronized (storage) {
            storage.clear();
        }
        plugin.getLogger().info("Magazyn Otchłani został wyczyszczony");
    }

    /**
     * Sprawdza, czy magazyn jest pusty
     */
    public boolean isEmpty() {
        synchronized (storage) {
            return storage.isEmpty();
        }
    }

    /**
     * Pobiera globalny indeks przedmiotu na podstawie strony i slotu
     */
    public int getGlobalIndex(int page, int slot, int itemsPerPage) {
        return (page * itemsPerPage) + slot;
    }

    /**
     * Sprawdza, czy dany globalny indeks istnieje
     */
    public boolean isValidIndex(int index) {
        synchronized (storage) {
            return index >= 0 && index < storage.size();
        }
    }
}

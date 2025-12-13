package dev.tremeq.abyss;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Menedżer wiadomości z obsługą kolorów legacy, HEX i Adventure API
 */
public class MessageManager {
    private final Abyss plugin;
    private FileConfiguration messages;
    private final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public MessageManager(Abyss plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    /**
     * Ładuje plik wiadomości zgodny z wybranym językiem
     */
    public void loadMessages() {
        String language = plugin.getConfig().getString("language", "pl");
        String fileName = "messages_" + language + ".yml";

        File messagesFile = new File(plugin.getDataFolder(), fileName);

        // Jeśli plik nie istnieje, skopiuj domyślny z resources
        if (!messagesFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                InputStream in = plugin.getResource(fileName);
                if (in != null) {
                    Files.copy(in, messagesFile.toPath());
                    in.close();
                } else {
                    plugin.getLogger().severe("Nie znaleziono pliku wiadomości: " + fileName);
                    // Fallback na messages_pl.yml
                    fileName = "messages_pl.yml";
                    messagesFile = new File(plugin.getDataFolder(), fileName);
                    if (!messagesFile.exists()) {
                        in = plugin.getResource(fileName);
                        if (in != null) {
                            Files.copy(in, messagesFile.toPath());
                            in.close();
                        }
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Błąd podczas tworzenia pliku wiadomości: " + e.getMessage());
                e.printStackTrace();
            }
        }

        messages = YamlConfiguration.loadConfiguration(messagesFile);
        plugin.getLogger().info("Załadowano wiadomości z pliku: " + fileName);
    }

    /**
     * Pobiera wiadomość z pliku i stosuje kolory
     */
    public String getMessage(String path) {
        String message = messages.getString(path, "&cBrak wiadomości: " + path);

        // Podstaw {prefix} zawsze
        String prefix = messages.getString("prefix", "");
        message = message.replace("{prefix}", colorize(prefix));

        return colorize(message);
    }

    /**
     * Pobiera wiadomość z podstawieniami zmiennych
     */
    public String getMessage(String path, String... replacements) {
        String message = getMessage(path);  // Prefix już podstawiony w getMessage(path)

        // Podstaw inne zmienne (pary: klucz, wartość)
        for (int i = 0; i < replacements.length - 1; i += 2) {
            String key = replacements[i];
            String value = replacements[i + 1];
            message = message.replace("{" + key + "}", value);
        }

        return message;
    }

    /**
     * Pobiera listę wiadomości (np. lore)
     */
    public List<String> getMessageList(String path) {
        List<String> rawList = messages.getStringList(path);
        List<String> colorized = new ArrayList<>();

        for (String line : rawList) {
            colorized.add(colorize(line));
        }

        return colorized;
    }

    /**
     * Pobiera listę wiadomości z podstawieniami
     */
    public List<String> getMessageList(String path, String... replacements) {
        List<String> list = getMessageList(path);
        List<String> result = new ArrayList<>();

        for (String line : list) {
            String replaced = line;

            // Podstaw zmienne
            for (int i = 0; i < replacements.length - 1; i += 2) {
                String key = replacements[i];
                String value = replacements[i + 1];
                replaced = replaced.replace("{" + key + "}", value);
            }

            result.add(replaced);
        }

        return result;
    }

    /**
     * Konwertuje string z kolorami na Adventure Component
     */
    public Component getComponent(String path) {
        String message = getMessage(path);
        return parseComponent(message);
    }

    /**
     * Konwertuje string z kolorami na Adventure Component z podstawieniami
     */
    public Component getComponent(String path, String... replacements) {
        String message = getMessage(path, replacements);
        return parseComponent(message);
    }

    /**
     * Parsuje string do Adventure Component
     */
    private Component parseComponent(String text) {
        // Adventure API automatycznie obsługuje & kody
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    /**
     * Koloryzuje tekst - obsługuje & kody oraz HEX (#FFFFFF)
     */
    public String colorize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Zamień HEX (#FFFFFF) na format Minecrafta
        text = translateHexCodes(text);

        // Zamień & na § dla kompatybilności
        text = text.replace('&', '§');

        return text;
    }

    /**
     * Konwertuje kody HEX (&#FFFFFF) na format obsługiwany przez Minecraft
     */
    private String translateHexCodes(String text) {
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hexCode = matcher.group(1);
            String replacement = convertHexToMinecraft(hexCode);
            matcher.appendReplacement(buffer, replacement);
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    /**
     * Konwertuje kod HEX na format Minecrafta (§x§R§R§G§G§B§B)
     */
    private String convertHexToMinecraft(String hex) {
        StringBuilder magic = new StringBuilder("§x");

        for (char c : hex.toCharArray()) {
            magic.append("§").append(c);
        }

        return magic.toString();
    }

    /**
     * Koloryzuje listę stringów
     */
    public List<String> colorizeList(List<String> list) {
        List<String> colorized = new ArrayList<>();
        for (String line : list) {
            colorized.add(colorize(line));
        }
        return colorized;
    }
}

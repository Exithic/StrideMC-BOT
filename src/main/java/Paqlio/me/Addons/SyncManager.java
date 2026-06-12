package Paqlio.me.Addons;

import Paqlio.me.BOT;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SyncManager {

    // Tymczasowe kody (nie zapisujemy ich do pliku, po restarcie znikają)
    private final Map<String, UUID> codes = new ConcurrentHashMap<>();

    // Baza połączonych kont (Klucz: UUID z MC, Wartość: ID z Discorda)
    private Map<String, String> linkedAccounts = new ConcurrentHashMap<>();

    private final File dataFile;
    private final Gson gson;

    public SyncManager() {
        this.dataFile = new File(BOT.getInstance().getDataFolder(), "linked_accounts.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        loadData();
    }

    public String generateCode(Player player) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random rnd = new Random();
        while (sb.length() < 6) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        String code = sb.toString();
        codes.put(code, player.getUniqueId());
        return code;
    }

    public UUID getPlayerByCode(String code) {
        return codes.remove(code.toUpperCase());
    }

    public void link(UUID uuid, String discordId) {
        linkedAccounts.put(uuid.toString(), discordId);
        saveDataAsync(); // Zapisujemy w tle, żeby nie zaciąć serwera
    }

    public boolean isLinked(UUID uuid) {
        return linkedAccounts.containsKey(uuid.toString());
    }

    public boolean isDiscordLinked(String discordId) {
        return linkedAccounts.containsValue(discordId);
    }

    public UUID getPlayerUUID(String discordId) {
        return linkedAccounts.entrySet().stream()
                .filter(e -> e.getValue().equals(discordId))
                .map(e -> UUID.fromString(e.getKey()))
                .findFirst().orElse(null);
    }

    // =========================================
    // SYSTEM ZAPISU I ODCZYTU (JSON)
    // =========================================
    private void loadData() {
        if (!dataFile.exists()) {
            BOT.getInstance().getLogger().info("Plik linked_accounts.json nie istnieje, zostanie utworzony przy pierwszym połączeniu.");
            return;
        }

        try (FileReader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<ConcurrentHashMap<String, String>>(){}.getType();
            Map<String, String> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                linkedAccounts = loaded;
            }
            BOT.getInstance().getLogger().info("✅ Załadowano " + linkedAccounts.size() + " połączonych kont z bazy JSON.");
        } catch (Exception e) {
            BOT.getInstance().getLogger().severe("✖ Wystąpił błąd podczas ładowania bazy kont JSON: " + e.getMessage());
        }
    }

    private void saveDataAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(BOT.getInstance(), () -> saveDataSync());
    }

    public void saveDataSync() {
        try {
            if (!dataFile.exists()) {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            }
            // Backup starego pliku
            if (dataFile.exists()) {
                var backup = new File(dataFile.getParentFile(), "linked_accounts_backup.json");
                java.nio.file.Files.copy(dataFile.toPath(), backup.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            try (FileWriter writer = new FileWriter(dataFile)) {
                gson.toJson(linkedAccounts, writer);
            }
        } catch (Exception e) {
            BOT.getInstance().getLogger().severe("✖ Nie udało się zapisać danych: " + e.getMessage());
        }
    }
}
package net.rypixel.doublelife;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.checkerframework.checker.units.qual.A;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class GameData implements Serializable {

    public HashMap<UUID, UserPair> uuidUserPair = new HashMap<>();
    public static JavaPlugin plugin;


    // Config
    public static ArrayList<UUID> predeterminedGroups = new ArrayList<>();
    public static boolean announceSoulmate = false;
    public static boolean tellSoulmate = false;
    public static int startingLives = 3;
    public static boolean canCraftEnchantingTableStatic = true;
    public static boolean lifeCountEnabled = false;

    public boolean canCraftEnchantingTable = true;

    GameData() {

    }

    public void saveData() {
        int saveVersion = 1;
        String output = "";
        output += String.valueOf(saveVersion) + "\n";

        ArrayList<UserPair> savedPairs = new ArrayList<>();

        for (Map.Entry<UUID, UserPair> entry : uuidUserPair.entrySet()) {
            if (savedPairs.contains(entry.getValue())) {
                continue;
            }
            savedPairs.add(entry.getValue());
            UserPair pair = entry.getValue();
            output += pair.player1.toString() + "," + pair.player2.toString() + "," + pair.isSharingHunger + "," + pair.sharedHunger + "," + pair.sharedLives + "," + pair.sharedHealth + "~";
        }
        output = output.substring(0, output.length() - 1) + "\n";
        output += canCraftEnchantingTable;

        String path = Path.of(Bukkit.getWorlds().get(0).getWorldFolder().getPath(), "save.doublelife").toString();

        try (PrintWriter out = new PrintWriter(path)) {
            out.println(output);
        } catch (Exception e) {
            try (PrintWriter out = new PrintWriter(path)) {
                out.println(output);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    public boolean isParticipating(UUID uuid) {
        return uuidUserPair.containsKey(uuid);
    }

//    Static things
    public static GameData readData() {
        String path = Path.of(Bukkit.getWorlds().get(0).getWorldFolder().getPath(), "save.doublelife").toString();
        GameData data = new GameData();

        try {
            String input = new String(Files.readAllBytes(Paths.get(path)));
            String[] sections = input.split("\n");
            Integer versionNumber = Integer.valueOf(sections[0]);
            String[] pairs = sections[1].split("~");
            boolean canCraftEnchantingTables = Boolean.valueOf(sections[2]);

            data.canCraftEnchantingTable = canCraftEnchantingTables;

            for (String str : pairs) {
                String[] parts = str.split(",");
                UUID player1 = UUID.fromString(parts[0]);
                UUID player2 = UUID.fromString(parts[1]);
                boolean isSharingHunger = Boolean.valueOf(parts[2]);
                double sharedHunger = Double.valueOf(parts[3]);
                int sharedLives = Integer.valueOf(parts[4]);
                double sharedHealth = Double.valueOf(parts[5]);

                UserPair pair = new UserPair(player1, player2, isSharingHunger, sharedLives);
                pair.sharedHunger = sharedHunger;
                pair.sharedHealth = sharedHealth;

                data.uuidUserPair.put(player1, pair);
                data.uuidUserPair.put(player2, pair);
            }
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            Bukkit.getLogger().warning("Could not read the DoubleLife save file... will create a new one");
            return null;
        }
    }

    public static GameData createData(boolean isSharingHunger) {
        ArrayList<Player> participatingPlayers = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (predeterminedGroups.contains(p)) {
                continue;
            }
            participatingPlayers.add(p);
        }

        GameData gameData = new GameData();
        gameData.canCraftEnchantingTable = GameData.canCraftEnchantingTableStatic;

        if (!lifeCountEnabled) {
            startingLives = -99;
        }

        Random random;
        final int playerCount = participatingPlayers.size();
        for (int i = 0; i < playerCount; i += 2) {
            random = new Random();
            Player player1 = participatingPlayers.get(random.nextInt(participatingPlayers.size()));
            participatingPlayers.remove(player1);
            random = new Random();
            Player player2 = participatingPlayers.get(random.nextInt(participatingPlayers.size()));
            participatingPlayers.remove(player2);
            UserPair pair = new UserPair(player1.getUniqueId(), player2.getUniqueId(), isSharingHunger, startingLives);
            gameData.uuidUserPair.put(player1.getUniqueId(), pair);
            gameData.uuidUserPair.put(player2.getUniqueId(), pair);
        }

        if (predeterminedGroups.size() > 0) {
            for (int i = 0; i < playerCount; i += 2) {
                random = new Random();
                UUID player1 = predeterminedGroups.get(0);
                participatingPlayers.remove(player1);
                random = new Random();
                UUID player2 = predeterminedGroups.get(0);
                UserPair pair = new UserPair(player1, player2, isSharingHunger, startingLives);
                gameData.uuidUserPair.put(player1, pair);
                gameData.uuidUserPair.put(player2, pair);
            }
        }

        for (Map.Entry<UUID, UserPair> entry : gameData.uuidUserPair.entrySet()) {
            Bukkit.getPlayer(entry.getKey()).sendTitle(ChatColor.GREEN + "Your soulmate is...", "", 20, 80, 0);
        }


        new BukkitRunnable() {
            public void run() {
                for (Map.Entry<UUID, UserPair> entry : gameData.uuidUserPair.entrySet()) {
                    if (gameData.tellSoulmate) {
                        UUID otherPlayer = entry.getValue().player1;
                        if (otherPlayer == entry.getKey()) {
                            otherPlayer = entry.getValue().player2;
                        }
                        Bukkit.getPlayer(entry.getKey()).sendTitle(ChatColor.GREEN + Bukkit.getPlayer(otherPlayer).getDisplayName(), "", 0, 40, 20);
                    } else {
                        Bukkit.getPlayer(entry.getKey()).sendTitle(ChatColor.GREEN + "??????", "", 0, 40, 20);
                    }
                }

                if (gameData.announceSoulmate) {
                    Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "---------------");
                    Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Teams:");
                    ArrayList<UserPair> saidPairs = new ArrayList<>();
                    for (Map.Entry<UUID, UserPair> entry : gameData.uuidUserPair.entrySet()) {
                        UserPair pair = entry.getValue();
                        if (saidPairs.contains(pair)) {
                            continue;
                        }
                        saidPairs.add(pair);
                        Bukkit.broadcastMessage(ChatColor.GREEN + Bukkit.getPlayer(pair.player1).getDisplayName() + " and " + Bukkit.getPlayer(pair.player2).getDisplayName());
                    }
                    Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "---------------");
                }
            }
        }.runTaskLater(plugin, 100L);

        return gameData;
    }
}

class UserPair implements Serializable {

    public int sharedLives;

    public double sharedHealth = 20;

    public boolean isSharingHunger;
    public double sharedHunger = 20;

    public UUID player1;
    public UUID player2;

    public long lastDamageUpdated = 0;

    UserPair(UUID player1, UUID player2, boolean isSharingHunger, int sharedLives) {
        this.player1 = player1;
        this.player2 = player2;
        this.isSharingHunger = isSharingHunger;
        this.sharedLives = sharedLives;
    }

    public double getHealth() {
        return sharedHealth;
    }

    public void setHealth(double sharedHealth) {
        if (System.currentTimeMillis() - lastDamageUpdated < 50) {
            return;
        }
        lastDamageUpdated = System.currentTimeMillis();
        boolean tookDamage = false;
        if (this.sharedHealth - sharedHealth > 0) {
            tookDamage = true;
        }
        this.sharedHealth = sharedHealth;
        if (this.sharedHealth > 20) {
            this.sharedHealth = 20;
        }
        Player tPlayer = Bukkit.getPlayer(player1);
        if (tPlayer != null) {
            if (tookDamage) {
                tPlayer.playEffect(EntityEffect.HURT);
            }
            tPlayer.setHealth(sharedHealth);
        }
        tPlayer = Bukkit.getPlayer(player2);
        if (tPlayer != null) {
            if (tookDamage) {
                tPlayer.playEffect(EntityEffect.HURT);
            }
            tPlayer.setHealth(sharedHealth);
        }
    }

    public void killPlayers(Player p) {
        if (System.currentTimeMillis() - lastDamageUpdated < 50) {
            return;
        }
        lastDamageUpdated = System.currentTimeMillis();
        Player tPlayer = Bukkit.getPlayer(player1);
        if (tPlayer != null && tPlayer != p) {
            tPlayer.setHealth(0);
        }
        tPlayer = Bukkit.getPlayer(player2);
        if (tPlayer != null && tPlayer != p) {
            tPlayer.setHealth(0);
        }
        if (sharedLives != -99) {
            sharedLives -= 1;
        }
        sharedHealth = 20;
    }

    public void refreshPlayers() {
        Player tPlayer = Bukkit.getPlayer(player1);
        if (tPlayer != null) {
            tPlayer.setHealth(sharedHealth);
        }
        tPlayer = Bukkit.getPlayer(player2);
        if (tPlayer != null) {
            tPlayer.setHealth(sharedHealth);
        }
    }
}
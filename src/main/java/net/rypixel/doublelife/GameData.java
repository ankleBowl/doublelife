package net.rypixel.doublelife;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
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
    public static boolean canCraftEnchantingTable = true;
    public static boolean lifeCountEnabled = true;
    public static boolean isSharingHunger = false;
    public static boolean customTntRecipe = true;




    public static boolean needDataReentryAfterUpdateForVersion2 = false;

    GameData() {

    }

    public void saveData() {
        int saveVersion = 3;

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
        output += canCraftEnchantingTable + "\n" + announceSoulmate + "\n" + tellSoulmate + "\n" + startingLives + "\n" + customTntRecipe;

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
            if (versionNumber == 1 || versionNumber == 2 || versionNumber == 3) {
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

                    GameData.isSharingHunger = isSharingHunger;
                }

                if (versionNumber > 1) {
                    announceSoulmate = Boolean.valueOf(sections[3]);
                    tellSoulmate = Boolean.valueOf(sections[4]);
                    startingLives = Integer.valueOf(sections[5]);

                    if (versionNumber > 2) {
                        customTntRecipe = Boolean.valueOf(sections[6]);
                    }
                } else {
                    needDataReentryAfterUpdateForVersion2 = true;
                }
            }

            if (customTntRecipe) {
                NamespacedKey key = new NamespacedKey(plugin, "custom_tnt");
                ShapedRecipe recipe = new ShapedRecipe(key, new ItemStack(Material.TNT));
                recipe.shape("PSP", "SGS", "PSP");
                recipe.setIngredient('P', Material.PAPER);
                recipe.setIngredient('S', Material.SAND);
                recipe.setIngredient('G', Material.GUNPOWDER);
                Bukkit.addRecipe(recipe);
            }

            return data;
        } catch (Exception e) {
            e.printStackTrace();
            Bukkit.getLogger().warning("Could not read the DoubleLife save file... will create a new one");
            return null;
        }
    }

    public static GameData createData(boolean gameStarted, GameData gameData) {
        ArrayList<Player> participatingPlayers = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (predeterminedGroups.contains(p)) {
                continue;
            }
            if (gameStarted) {
                if (gameData.uuidUserPair.containsKey(p.getUniqueId())) {
                    continue;
                }
            }

            participatingPlayers.add(p);
        }

        if (!gameStarted) {
            gameData = new GameData();

            if (customTntRecipe) {
                try {
                    NamespacedKey key = new NamespacedKey(plugin, "custom_tnt");
                    ShapedRecipe recipe = new ShapedRecipe(key, new ItemStack(Material.TNT));
                    recipe.shape("PSP", "SGS", "PSP");
                    recipe.setIngredient('P', Material.PAPER);
                    recipe.setIngredient('S', Material.SAND);
                    recipe.setIngredient('G', Material.GUNPOWDER);
                    Bukkit.addRecipe(recipe);
                } catch (Exception e) {

                }
            }
        }

        if (!lifeCountEnabled) {
            startingLives = -99;
        }

        if (participatingPlayers.size() % 2 != 0) {
            participatingPlayers.remove(0);
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
                UUID player1 = predeterminedGroups.get(0);
                predeterminedGroups.remove(player1);
                UUID player2 = predeterminedGroups.get(0);
                predeterminedGroups.remove(player2);
                UserPair pair = new UserPair(player1, player2, isSharingHunger, startingLives);
                gameData.uuidUserPair.put(player1, pair);
                gameData.uuidUserPair.put(player2, pair);
            }
        }

        for (Map.Entry<UUID, UserPair> entry : gameData.uuidUserPair.entrySet()) {
            Bukkit.getPlayer(entry.getKey()).sendTitle(ChatColor.GREEN + "Your soulmate is...", "", 20, 80, 0);
        }

        GameData finalGameData = gameData;
        new BukkitRunnable() {
            public void run() {
                for (Map.Entry<UUID, UserPair> entry : finalGameData.uuidUserPair.entrySet()) {
                    if (finalGameData.tellSoulmate) {
                        UUID otherPlayer = entry.getValue().player1;
                        if (otherPlayer == entry.getKey()) {
                            otherPlayer = entry.getValue().player2;
                        }
                        Bukkit.getPlayer(entry.getKey()).sendTitle(ChatColor.GREEN + Bukkit.getPlayer(otherPlayer).getDisplayName(), "", 0, 40, 20);
                    } else {
                        Bukkit.getPlayer(entry.getKey()).sendTitle(ChatColor.GREEN + "??????", "", 0, 40, 20);
                    }
                }

                if (finalGameData.announceSoulmate) {
                    Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "---------------");
                    Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Teams:");
                    ArrayList<UserPair> saidPairs = new ArrayList<>();
                    for (Map.Entry<UUID, UserPair> entry : finalGameData.uuidUserPair.entrySet()) {
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

    public void refreshPlayers() {
        ArrayList<Player> unlinkedPlayers = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!uuidUserPair.containsKey(p.getUniqueId())) {
                unlinkedPlayers.add(p);
            }
        }
        if (unlinkedPlayers.size() % 2 != 0) {
            unlinkedPlayers.remove(0);
        }


        Random random;
        final int playerCount = unlinkedPlayers.size();
        for (int i = 0; i < playerCount; i += 2) {
            random = new Random();
            Player player1 = unlinkedPlayers.get(random.nextInt(unlinkedPlayers.size()));
            unlinkedPlayers.remove(player1);
            random = new Random();
            Player player2 = unlinkedPlayers.get(random.nextInt(unlinkedPlayers.size()));
            unlinkedPlayers.remove(player2);
            UserPair pair = new UserPair(player1.getUniqueId(), player2.getUniqueId(), isSharingHunger, startingLives);
            uuidUserPair.put(player1.getUniqueId(), pair);
            uuidUserPair.put(player2.getUniqueId(), pair);
        }

        for (Player p : unlinkedPlayers) {
            p.sendTitle(ChatColor.GREEN + "Your soulmate is...", "", 20, 80, 0);
        }
        new BukkitRunnable() {
            public void run() {
                for (Player p : unlinkedPlayers) {
                    UserPair pair = uuidUserPair.get(p.getUniqueId());
                    if (tellSoulmate) {
                        UUID otherPlayer = pair.player1;
                        if (otherPlayer == p.getUniqueId()) {
                            otherPlayer = pair.player2;
                        }
                        p.sendTitle(ChatColor.GREEN + Bukkit.getPlayer(otherPlayer).getDisplayName(), "", 0, 40, 20);
                    } else {
                        p.sendTitle(ChatColor.GREEN + "??????", "", 0, 40, 20);
                    }
                }
                if (announceSoulmate) {
                    Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "---------------");
                    Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Teams:");
                    ArrayList<UserPair> saidPairs = new ArrayList<>();
                    for (Map.Entry<UUID, UserPair> entry : uuidUserPair.entrySet()) {
                        UserPair pair = entry.getValue();
                        if (saidPairs.contains(pair)) {
                            continue;
                        }
                        saidPairs.add(pair);
                        Bukkit.broadcastMessage(ChatColor.GREEN + Bukkit.getOfflinePlayer(pair.player1).getName() + " and " + Bukkit.getOfflinePlayer(pair.player2).getName());
                    }
                    Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "---------------");
                }
            }
        }.runTaskLater(plugin, 100L);
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

    public void setHealth(double sharedHealth, Player attackedPlayer) {
        if (System.currentTimeMillis() - lastDamageUpdated < 1) {
            return;
        }
        lastDamageUpdated = System.currentTimeMillis();
        this.sharedHealth = sharedHealth;
        if (this.sharedHealth > 20) {
            this.sharedHealth = 20;
        }
        Player tPlayer = Bukkit.getPlayer(player1);
        if (tPlayer != null) {
            tPlayer.setHealth(sharedHealth);
        }
        tPlayer = Bukkit.getPlayer(player2);
        if (tPlayer != null) {
            tPlayer.setHealth(sharedHealth);
        }
    }

    public void damage(Player p, double damageAmount) {
        if (System.currentTimeMillis() - lastDamageUpdated < 1) {
            return;
        }
        lastDamageUpdated = System.currentTimeMillis();
        Player tPlayer = Bukkit.getPlayer(player1);
        if (tPlayer != null && p != tPlayer) {
            tPlayer.setHealth(damageAmount);
            tPlayer.playEffect(EntityEffect.HURT);
        }
        tPlayer = Bukkit.getPlayer(player2);
        if (tPlayer != null && p != tPlayer) {
            tPlayer.setHealth(damageAmount);
            tPlayer.playEffect(EntityEffect.HURT);
        }
    }

    public void killPlayers(Player p) {
        if (System.currentTimeMillis() - lastDamageUpdated < 1) {
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

    public void killPlayersWithTotem(Player p, UUID otherPlayer, boolean otherPlayerHoldingTotem) {
        // Player p should be the player with the totem, preferably the one that took damage
        if (System.currentTimeMillis() - lastDamageUpdated < 50) {
            return;
        }
        lastDamageUpdated = System.currentTimeMillis();
        Player tPlayer = Bukkit.getPlayer(otherPlayer);
        if (otherPlayerHoldingTotem) {
            p.damage(10000);
        }
        if (tPlayer != null) {
            tPlayer.playEffect(EntityEffect.TOTEM_RESURRECT);
            tPlayer.playEffect(EntityEffect.HURT);
            tPlayer.setHealth(1);
        }
    }

    public void refreshPlayers() {
        Player tPlayer = Bukkit.getPlayer(player1);
        if (tPlayer != null) {
            tPlayer.setHealth(sharedHealth);
            tPlayer.setFoodLevel((int) sharedHunger);
        }
        tPlayer = Bukkit.getPlayer(player2);
        if (tPlayer != null) {
            tPlayer.setHealth(sharedHealth);
            tPlayer.setFoodLevel((int) sharedHunger);
        }
    }

    public void setHunger(int newHunger) {
        sharedHunger = newHunger;
        Player tPlayer = Bukkit.getPlayer(player1);
        if (tPlayer != null) {
            tPlayer.setFoodLevel(newHunger);
        }
        tPlayer = Bukkit.getPlayer(player2);
        if (tPlayer != null) {
            tPlayer.setFoodLevel(newHunger);
        }
    }
}
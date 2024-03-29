package net.rypixel.doublelife;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
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

    public static ArrayList<UUID> killOnLogin = new ArrayList<UUID>();

    // Config
    public static ArrayList<UUID> predeterminedGroups = new ArrayList<>();
    public static boolean announceSoulmate = false;
    public static boolean tellSoulmate = false;
    public static int startingLives = 3;
    public static boolean canCraftEnchantingTable = true;
    public static boolean lifeCountEnabled = true;
    public static boolean isSharingHunger = false;
    public static boolean customTntRecipe = true;
    public static boolean anyPlayerCanRefresh = false;
    public static boolean isSharingXpGlobal = false;
    public static boolean isSharingEffects = false;
    public static boolean zombieBackups = false;



    public static boolean needDataReentryAfterUpdateForVersion2 = false;

    GameData() {

    }

    public void saveData() {
        int saveVersion = 5;

        String output = "";
        output += String.valueOf(saveVersion) + "\n";

        ArrayList<UserPair> savedPairs = new ArrayList<>();

        // First, save the data relating to the game settings and pairs in save.doublelife
        for (Map.Entry<UUID, UserPair> entry : uuidUserPair.entrySet()) {
            if (savedPairs.contains(entry.getValue())) {
                continue;
            }
            savedPairs.add(entry.getValue());
            UserPair pair = entry.getValue();
            output += pair.player1.toString() + "," + pair.player2.toString() + "," + pair.isSharingHunger + "," + pair.sharedHunger + "," + pair.sharedLives + "," + pair.sharedHealth + "," + pair.isSharingXp + "," + pair.xpAmount + "," + pair.sharingEffects + "~";
        }
        output = output.substring(0, output.length() - 1) + "\n";
        output += canCraftEnchantingTable + "\n" + announceSoulmate + "\n" + tellSoulmate + "\n" + startingLives + "\n" + customTntRecipe + "\n" + anyPlayerCanRefresh + "\n" + zombieBackups;

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


//      Now save the players that have been killed when they were offline
        String path1 = Path.of(Bukkit.getWorlds().get(0).getWorldFolder().getPath(), "killonlogin.doublelife").toString();
        String output1 = "";
        for (UUID uuid : killOnLogin) {
            output1 = output1 + uuid.toString() + ",";
        }
        if (output1.length() != 0) {
            output1 = output1.substring(0, output1.length() - 1);
        }
        try (PrintWriter out = new PrintWriter(path1)) {
            out.println(output1);
        } catch (Exception e) {
            try (PrintWriter out = new PrintWriter(path1)) {
                out.println(output1);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    public static void readKillOnLogin() {
        String path = Path.of(Bukkit.getWorlds().get(0).getWorldFolder().getPath(), "killonlogin.doublelife").toString();
        try {
            String input = new String(Files.readAllBytes(Paths.get(path)));
            String[] uuids = input.split(",");
            for (String uuid : uuids) {
                Bukkit.broadcastMessage(uuid);
                killOnLogin.add(UUID.fromString(uuid));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    Static things
    public static GameData readData() {
        String path = Path.of(Bukkit.getWorlds().get(0).getWorldFolder().getPath(), "save.doublelife").toString();
        GameData data = new GameData();
        try {
            String input = new String(Files.readAllBytes(Paths.get(path)));
            String[] sections = input.split("\n");
            Integer versionNumber = Integer.valueOf(sections[0]);
            if (versionNumber == 1 || versionNumber == 2 || versionNumber == 3 || versionNumber == 4 || versionNumber == 5) {
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

                    boolean isSharingXp = false;
                    int sharedXp = 0;
                    boolean sharingEffects = false;
                    if (versionNumber > 3) {
                        isSharingXp = Boolean.valueOf(parts[6]);
                        sharedXp = Integer.valueOf(parts[7]);
                        sharingEffects = Boolean.valueOf(parts[8]);
                    }

                    UserPair pair = new UserPair(player1, player2, isSharingHunger, sharedLives);
                    pair.sharedHunger = sharedHunger;
                    pair.sharedHealth = sharedHealth;

                    pair.isSharingXp = isSharingXp;
                    pair.xpAmount = sharedXp;
                    pair.sharingEffects = sharingEffects;

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

                        if (versionNumber > 3) {
                            anyPlayerCanRefresh = Boolean.valueOf(sections[7]);

                            if (versionNumber > 4) {
                                zombieBackups = Boolean.valueOf(sections[8]);
                            }
                        }
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
            pair.isSharingXp = isSharingXpGlobal;
            pair.sharingEffects = isSharingEffects;
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
                pair.isSharingXp = isSharingXpGlobal;
                pair.sharingEffects = isSharingEffects;
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

//    public void refreshPlayers() {
//        ArrayList<Player> unlinkedPlayers = new ArrayList<>();
//        for (Player p : Bukkit.getOnlinePlayers()) {
//            if (!uuidUserPair.containsKey(p.getUniqueId())) {
//                unlinkedPlayers.add(p);
//            }
//        }
//        if (unlinkedPlayers.size() % 2 != 0) {
//            unlinkedPlayers.remove(0);
//        }
//
//
//        Random random;
//        final int playerCount = unlinkedPlayers.size();
//        for (int i = 0; i < playerCount; i += 2) {
//            random = new Random();
//            Player player1 = unlinkedPlayers.get(random.nextInt(unlinkedPlayers.size()));
//            unlinkedPlayers.remove(player1);
//            random = new Random();
//            Player player2 = unlinkedPlayers.get(random.nextInt(unlinkedPlayers.size()));
//            unlinkedPlayers.remove(player2);
//            UserPair pair = new UserPair(player1.getUniqueId(), player2.getUniqueId(), isSharingHunger, startingLives);
//            uuidUserPair.put(player1.getUniqueId(), pair);
//            uuidUserPair.put(player2.getUniqueId(), pair);
//        }
//
//        for (Player p : unlinkedPlayers) {
//            p.sendTitle(ChatColor.GREEN + "Your soulmate is...", "", 20, 80, 0);
//        }
//        new BukkitRunnable() {
//            public void run() {
//                for (Player p : unlinkedPlayers) {
//                    UserPair pair = uuidUserPair.get(p.getUniqueId());
//                    if (tellSoulmate) {
//                        UUID otherPlayer = pair.player1;
//                        if (otherPlayer == p.getUniqueId()) {
//                            otherPlayer = pair.player2;
//                        }
//                        p.sendTitle(ChatColor.GREEN + Bukkit.getPlayer(otherPlayer).getDisplayName(), "", 0, 40, 20);
//                    } else {
//                        p.sendTitle(ChatColor.GREEN + "??????", "", 0, 40, 20);
//                    }
//                }
//                if (announceSoulmate) {
//                    Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "---------------");
//                    Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Teams:");
//                    ArrayList<UserPair> saidPairs = new ArrayList<>();
//                    for (Map.Entry<UUID, UserPair> entry : uuidUserPair.entrySet()) {
//                        UserPair pair = entry.getValue();
//                        if (saidPairs.contains(pair)) {
//                            continue;
//                        }
//                        saidPairs.add(pair);
//                        Bukkit.broadcastMessage(ChatColor.GREEN + Bukkit.getOfflinePlayer(pair.player1).getName() + " and " + Bukkit.getOfflinePlayer(pair.player2).getName());
//                    }
//                    Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "---------------");
//                }
//            }
//        }.runTaskLater(plugin, 100L);
//    }
}

class UserPair implements Serializable {

    public int sharedLives;

    public double sharedHealth = 20;

    public boolean isSharingHunger;
    public double sharedHunger = 20;

    public UUID player1;
    public UUID player2;

    public long lastDamageUpdated = 0;

    public int xpAmount = 0;
    public boolean isSharingXp = false;
    public boolean sharingEffects = false;

    UserPair(UUID player1, UUID player2, boolean isSharingHunger, int sharedLives) {
        this.player1 = player1;
        this.player2 = player2;
        this.isSharingHunger = isSharingHunger;
        this.sharedLives = sharedLives;
    }

    public void setHealth(double sharedHealth) {
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
        sharedHealth = damageAmount;
    }

    public void killPlayers(Player p) {
        if (System.currentTimeMillis() - lastDamageUpdated < 1) {
            return;
        }
        lastDamageUpdated = System.currentTimeMillis();
        tryKillPlayer(player1, p);
        tryKillPlayer(player2, p);
        if (sharedLives != -99) {
            sharedLives -= 1;
        }
        sharedHealth = 20;
    }

    public void tryKillPlayer(UUID uuid, Player playerWhoDied) {
        Player tPlayer = Bukkit.getPlayer(uuid);
        if (tPlayer == playerWhoDied && playerWhoDied != null) {
            return;
        }
        if (tPlayer != null) {
            tPlayer.setHealth(0);
        } else {
            if (!GameData.killOnLogin.contains(uuid)) {
                GameData.killOnLogin.add(uuid);
            }
        }
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
            if (isSharingHunger) {
                tPlayer.setFoodLevel((int) sharedHunger);
            }
            if (isSharingXp) {
                tPlayer.setExp(0.0F);
                tPlayer.setLevel(0);
                tPlayer.giveExp(xpAmount);
            }
        }
        tPlayer = Bukkit.getPlayer(player2);
        if (tPlayer != null) {
            tPlayer.setHealth(sharedHealth);
            if (isSharingHunger) {
                tPlayer.setFoodLevel((int) sharedHunger);
            }
            if (isSharingXp) {
                tPlayer.setExp(0.0F);
                tPlayer.setLevel(0);
                tPlayer.giveExp(xpAmount);
            }
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

    public void setXp(int newXP) {
        xpAmount = newXP;
        Player tPlayer = Bukkit.getPlayer(player1);
        if (tPlayer != null) {
            Bukkit.broadcastMessage("Setting total experience of " + tPlayer.getName() + " to " + xpAmount);
            tPlayer.setExp(0.0F);
            tPlayer.setLevel(0);
            tPlayer.giveExp(xpAmount);
        }
        tPlayer = Bukkit.getPlayer(player2);
        if (tPlayer != null) {
            Bukkit.broadcastMessage("Setting total experience of " + tPlayer.getName() + " to " + xpAmount);
            tPlayer.setExp(0.0F);
            tPlayer.setLevel(0);
            tPlayer.giveExp(xpAmount);
//            tPlayer.setTotalExperience(xpAmount);
        }
    }

    public UUID getOtherUUID(UUID uuid) {
        if (uuid == player1) {
            return player2;
        } else {
            return player1;
        }
    }

    public void refreshEffects(UUID playerToRefresh, UUID playerToRefreshFrom) {
        if (Bukkit.getPlayer(playerToRefresh) == null || Bukkit.getPlayer(playerToRefreshFrom) == null) {
            return;
        }
        Player refreshPlayer = Bukkit.getPlayer(playerToRefresh);
        Player refreshFromPlayer = Bukkit.getPlayer(playerToRefreshFrom);

        final Collection<PotionEffect> potionEffects = refreshPlayer.getActivePotionEffects();
        for (PotionEffect effect : potionEffects) {
            refreshPlayer.removePotionEffect(effect.getType());
        }

        for (PotionEffect effect : refreshFromPlayer.getActivePotionEffects()) {
            refreshPlayer.addPotionEffect(effect);
        }
    }
}
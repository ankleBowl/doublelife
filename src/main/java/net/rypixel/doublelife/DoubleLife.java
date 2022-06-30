package net.rypixel.doublelife;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.spigotmc.event.entity.EntityMountEvent;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

public final class DoubleLife extends JavaPlugin implements Listener {

    public GameData gameData;
    boolean gameStarted = true;
    boolean gameDataExists = true;
    boolean gameFrozen = false;

    public Team threeLives;
    public Team twoLives;
    public Team oneLife;
    public Team dead;
    public Scoreboard scoreboard;

    boolean isSharingHunger = false;

    @Override
    public void onEnable() {
        // Plugin startup logic
        GameData.plugin = this;
        gameData = GameData.readData();
        if (gameData == null) {
            gameStarted = false;
            gameDataExists = false;
        }
        Bukkit.getPluginManager().registerEvents(this, this);
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        registerTeams();
    }

    void registerTeams() {
        threeLives = scoreboard.registerNewTeam("3");
        threeLives.setColor(ChatColor.GREEN);
        twoLives = scoreboard.registerNewTeam("2");
        twoLives.setColor(ChatColor.YELLOW);
        oneLife = scoreboard.registerNewTeam("1");
        oneLife.setColor(ChatColor.RED);
        dead = scoreboard.registerNewTeam("Dead");
        dead.setColor(ChatColor.BLACK);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        threeLives.unregister();
        twoLives.unregister();
        oneLife.unregister();
        dead.unregister();
        gameData.saveData();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("doublelife") && sender.isOp()) {
            if (args.length < 1) {
                if (sender instanceof Player) {
                    sender.sendMessage("Usage: /doublelife [start/settings/stop/restart/freeze/help]");
                } else {
                    sender.sendMessage("Usage: /doublelife [start/stop/restart/freeze/help]");
                    sender.sendMessage("To view settings, run this command on a Minecraft client");
                }
                return true;
            }
            switch (args[0]) {
                case "start":
                    if (gameStarted) {
                        return false;
                    }
                    if (gameDataExists) {
                        return true;
                    }
                    gameData = GameData.createData(gameStarted, null);
                    for (Map.Entry<UUID, UserPair> pair : gameData.uuidUserPair.entrySet()) {
                        if (pair.getValue().sharedLives > 2) {
                            threeLives.addPlayer(Bukkit.getOfflinePlayer(pair.getValue().player1));
                            threeLives.addPlayer(Bukkit.getOfflinePlayer(pair.getValue().player2));
                        } else if (pair.getValue().sharedLives == 2) {
                            twoLives.addPlayer(Bukkit.getOfflinePlayer(pair.getValue().player1));
                            twoLives.addPlayer(Bukkit.getOfflinePlayer(pair.getValue().player2));
                        } else if (pair.getValue().sharedLives == 1) {
                            oneLife.addPlayer(Bukkit.getOfflinePlayer(pair.getValue().player1));
                            oneLife.addPlayer(Bukkit.getOfflinePlayer(pair.getValue().player2));
                        } else {
                            dead.addPlayer(Bukkit.getOfflinePlayer(pair.getValue().player1));
                            dead.addPlayer(Bukkit.getOfflinePlayer(pair.getValue().player2));
                        }

                    }
                    gameStarted = true;
                    gameData.saveData();
                    gameDataExists = true;
                    break;
                case "settings":
                    if (!(sender instanceof Player)) {
                        return false;
                    }
                    if (GameData.needDataReentryAfterUpdateForVersion2) {
                        Inventory inv = Inventories.getSettingsMenu();
                        ((Player) sender).openInventory(inv);
                    }
                    if (gameStarted) {
                        sender.sendMessage("Because the game has already begun, settings such as life count will only apply for new players");
                    }
                    Inventory inv = Inventories.getSettingsMenu();
                    ((Player) sender).openInventory(inv);
                    break;
                case "restart":
                    if (args.length < 2) {
                        sender.sendMessage("\"This command will remove all save data for DoubleLife. Please run /doublelife restart confirm to allow this.\"");
                        return false;
                    }
                    gameStarted = true;
                    gameData = GameData.createData(false, gameData);
                    for (Map.Entry<UUID, UserPair> pair : gameData.uuidUserPair.entrySet()) {
                        if (pair.getValue().sharedLives > 2) {
                            threeLives.addPlayer(Bukkit.getOfflinePlayer(pair.getValue().player1));
                            threeLives.addPlayer(Bukkit.getOfflinePlayer(pair.getValue().player2));
                        } else if (pair.getValue().sharedLives == 2) {
                            twoLives.addPlayer(Bukkit.getOfflinePlayer(pair.getValue().player1));
                            twoLives.addPlayer(Bukkit.getOfflinePlayer(pair.getValue().player2));
                        } else if (pair.getValue().sharedLives == 1) {
                            oneLife.addPlayer(Bukkit.getOfflinePlayer(pair.getValue().player1));
                            oneLife.addPlayer(Bukkit.getOfflinePlayer(pair.getValue().player2));
                        } else {
                            dead.addPlayer(Bukkit.getOfflinePlayer(pair.getValue().player1));
                            dead.addPlayer(Bukkit.getOfflinePlayer(pair.getValue().player2));
                        }

                    }
                    gameData.saveData();
                    gameDataExists = true;
                    break;
                case "stop":
                    gameStarted = false;
                    sender.sendMessage(ChatColor.YELLOW + "The game has been paused. Players will take damage normally");
                    break;
                case "freeze":
                    if (!gameStarted) {
                        sender.sendMessage(ChatColor.RED + "The game must be started to freeze!");
                        return false;
                    }
                    if (!gameFrozen) {
                        gameFrozen = true;
                        Bukkit.broadcastMessage(ChatColor.YELLOW + "The game has been frozen!");
                        for (World w : Bukkit.getWorlds()) {
                            w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
                            for (Entity e : w.getEntities()) {
                                if (e instanceof Mob) {
                                    ((Mob) e).setTarget(null);
                                }
                            }
                        }
                    } else {
                        Bukkit.broadcastMessage(ChatColor.GREEN + "The game has been resumed!");
                        for (World w : Bukkit.getWorlds()) {
                            w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
                        }
                        gameFrozen = false;
                    }
                    break;
                case "refresh":
                    if (!gameStarted) {
                        sender.sendMessage(ChatColor.RED + "The game must be started to refresh!");
                        return false;
                    }
                    gameData = GameData.createData(gameStarted, gameData);
                    for (Map.Entry<UUID, UserPair> pair : gameData.uuidUserPair.entrySet()) {
                        if (pair.getValue().sharedLives > 2) {
                            threeLives.addPlayer(Bukkit.getOfflinePlayer(pair.getValue().player1));
                            threeLives.addPlayer(Bukkit.getOfflinePlayer(pair.getValue().player2));
                        } else if (pair.getValue().sharedLives == 2) {
                            twoLives.addPlayer(Bukkit.getOfflinePlayer(pair.getValue().player1));
                            twoLives.addPlayer(Bukkit.getOfflinePlayer(pair.getValue().player2));
                        } else if (pair.getValue().sharedLives == 1) {
                            oneLife.addPlayer(Bukkit.getOfflinePlayer(pair.getValue().player1));
                            oneLife.addPlayer(Bukkit.getOfflinePlayer(pair.getValue().player2));
                        } else {
                            dead.addPlayer(Bukkit.getOfflinePlayer(pair.getValue().player1));
                            dead.addPlayer(Bukkit.getOfflinePlayer(pair.getValue().player2));
                        }

                    }
                    gameData.saveData();
                    break;
                case "help":
                    if (sender instanceof Player) {
                        Player p = (Player) sender;
                        p.sendMessage("Usage: /doublelife [start/settings/stop/restart/freeze/help]");
                    } else {
                        Bukkit.getLogger().info("Usage: /doublelife [start/stop/restart/freeze/help]");
                        Bukkit.getLogger().info("To view settings, run this command on a Minecraft client");
                    }
                    break;
            }
        }
        return true;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().setScoreboard(scoreboard);
        if (!gameStarted) {
            return;
        }
        UUID playerUUID = event.getPlayer().getUniqueId();
        UserPair userPair = gameData.uuidUserPair.get(playerUUID);
        if (userPair == null) {
            return;
        }
        userPair.refreshPlayers();
        if (userPair.sharedLives == -1) {
            event.getPlayer().setScoreboard(null);
            return;
        }
        if (userPair.sharedLives == 0) {
            event.getPlayer().setGameMode(GameMode.SPECTATOR);
        }
        if (userPair.sharedLives > 2) {
            threeLives.addPlayer(event.getPlayer());
        } else if (userPair.sharedLives == 2) {
            twoLives.addPlayer(event.getPlayer());
        } else if (userPair.sharedLives == 1) {
            oneLife.addPlayer(event.getPlayer());
        } else {
            dead.addPlayer(event.getPlayer());
        }


        //Special version 1 to version 2 upgrading stuffs
        if (GameData.needDataReentryAfterUpdateForVersion2) {
            if (event.getPlayer().isOp()) {
                event.getPlayer().sendMessage("You've updated DoubleLife from version < 1.1.0. More details were added to the save file in this version. Please use /doublelife settings and resubmit your settings from first use.");
                event.getPlayer().sendMessage("---------------------------");
                event.getPlayer().sendMessage("Why was the save file changed? This was to allow players to join after the first start, but still have the correct settings");
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!gameStarted) {
            return;
        }
        if (event.getEntity() instanceof Player) {
            if (((Player) event.getEntity()).getHealth() - event.getFinalDamage() <= 0) {
                killPlayers((Player) event.getEntity());
            } else {
                onHealthChange((Player) event.getEntity(), ((Player) event.getEntity()).getHealth() - event.getFinalDamage());
                event.setDamage(0);
            }
        }
    }

    @EventHandler
    public void onEntityRegen(EntityRegainHealthEvent event) {
        if (!gameStarted) {
            return;
        }
        if (gameFrozen) {
            event.setCancelled(true);
            return;
        }
        if (event.getEntity() instanceof Player) {
            onHealthChange((Player) event.getEntity(), ((Player) event.getEntity()).getHealth() + event.getAmount());
            event.setCancelled(true);
        }
    }

    public void onHealthChange(Player p, double newHealth) {
        UUID playerUUID = p.getUniqueId();
        if (gameData.uuidUserPair.containsKey(playerUUID)) {
            gameData.uuidUserPair.get(playerUUID).setHealth(newHealth);
        }
    }

    public void killPlayers(Player p) {
        UUID playerUUID = p.getUniqueId();
        if (gameData.uuidUserPair.containsKey(playerUUID)) {
            UserPair pair = gameData.uuidUserPair.get(playerUUID);
            pair.killPlayers(p);
            if (pair.sharedLives > 2) {
                return;
            } else if (pair.sharedLives == 2) {
                threeLives.removePlayer(Bukkit.getOfflinePlayer(pair.player1));
                threeLives.removePlayer(Bukkit.getOfflinePlayer(pair.player2));
                twoLives.addPlayer(Bukkit.getOfflinePlayer(pair.player1));
                twoLives.addPlayer(Bukkit.getOfflinePlayer(pair.player2));
            } else if (gameData.uuidUserPair.get(playerUUID).sharedLives == 1) {
                twoLives.removePlayer(Bukkit.getOfflinePlayer(pair.player1));
                twoLives.removePlayer(Bukkit.getOfflinePlayer(pair.player2));
                oneLife.addPlayer(Bukkit.getOfflinePlayer(pair.player1));
                oneLife.addPlayer(Bukkit.getOfflinePlayer(pair.player2));
            } else {
                oneLife.removePlayer(Bukkit.getOfflinePlayer(pair.player1));
                oneLife.removePlayer(Bukkit.getOfflinePlayer(pair.player2));
                dead.addPlayer(Bukkit.getOfflinePlayer(pair.player1));
                dead.addPlayer(Bukkit.getOfflinePlayer(pair.player2));
            }
        }
    }

    void log(String string) {
        Bukkit.broadcastMessage(string);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = null;
        if (event.getView().getTitle().equalsIgnoreCase("Settings")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) {
                return;
            }
            boolean refreshView = true;
            switch (event.getCurrentItem().getType()) {
                case BOOK:
                    GameData.tellSoulmate = !GameData.tellSoulmate;
                    break;
                case GOAT_HORN:
                    GameData.announceSoulmate = !GameData.announceSoulmate;
//                    for showing in chat
                    break;
                case ENCHANTING_TABLE:
                    GameData.canCraftEnchantingTable = !GameData.canCraftEnchantingTable;
                    break;
                case TNT:
                    GameData.customTntRecipe = !GameData.customTntRecipe;
                    break;
                case COOKED_BEEF:
                    GameData.isSharingHunger = !GameData.isSharingHunger;
                    if (gameDataExists) {
                        ArrayList<UserPair> userPairs = new ArrayList<>();
                        for (Map.Entry<UUID, UserPair> entry : gameData.uuidUserPair.entrySet()) {
                            if (!userPairs.contains(entry.getValue())) {
                                userPairs.add(entry.getValue());
                            }
                        }
                        for (UserPair pair : userPairs) {
                            pair.isSharingHunger = GameData.isSharingHunger;
                        }
                    }
                    break;
                case NETHER_STAR:
                    inv = Inventories.getLifeCountManager();
                    event.getWhoClicked().openInventory(inv);
                    refreshView = false;
                    break;
                case TOTEM_OF_UNDYING:
                    inv = Inventories.getPredeterminedMenu(0);
                    event.getWhoClicked().openInventory(inv);
                    refreshView = false;
                    break;

            }
            if (refreshView) {
                inv = Inventories.getSettingsMenu();
                ((Player) event.getWhoClicked()).openInventory(inv);
            }
            return;
        }

        if (event.getView().getTitle().equalsIgnoreCase("Settings - Life Count")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) {
                return;
            }
            boolean refreshView = true;
            switch (event.getCurrentItem().getType()) {
                case ARROW:
                    inv = Inventories.getSettingsMenu();
                    event.getWhoClicked().openInventory(inv);
                    refreshView = false;
                    break;
                case COAL:
                    if (GameData.startingLives > 1) {
                        GameData.startingLives -= 1;
                    }
                    break;
                case DIAMOND:
                    if (GameData.startingLives < 99) {
                        GameData.startingLives += 1;
                    }
                    break;
                case REDSTONE:
                    GameData.lifeCountEnabled = !GameData.lifeCountEnabled;
                    break;
            }
            if (refreshView) {
                inv = Inventories.getLifeCountManager();
                ((Player) event.getWhoClicked()).openInventory(inv);
            }
            return;
        }

        if (event.getView().getTitle().startsWith("Predetermined Groups - ")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) {
                return;
            }
            int scrollAmount = Integer.parseInt(event.getView().getTitle().substring(event.getView().getTitle().length() - 1, event.getView().getTitle().length()));
            boolean refreshView = true;
            switch (event.getCurrentItem().getType()) {
                case NETHER_STAR:
                    refreshView = false;
                    inv = Inventories.createPredeterminedTeam(null, gameStarted, gameData);
                    ((Player) event.getWhoClicked()).openInventory(inv);
                    break;
                case COAL:
                    scrollAmount -= 1;
                    break;
                case DIAMOND:
                    scrollAmount += 1;
                    break;
                case BARRIER:
                    int clickedIndex = ((event.getSlot() - 8) % 9) + scrollAmount;
                    GameData.predeterminedGroups.remove(clickedIndex * 2);
                    GameData.predeterminedGroups.remove(clickedIndex * 2);
                    break;
                case ARROW:
                    inv = Inventories.getSettingsMenu();
                    event.getWhoClicked().openInventory(inv);
                    refreshView = false;
            }
            if (refreshView) {
                inv = Inventories.getPredeterminedMenu(scrollAmount);
                ((Player) event.getWhoClicked()).openInventory(inv);
            }
            return;
        }

        if (event.getView().getTitle().startsWith("Create Predetermined Group")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) {
                return;
            }
            UUID lastSelectedUUID = null;
            if (event.getInventory().getItem(53) != null) {
                lastSelectedUUID = ((SkullMeta) event.getInventory().getItem(53).getItemMeta()).getOwningPlayer().getUniqueId();
            }
            UUID selectedUUID = null;
            switch (event.getCurrentItem().getType()) {
                case PLAYER_HEAD:
                    selectedUUID = ((SkullMeta) event.getCurrentItem().getItemMeta()).getOwningPlayer().getUniqueId();
                    if (lastSelectedUUID != null && selectedUUID != null) {
                        GameData.predeterminedGroups.add(selectedUUID);
                        GameData.predeterminedGroups.add(lastSelectedUUID);
                        inv = Inventories.getPredeterminedMenu(0);
                        ((Player) event.getWhoClicked()).openInventory(inv);
                    } else {
                        inv = Inventories.createPredeterminedTeam(selectedUUID, gameStarted, gameData);
                        ((Player) event.getWhoClicked()).openInventory(inv);
                    }
                    break;
            }
        }
    }

    @EventHandler
    public void onPlayerCraft(CraftItemEvent event) {
        if (gameStarted) {
            if (event.getRecipe().getResult().getType() == Material.ENCHANTING_TABLE && !GameData.canCraftEnchantingTable) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!gameStarted) {
            return;
        }
        UserPair pair = gameData.uuidUserPair.get(event.getPlayer().getUniqueId());
        if (pair.sharedLives == -99) {
            return;
        }
        if (pair.sharedLives < 1) {
            event.getPlayer().setGameMode(GameMode.SPECTATOR);
        }

        if (pair.sharedLives > 2) {
            event.getPlayer().sendTitle(ChatColor.GREEN + String.valueOf(pair.sharedLives), ChatColor.GREEN + "Lives remaining...", 10, 40, 10);
        } else if (pair.sharedLives == 2) {
            event.getPlayer().sendTitle(ChatColor.YELLOW + String.valueOf(pair.sharedLives), ChatColor.YELLOW + "Lives remaining...", 10, 40, 10);
        } else if (pair.sharedLives == 1) {
            event.getPlayer().sendTitle(ChatColor.RED + String.valueOf(pair.sharedLives), ChatColor.RED + "Lives remaining...", 10, 40, 10);
        } else {
            event.getPlayer().sendTitle(ChatColor.BLACK + "YOU ARE DEAD", "", 10, 40, 10);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (gameFrozen) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (gameFrozen) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (gameStarted) {
            Player hungerChanged = (Player) event.getEntity();
            UserPair pair = gameData.uuidUserPair.get(hungerChanged.getUniqueId());
            if (pair != null) {
                event.setCancelled(true);
                pair.setHunger(event.getFoodLevel());
            }
        }
    }
}

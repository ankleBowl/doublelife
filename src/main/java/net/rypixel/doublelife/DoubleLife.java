package net.rypixel.doublelife;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.spigotmc.event.entity.EntityMountEvent;

import java.util.*;

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



    public HashMap<Husk, UUID> huskToUUID = new HashMap<Husk, UUID>();
    public HashMap<UUID, Husk> uuidToHusk = new HashMap<UUID, Husk>();



    @Override
    public void onEnable() {
        // Plugin startup logic
        GameData.plugin = this;
        gameData = GameData.readData();
        if (gameData == null) {
            gameStarted = false;
            gameDataExists = false;
        } else {
            GameData.readKillOnLogin();
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
        if (gameData != null) {
            gameData.saveData();
        }
        if (GameData.zombieBackups) {
            for (Map.Entry<Husk, UUID> entry : huskToUUID.entrySet()) {
                entry.getKey().remove();
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("doublelife")) {
            if (!sender.isOp()) {
                if (args.length > 0) {
                    if (args[0].equals("refresh") && GameData.anyPlayerCanRefresh) {
                        refreshGame(sender);
                        return true;
                    }
                }
                sender.sendMessage(ChatColor.RED + "You must be OP to use doublelife commands!");
                return false;
            }
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
                        sender.sendMessage(ChatColor.RED + "The game has already started!");
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
                        sender.sendMessage("This command will remove all save data for DoubleLife. Please run /doublelife restart confirm to allow this.");
                        return false;
                    }
                    gameStarted = true;
                    gameData = GameData.createData(false, null);
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
                case "pause":
                    gameStarted = false;
                    Bukkit.broadcastMessage(ChatColor.YELLOW + "The game has been paused. Players will take damage normally");
                    break;
                case "resume":
                    if (gameDataExists == true) {
                        gameStarted = true;
                        Bukkit.broadcastMessage(ChatColor.GREEN + "The game has been resumed!");
                    } else {
                        sender.sendMessage(ChatColor.RED + "You cannot resume a game if the game has not been started. Use \"/doublelife start\" first!");
                    }
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
                    refreshGame(sender);
                    break;
                case "link":
                    if (args.length < 2) {
                        sender.sendMessage(ChatColor.RED + "Error: Improper usage. Please use /doublelife link (name) (name)");
                    }
                    OfflinePlayer player1 = Bukkit.getOfflinePlayer(args[1]);
                    OfflinePlayer player2 = Bukkit.getOfflinePlayer(args[2]);
                    if (player1 != null && player2 != null && !GameData.predeterminedGroups.contains(player1) && !GameData.predeterminedGroups.contains(player2)) {
                        GameData.predeterminedGroups.add(player1.getUniqueId());
                        GameData.predeterminedGroups.add(player2.getUniqueId());
                    } else {
                        sender.sendMessage(ChatColor.RED + "Player names are incorrect or already registered in pairs");
                    }
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

    boolean refreshGame(CommandSender sender) {
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
        if (GameData.killOnLogin.contains(playerUUID)) {
            GameData.killOnLogin.remove(playerUUID);
            event.getPlayer().setHealth(0);
            event.getPlayer().spigot().respawn();
        }
        userPair.refreshPlayers();
        if (userPair.sharingEffects) {
            userPair.refreshEffects(playerUUID, userPair.getOtherUUID(playerUUID));
        }
        if (GameData.zombieBackups) {
            Husk zombie = uuidToHusk.get(playerUUID);
            if (zombie != null) {
                uuidToHusk.remove(playerUUID);
                huskToUUID.remove(zombie);
                zombie.remove();
            }
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
        if (event.getEntity() instanceof Husk && GameData.zombieBackups) {
            HandleZombie.onEntityDamage(event, this);
        }
        if (event.getEntity() instanceof Player) {
            Player p = (Player) event.getEntity();

            Player holdingTotem = null;
            UUID otherPlayer = null;
            boolean otherPlayerHoldingTotem = false;
            if (gameData.uuidUserPair.containsKey(p.getUniqueId())) {
                UserPair pair = gameData.uuidUserPair.get(p.getUniqueId());
                if (pair.player1.toString().equals(p.getUniqueId().toString())) {
                    otherPlayer = pair.player2;
                } else {
                    otherPlayer = pair.player1;
                }
                if (Bukkit.getPlayer(otherPlayer) != null) {
                    if (Bukkit.getPlayer(otherPlayer).getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING) {
                        holdingTotem = Bukkit.getPlayer(otherPlayer);
                        otherPlayerHoldingTotem = true;
                    } else if (Bukkit.getPlayer(otherPlayer).getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING) {
                        holdingTotem = Bukkit.getPlayer(otherPlayer);
                        otherPlayerHoldingTotem = true;
                    }
                }
                if (p.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING) {
                    holdingTotem = p;
                    otherPlayerHoldingTotem = false;
                } else if (p.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING) {
                    holdingTotem = p;
                    otherPlayerHoldingTotem = false;
                }
            }

            if (p.getHealth() - event.getFinalDamage() <= 0) {
                if (holdingTotem != null) {
                    if (otherPlayerHoldingTotem == true) {
                        event.setCancelled(true);
                    }
                    UUID playerNotWithTotem = null;
                    UserPair pair = gameData.uuidUserPair.get(p.getUniqueId());
                    if (pair.player1.toString().equals(holdingTotem.getUniqueId().toString())) {
                        playerNotWithTotem = pair.player2;
                    } else {
                        playerNotWithTotem = pair.player1;
                    }
                    killWithTotem(holdingTotem, playerNotWithTotem, otherPlayerHoldingTotem);
                } else {
                    killPlayers(p);
                }
            } else {
                damagePair(p, event.getFinalDamage());
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
            if (gameData.uuidUserPair.containsKey(((Player) event.getEntity()).getUniqueId())) {
                onHealthChange((Player) event.getEntity(), ((Player) event.getEntity()).getHealth() + event.getAmount());
                event.setCancelled(true);
            }
        }
    }

    public void onHealthChange(Player p, double newHealth) {
        UUID playerUUID = p.getUniqueId();
        if (gameData.uuidUserPair.containsKey(playerUUID)) {
            gameData.uuidUserPair.get(playerUUID).setHealth(newHealth);
        }
    }

    public void damagePair(Player p, double damageAmount) {
        UUID playerUUID = p.getUniqueId();
        if (gameData.uuidUserPair.containsKey(playerUUID)) {
            gameData.uuidUserPair.get(playerUUID).damage(p, p.getHealth() - damageAmount);
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
                oneLife.addPlayer(Bukkit.getOfflinePlayer(pair.player1));
                oneLife.addPlayer(Bukkit.getOfflinePlayer(pair.player2));
            } else {
                oneLife.removePlayer(Bukkit.getOfflinePlayer(pair.player1));
                oneLife.removePlayer(Bukkit.getOfflinePlayer(pair.player2));
                dead.addPlayer(Bukkit.getOfflinePlayer(pair.player1));
                dead.addPlayer(Bukkit.getOfflinePlayer(pair.player2));
            }

            if (GameData.zombieBackups) {
                if (uuidToHusk.containsKey(pair.player1)) { uuidToHusk.get(pair.player1).remove(); }
                if (uuidToHusk.containsKey(pair.player2)) { uuidToHusk.get(pair.player1).remove(); }
            }
        }
    }

    public void killWithTotem(Player p, UUID otherPlayer, boolean otherPlayerHoldingTotem) {
        UUID playerUUID = p.getUniqueId();
        if (gameData.uuidUserPair.containsKey(playerUUID)) {
            UserPair pair = gameData.uuidUserPair.get(playerUUID);
            pair.killPlayersWithTotem(p, otherPlayer, otherPlayerHoldingTotem);
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
                case EXPERIENCE_BOTTLE:
                    GameData.isSharingXpGlobal = !GameData.isSharingXpGlobal;
                    break;
                case POTION:
                    GameData.isSharingEffects = !GameData.isSharingEffects;
                    if (gameDataExists) {
                        ArrayList<UserPair> userPairs = new ArrayList<>();
                        for (Map.Entry<UUID, UserPair> entry : gameData.uuidUserPair.entrySet()) {
                            if (!userPairs.contains(entry.getValue())) {
                                userPairs.add(entry.getValue());
                            }
                        }
                        for (UserPair pair : userPairs) {
                            pair.sharingEffects = GameData.isSharingEffects;
                        }
                    }
                    break;
                case CONDUIT:
                    GameData.anyPlayerCanRefresh = !GameData.anyPlayerCanRefresh;
                    break;
                case ZOMBIE_SPAWN_EGG:
                    GameData.zombieBackups = !GameData.zombieBackups;
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
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!gameStarted) {
            return;
        }
        if (!GameData.zombieBackups) {
            return;
        }
        Player p = event.getPlayer();
        UserPair pair = gameData.uuidUserPair.get(p.getUniqueId());
        if (pair == null) {
            return;
        }
        if (pair.sharedLives > 0 || pair.sharedLives == -99) {
            Husk zombie = (Husk) p.getWorld().spawnEntity(p.getLocation(), EntityType.HUSK);
            zombie.setAI(false);
            zombie.setCustomName(p.getDisplayName());
            zombie.setCustomNameVisible(true);
            zombie.setBaby(false);
            zombie.getEquipment().clear();
            zombie.getEquipment().setArmorContents(p.getInventory().getArmorContents());
            zombie.getEquipment().setItemInMainHand(p.getInventory().getItemInMainHand());
            zombie.getEquipment().setItemInOffHand(p.getInventory().getItemInOffHand());
            zombie.addPotionEffects(p.getActivePotionEffects());
            huskToUUID.put(zombie, p.getUniqueId());
            uuidToHusk.put(p.getUniqueId(), zombie);
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
            if (pair != null && pair.isSharingHunger) {
                event.setCancelled(true);
                pair.setHunger(event.getFoodLevel());
            }
        }
    }

    @EventHandler
    public void onPlayerXpChange(PlayerExpChangeEvent event) {
        if (gameStarted) {
            Player xpChanged = event.getPlayer();
            UserPair pair = gameData.uuidUserPair.get(xpChanged.getUniqueId());
            if (pair != null && pair.isSharingXp) {
                int newAmount = event.getAmount();
                event.setAmount(0);
                int currentAmount = pair.xpAmount;
                pair.setXp(newAmount + currentAmount);
            }
        }
    }

    @EventHandler
    public void onPlayerEnchant(EnchantItemEvent event) {
        if (gameStarted) {
            Player xpChanged = event.getEnchanter();
            UserPair pair = gameData.uuidUserPair.get(xpChanged.getUniqueId());
            if (pair != null && pair.isSharingXp) {
                int levelsToLose = event.getExpLevelCost();
                int xpCost = levelsToXp(xpChanged.getLevel()) - levelsToXp(xpChanged.getLevel() - event.getExpLevelCost());
                int currentAmount = pair.xpAmount;
                pair.setXp(currentAmount - xpCost);
                event.setExpLevelCost(0);
            }
        }
    }

//    Thanks to Andrew900460 in https://www.spigotmc.org/threads/converting-between-xp-and-levels.156592/
    public static int levelsToXp(int levels) {
        if (levels <= 16) {
            return (int) (Math.pow(levels, 2)+6*levels);
        } else if (levels >= 17 && levels <= 31) {
            return (int) (2.5*Math.pow(levels, 2)-40.5*levels+360);
        } else if (levels >= 32) {
            return (int) (4.5*Math.pow(levels, 2)-162.5*levels+2220);
        }
        return -1;
    }

    @EventHandler
    public void onEffectChange(EntityPotionEffectEvent event) {
        if (event.getCause() == EntityPotionEffectEvent.Cause.PLUGIN) {
            return;
        }
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        if (!gameStarted) {
            return;
        }
        Player p = (Player) event.getEntity();
        UserPair pair = gameData.uuidUserPair.get(p.getUniqueId());
        if (pair != null) {
            if (pair.sharingEffects) {
                event.setCancelled(true);
                switch (event.getAction()) {
                    case REMOVED:
                        PotionEffectType typeToRemove = event.getModifiedType();
                        if (Bukkit.getPlayer(pair.player1) != null) {
                            Bukkit.getPlayer(pair.player1).removePotionEffect(typeToRemove);
                        }
                        if (Bukkit.getPlayer(pair.player2) != null) {
                            Bukkit.getPlayer(pair.player2).removePotionEffect(typeToRemove);
                        }
                        break;
                    case CLEARED:
                        if (Bukkit.getPlayer(pair.player1) != null) {
                            final Collection<PotionEffect> potionEffects = Bukkit.getPlayer(pair.player1).getActivePotionEffects();
                            for (PotionEffect effect : potionEffects) {
                                Bukkit.getPlayer(pair.player1).removePotionEffect(effect.getType());
                            }
                        }
                        if (Bukkit.getPlayer(pair.player2) != null) {
                            final Collection<PotionEffect> potionEffects = Bukkit.getPlayer(pair.player2).getActivePotionEffects();
                            for (PotionEffect effect : potionEffects) {
                                Bukkit.getPlayer(pair.player2).removePotionEffect(effect.getType());
                            }
                        }
                        break;
                    default:
                        PotionEffect newEffect = event.getNewEffect();
                        if (Bukkit.getPlayer(pair.player1) != null) {
                            Bukkit.getPlayer(pair.player1).addPotionEffect(newEffect);
                        }
                        if (Bukkit.getPlayer(pair.player2) != null) {
                            Bukkit.getPlayer(pair.player2).addPotionEffect(newEffect);
                        }
                        break;
                }
            }
        }
    }
}

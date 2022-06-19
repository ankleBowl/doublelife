package net.rypixel.doublelife;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.UUID;

public final class DoubleLife extends JavaPlugin implements Listener {

    public GameData gameData;
    boolean gameStarted = true;

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
        if (label.equalsIgnoreCase("doublelife")) {
            if (args.length < 1) {
                if (sender instanceof Player) {
                    Player p = (Player) sender;
                    p.sendMessage("Usage: /doublelife [start/settings/help]");
                } else {
                    Bukkit.getLogger().info("Usage: /doublelife [start/help]");
                    Bukkit.getLogger().info("To view settings, run this command on a Minecraft client");
                }
                return true;
            }
            switch (args[0]) {
                case "start":
                    if (gameStarted) {
                        return false;
                    }
                    gameStarted = true;
                    gameData = GameData.createData(isSharingHunger);
                    for (Map.Entry<UUID, UserPair> pair : gameData.uuidUserPair.entrySet()) {
                        threeLives.addPlayer(Bukkit.getOfflinePlayer(pair.getValue().player1));
                        threeLives.addPlayer(Bukkit.getOfflinePlayer(pair.getValue().player2));
                    }
                    gameData.saveData();
                    break;
                case "settings":
                    if (!(sender instanceof Player)) {
                        return false;
                    }
                    if (gameStarted) {
                        return false;
                    }
                    Inventory inv = Inventories.getSettingsMenu();
                    ((Player) sender).openInventory(inv);
                    break;
                case "help":
                    if (sender instanceof Player) {
                        Player p = (Player) sender;
                        p.sendMessage("Usage: /doublelife [start/settings/help]");
                    } else {
                        Bukkit.getLogger().info("Usage: /doublelife [start/help]");
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
        userPair.refreshPlayers();
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
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!gameStarted) {
            return;
        }
        if (event.getEntity() instanceof Player) {
            if (((Player) event.getEntity()).getHealth() - event.getFinalDamage() <= 0) {
                killPlayers((Player) event.getEntity());
//                event.setCancelled(true);
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
        if (event.getEntity() instanceof Player) {
            onHealthChange((Player) event.getEntity(), ((Player) event.getEntity()).getHealth() + event.getAmount());
            event.setCancelled(true);
        }
    }

    public void onHealthChange(Player p, double newHealth) {
        UUID playerUUID = p.getUniqueId();
        gameData.uuidUserPair.get(playerUUID).setHealth(newHealth);
    }

    public void killPlayers(Player p) {
        UUID playerUUID = p.getUniqueId();
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

    void log(String string) {
        Bukkit.broadcastMessage(string);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!gameStarted && event.getView().getTitle().equalsIgnoreCase("Settings")) {
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
                    GameData.canCraftEnchantingTableStatic = !GameData.canCraftEnchantingTableStatic;
                    break;
                case NETHER_STAR:
                    Inventory inv = Inventories.getLifeCountManager();
                    event.getWhoClicked().openInventory(inv);
                    refreshView = false;
                    break;
            }
            if (refreshView) {
                Inventory inv = Inventories.getSettingsMenu();
                ((Player) event.getWhoClicked()).openInventory(inv);
            }
            return;
        }

        if (!gameStarted && event.getView().getTitle().equalsIgnoreCase("Settings - Life Count")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) {
                return;
            }
            boolean refreshView = true;
            switch (event.getCurrentItem().getType()) {
                case ARROW:
                    Inventory inv = Inventories.getSettingsMenu();
                    event.getWhoClicked().openInventory(inv);
                    refreshView = false;
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
            }
            if (refreshView) {
                Inventory inv = Inventories.getLifeCountManager();
                ((Player) event.getWhoClicked()).openInventory(inv);
            }
            return;
        }
    }

    @EventHandler
    public void onPlayerCraft(CraftItemEvent event) {
        if (gameStarted && event.getRecipe().getResult().getType() == Material.ENCHANTING_TABLE && gameData.canCraftEnchantingTable) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!gameStarted) {
            return;
        }
        UserPair pair = gameData.uuidUserPair.get(event.getPlayer().getUniqueId());
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
}

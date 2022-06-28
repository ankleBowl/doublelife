package net.rypixel.doublelife;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.UUID;

public class Inventories {

    public static Inventory getSettingsMenu() {
        Inventory inv = Bukkit.createInventory(null, 9, "Settings");

        ArrayList<String> lore = new ArrayList<>();

        lore.add("Toggles showing the");
        lore.add("player who their");
        lore.add("soulmate is");
        lore.add("");
        inv.setItem(0, getItem(Material.BOOK, "Show Soulmate Name", GameData.tellSoulmate, lore));
        lore.clear();

        lore.add("Toggles showing all");
        lore.add("the soulmate pairs in");
        lore.add("the game chat");
        inv.setItem(1, getItem(Material.GOAT_HORN, "Announce Soulmate Name", GameData.announceSoulmate, lore));
        lore.clear();

        lore.add("Disables crafting enchantment");
        lore.add("tables");
        lore.add("(For servers that have");
        lore.add("only one enchanting table)");
        inv.setItem(2, getItem(Material.ENCHANTING_TABLE, "Uncraftable Enchating Table", GameData.canCraftEnchantingTable, lore));
        lore.clear();

        lore.add("Toggles the custom TNT");
        lore.add("recipe that uses less");
        lore.add("gunpowder");
        inv.setItem(3, getItem(Material.TNT, "Custom TNT Recipe", GameData.canCraftEnchantingTable, lore));
        lore.clear();

        lore.add("Allows the pairs to");
        lore.add("share a hunger bar");
        inv.setItem(4, getItem(Material.COOKED_BEEF, "Hunger Sharing", GameData.isSharingHunger, lore));
        lore.clear();

        lore.add("Manage the amount of");
        lore.add("starting lives each");
        lore.add("team has");
        inv.setItem(5, getItem(Material.NETHER_STAR, "Manage Lives", lore));
        lore.clear();

        lore.add("Manage the predetermined");
        lore.add("starting teams");
        inv.setItem(6, getItem(Material.TOTEM_OF_UNDYING, "Manage Teams", lore));
        lore.clear();

        return inv;
    }



    public static Inventory getPredeterminedMenu(int scrollAmount) {
        Inventory inv = Bukkit.createInventory(null, 54, "Predetermined Groups - " + scrollAmount);

        ArrayList<String> lore = new ArrayList<>();
        ItemStack empty = getItem(Material.GRAY_STAINED_GLASS_PANE, " ", lore);

        for (int i = 0; i < 5; i++) {
            if (GameData.predeterminedGroups.size() > (scrollAmount + i) * 2) {
                inv.setItem(i * 9, empty);
                inv.setItem(i * 9 + 1, getItem(Material.PLAYER_HEAD, Bukkit.getOfflinePlayer(GameData.predeterminedGroups.get((scrollAmount + i) * 2)).getName(), lore));
                inv.setItem(i * 9 + 2, empty);
                inv.setItem(i * 9 + 3, getItem(Material.PLAYER_HEAD, Bukkit.getOfflinePlayer(GameData.predeterminedGroups.get((scrollAmount + i) * 2 + 1)).getName(), lore));
                inv.setItem(i * 9 + 4, empty);
                inv.setItem(i * 9 + 5, empty);
                inv.setItem(i * 9 + 6, empty);
                inv.setItem(i * 9 + 7, getItem(Material.BARRIER, ChatColor.RED + "Remove Group", lore));
                inv.setItem(i * 9 + 8, empty);
            }
        }

        if ((scrollAmount) * 2 < GameData.predeterminedGroups.size()) {
            inv.setItem(53, getItem(Material.DIAMOND, "Down", lore));
        }
        if (scrollAmount > 0) {
            inv.setItem(52, getItem(Material.COAL, "Up", lore));
        }
        inv.setItem(51, getItem(Material.NETHER_STAR, "Add Team", lore));
        inv.setItem(50, getItem(Material.ARROW, "Back", lore));

        inv.setItem(45, empty);
        inv.setItem(46, empty);
        inv.setItem(47, empty);
        inv.setItem(48, empty);
        inv.setItem(49, empty);
        return inv;
    }

    public static Inventory createPredeterminedTeam(UUID uuid, boolean gameStarted, GameData gameData) {
        Inventory inv = Bukkit.createInventory(null, 54, "Create Predetermined Group");

        ArrayList<String> lore = new ArrayList<>();
        int i = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (GameData.predeterminedGroups.contains(p.getUniqueId())) {
                continue;
            }
            if (p.getUniqueId() == uuid) {
                continue;
            }
            if (gameStarted) {
                if (gameData.uuidUserPair.containsKey(p.getUniqueId())) {
                    continue;
                }
            }
            ItemStack head = getItem(Material.PLAYER_HEAD, p.getDisplayName(), lore);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(p);
            head.setItemMeta(meta);

            inv.setItem(i, head);

            i += 1;
            if (i == 45) {
                break;
            }
        }

        if (uuid != null) {
            ItemStack head = getItem(Material.PLAYER_HEAD, Bukkit.getOfflinePlayer(uuid).getName(), lore);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
            head.setItemMeta(meta);
            inv.setItem(53, head);
        }

        return inv;
    }

    public static Inventory getLifeCountManager() {
        Inventory inv = Bukkit.createInventory(null, 9, "Settings - Life Count");

        ArrayList<String> lore = new ArrayList<>();

        if (GameData.lifeCountEnabled) {
            lore.add("Decrease the starting");
            lore.add("life count");
            lore.add("");
            lore.add("Currently: " + GameData.startingLives);
            inv.setItem(3, getItem(Material.COAL, "Decrease", lore));
            lore.clear();

            if (GameData.startingLives == 1) {
                inv.setItem(4, getItem(Material.NETHER_STAR, GameData.startingLives + " Life", lore));
            } else {
                inv.setItem(4, getItem(Material.NETHER_STAR, GameData.startingLives + " Lives", lore));
            }

            lore.add("Increase the starting");
            lore.add("life count");
            lore.add("");
            lore.add("Currently: " + GameData.startingLives);
            inv.setItem(5, getItem(Material.DIAMOND, "Increase", lore));
            lore.clear();
        }

        lore.add("Toggle if players have");
        lore.add("a life count or if they");
        lore.add("live forever.");
        inv.setItem(7, getItem(Material.REDSTONE, "Toggle Lives", GameData.lifeCountEnabled, lore));
        lore.clear();

        inv.setItem(8, getItem(Material.ARROW, "Back", lore));
        lore.clear();

        return inv;
    }

    public static ItemStack getItem(Material material, String name, boolean isSelected, ArrayList<String> oldLore) {
        ArrayList<String> lore = new ArrayList<>();
        for (String str : oldLore) {
            lore.add(ChatColor.WHITE + str);
        }
        lore.add("");
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + name);
        if (isSelected) {
            meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
            lore.add(ChatColor.GREEN + "Enabled: True");
        } else {
            lore.add(ChatColor.RED + "Enabled: False");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack getItem(Material material, String name, ArrayList<String> oldLore) {
        ArrayList<String> lore = new ArrayList<>();
        for (String str : oldLore) {
            lore.add(ChatColor.WHITE + str);
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}

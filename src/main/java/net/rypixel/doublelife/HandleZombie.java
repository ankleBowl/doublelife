package net.rypixel.doublelife;

import org.bukkit.Bukkit;
import org.bukkit.entity.Husk;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.UUID;

public class HandleZombie {

    public static void onEntityDamage(EntityDamageEvent event, DoubleLife doubleLife) {
        Husk zombie = (Husk) event.getEntity();
        if (!doubleLife.huskToUUID.containsKey(zombie)) {
            return;
        }
        UUID playerUUID = doubleLife.huskToUUID.get(zombie);
        UserPair pair = doubleLife.gameData.uuidUserPair.get(playerUUID);

        if (pair.sharedHealth - event.getFinalDamage() <= 0) {
            event.setCancelled(true);
            zombie.remove();
            killPlayers(zombie, doubleLife);
            event.setCancelled(true);
        } else {
            Bukkit.broadcastMessage(String.valueOf(event.getFinalDamage()));
            damagePair(pair, event.getFinalDamage(), doubleLife);
            event.setCancelled(true);
        }
    }

    public static void killPlayers(Husk zombie, DoubleLife doubleLife) {
        UUID playerUUID = doubleLife.huskToUUID.get(zombie);
        UserPair pair = doubleLife.gameData.uuidUserPair.get(playerUUID);
        pair.killPlayers(null);
        if (doubleLife.uuidToHusk.containsKey(pair.player1)) {
            doubleLife.uuidToHusk.get(pair.player1).remove();
            doubleLife.uuidToHusk.remove(pair.player1);
        }
        if (doubleLife.uuidToHusk.containsKey(pair.player2)) {
            doubleLife.uuidToHusk.get(pair.player2).remove();
            doubleLife.uuidToHusk.remove(pair.player2);
        }
        if (pair.sharedLives > 2) {
            return;
        } else if (pair.sharedLives == 2) {
            doubleLife.threeLives.removePlayer(Bukkit.getOfflinePlayer(pair.player1));
            doubleLife.threeLives.removePlayer(Bukkit.getOfflinePlayer(pair.player2));
            doubleLife.twoLives.addPlayer(Bukkit.getOfflinePlayer(pair.player1));
            doubleLife.twoLives.addPlayer(Bukkit.getOfflinePlayer(pair.player2));
        } else if (doubleLife.gameData.uuidUserPair.get(playerUUID).sharedLives == 1) {
            doubleLife.twoLives.removePlayer(Bukkit.getOfflinePlayer(pair.player1));
            doubleLife.twoLives.removePlayer(Bukkit.getOfflinePlayer(pair.player2));
            doubleLife.oneLife.addPlayer(Bukkit.getOfflinePlayer(pair.player1));
            doubleLife.oneLife.addPlayer(Bukkit.getOfflinePlayer(pair.player1));
            doubleLife.oneLife.addPlayer(Bukkit.getOfflinePlayer(pair.player2));
        } else {
            doubleLife.oneLife.removePlayer(Bukkit.getOfflinePlayer(pair.player1));
            doubleLife.oneLife.removePlayer(Bukkit.getOfflinePlayer(pair.player2));
            doubleLife.dead.addPlayer(Bukkit.getOfflinePlayer(pair.player1));
            doubleLife.dead.addPlayer(Bukkit.getOfflinePlayer(pair.player2));
        }
    }

    static void damagePair(UserPair pair, double finalDamage, DoubleLife doubleLife) {
        pair.damage(null, pair.sharedHealth - finalDamage);
    }
}

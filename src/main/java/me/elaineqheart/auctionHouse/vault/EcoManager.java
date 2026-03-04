package me.elaineqheart.auctionHouse.vault;

import me.elaineqheart.auctionHouse.data.persistentStorage.local.data.ConfigManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class EcoManager {

    // Sprawdza czy jest ustawiona waluta w postaci itemu
    public static boolean useItemEconomy() {
        return getCurrencyItem() != null;
    }

    public static ItemStack getCurrencyItem() {
        return ConfigManager.layout.getCustomFile().getItemStack("currency-item");
    }

    public static void setCurrencyItem(ItemStack item) {
        ConfigManager.layout.getCustomFile().set("currency-item", item);
        ConfigManager.layout.save();
    }

    // Pobiera stan konta (lub liczy itemy w eq)
    public static double getBalance(Player p) {
        if (!useItemEconomy()) return VaultHook.getEconomy().getBalance(p);
        
        int count = 0;
        for (ItemStack i : p.getInventory().getContents()) {
            if (i != null && i.isSimilar(getCurrencyItem())) count += i.getAmount();
        }
        return count;
    }

    // Zabiera kase lub itemy
    public static void withdraw(Player p, double amount) {
        if (!useItemEconomy()) {
            VaultHook.getEconomy().withdrawPlayer(p, amount);
            return;
        }
        int remaining = (int) amount;
        for (int i = 0; i < p.getInventory().getSize(); i++) {
            ItemStack item = p.getInventory().getItem(i);
            if (item != null && item.isSimilar(getCurrencyItem())) {
                if (item.getAmount() <= remaining) {
                    remaining -= item.getAmount();
                    p.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - remaining);
                    remaining = 0;
                }
                if (remaining <= 0) break;
            }
        }
    }

    // Daje kase lub wyrzuca fizyczny item do gracza
    public static void deposit(OfflinePlayer p, double amount) {
        if (!useItemEconomy()) {
            VaultHook.getEconomy().depositPlayer(p, amount);
            return;
        }
        if (p.isOnline() && p.getPlayer() != null) {
            Player player = p.getPlayer();
            ItemStack toGive = getCurrencyItem().clone();
            toGive.setAmount((int) amount);
            
            // Jesli ma pelne eq, wyrzuca walute na ziemie
            HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(toGive);
            for (ItemStack drop : leftOver.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
        // Jeśli jest offline, plugin natywnie i tak przechowuje to do czasu aż gracz zaloguje się
        // i odbierze przedmioty w menu "Collect Sold Item" lub natywnym Auto-Collect.
    }
}

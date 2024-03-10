package org.atm;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class AtmGui implements Listener {

    private final Economy econ;

    public AtmGui(Economy econ) {
        this.econ = econ;
    }

    public void openGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, InventoryType.HOPPER, ChatColor.DARK_GREEN + "ATM");

        inv.setItem(0, createGuiItem(Material.GREEN_WOOL, ChatColor.GREEN + "Dépôt Tout"));
        inv.setItem(1, createGuiItem(Material.YELLOW_WOOL, ChatColor.YELLOW + "Retirer 5", ChatColor.WHITE + "Clique pour retirer $5"));
        inv.setItem(2, createGuiItem(Material.ORANGE_WOOL, ChatColor.GOLD + "Retirer 10", ChatColor.WHITE + "Clique pour retirer $10"));
        inv.setItem(3, createGuiItem(Material.RED_WOOL, ChatColor.RED + "Retirer 20", ChatColor.WHITE + "Clique pour retirer $20"));
        inv.setItem(4, createGuiItem(Material.PAPER, ChatColor.AQUA + "Solde Actuel", ChatColor.WHITE + "Votre solde: $" + econ.getBalance(player)));


        player.openInventory(inv);
    }

    private ItemStack createGuiItem(final Material material, final String name, final String... lore) {
        final ItemStack item = new ItemStack(material, 1);
        final ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));

        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ChatColor.DARK_GREEN + "ATM")) {
            event.setCancelled(true);
            final Player player = (Player) event.getWhoClicked();
            final ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            switch (clickedItem.getType()) {
                case GREEN_WOOL:
                    player.performCommand("atm deposit");
                    break;
                case YELLOW_WOOL:
                    player.performCommand("atm withdraw 5");
                    break;
                case ORANGE_WOOL:
                    player.performCommand("atm withdraw 10");
                    break;
                case RED_WOOL:
                    player.performCommand("atm withdraw 20");
                    break;
                case PAPER:
                    // Juste pour afficher le solde, aucune action nécessaire ici.
                    break;
                default:
                    break;
            }
            player.closeInventory(); // Fermer l'inventaire après une action pour éviter les clics multiples.
        }
    }
}
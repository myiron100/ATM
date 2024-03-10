package org.atm;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;



import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ATM extends JavaPlugin implements CommandExecutor {

        private Economy econ;

    @Override
    public void onEnable() {
        if (!setupEconomy()) {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Enregistrement de la commande atm avec l'instance actuelle comme exécuteur
        Objects.requireNonNull(this.getCommand("atm")).setExecutor(this);

        // Enregistrement de la commande atmgui pour ouvrir le GUI
        Objects.requireNonNull(this.getCommand("atmgui")).setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Seuls les joueurs peuvent exécuter cette commande.");
                return true;
            }

            Player player = (Player) sender;
            new AtmGui(econ).openGui(player);
            return true;
        });

        // Enregistrement du Listener pour le GUI
        getServer().getPluginManager().registerEvents(new AtmGui(econ), this);
    }


        private boolean setupEconomy() {
            if (getServer().getPluginManager().getPlugin("Vault") == null) {
                return false;
            }
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp == null) {
                return false;
            }
            econ = rsp.getProvider();
            return true;
        }

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Cette commande ne peut être utilisée que par un joueur.");
                return true;
            }

            Player player = (Player) sender;

            if (args.length == 0) {
                player.sendMessage("Usage: /atm <deposit|withdraw> [amount]");
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "deposit":
                    depositItems(player);
                    break;
                case "withdraw":
                    if (args.length < 2) {
                        player.sendMessage("Usage: /atm withdraw [amount]");
                        return true;
                    }
                    withdrawItems(player, Integer.parseInt(args[1]));
                    break;
                default:
                    player.sendMessage("Usage: /atm <deposit|withdraw> [amount]");
                    break;
            }

            return true;
        }

        private void depositItems(Player player) {
            ItemStack[] items = player.getInventory().getContents();
            double totalValue = 0;
            Map<Material, Integer> valueMap = getValueMap();

            for (ItemStack item : items) {
                if (item == null) continue;
                if (valueMap.containsKey(item.getType())) {
                    totalValue += valueMap.get(item.getType()) * item.getAmount();
                    player.getInventory().remove(item);
                }
            }

            econ.depositPlayer(player, totalValue);
            player.sendMessage(String.format("Vous avez déposé des minerais pour une valeur de $%.2f dans votre compte.", totalValue));
        }

    private void withdrawItems(Player player, int amount) {
        if (amount <= 0) {
            player.sendMessage("Le montant doit être supérieur à 0.");
            return;
        }

        if (!econ.has(player, amount)) {
            player.sendMessage("Vous n'avez pas assez d'argent dans votre compte.");
            return;
        }

        // Cette liste doit être ordonnée par valeur décroissante
        List<Map.Entry<Material, Integer>> sortedMaterials = new ArrayList<>(getValueMap().entrySet());
        sortedMaterials.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        int remainingAmount = amount;
        Map<Material, Integer> materialsToWithdraw = new LinkedHashMap<>();

        for (Map.Entry<Material, Integer> entry : sortedMaterials) {
            int materialValue = entry.getValue();
            Material material = entry.getKey();
            int maxItems = remainingAmount / materialValue;
            if (maxItems > 0) {
                int totalValue = maxItems * materialValue;
                remainingAmount -= totalValue;
                materialsToWithdraw.put(material, maxItems);

                if (remainingAmount == 0) break; // Stop if the exact amount is matched
            }
        }

        if (remainingAmount > 0) {
            player.sendMessage(ChatColor.RED + "Impossible de retirer le montant exact. Retrait du montant le plus proche.");
        }

        // Process the withdrawal
        econ.withdrawPlayer(player, amount - remainingAmount); // Withdraw the closest possible amount
        for (Map.Entry<Material, Integer> entry : materialsToWithdraw.entrySet()) {
            player.getInventory().addItem(new ItemStack(entry.getKey(), entry.getValue()));
            player.sendMessage(String.format(ChatColor.GREEN + "Vous avez retiré %d de %s.", entry.getValue(), entry.getKey().toString().toLowerCase()));
        }
        player.sendMessage(ChatColor.GREEN + "Transaction réussie pour un montant de $" + (amount - remainingAmount) + ".");
    }


        private Map<Material, Integer> getValueMap() {
            Map<Material, Integer> valueMap = new HashMap<>();
            valueMap.put(Material.IRON_INGOT, 5);
            valueMap.put(Material.GOLD_INGOT, 10);
            //valueMap.put(Material.DIAMOND, 20);
            return valueMap;
        }
    }

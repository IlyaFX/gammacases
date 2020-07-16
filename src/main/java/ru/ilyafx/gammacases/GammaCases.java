package ru.ilyafx.gammacases;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;
import ru.ilyafx.gammacases.animation.AnimationProvider;
import ru.ilyafx.gammacases.animation.impl.CircleAnimationProvider;
import ru.ilyafx.gammacases.type.Case;
import ru.ilyafx.gammacases.type.CaseItem;
import ru.ilyafx.gammacases.type.Key;
import ru.ilyafx.gammacases.util.DataUtil;
import ru.ilyafx.gammacases.util.NBTUtil;
import ru.ilyafx.gammacases.util.V3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class GammaCases extends JavaPlugin implements Listener {

    @Getter
    private static GammaCases instance;

    private Map<String, Case> caseMap = new HashMap<>();
    private Map<V3, CaseData> caseDataMap = new HashMap<>();
    private Map<String, String> messages = new HashMap<>();
    private final Map<String, AnimationProvider> animationProviderMap = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        registerAnimation("circle", new CircleAnimationProvider());
        reload();
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("gammacases").setExecutor((sender, command, label, args) -> {
            if (!sender.hasPermission("gammacases.*"))
                return true;
            if (args.length == 0) {
                sender.sendMessage("/gammacases addblock/reload/givekey");
                return true;
            }
            if (args.length == 1) {
                switch (args[0]) {
                    case "reload":
                        reload();
                        sender.sendMessage("Reloaded!");
                        break;
                    case "addblock":
                        sender.sendMessage("Use: /gammacases addblock CASE_ID");
                        break;
                    case "givekey":
                        sender.sendMessage("Use: /gammacases givekey PLAYER CASE_ID KEY_ID AMOUNT");
                        break;
                }
                return true;
            }
            if (args.length == 2 && args[0].equals("addblock")) {
                String caseId = args[1];
                Case caseObj = caseMap.get(caseId);
                if (caseObj == null) {
                    sender.sendMessage("Supported cases: " + String.join(",", caseMap.keySet()));
                    return true;
                }
                Player player = (Player) sender;
                Block block = player.getTargetBlock(null, 50);
                if (block.getType() == Material.AIR) {
                    sender.sendMessage("You have to look at the block");
                    return true;
                }
                caseDataMap.put(V3.fromLocation(block.getLocation()), new CaseData(caseObj, false));
                List<String> list = getConfig().getStringList("cases." + caseId + ".blocks");
                list.add(DataUtil.serializeLocation(block.getLocation(), true));
                getConfig().set("cases." + caseId + ".blocks", list);
                saveConfig();
            }
            if (args.length == 5 && args[0].equals("givekey")) {
                try {
                    Player player = Objects.requireNonNull(Bukkit.getPlayer(args[1]), "Player must be online");
                    Case caseObj = Objects.requireNonNull(caseMap.get(args[2]), "Supported cases: " + String.join(",", caseMap.keySet()));
                    Key key = Objects.requireNonNull(caseObj.getKeys().get(args[3]), "Supported keys: " + String.join("," + caseObj.getKeys().keySet()));
                    int amount = Integer.parseInt(args[4]);
                    player.getInventory().addItem(key.supply(amount));
                    sender.sendMessage("Success!");
                } catch (NullPointerException exc) {
                    sender.sendMessage(exc.getMessage());
                } catch (NumberFormatException ex) {
                    sender.sendMessage("Bad amount provided: " + args[4]);
                }
            }
            return true;
        });
    }

    @EventHandler
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && event.getHand().equals(EquipmentSlot.HAND)) {
            V3 v3 = V3.fromLocation(event.getClickedBlock().getLocation());
            CaseData caseData;
            if ((caseData = caseDataMap.get(v3)) != null) {
                event.setCancelled(true);
                if (caseData.isLocked()) {
                    event.getPlayer().sendMessage(messages.get("opening"));
                    return;
                }
                ItemStack item = event.getItem();
                String value;
                if (item != null && (value = NBTUtil.readNBT(item, "gammacase")) != null) {
                    if (!value.equals(caseData.getCaseObj().getId())) {
                        event.getPlayer().sendMessage(messages.get("badkey"));
                        return;
                    }
                    item.setAmount(item.getAmount() - 1);
                    caseData.getCaseObj().open(event.getPlayer(), v3, caseData);
                } else {
                    event.getPlayer().sendMessage(messages.get("notkey"));
                }
            }
        }
    }

    public void registerAnimation(String id, AnimationProvider provider) {
        animationProviderMap.put(id, provider);
    }

    @SuppressWarnings("all")
    private void reload() {
        reloadConfig();
        Map<V3, CaseData> map = new HashMap<>();
        Map<String, Case> caseMap = new HashMap<>();
        this.messages = getConfig().getConfigurationSection("messages").getKeys(false).stream().collect(Collectors.toMap(id -> id, id -> ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages." + id))));
        getConfig().getConfigurationSection("cases").getKeys(false).forEach(caseId -> {
            ConfigurationSection caseSection = getConfig().getConfigurationSection("cases." + caseId);
            Map<String, CaseItem> caseItems = caseSection.getConfigurationSection("items").getKeys(false).stream().map(itemId -> {
                ConfigurationSection itemSection = caseSection.getConfigurationSection("items." + itemId);
                MaterialData materialData = DataUtil.dataFromString(itemSection.getString("type"));
                return new CaseItem(itemId, materialData.getItemType(), materialData.getData(), ChatColor.translateAlternateColorCodes('&', itemSection.getString("name")), itemSection.getDouble("chance"), itemSection.getStringList("commands"));
            }).collect(Collectors.toMap(CaseItem::getId, item -> item));
            Map<String, Key> keys = caseSection.getConfigurationSection("keys").getKeys(false).stream().map(keyId -> {
                ConfigurationSection keySection = caseSection.getConfigurationSection("keys." + keyId);
                List<String> lore = keySection.getStringList("lore").stream().map(line -> ChatColor.translateAlternateColorCodes('&', line)).collect(Collectors.toList());
                String name = ChatColor.translateAlternateColorCodes('&', keySection.getString("name"));
                MaterialData data = DataUtil.dataFromString(keySection.getString("type"));
                return new Key(caseId, keyId, data.getItemType(), data.getData(), name, lore);
            }).collect(Collectors.toMap(Key::getId, key -> key));
            String animationId = caseSection.contains("animation") ? caseSection.getString("animation") : "circle";
            Case caseObj = new Case(caseId, () -> animationProviderMap.get(animationId), caseItems, keys);
            caseMap.put(caseId, caseObj);
            caseSection.getStringList("blocks").stream().map(location -> V3.fromLocation(DataUtil.deserializeLocation(location))).forEach(v3 -> map.put(v3, new CaseData(caseObj, false)));
        });
        this.caseDataMap = map;
        this.caseMap = caseMap;
    }

    @AllArgsConstructor
    @Getter
    public static class CaseData {

        private final Case caseObj;
        @Setter
        private volatile boolean locked;

        public void lock() {
            locked = true;
        }

        public void unlock() {
            locked = false;
        }

    }
}

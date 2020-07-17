package ru.ilyafx.gammacases;

import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;
import ru.ilyafx.gammacases.animation.AnimationProvider;
import ru.ilyafx.gammacases.animation.impl.CircleAnimationProvider;
import ru.ilyafx.gammacases.database.Database;
import ru.ilyafx.gammacases.smartinvs.ClickableItem;
import ru.ilyafx.gammacases.smartinvs.InventoryManager;
import ru.ilyafx.gammacases.smartinvs.SmartInventory;
import ru.ilyafx.gammacases.smartinvs.content.InventoryContents;
import ru.ilyafx.gammacases.smartinvs.content.InventoryProvider;
import ru.ilyafx.gammacases.smartinvs.content.Pagination;
import ru.ilyafx.gammacases.smartinvs.content.SlotIterator;
import ru.ilyafx.gammacases.type.Case;
import ru.ilyafx.gammacases.type.CaseItem;
import ru.ilyafx.gammacases.util.DataUtil;
import ru.ilyafx.gammacases.util.NBTUtil;
import ru.ilyafx.gammacases.util.V3;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GammaCases extends JavaPlugin implements Listener {

    @Getter
    private static GammaCases instance;

    @Getter
    private InventoryManager inventoryManager;

    @Getter
    private Database database;

    private Map<String, String> messages = new HashMap<>();
    private final Map<String, AnimationProvider> animationProviderMap = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        registerAnimation("circle", new CircleAnimationProvider());
        reload();
        this.database = new Database(getConfig().getString("data.host"), getConfig().getInt("data.port"), getConfig().getString("data.user"), getConfig().getString("data.password"), getConfig().getString("data.database"));
        database.init(this);
        this.inventoryManager = new InventoryManager(this);
        inventoryManager.init();
        getCommand("gammacases").setExecutor((sender, command, label, args) -> {
            if (!sender.hasPermission("gammacases.*"))
                return true;
            if (args.length == 0) {
                sender.sendMessage("/gammacases addblock/reload/givebox/seticon/additem/addbox");
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
                    case "givebox":
                        sender.sendMessage("Use: /gammacases givebox PLAYER BOX_ID AMOUNT");
                        break;
                    case "seticon":
                        sender.sendMessage("Use: /gammacases seticon BOX_ID");
                        break;
                    case "additem":
                        sender.sendMessage("Use: /gammacases additem BOX_ID CHANCE NAME COMMANDS(split by ;;;)");
                        break;
                    case "addbox":
                        sender.sendMessage("Use: /gammacases addbox CASE_ID BOX_ID");
                        break;
                }
                return true;
            }
            if (args.length == 3 && args[0].equals("addbox")) {
                String caseId = args[1];
                String boxId = args[2];
                database.addBox(caseId, boxId);
                sender.sendMessage("Done!");
            }
            if (args.length >= 5 && args[0].equals("additem")) {
                Player player = (Player) sender;
                ItemStack itemStack = player.getInventory().getItemInMainHand();
                if (itemStack.getType() == Material.AIR) {
                    player.sendMessage("You must hold an itemstack in your main hand");
                    return true;
                }
                String boxId = args[1];
                double chance = Double.parseDouble(args[2]);
                String name = args[3];
                String commands = Stream.of(args).skip(4).collect(Collectors.joining(" "));
                database.addItem(boxId, itemStack.getData(), name, chance, commands);
                sender.sendMessage("Done!");
            }
            if (args.length == 2 && args[0].equals("addblock")) {
                String caseId = args[1];
                Case caseObj = database.getCasesMap().get(caseId);
                if (caseObj == null) {
                    sender.sendMessage("Supported cases: " + String.join(",", database.getCasesMap().keySet()));
                    return true;
                }
                Player player = (Player) sender;
                Block block = player.getTargetBlock(null, 50);
                if (block.getType() == Material.AIR) {
                    sender.sendMessage("You have to look at the block");
                    return true;
                }
                database.addLocation(caseObj.getId(), block.getLocation());
                sender.sendMessage("Done!");
            }
            if (args.length == 2 && args[0].equals("seticon")) {
                Player source = ((Player) sender);
                ItemStack itemStack = source.getInventory().getItemInMainHand();
                if (itemStack.getType() == Material.AIR) {
                    source.sendMessage("You must hold an itemstack in your main hand");
                    return true;
                }
                String boxId = args[1];
                if (!database.getBoxesItemsMap().containsKey(boxId))
                    throw new NullPointerException("Supported boxes: " + String.join("," + database.getBoxesItemsMap().keySet()));
                database.setIcon(boxId, itemStack);
                sender.sendMessage("Done!");
            }
            if (args.length == 4 && args[0].equals("givebox")) {
                try {
                    Player player = Objects.requireNonNull(Bukkit.getPlayer(args[1]), "Player must be online");
                    String boxId = args[2];
                    if (!database.getBoxesItemsMap().containsKey(boxId))
                        throw new NullPointerException("Supported boxes: " + String.join("," + database.getBoxesItemsMap().keySet()));
                    int amount = Integer.parseInt(args[3]);
                    for (int i = 0; i<amount; i++) {
                        Database.Box box = new Database.Box(UUID.randomUUID().toString(), boxId, player.getUniqueId());
                        database.addBox(box);
                    }
                    sender.sendMessage("Success!");
                } catch (NullPointerException exc) {
                    sender.sendMessage(exc.getMessage());
                } catch (NumberFormatException ex) {
                    sender.sendMessage("Bad amount provided: " + args[3]);
                }
            }
            return true;
        });
    }

    @EventHandler
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && event.getHand().equals(EquipmentSlot.HAND)) {
            V3 v3 = V3.fromLocation(event.getClickedBlock().getLocation());
            Database.CaseData caseData;
            if ((caseData = database.getCaseDataMap().get(v3)) != null) {
                event.setCancelled(true);
                if (caseData.isLocked()) {
                    event.getPlayer().sendMessage(messages.get("opening"));
                    return;
                }
                openCaseGui(event.getPlayer(), caseData, 0);
            }
        }
    }

    @EventHandler
    public void onPlayerLogin(AsyncPlayerPreLoginEvent event) {
        database.onLogin(event);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        database.getUserDataMap().remove(event.getPlayer().getUniqueId());
    }

    public void registerAnimation(String id, AnimationProvider provider) {
        animationProviderMap.put(id, provider);
    }

    public AnimationProvider getAnimation(String id) {
        return animationProviderMap.get(id);
    }

    @SuppressWarnings("all")
    private void reload() {
        reloadConfig();
        this.messages = getConfig().getConfigurationSection("messages").getKeys(false).stream().collect(Collectors.toMap(id -> id, id -> ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages." + id))));
    }

    private void openCaseGui(Player player, Database.CaseData caseObj, int page) {
        SmartInventory.builder().provider(new CaseInventoryProvider(caseObj)).title("Кейс").build().open(player, page);
    }

    public void simpleTimer(Runnable runnable, long ticks) {
        runnable.run();
        Bukkit.getScheduler().runTaskTimer(this, runnable, ticks, ticks);
    }

    @RequiredArgsConstructor
    private class CaseInventoryProvider implements InventoryProvider {

        private final Database.CaseData caseData;

        private byte data = 0;
        private int tick = 0;

        @Override
        public void init(Player player, InventoryContents contents) {
            Case caseObj = caseData.getCaseObj().get();
            if (caseObj == null)
                return;
            Map<String, List<Database.Box>> playerData = database.getUserDataMap().get(player.getUniqueId());
            ClickableItem[] items = playerData
                    .entrySet()
                    .stream()
                    .filter(entry -> caseObj.canOpen(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .flatMap(List::stream)
                    .map(box -> {
                        ItemStack item = database.getBoxItemStack(box.getBoxId());
                        if (item == null)
                            return null;
                        return ClickableItem.of(item, event -> {
                            if (!caseData.isLocked()) {
                                database.removeBox(box);
                                playerData.get(box.getBoxId()).removeIf(boxObj -> boxObj.getId().equals(box.getId()));
                                caseObj.open(player, box.getBoxId(), caseData);
                                player.closeInventory();
                            }
                        });
                    })
                    .toArray(ClickableItem[]::new);
            contents.setProperty("items", items);
        }

        @Override
        public void update(Player player, InventoryContents contents) {
            ClickableItem[] items = contents.property("items");
            Pagination pagination = contents.pagination();
            pagination.setItems(items);
            pagination.setItemsPerPage(7 * 4);
            SlotIterator iterator = contents.newIterator(SlotIterator.Type.HORIZONTAL, 1, 1);
            for (int i = 1; i<=4; i++)
                iterator.blacklist(i, 0).blacklist(i, 8);
            pagination.addToIterator(iterator);
            if (tick++ % 10 == 0) {
                contents.fillBorders(ClickableItem.empty(DataUtil.itemStack(Material.LEGACY_STAINED_GLASS_PANE, data, 1, "  ")));
                data = (byte) ((data + 1) % 16);
            }
            contents.set(5, 1, ClickableItem.of(DataUtil.itemStack(Material.ARROW, 1, "§eПредыдущая страница"), event -> openCaseGui(player, caseData, pagination.previous().getPage())));
            contents.set(5, 7, ClickableItem.of(DataUtil.itemStack(Material.ARROW, 1, "§eСледующая страница"), event -> openCaseGui(player, caseData, pagination.next().getPage())));
        }
    }

}

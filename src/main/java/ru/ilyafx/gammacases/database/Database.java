package ru.ilyafx.gammacases.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import ru.ilyafx.gammacases.GammaCases;
import ru.ilyafx.gammacases.database.factory.AsyncQueryFactory;
import ru.ilyafx.gammacases.database.factory.SyncQueryFactory;
import ru.ilyafx.gammacases.type.Case;
import ru.ilyafx.gammacases.type.CaseItem;
import ru.ilyafx.gammacases.util.DataUtil;
import ru.ilyafx.gammacases.util.V3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Getter
public class Database {

    private static final Consumer<Throwable> CATCHER = Throwable::printStackTrace;
    public static ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 10L, TimeUnit.SECONDS, new SynchronousQueue<>());
    static {
        EXECUTOR.allowCoreThreadTimeOut(true);
        EXECUTOR.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    }

    private final String host;
    private final int port;
    private final String user;
    private final String password;
    private final String database;

    private SyncQueryFactory sync;
    private AsyncQueryFactory async;

    private final Map<UUID, Map<String, List<Box>>> userDataMap = new ConcurrentHashMap<>();
    private Map<String, ItemStack> itemsMap = new HashMap<>(1);
    private Map<String, Case> casesMap = new HashMap<>(1);
    private Map<String, List<CaseItem>> boxesItemsMap = new HashMap<>(1);
    private Map<V3, CaseData> caseDataMap = new HashMap<>(1);

    public void init(GammaCases cases) {
        HikariConfig config = new HikariConfig();
        config.setConnectionTimeout(5000);
        config.setMaximumPoolSize(10);
        config.setAutoCommit(true);
        config.setJdbcUrl(String.format("jdbc:mysql://%s:%s/%s" +
                "?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC", host, port, database));
        config.setUsername(user);
        config.setPassword(password);
        HikariDataSource dataSource = new HikariDataSource(config);

        this.sync = new SyncQueryFactory(dataSource, CATCHER);
        this.async = new AsyncQueryFactory(dataSource, CATCHER, EXECUTOR);

        initializeTables();
        long reloadTime = 20*60*2;
        cases.simpleTimer(this::reloadBoxesItems, reloadTime);
        cases.simpleTimer(this::reloadBoxesIcons, reloadTime);
        cases.simpleTimer(this::reloadCasesBoxes, reloadTime);
        cases.simpleTimer(this::reloadCasesLocations, reloadTime);
    }

    // language=sql
    private void initializeTables() {
        this.sync.unsafeUpdate("CREATE TABLE IF NOT EXISTS `cases_locations` (`location` varchar(150) NOT NULL, `case` varchar(50) NOT NULL, PRIMARY KEY (`location`)) ENGINE=InnoDB DEFAULT CHARSET=utf8");
        this.sync.unsafeUpdate("CREATE TABLE IF NOT EXISTS `boxes_icons` (`box` varchar(50) NOT NULL, `icon` TEXT NOT NULL, PRIMARY KEY (`box`)) ENGINE=InnoDB DEFAULT CHARSET=utf8");
        this.sync.unsafeUpdate("CREATE TABLE IF NOT EXISTS `cases_boxes` (`case` varchar(150) NOT NULL, `box` varchar(50) NOT NULL, PRIMARY KEY (`case`,`box`)) ENGINE=InnoDB DEFAULT CHARSET=utf8");
        this.sync.unsafeUpdate("CREATE TABLE IF NOT EXISTS `players_boxes` (`id` varchar(50) NOT NULL, `player` varchar(50) NOT NULL, `box` varchar(50) NOT NULL, PRIMARY KEY (`id`)) ENGINE=InnoDB DEFAULT CHARSET=utf8");
        this.sync.unsafeUpdate("CREATE TABLE IF NOT EXISTS `boxes_items` (`id` int(11) NOT NULL AUTO_INCREMENT, `box` varchar(50) NOT NULL, `type` varchar(50) NOT NULL, `name` varchar(100) NOT NULL, `chance` double NOT NULL, `commands` TEXT NOT NULL, PRIMARY KEY (`id`)) ENGINE=InnoDB DEFAULT CHARSET=utf8");
    }

    public void addBox(String caseId, String boxId) {
        Case caseObj = casesMap.computeIfAbsent(caseId, __ -> new Case(caseId, () -> GammaCases.getInstance().getAnimation("circle"), new HashMap<>()));
        caseObj.getItemsSupplier().put(boxId, () -> boxesItemsMap.get(boxId));
        this.async.prepareUpdate("INSERT INTO `cases_boxes` VALUES (?,?)", ps -> {
            try {
                ps.setString(1, caseId);
                ps.setString(2, boxId);
            } catch(Exception ignored) {}
        });
    }

    public void addItem(String boxId, MaterialData data, String name, double chance, String commands) {
        CaseItem caseItem = new CaseItem(UUID.randomUUID().toString(), data.getItemType(), data.getData(), ChatColor.translateAlternateColorCodes('&', name), chance, Stream.of(commands.split(";;;")).collect(Collectors.toList()));
        boxesItemsMap.computeIfAbsent(boxId, __ -> new ArrayList<>()).add(caseItem);
        this.async.prepareUpdate("INSERT INTO `boxes_items` (`box`,`type`,`name`,`chance`,`commands`) VALUES (?,?,?,?,?)", ps -> {
            try {
                ps.setString(1, boxId);
                ps.setString(2, DataUtil.dataToString(data));
                ps.setString(3, name);
                ps.setDouble(4, chance);
                ps.setString(5, commands);
            } catch(Exception ignored) {}
        });
    }

    private void reloadBoxesIcons() {
        this.async.unsafeGet("SELECT * FROM `boxes_icons`").thenAccept(res -> {
            Map<String, ItemStack> items = new HashMap<>();
            res.all().forEach(sec -> {
                String boxId = sec.lookupValue("box");
                String iconData = sec.lookupValue("icon");
                items.put(boxId, DataUtil.deserealizeItem(iconData));
            });
            itemsMap = items;
        });
    }

    public void setIcon(String boxId, ItemStack icon) {
        String iconData = DataUtil.serealizeItem(icon);
        itemsMap.put(boxId, icon.clone());
        this.async.prepareUpdate("REPLACE INTO `boxes_icons` VALUES (?,?)", ps -> {
            try {
                ps.setString(1, boxId);
                ps.setString(2, iconData);
            } catch(Exception ignored) {}
        });
    }

    private void reloadCasesBoxes() {
        this.async.unsafeGet("SELECT * FROM `cases_boxes`").thenAccept(res -> {
            Map<String, Case> casesMap = new HashMap<>();
            res.all().forEach(sec -> {
                String caseId = sec.lookupValue("case");
                String boxId = sec.lookupValue("box");
                casesMap.computeIfAbsent(caseId, __ -> new Case(caseId, () -> GammaCases.getInstance().getAnimation("circle"), new HashMap<>())).getItemsSupplier().put(boxId, () -> boxesItemsMap.get(boxId));
            });
            this.casesMap = casesMap;
        });
    }

    private void reloadCasesLocations() {
        this.async.unsafeGet("SELECT * FROM `cases_locations`").thenAccept(res -> {
            Map<V3, CaseData> dataMap = new HashMap<>();
            res.all().forEach(sec -> {
                V3 v3 = V3.fromLocation(DataUtil.deserializeLocation(sec.lookupValue("location")));
                String caseId = sec.lookupValue("case");
                dataMap.put(v3, new CaseData(() -> casesMap.get(caseId), v3, false));
            });
            this.caseDataMap = dataMap;
        });
    }

    private void reloadBoxesItems() {
        this.async.unsafeGet("SELECT * FROM `boxes_items`").thenAccept(res -> {
            Map<String, List<CaseItem>> itemsMap = new HashMap<>();
            res.all().forEach(sec -> {
                String box = sec.lookupValue("box");
                MaterialData data = DataUtil.dataFromString(sec.lookupValue("type"));
                String name = ChatColor.translateAlternateColorCodes('&', sec.lookupValue("name"));
                CaseItem item = new CaseItem("" + sec.lookupValue("id"), data.getItemType(), data.getData(), name, sec.lookupValue("chance"), Stream.of(((String) sec.lookupValue("commands")).split(";;;")).collect(Collectors.toList()));
                itemsMap.computeIfAbsent(box, __ -> new ArrayList<>()).add(item);
            });
            boxesItemsMap = itemsMap;
        });
    }

    public void addLocation(String caseId, Location loc) {
        V3 v3 = V3.fromLocation(loc);
        caseDataMap.put(v3, new CaseData(() -> casesMap.get(caseId), v3, false));
        String serLoc = DataUtil.serializeLocation(loc, true);
        this.async.prepareUpdate("REPLACE INTO `cases_locations` VALUES (?,?)", ps -> {
            try {
                ps.setString(1, serLoc);
                ps.setString(2, caseId);
            } catch(Exception ignored) {}
        });
    }

    public void addBox(Box box) {
        Map<String, List<Box>> localMap = userDataMap.get(box.getOwner());
        if (localMap != null) {
            localMap.computeIfAbsent(box.getBoxId(), __ -> new ArrayList<>()).add(box);
        }
        this.async.prepareUpdate("INSERT INTO `players_boxes` VALUES (?,?,?)", ps -> {
            try {
                ps.setString(1, box.getId());
                ps.setString(2, box.getOwner().toString());
                ps.setString(3, box.getBoxId());
            } catch(Exception ignored) {}
        });
    }

    public void removeBox(Box box) {
        this.async.prepareUpdate("DELETE FROM `players_boxes` WHERE `id` = ?", ps -> {
            try {
                ps.setString(1, box.getId());
            } catch(Exception ignored) {}
        });
    }

    public ItemStack getBoxItemStack(String boxId) {
        return itemsMap.get(boxId);
    }

    public void onLogin(AsyncPlayerPreLoginEvent event) {
        try {
            Map<String, List<Box>> boxesMap = new HashMap<>();
            this.sync.prepareGet("SELECT * FROM `players_boxes` WHERE `player` = ?", ps -> {
                try {
                    ps.setString(1, event.getUniqueId().toString());
                } catch (Exception ignored) {
                }
            }).all().forEach(sec -> {
                String boxId = sec.lookupValue("box");
                String id = sec.lookupValue("id");
                List<Box> boxesList = boxesMap.computeIfAbsent(boxId, __ -> new ArrayList<>());
                boxesList.add(new Box(id, boxId, event.getUniqueId()));
            });
            userDataMap.put(event.getUniqueId(), boxesMap);
        } catch(Exception ex) {
            ex.printStackTrace();
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
            event.setKickMessage("Ошибка при получении данных о кейсах. Заходи чуть позже!");
        }
    }

    @AllArgsConstructor
    @Getter
    public static class Box {

        private String id;
        private String boxId;
        private UUID owner;

    }

    @AllArgsConstructor
    @Getter
    public static class CaseData {

        private final Supplier<Case> caseObj;
        private final V3 v3;
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

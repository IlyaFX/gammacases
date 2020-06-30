package ru.ilyafx.gammacases.type;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Consumer;

@AllArgsConstructor
@Getter
public class CaseItem {

    private final String id;
    private final Material type;
    private final short data;
    private final String name;
    private final double chance;
    private final Consumer<Player> consumer;

    public CaseItem(String id, Material type, String name, double chance, Consumer<Player> consumer) {
        this(id, type, (short) 0, name, chance, consumer);
    }

    public CaseItem(String id, Material type, short data, String name, double chance, List<String> commands) {
        this(id, type, data, name, chance, (player) -> commands.forEach(command -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()).replace("%player_uid%", player.getUniqueId().toString()))));
    }

    public CaseItem(String id, Material type, String name, double chance, List<String> commands) {
        this(id, type, (short) 0, name, chance, commands);
    }

    public void execute(Player player) {
        consumer.accept(player);
    }

}

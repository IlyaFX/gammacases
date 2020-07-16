package ru.ilyafx.gammacases.type;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.ilyafx.gammacases.util.NBTUtil;

import java.util.List;
import java.util.function.Function;

@Getter
public class Key {

    private final String id;
    private final Function<Integer, ItemStack> supplier;

    public Key(String caseOwnerId, String id, Material type, short data, String name, List<String> lore) {
        this.id = id;
        this.supplier = (count) -> {
            ItemStack item = new ItemStack(type, count, data);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
            return NBTUtil.writeNBT(item, "gammacase", caseOwnerId);
        };
    }

    public ItemStack supply(int amount) {
        return supplier.apply(amount);
    }

    public ItemStack supply() {
        return supply(1);
    }

}

package ru.ilyafx.gammacases.util;

import lombok.experimental.UtilityClass;
import net.minecraft.server.v1_13_R2.NBTTagCompound;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;

@UtilityClass
public class NBTUtil {

    public org.bukkit.inventory.ItemStack writeNBT(org.bukkit.inventory.ItemStack item, String key, String val) {
        net.minecraft.server.v1_13_R2.ItemStack i = CraftItemStack.asNMSCopy(item);
        NBTTagCompound comp = (i.getTag() != null ? i.getTag() : new NBTTagCompound());
        comp.setString(key, val);
        i.setTag(comp);
        return CraftItemStack.asBukkitCopy(i);
    }

    public String readNBT(org.bukkit.inventory.ItemStack item, String key) {
        net.minecraft.server.v1_13_R2.ItemStack i = CraftItemStack.asNMSCopy(item);
        return (i.getTag() != null && i.getTag().hasKey(key) ? i.getTag().getString(key) : null);
    }

}

package ru.ilyafx.gammacases.util;

import lombok.experimental.UtilityClass;
import net.minecraft.server.v1_13_R2.NBTCompressedStreamTools;
import net.minecraft.server.v1_13_R2.NBTTagCompound;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.util.Base64;
import java.util.List;

@UtilityClass
public class DataUtil {

    public String serializeLocation(Location location, boolean block) {
        if (block)
            return location.getWorld().getName() + ";" + location.getBlockX() + ";" + location.getBlockY() + ";" + location.getBlockZ();
        else
            return location.getWorld().getName() + ";" + round(location.getX(), 2) + ";" + round(location.getY(), 2) + ";" + round(location.getZ(), 2) + ";" + round(location.getYaw(), 2) + ";" + round(location.getPitch(), 2);
    }

    public Location deserializeLocation(String location) {
        String[] split = location.split(";");
        Location loc = new Location(Bukkit.getWorld(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]), Double.parseDouble(split[3]));
        if (split.length > 4) {
            loc.setYaw(Float.parseFloat(split[4]));
            loc.setPitch(Float.parseFloat(split[5]));
        }
        return loc;
    }

    public String dataToString(MaterialData data) {
        return data.getItemType().name() + ":" + data.getData();
    }

    public MaterialData dataFromString(String s) {
        byte data = 0;
        String[] split = s.split(":");
        if (split.length != 1)
            data = Byte.parseByte(split[1]);
        String typeStr = split[0];
        Material type = Material.getMaterial(typeStr);
        if (type == null)
            throw new RuntimeException("Bad data provided: " + s);
        return new MaterialData(type, data);
    }

    private String round(double val, int n) {
        return String.format("%." + n + "f", val);
    }

    public ItemStack itemStack(Material type, int amount, String name) {
        return itemStack(type, (short) 0, amount, name, null);
    }

    public ItemStack itemStack(Material type, short data, int amount, String name) {
        return itemStack(type, data, amount, name, null);
    }

    public ItemStack itemStack(Material type, int amount, String name, List<String> lore) {
        return itemStack(type, (byte) 0, amount, name, lore);
    }

    public ItemStack itemStack(Material type, short data, int amount, String name, List<String> lore) {
        ItemStack item = new ItemStack(type, amount, data);
        ItemMeta meta = item.getItemMeta();
        if (name != null)
            meta.setDisplayName(name);
        if (lore != null)
            meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public String serealizeItem(org.bukkit.inventory.ItemStack stack /* bukkit */) {
        if (stack == null)
            return "null";
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            NBTTagCompound compound = new NBTTagCompound();
            CraftItemStack.asNMSCopy(stack).save(compound);
            NBTCompressedStreamTools.a(compound, stream);
        } catch (Exception ex) {
            ex.printStackTrace();
            return "null";
        }
        return Base64.getEncoder().encodeToString(stream.toByteArray());
    }

    public org.bukkit.inventory.ItemStack deserealizeItem(String string) {
        if ("null".equals(string))
            return null;
        try {
            ByteArrayInputStream stream = new ByteArrayInputStream(Base64.getDecoder().decode(string));
            NBTTagCompound compound = NBTCompressedStreamTools.a(stream);
            Constructor<net.minecraft.server.v1_13_R2.ItemStack> constructor = net.minecraft.server.v1_13_R2.ItemStack.class.getDeclaredConstructor(NBTTagCompound.class);
            constructor.setAccessible(true);
            return CraftItemStack.asBukkitCopy(constructor.newInstance(compound));
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

}

package ru.ilyafx.gammacases.util;

import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.material.MaterialData;

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

}

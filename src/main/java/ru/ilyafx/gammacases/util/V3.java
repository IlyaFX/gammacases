package ru.ilyafx.gammacases.util;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class V3 {

    private World world;
    private int x, y, z;

    public Location toLocation() {
        return new Location(world, x, y, z);
    }

    public static V3 fromLocation(Location location) {
        return new V3(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

}

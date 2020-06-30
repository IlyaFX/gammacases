package ru.ilyafx.gammacases.animation.impl;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import ru.ilyafx.gammacases.animation.AnimationProvider;
import ru.ilyafx.gammacases.type.Case;
import ru.ilyafx.gammacases.type.CaseItem;
import ru.ilyafx.gammacases.util.V3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CircleAnimationProvider implements AnimationProvider {

    private static final int ROTATION_DURATION = 5 * 20;
    private static final double VELOCITY_MULTIPLIER = 0.990;
    private static final double RADIUS = 1.2;
    private static final double Y_OFFSET = 0.5;
    private static final double Z_OFFSET = 0;

    @Override
    public CompletableFuture<Void> startAnimation(Plugin plugin, V3 location, Case caseObj, CaseItem droppedItem) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        new CaseWheel(plugin, future, location, caseObj, droppedItem);
        return future;
    }

    private static class CaseWheel implements Runnable {

        private final Plugin plugin;
        private final CompletableFuture<Void> future;
        private final Block block;
        private final List<CaseItem> items;
        private final CaseItem selected;
        private final List<Hologram> holograms = new ArrayList<>();

        private final boolean zPlane;
        private final double angleGap;

        private double angularVelocity = 10.0;

        private double angleOffset;

        private final int task;

        public CaseWheel(Plugin plugin, CompletableFuture<Void> future, V3 v3, Case caseObj, CaseItem droppedItem) {
            this.plugin = plugin;
            this.future = future;
            this.block = v3.toLocation().getBlock();
            this.items = new ArrayList<>(caseObj.getItems().values());
            Collections.shuffle(items);
            this.selected = droppedItem;
            this.zPlane = block.getData() == 2 || block.getData() == 3;
            this.angleGap = 360.0 / items.size();
            this.angleOffset = -getFinalOffset() - this.angleGap * items.indexOf(this.selected);
            start();
            this.task = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this, 0, 1);
        }

        private void start() {
            Location center = this.block.getLocation().add(0.5, 0.5, 0.5);

            double angle = this.angleOffset;

            for (CaseItem item : this.items) {
                Location loc = center.clone();
                moveToCircle(angle, loc);

                Hologram hologram = HologramsAPI.createHologram(plugin, loc);
                this.holograms.add(hologram);

                hologram.appendTextLine(item.getName());
                hologram.appendItemLine(new ItemStack(item.getType(), 1, item.getData()));

                angle += this.angleGap;
            }
        }

        private int tick;

        @Override
        public void run() {
            Location center = this.block.getLocation().add(0.5, 0.5, 0.5);

            this.angleOffset += this.angularVelocity;

            double angle = this.angleOffset;

            for (Hologram hologram : this.holograms) {
                Location loc = center.clone();
                moveToCircle(angle, loc);

                hologram.teleport(loc);

                angle += this.angleGap;
            }

            this.angularVelocity *= VELOCITY_MULTIPLIER;

            if (this.tick++ >= ROTATION_DURATION) {
                finish();
            }
        }

        private void finish() {
            Bukkit.getScheduler().cancelTask(this.task);

            Location center = this.block.getLocation().add(0.5, 0.5, 0.5);
            double angle = -this.angleGap * items.indexOf(this.selected);

            for (Hologram hologram : this.holograms) {
                Location loc = center.clone();
                moveToCircle(angle, loc);

                hologram.teleport(loc);

                angle += this.angleGap;
            }

            future.complete(null);

            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, this::removeHolograms, 30);
        }

        private double getFinalOffset() {
            double velocity = this.angularVelocity;
            double offset = 0;

            for (int tick = 0; tick < ROTATION_DURATION; tick++) {
                offset += velocity;

                velocity *= VELOCITY_MULTIPLIER;
            }

            return offset;
        }

        private void moveToCircle(double angle, Location loc) {
            angle = angle % 360;
            if (angle < 0) {
                angle += 360;
            }

            double vertDelta = Math.cos(Math.toRadians(angle)) * RADIUS + Y_OFFSET;
            double horizDelta = Math.sin(Math.toRadians(angle)) * RADIUS + Z_OFFSET;

            if (this.zPlane) {
                loc.add(horizDelta, vertDelta, 0);
            } else {
                loc.add(0, vertDelta, horizDelta);
            }
        }

        private int removeTask;

        public void removeHolograms() {
            Vector center = this.block.getLocation().add(0.5, 0.5, 0.5).toVector();

            this.removeTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                for (Iterator<Hologram> i = this.holograms.iterator(); i.hasNext(); ) {
                    Hologram hologram = i.next();

                    Location loc = hologram.getLocation();
                    Vector vector = center.clone().subtract(loc.toVector());

                    if (vector.length() < 0.2) {
                        hologram.delete();
                        i.remove();
                    } else {
                        hologram.teleport(loc.add(vector.normalize().multiply(0.2)));
                    }
                }

                if (this.holograms.isEmpty()) {
                    Bukkit.getScheduler().cancelTask(this.removeTask);
                }
            }, 0, 1);
        }

    }

}

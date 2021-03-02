/*
 * Copyright (C) filoghost and contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package me.filoghost.holographicdisplays.object.base;

import me.filoghost.fcommons.logging.Log;
import me.filoghost.holographicdisplays.api.handler.TouchHandler;
import me.filoghost.holographicdisplays.core.hologram.StandardHologram;
import me.filoghost.holographicdisplays.core.hologram.StandardTouchableLine;
import me.filoghost.holographicdisplays.core.nms.entity.NMSArmorStand;
import me.filoghost.holographicdisplays.core.nms.entity.NMSSlime;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Useful class that implements TouchablePiece. The downside is that subclasses must extend this, and cannot extend other classes.
 * But all the current items are touchable.
 */
public abstract class BaseTouchableLine extends BaseHologramLine implements StandardTouchableLine {

    private static final double SLIME_HEIGHT = 0.5;
    
    private static final Map<Player, Long> anticlickSpam = new WeakHashMap<>();

    private TouchHandler touchHandler;

    private NMSSlime slimeEntity;
    private NMSArmorStand vehicleEntity;
    

    protected BaseTouchableLine(StandardHologram parent) {
        super(parent);
    }

    @Override
    public void onTouch(Player player) {
        if (touchHandler == null || !getHologram().isVisibleTo(player)) {
            return;
        }

        Long lastClick = anticlickSpam.get(player);
        if (lastClick != null && System.currentTimeMillis() - lastClick < 100) {
            return;
        }

        anticlickSpam.put(player, System.currentTimeMillis());

        try {
            touchHandler.onTouch(player);
        } catch (Throwable t) {
            Log.warning("The plugin " + getHologram().getOwnerPlugin().getName() + " generated an exception" 
                    + " when the player " + player.getName() + " touched a hologram.", t);
        }
    }

    public void setTouchHandler(TouchHandler touchHandler) {
        this.touchHandler = touchHandler;
        
        if (touchHandler != null && slimeEntity == null && super.isSpawned()) {
            // If the touch handler was null before and no entity has been spawned, spawn it now.
            spawnSlime(getWorld(), getX(), getY(), getZ());
            
        } else if (touchHandler == null) {
            // Opposite case, the touch handler was not null and an entity was spawned, but now it's useless.
            despawnSlime();
        }
    }

    public TouchHandler getTouchHandler() {
        return this.touchHandler;
    }

    @Override
    public void spawnEntities(World world, double x, double y, double z) {
        if (touchHandler != null) {
            spawnSlime(world, x, y, z);
        }
    }

    @Override
    public void teleportEntities(double x, double y, double z) {
        if (vehicleEntity != null) {
            vehicleEntity.setLocationNMS(x, getSlimeSpawnY(y), z);
        }
        if (slimeEntity != null) {
            slimeEntity.setLocationNMS(x, getSlimeSpawnY(y), z);
        }
    }

    @Override
    public void despawnEntities() {
        despawnSlime();
    }

    private void spawnSlime(World world, double x, double y, double z) {
        despawnSlime();
        
        if (world != null) {
            slimeEntity = getNMSManager().spawnNMSSlime(world, x, getSlimeSpawnY(y), z, this);
            vehicleEntity = getNMSManager().spawnNMSArmorStand(world, x, getSlimeSpawnY(y), z, this);

            slimeEntity.setPassengerOfNMS(vehicleEntity);
        }
    }

    private void despawnSlime() {
        if (slimeEntity != null) {
            slimeEntity.killEntityNMS();
            slimeEntity = null;
        }

        if (vehicleEntity != null) {
            vehicleEntity.killEntityNMS();
            vehicleEntity = null;
        }
    }

    private double getSlimeSpawnY(double y) {
        return y + ((getHeight() - SLIME_HEIGHT) / 2) + getSlimeSpawnOffset();
    }

    private double getSlimeSpawnOffset() {
        return 0;
    }

    @Override
    public void collectEntityIDs(Collection<Integer> collector) {
        if (vehicleEntity != null) {
            collector.add(vehicleEntity.getIdNMS());
        }
        if (slimeEntity != null) {
            collector.add(slimeEntity.getIdNMS());
        }
    }

    @Override
    public NMSArmorStand getNMSSlimeVehicle() {
        return vehicleEntity;
    }

    @Override
    public NMSSlime getNMSSlime() {
        return slimeEntity;
    }

}
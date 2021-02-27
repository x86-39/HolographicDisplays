/*
 * Copyright (C) filoghost and contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package me.filoghost.holographicdisplays.commands.subs;

import me.filoghost.fcommons.command.sub.SubCommandContext;
import me.filoghost.holographicdisplays.Colors;
import me.filoghost.holographicdisplays.commands.HologramSubCommand;
import me.filoghost.holographicdisplays.core.nms.NMSManager;
import me.filoghost.holographicdisplays.core.nms.entity.NMSEntityBase;
import me.filoghost.holographicdisplays.core.object.base.BaseHologram;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class DebugCommand extends HologramSubCommand {

    private final NMSManager nmsManager;
    
    public DebugCommand(NMSManager nmsManager) {
        super("debug");
        setShowInHelpCommand(false);
        setDescription("Displays information useful for debugging.");
        
        this.nmsManager = nmsManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args, SubCommandContext context) {
        boolean foundAnyHologram = false;

        for (World world : Bukkit.getWorlds()) {
            Map<BaseHologram, HologramDebugInfo> hologramsDebugInfo = new HashMap<>();

            for (Chunk chunk : world.getLoadedChunks()) {
                for (Entity entity : chunk.getEntities()) {
                    NMSEntityBase nmsEntity = nmsManager.getNMSEntityBase(entity);

                    if (nmsEntity == null) {
                        continue;
                    }

                    BaseHologram ownerHologram = nmsEntity.getHologramLine().getBaseParent();
                    HologramDebugInfo hologramDebugInfo = hologramsDebugInfo.computeIfAbsent(ownerHologram, mapKey -> new HologramDebugInfo());

                    if (nmsEntity.isDeadNMS()) {
                        hologramDebugInfo.deadEntities++;
                    } else {
                        hologramDebugInfo.aliveEntities++;
                    }
                }
            }

            if (!hologramsDebugInfo.isEmpty()) {
                foundAnyHologram = true;
                sender.sendMessage(Colors.PRIMARY + "Holograms in world '" + world.getName() + "':");

                for (Entry<BaseHologram, HologramDebugInfo> entry : hologramsDebugInfo.entrySet()) {
                    BaseHologram hologram = entry.getKey();
                    HologramDebugInfo debugInfo = entry.getValue();
                    sender.sendMessage(Colors.PRIMARY_SHADOW + "- '" + hologram.toFormattedString() + "': " + hologram.size() + " lines, "
                            + debugInfo.getTotalEntities() + " entities (" + debugInfo.aliveEntities + " alive, " + debugInfo.deadEntities + " dead)");
                }
            }
        }

        if (!foundAnyHologram) {
            sender.sendMessage(Colors.ERROR + "Couldn't find any loaded hologram (holograms may be in unloaded chunks).");
        }
    }
    
    private static class HologramDebugInfo {
        
        private int aliveEntities;
        private int deadEntities;
        
        public int getTotalEntities() {
            return aliveEntities + deadEntities;
        }
        
    }
    

}

package org.bukkit.craftbukkit.entity;

import net.minecraft.server.EntityWaterAnimal;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.WaterMob;

public class CraftWaterMob extends CraftLivingEntity implements WaterMob {

    public CraftWaterMob(CraftServer server, EntityWaterAnimal entity) {
    }

    @Override
    public EntityWaterAnimal getHandle() {
        return null;
    }

    @Override
    public String toString() {
        return null;
    }
}
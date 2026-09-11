package net.susu609.royalarena.entity.building;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public final class KingTowerEntity extends TowerHitboxEntity {
    public KingTowerEntity(EntityType<? extends KingTowerEntity> type,
            Level level)
    {
        super(type, level);
    }
}

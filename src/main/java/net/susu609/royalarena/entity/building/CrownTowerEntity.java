package net.susu609.royalarena.entity.building;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public final class CrownTowerEntity extends TowerHitboxEntity {
    public CrownTowerEntity(EntityType<? extends CrownTowerEntity> type,
            Level level)
    {
        super(type, level);
    }
}

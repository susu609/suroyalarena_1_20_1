package net.susu609.royalarena.entity.building;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

/**
 * Physical representative base for a logical arena tower. Match HP and win
 * state remain authoritative in MatchInstance/TowerState.
 */
public class TowerHitboxEntity extends Entity {
    public TowerHitboxEntity(EntityType<? extends TowerHitboxEntity> type,
            Level level)
    {
        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
        setInvulnerable(true);
    }

    @Override protected void defineSynchedData() {
    }
    @Override public boolean hurt(DamageSource source, float amount) {
        return false;
    }
    @Override public boolean isPickable() {
        return true;
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
    }
    @Override public boolean isAttackable() {
        return false;
    }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}

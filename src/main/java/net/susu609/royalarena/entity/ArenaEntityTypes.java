package net.susu609.royalarena.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.susu609.royalarena.RoyalArena;
import net.susu609.royalarena.entity.building.CrownTowerEntity;
import net.susu609.royalarena.entity.building.KingTowerEntity;
import net.susu609.royalarena.entity.building.TowerHitboxEntity;
import net.susu609.royalarena.entity.building.TowerVisualType;
import net.susu609.royalarena.tower.TowerKind;

public final class ArenaEntityTypes {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(
                    ForgeRegistries.ENTITY_TYPES,
                    RoyalArena.MOD_ID);

    /**
     * Legacy invisible Part 11 target entity. Kept registered for
     * compatibility, while new matches spawn the typed visible tower entities
     * below.
     */
    public static final RegistryObject<EntityType<TowerHitboxEntity>>
            TOWER_HITBOX = ENTITY_TYPES.register(
            "tower_hitbox", () -> EntityType.Builder.of(
                            TowerHitboxEntity::new, MobCategory.MISC).sized(2.4F, 4.5F)
                    .noSave().noSummon().clientTrackingRange(8)
                    .updateInterval(20)
                    .build(RoyalArena.MOD_ID + ":tower_hitbox"));

    public static final RegistryObject<EntityType<CrownTowerEntity>>
            CROWN_TOWER = ENTITY_TYPES.register(
            "crown_tower",
            () -> EntityType.Builder.of(
                            CrownTowerEntity::new,
                            MobCategory.MISC).sized(
                            TowerVisualType.CROWN.width(),
                            TowerVisualType.CROWN.height()).noSave().noSummon()
                    .clientTrackingRange(10).updateInterval(20)
                    .build(RoyalArena.MOD_ID + ":crown_tower"));

    public static final RegistryObject<EntityType<KingTowerEntity>> KING_TOWER =
            ENTITY_TYPES.register(
                    "king_tower", () -> EntityType.Builder.of(
                                    KingTowerEntity::new, MobCategory.MISC)
                            .sized(
                                    TowerVisualType.KING.width(),
                                    TowerVisualType.KING.height()).noSave()
                            .noSummon().clientTrackingRange(10)
                            .updateInterval(20)
                            .build(RoyalArena.MOD_ID + ":king_tower"));

    private ArenaEntityTypes() {
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }

    public static TowerHitboxEntity createTower(TowerKind kind, Level level) {
        return switch (TowerVisualType.from(kind)) {
            case KING -> KING_TOWER.get().create(level);
            case CROWN -> CROWN_TOWER.get().create(level);
        };
    }
}

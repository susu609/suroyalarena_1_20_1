package net.susu609.royalarena.entity.building;

import net.susu609.royalarena.tower.TowerKind;

/**
 * Physical/rendering variant for a logical tower kind, sized in world blocks.
 * The arena uses even sizes only, so the 3-block-wide Crown Tower model is
 * stretched horizontally to a 4x4 footprint (height stays 4).
 */
public enum TowerVisualType {
    CROWN(TowerKind.PRINCESS, 4.0F, 4.0F, 0.0D, 4.0F / 3.0F),
    KING(TowerKind.KING, 4.0F, 4.0F, 0.0D, 1.0F);

    private final TowerKind towerKind;
    private final float width;
    private final float height;
    private final double blockGridCenterOffset;
    private final float modelScaleXZ;

    TowerVisualType(TowerKind towerKind, float width, float height,
            double blockGridCenterOffset, float modelScaleXZ)
    {
        this.towerKind = towerKind;
        this.width = width;
        this.height = height;
        this.blockGridCenterOffset = blockGridCenterOffset;
        this.modelScaleXZ = modelScaleXZ;
    }
    public static TowerVisualType from(TowerKind kind) {
        return kind == TowerKind.KING ? KING : CROWN;
    }
    /**
     * World-space offset from the integer tower anchor needed to align
     * model-sized cubes to block boundaries. Odd-width structures center on a
     * block (+0.5); even-width structures center between blocks (+0.0).
     */
    public double blockGridCenterOffset() {
        return blockGridCenterOffset;
    }
    /** Horizontal scale applied to the Blockbench model when rendering. */
    public float modelScaleXZ() {
        return modelScaleXZ;
    }
    public float height() {
        return height;
    }
    public TowerKind towerKind() {
        return towerKind;
    }
    public float width() {
        return width;
    }
}

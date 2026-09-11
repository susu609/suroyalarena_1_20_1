package net.susu609.royalarena.entity.building;

import net.susu609.royalarena.tower.TowerKind;

/**
 * Physical/rendering variant for a logical tower kind. Sizes intentionally
 * match the uploaded Blockbench model bounds in world blocks.
 */
public enum TowerVisualType {
    CROWN(TowerKind.PRINCESS, 3.0F, 4.0F, 0.5D),
    KING(TowerKind.KING, 4.0F, 4.0F, 0.0D);

    private final TowerKind towerKind;
    private final float width;
    private final float height;
    private final double blockGridCenterOffset;

    TowerVisualType(TowerKind towerKind, float width, float height,
            double blockGridCenterOffset)
    {
        this.towerKind = towerKind;
        this.width = width;
        this.height = height;
        this.blockGridCenterOffset = blockGridCenterOffset;
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

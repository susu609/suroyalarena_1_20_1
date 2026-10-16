package net.susu609.royalarena.combat;

/** Where the units of one card stand around the tapped tile. */
public final class Formation {
    private Formation() {
    }

    /**
     * Offsets (dx, dz) for {@code count} units spaced {@code spacing} apart,
     * centred on (0, 0): a row for 2, an equilateral triangle for 3, then a grid whose rows are centred and whose centroid is (0, 0).
     */
    public static double[][] offsets(int count, double spacing) {
        double[][] out = new double[count][2];
        if (count <= 1)
            return out;
        if (count == 3) {
            double height = spacing * Math.sqrt(3.0D) / 2.0D;
            out[0] = new double[] {0.0D, -height * 2.0D / 3.0D};
            out[1] = new double[] {-spacing * 0.5D, height / 3.0D};
            out[2] = new double[] {spacing * 0.5D, height / 3.0D};
            return out;
        }
        int columns = count == 2 ? 2 : (int) Math.ceil(Math.sqrt(count));
        double meanX = 0.0D;
        double meanZ = 0.0D;
        for (int i = 0; i < count; i++) {
            int row = i / columns;
            int col = i % columns;
            int inRow = Math.min(columns, count - row * columns);
            out[i][0] = (col - (inRow - 1) / 2.0D) * spacing;
            out[i][1] = row * spacing;
            meanX += out[i][0] / count;
            meanZ += out[i][1] / count;
        }
        for (double[] offset : out) {
            offset[0] -= meanX;
            offset[1] -= meanZ;
        }
        return out;
    }
}

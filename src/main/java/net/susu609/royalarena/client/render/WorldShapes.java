package net.susu609.royalarena.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

/** Flat shapes drawn on the arena floor in world space. */
public final class WorldShapes {
    private WorldShapes() {
    }

    /**
     * Translucent disc of {@code radius} blocks centred on (cx, cz) at height
     * y, with an outline. The pose must be in world space.
     */
    public static void disc(PoseStack pose, MultiBufferSource buffers,
            double centerX, double y, double centerZ, double radius, int rgb,
            float alpha, float lineAlpha)
    {
        if (radius <= 0.01D)
            return;
        float r = ((rgb >> 16) & 0xFF) / 255.0F;
        float g = ((rgb >> 8) & 0xFF) / 255.0F;
        float b = (rgb & 0xFF) / 255.0F;
        float cx = (float) centerX;
        float cz = (float) centerZ;
        float fy = (float) y + 0.02F;
        int segments = 40;
        Matrix4f m = pose.last().pose();
        VertexConsumer fill = buffers.getBuffer(RenderType.debugQuads());
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2.0D * i / segments;
            double a1 = Math.PI * 2.0D * (i + 1) / segments;
            float x0 = cx + (float) (Math.cos(a0) * radius);
            float z0 = cz + (float) (Math.sin(a0) * radius);
            float x1 = cx + (float) (Math.cos(a1) * radius);
            float z1 = cz + (float) (Math.sin(a1) * radius);
            // A fan slice as a quad with two vertices on the centre.
            fill.vertex(m, cx, fy, cz).color(r, g, b, alpha).endVertex();
            fill.vertex(m, x1, fy, z1).color(r, g, b, alpha).endVertex();
            fill.vertex(m, x0, fy, z0).color(r, g, b, alpha).endVertex();
            fill.vertex(m, cx, fy, cz).color(r, g, b, alpha).endVertex();
        }
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2.0D * i / segments;
            double a1 = Math.PI * 2.0D * (i + 1) / segments;
            float x0 = cx + (float) (Math.cos(a0) * radius);
            float z0 = cz + (float) (Math.sin(a0) * radius);
            float x1 = cx + (float) (Math.cos(a1) * radius);
            float z1 = cz + (float) (Math.sin(a1) * radius);
            float nx = x1 - x0;
            float nz = z1 - z0;
            float length = (float) Math.sqrt(nx * nx + nz * nz);
            nx /= length;
            nz /= length;
            lines.vertex(m, x0, fy, z0).color(r, g, b, lineAlpha)
                    .normal(pose.last().normal(), nx, 0.0F, nz).endVertex();
            lines.vertex(m, x1, fy, z1).color(r, g, b, lineAlpha)
                    .normal(pose.last().normal(), nx, 0.0F, nz).endVertex();
        }
    }

    /** A straight line between two world points. The pose must be in world space. */
    public static void line(PoseStack pose, MultiBufferSource buffers,
            double x0, double y0, double z0, double x1, double y1, double z1,
            int rgb, float alpha)
    {
        float r = ((rgb >> 16) & 0xFF) / 255.0F;
        float g = ((rgb >> 8) & 0xFF) / 255.0F;
        float b = (rgb & 0xFF) / 255.0F;
        float nx = (float) (x1 - x0);
        float ny = (float) (y1 - y0);
        float nz = (float) (z1 - z0);
        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length < 1.0E-4F)
            return;
        nx /= length;
        ny /= length;
        nz /= length;
        Matrix4f m = pose.last().pose();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        lines.vertex(m, (float) x0, (float) y0, (float) z0)
                .color(r, g, b, alpha)
                .normal(pose.last().normal(), nx, ny, nz).endVertex();
        lines.vertex(m, (float) x1, (float) y1, (float) z1)
                .color(r, g, b, alpha)
                .normal(pose.last().normal(), nx, ny, nz).endVertex();
    }
}

package net.susu609.royalarena.client.camera;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.phys.Vec3;
import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.client.state.ClientMatchState;

public final class ArenaCameraController {
    private static final Vec3 WORLD_UP = new Vec3(0.0D, 1.0D, 0.0D);
    private static Marker cameraEntity;
    private static boolean mouseReleasedForArena;

    private ArenaCameraController() {
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!ClientMatchState.active() || minecraft.level == null ||
                minecraft.player == null)
        {
            release();
            return;
        }
        if (cameraEntity == null || cameraEntity.level() != minecraft.level) {
            cameraEntity = new Marker(EntityType.MARKER, minecraft.level);
        }
        ArenaCameraTransform transform =
                ArenaCameraProfile.DEFAULT.transform(
                        ClientMatchState.arena(),
                        ClientMatchState.side());
        cameraEntity.absMoveTo(
                transform.x(), transform.y(), transform.z(), transform.yaw(),
                transform.pitch());
        if (minecraft.getCameraEntity() != cameraEntity)
            minecraft.setCameraEntity(cameraEntity);
        if (minecraft.screen == null &&
                minecraft.mouseHandler.isMouseGrabbed())
        {
            minecraft.mouseHandler.releaseMouse();
            mouseReleasedForArena = true;
        }
    }
    public static void release() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean hadArenaCamera = cameraEntity != null;
        if (hadArenaCamera && minecraft.getCameraEntity() == cameraEntity &&
                minecraft.player != null)
        {
            minecraft.setCameraEntity(minecraft.player);
        }
        cameraEntity = null;
        if (mouseReleasedForArena && minecraft.player != null &&
                minecraft.screen == null &&
                !minecraft.mouseHandler.isMouseGrabbed())
        {
            minecraft.mouseHandler.grabMouse();
        }
        mouseReleasedForArena = false;
    }
    /**
     * Projects the current OS cursor through the fixed strategic camera onto
     * the arena floor. The server still validates whether the returned X/Z
     * belongs to the local side's deployment area.
     */
    public static BlockPos projectCursorToArena() {
        Minecraft minecraft = Minecraft.getInstance();
        if (cameraEntity == null || minecraft.level == null ||
                !ClientMatchState.active())
            return null;
        Window window = minecraft.getWindow();
        int width = window.getScreenWidth();
        int height = window.getScreenHeight();
        if (width <= 0 || height <= 0)
            return null;
        double mouseX = minecraft.mouseHandler.xpos();
        double mouseY = minecraft.mouseHandler.ypos();
        double ndcX = (mouseX / width) * 2.0D - 1.0D;
        double ndcY = 1.0D - (mouseY / height) * 2.0D;
        Vec3 forward = cameraEntity.getViewVector(1.0F).normalize();
        Vec3 right = forward.cross(WORLD_UP).normalize();
        Vec3 up = right.cross(forward).normalize();
        double fovRadians = Math.toRadians(minecraft.options.fov().get());
        double tangent = Math.tan(fovRadians * 0.5D);
        double aspect = width / (double) height;
        Vec3 direction = forward.add(right.scale(ndcX * tangent * aspect))
                .add(up.scale(ndcY * tangent)).normalize();
        if (direction.y >= -1.0E-5D)
            return null;
        ArenaLayout arena = ClientMatchState.arena();
        Vec3 origin = cameraEntity.getEyePosition(1.0F);
        double planeY = arena.floorY() + 1.01D;
        double distance = (planeY - origin.y) / direction.y;
        if (distance <= 0.0D || distance > 96.0D)
            return null;
        Vec3 hit = origin.add(direction.scale(distance));
        return new BlockPos(
                (int) Math.floor(hit.x), arena.floorY(),
                (int) Math.floor(hit.z));
    }
    public static double cursorGuiX() {
        Minecraft minecraft = Minecraft.getInstance();
        Window window = minecraft.getWindow();
        return minecraft.mouseHandler.xpos() * window.getGuiScaledWidth() /
                (double) window.getScreenWidth();
    }
    public static double cursorGuiY() {
        Minecraft minecraft = Minecraft.getInstance();
        Window window = minecraft.getWindow();
        return minecraft.mouseHandler.ypos() * window.getGuiScaledHeight() /
                (double) window.getScreenHeight();
    }
}

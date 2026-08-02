package net.swzo.brassworksmissions.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.createmod.catnip.gui.element.DelegatedStencilElement;
import net.createmod.catnip.gui.element.ScreenElement;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.swzo.brassworksmissions.BrassworksmissionsMod;
import org.joml.Matrix4f;

public class MissionIcons implements ScreenElement {

    public static final ResourceLocation ICON_ATLAS = ResourceLocation.fromNamespaceAndPath(BrassworksmissionsMod.MODID, "textures/gui/mission_icons.png");
    public static final int ICON_ATLAS_SIZE = 256;

    private static int x = 0, y = -1;
    private final int iconX;
    private final int iconY;

    public static final MissionIcons
            I_CLAIM = newRow(),
            I_ACCEPT = next(),
            I_REROLL = next();

    public MissionIcons(int x, int y) {
        this.iconX = x * 16;
        this.iconY = y * 16;
    }

    private static MissionIcons next() {
        return new MissionIcons(++x, y);
    }

    private static MissionIcons newRow() {
        return new MissionIcons(x = 0, ++y);
    }

    @OnlyIn(Dist.CLIENT)
    public void bind() {

    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(GuiGraphics graphics, int x, int y) {
        graphics.blit(ICON_ATLAS, x, y, 0, this.iconX, this.iconY, 16, 16, ICON_ATLAS_SIZE, ICON_ATLAS_SIZE);
    }

    @OnlyIn(Dist.CLIENT)
    public void render(PoseStack ms, MultiBufferSource buffer, int color) {
        VertexConsumer builder = buffer.getBuffer(RenderType.text(ICON_ATLAS));
        Matrix4f matrix = ms.last().pose();
        Color rgb = new Color(color);
        int light = LightTexture.FULL_BRIGHT;

        Vec3 vec1 = new Vec3(0, 0, 0);
        Vec3 vec2 = new Vec3(0, 1, 0);
        Vec3 vec3 = new Vec3(1, 1, 0);
        Vec3 vec4 = new Vec3(1, 0, 0);

        float u1 = iconX / (float) ICON_ATLAS_SIZE;
        float u2 = (iconX + 16) / (float) ICON_ATLAS_SIZE;
        float v1 = iconY / (float) ICON_ATLAS_SIZE;
        float v2 = (iconY + 16) / (float) ICON_ATLAS_SIZE;

        vertex(builder, matrix, vec1, rgb, u1, v1, light);
        vertex(builder, matrix, vec2, rgb, u1, v2, light);
        vertex(builder, matrix, vec3, rgb, u2, v2, light);
        vertex(builder, matrix, vec4, rgb, u2, v1, light);
    }

    @OnlyIn(Dist.CLIENT)
    private void vertex(VertexConsumer builder, Matrix4f matrix, Vec3 vec, Color rgb, float u, float v, int light) {
        builder.addVertex(matrix, (float) vec.x, (float) vec.y, (float) vec.z)
                .setColor(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), 255)
                .setUv(u, v)
                .setLight(light);
    }

    @OnlyIn(Dist.CLIENT)
    public DelegatedStencilElement asStencil() {

        return new DelegatedStencilElement().withStencilRenderer((ms, w, h, alpha) -> this.render(new GuiGraphics(Minecraft.getInstance(), Minecraft.getInstance().renderBuffers().bufferSource()), 0, 0)).withBounds(16, 16);
    }
}

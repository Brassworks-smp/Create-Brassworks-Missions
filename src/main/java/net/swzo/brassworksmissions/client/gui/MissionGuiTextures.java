package net.swzo.brassworksmissions.client.gui;

import net.createmod.catnip.gui.TextureSheetSegment;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.element.ScreenElement;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.swzo.brassworksmissions.BrassworksmissionsMod;


public enum MissionGuiTextures implements ScreenElement, TextureSheetSegment {


    BUTTON("custom_widgets", 0, 0, 18, 18),
    BUTTON_HOVER("custom_widgets", 18, 0, 18, 18),
    BUTTON_DOWN("custom_widgets", 36, 0, 18, 18),
    BUTTON_DISABLED("custom_widgets", 54, 0, 18, 18);

    public final ResourceLocation location;
    private final int width;
    private final int height;
    private final int startX;
    private final int startY;

    MissionGuiTextures(String location, int startX, int startY, int width, int height) {
        this(BrassworksmissionsMod.MODID, location, startX, startY, width, height);
    }

    MissionGuiTextures(String namespace, String location, int startX, int startY, int width, int height) {
        this.location = ResourceLocation.fromNamespaceAndPath(namespace, "textures/gui/" + location + ".png");
        this.width = width;
        this.height = height;
        this.startX = startX;
        this.startY = startY;
    }

    @Override
    public ResourceLocation getLocation() {
        return location;
    }

    @OnlyIn(Dist.CLIENT)
    public void render(GuiGraphics graphics, int x, int y) {
        graphics.blit(location, x, y, getStartX(), getStartY(), getWidth(), getHeight());
    }

    @OnlyIn(Dist.CLIENT)
    public void render(GuiGraphics graphics, int x, int y, Color c) {
        bind();
        UIRenderHelper.drawColoredTexture(graphics, c, x, y, getStartX(), getStartY(), getWidth(), getHeight());
    }

    @Override
    public int getStartX() {
        return startX;
    }

    @Override
    public int getStartY() {
        return startY;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }
}



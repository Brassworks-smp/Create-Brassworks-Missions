package net.swzo.brassworksmissions.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.swzo.brassworksmissions.BrassworksmissionsMod;
import net.swzo.brassworksmissions.config.Config;
import net.swzo.brassworksmissions.missions.ActiveMission;
import net.swzo.brassworksmissions.network.BrassworksmissionsModVariables;

import java.util.List;

@EventBusSubscriber(modid = BrassworksmissionsMod.MODID, value = Dist.CLIENT)
public class MissionHud {

    private static final ResourceLocation NINE_SLICE_BACKGROUND = ResourceLocation.fromNamespaceAndPath(BrassworksmissionsMod.MODID, "textures/gui/mission_background.png");
    private static final ResourceLocation MISSION_BARS_TEXTURE = ResourceLocation.fromNamespaceAndPath(BrassworksmissionsMod.MODID, "textures/gui/mission_bars.png");
    private static final float BACKGROUND_OPACITY = 0.8f;
    private static final int PADDING_TOP = 5;
    private static final int PADDING_RIGHT = 4;
    private static final int PADDING_BOTTOM = 4;
    private static final int PADDING_LEFT = 4;
    private static final int SLICE_SIZE = 3;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Player player = mc.player;
        BrassworksmissionsModVariables.PlayerVariables playerVariables = player.getData(BrassworksmissionsModVariables.PLAYER_VARIABLES);

        if (playerVariables.trackedMissions.isEmpty()) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        float scale = Config.CLIENT.HUD_SIZE.get().getScale();

        int screenWidth = (int) (guiGraphics.guiWidth() / scale);
        int screenHeight = (int) (guiGraphics.guiHeight() / scale);
        int xOffset = (int) (Config.CLIENT.HUD_X_OFFSET.get() / scale);
        int yOffset = (int) (Config.CLIENT.HUD_Y_OFFSET.get() / scale);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, 1.0f);

        int currentY;
        if (Config.CLIENT.BOTTOM_ALIGN_HUD.get()) {
            int totalHeight = 0;
            for (int missionSlot : playerVariables.trackedMissions) {
                ActiveMission mission = playerVariables.missionData.getMission(missionSlot);
                if (mission != null) totalHeight += calculateMissionHeight(mission) + 5;
            }
            currentY = screenHeight - totalHeight - yOffset + 5;
        } else {
            currentY = yOffset;
        }

        for (int missionSlot : playerVariables.trackedMissions) {
            if (missionSlot >= 0 && missionSlot < playerVariables.missionData.getMissions().length) {
                ActiveMission mission = playerVariables.missionData.getMission(missionSlot);
                if (mission != null) {
                    int missionHeight = drawMission(guiGraphics, mission, screenWidth, currentY, xOffset);
                    currentY += missionHeight + 5;
                }
            }
        }
        guiGraphics.pose().popPose();
    }

    private static int drawMission(GuiGraphics guiGraphics, ActiveMission mission, int screenWidth, int y, int xOffset) {
        Minecraft mc = Minecraft.getInstance();

        final int contentWidth = 160;
        final int lineSpacing = 3;
        final int barHeight = 5;

        boolean isComplete = mission.isComplete();
        int titleColor = isComplete ? MissionUIHelper.COMPLETED_TITLE_COLOR : MissionUIHelper.INCOMPLETE_TITLE_COLOR;
        Component titleComponent = Component.literal(mission.getTitle()).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(titleColor)));

        Component description = MissionUIHelper.getMissionDescription(mission);

        List<FormattedCharSequence> descriptionLines = mc.font.split(description, contentWidth);
        int descriptionHeight = descriptionLines.size() * mc.font.lineHeight;

        Component progressPrefix = Component.translatable("gui.brassworksmissions.ui.progress")
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(MissionUIHelper.PROGRESS_TEXT_COLOR)));
        Component progressComponent = MissionUIHelper.getFormattedProgress(mission, progressPrefix);

        Component rewardComponent = MissionUIHelper.getFormattedReward(mission);
        boolean hasReward = !rewardComponent.getString().isEmpty();

        int contentHeight = 0;
        contentHeight += mc.font.lineHeight;
        contentHeight += lineSpacing;
        contentHeight += descriptionHeight;
        contentHeight += lineSpacing;
        contentHeight += mc.font.lineHeight;
        contentHeight += lineSpacing;
        if (hasReward) {
            contentHeight += mc.font.lineHeight;
            contentHeight += lineSpacing;
        }
        contentHeight += barHeight;

        int bgWidth = contentWidth + PADDING_LEFT + PADDING_RIGHT;
        int bgHeight = contentHeight + PADDING_TOP + PADDING_BOTTOM;

        int x;
        if (Config.CLIENT.LEFT_ALIGN_HUD.get()) {
            x = xOffset;
        } else {
            x = screenWidth - bgWidth - xOffset;
        }

        drawNineSliceManually(guiGraphics, NINE_SLICE_BACKGROUND, x, y, bgWidth, bgHeight);

        int contentX = x + PADDING_LEFT + 1;
        int contentY = y + PADDING_TOP;
        int currentY = contentY;

        guiGraphics.drawString(mc.font, titleComponent, contentX, currentY, 0, true);
        currentY += mc.font.lineHeight + lineSpacing;

        for (FormattedCharSequence line : descriptionLines) {
            guiGraphics.drawString(mc.font, line, contentX, currentY, 0, true);
            currentY += mc.font.lineHeight;
        }
        currentY += lineSpacing;

        guiGraphics.drawString(mc.font, progressComponent, contentX, currentY, 0, true);
        currentY += mc.font.lineHeight + lineSpacing;

        if (hasReward) {
            guiGraphics.drawString(mc.font, rewardComponent, contentX, currentY, 0, true);
            currentY += mc.font.lineHeight + lineSpacing;
        }

        float progress = Math.min(1.0f, (float) mission.getProgress() / mission.getRequiredAmount());
        final int vEmpty = 0;
        final int vYellow = 5;
        final int vGreen = 10;
        int vFilled = isComplete ? vGreen : vYellow;

        drawProgressiveBar(guiGraphics, contentX - 1, currentY, contentWidth, barHeight, progress, vEmpty, vFilled);

        return bgHeight;
    }

    private static void drawNineSliceManually(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int width, int height) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, BACKGROUND_OPACITY);

        final int corner = SLICE_SIZE;
        final int textureSize = 9;
        final int centerSliceTextureSize = textureSize - 2 * corner;

        int centerWidth = width - (2 * corner);
        int centerHeight = height - (2 * corner);

        int uTopLeft = 0, vTopLeft = 0;
        int uTopCenter = corner, vTopCenter = 0;
        int uTopRight = corner + centerSliceTextureSize, vTopRight = 0;
        int uMiddleLeft = 0, vMiddleLeft = corner;
        int uMiddleCenter = corner, vMiddleCenter = corner;
        int uMiddleRight = corner + centerSliceTextureSize, vMiddleRight = corner;
        int uBottomLeft = 0, vBottomLeft = corner + centerSliceTextureSize;
        int uBottomCenter = corner, vBottomCenter = corner + centerSliceTextureSize;
        int uBottomRight = corner + centerSliceTextureSize, vBottomRight = corner + centerSliceTextureSize;

        guiGraphics.blit(texture, x, y, uTopLeft, vTopLeft, corner, corner, textureSize, textureSize);
        guiGraphics.blit(texture, x + width - corner, y, uTopRight, vTopRight, corner, corner, textureSize, textureSize);
        guiGraphics.blit(texture, x, y + height - corner, uBottomLeft, vBottomLeft, corner, corner, textureSize, textureSize);
        guiGraphics.blit(texture, x + width - corner, y + height - corner, uBottomRight, vBottomRight, corner, corner, textureSize, textureSize);

        if (centerWidth > 0) {
            guiGraphics.blit(texture, x + corner, y, centerWidth, corner, uTopCenter, vTopCenter, centerSliceTextureSize, corner, textureSize, textureSize);
            guiGraphics.blit(texture, x + corner, y + height - corner, centerWidth, corner, uBottomCenter, vBottomCenter, centerSliceTextureSize, corner, textureSize, textureSize);
        }
        if (centerHeight > 0) {
            guiGraphics.blit(texture, x, y + corner, corner, centerHeight, uMiddleLeft, vMiddleLeft, corner, centerSliceTextureSize, textureSize, textureSize);
            guiGraphics.blit(texture, x + width - corner, y + corner, corner, centerHeight, uMiddleRight, vMiddleRight, corner, centerSliceTextureSize, textureSize, textureSize);
        }
        if (centerWidth > 0 && centerHeight > 0) {
            guiGraphics.blit(texture, x + corner, y + corner, centerWidth, centerHeight, uMiddleCenter, vMiddleCenter, centerSliceTextureSize, centerSliceTextureSize, textureSize, textureSize);
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static void drawProgressiveBar(GuiGraphics guiGraphics, int x, int y, int width, int height, float progress, int vEmpty, int vFilled) {
        final int textureSheetWidth = 11;
        final int textureSheetHeight = 15;
        final int leftCapWidth = 5;
        final int rightCapWidth = 5;
        final int middleTextureX = 5;
        final int rightCapTextureX = 6;

        int middleBarWidth = width - leftCapWidth - rightCapWidth;

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.blit(MISSION_BARS_TEXTURE, x, y, 0, vEmpty, leftCapWidth, height, textureSheetWidth, textureSheetHeight);
        if (middleBarWidth > 0) {
            guiGraphics.blit(MISSION_BARS_TEXTURE, x + leftCapWidth, y, middleBarWidth, height, middleTextureX, vEmpty, 1, height, textureSheetWidth, textureSheetHeight);
        }
        guiGraphics.blit(MISSION_BARS_TEXTURE, x + width - rightCapWidth, y, rightCapTextureX, vEmpty, rightCapWidth, height, textureSheetWidth, textureSheetHeight);

        int progressPixels = (int) (width * progress);
        if (progressPixels <= 0) {
            RenderSystem.disableBlend();
            return;
        }

        int filledLeftWidth = Math.min(progressPixels, leftCapWidth);
        if (filledLeftWidth > 0) {
            guiGraphics.blit(MISSION_BARS_TEXTURE, x, y, 0, vFilled, filledLeftWidth, height, textureSheetWidth, textureSheetHeight);
        }

        if (progressPixels > leftCapWidth && middleBarWidth > 0) {
            int filledMiddleWidth = Math.min(progressPixels - leftCapWidth, middleBarWidth);
            if (filledMiddleWidth > 0) {
                guiGraphics.blit(MISSION_BARS_TEXTURE, x + leftCapWidth, y, filledMiddleWidth, height, middleTextureX, vFilled, 1, height, textureSheetWidth, textureSheetHeight);
            }
        }

        if (progressPixels > leftCapWidth + middleBarWidth && rightCapWidth > 0) {
            int filledRightWidth = progressPixels - (leftCapWidth + middleBarWidth);
            if (filledRightWidth > 0) {
                guiGraphics.blit(MISSION_BARS_TEXTURE, x + leftCapWidth + middleBarWidth, y, rightCapTextureX, vFilled, filledRightWidth, height, textureSheetWidth, textureSheetHeight);
            }
        }

        RenderSystem.disableBlend();
    }

    public ActiveMission getMissionDataForSlot(Player player, int missionSlot) {
        if (player == null || missionSlot < 1) {
            return null;
        }
        BrassworksmissionsModVariables.PlayerVariables playerVariables = player.getData(BrassworksmissionsModVariables.PLAYER_VARIABLES);
        int missionIndex = missionSlot - 1;
        if (missionIndex < 0 || missionIndex >= playerVariables.missionData.getMissions().length) {
            return null;
        }
        return playerVariables.missionData.getMission(missionIndex);
    }

    private static int calculateMissionHeight(ActiveMission mission) {
        Minecraft mc = Minecraft.getInstance();
        final int contentWidth = 160;
        final int lineSpacing = 3;
        final int barHeight = 5;

        Component description = MissionUIHelper.getMissionDescription(mission);
        int descriptionHeight = mc.font.split(description, contentWidth).size() * mc.font.lineHeight;

        Component rewardComponent = MissionUIHelper.getFormattedReward(mission);
        boolean hasReward = !rewardComponent.getString().isEmpty();

        int contentHeight = mc.font.lineHeight + lineSpacing + descriptionHeight + lineSpacing + mc.font.lineHeight + lineSpacing + barHeight;
        if (hasReward) contentHeight += mc.font.lineHeight + lineSpacing;
        return contentHeight + PADDING_TOP + PADDING_BOTTOM;
    }
}
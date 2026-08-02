package net.swzo.brassworksmissions.client.gui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
import net.swzo.brassworksmissions.event.MissionResetHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class UiScreenRenderHelper {

    public static void renderPlayerModel(GuiGraphics guiGraphics, int mouseX, int mouseY, Player player, int screenX, int screenY, float partialTicks) {
        if (player == null) {
            return;
        }

        guiGraphics.pose().pushPose();
        int renderX = screenX + 230;
        int renderY = screenY + 180;
        int scale = 45;
        float yLookFactor = (float) Math.atan((renderX - mouseX) / 40.0F);
        float xLookFactor = (float) Math.atan(((renderY - 75) - mouseY) / 40.0F);

        guiGraphics.pose().translate(renderX, renderY, 100.0D);
        guiGraphics.pose().scale(scale, scale, -scale);
        guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(180.0F));

        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        MultiBufferSource.BufferSource bufferSource = guiGraphics.bufferSource();

        float originalYBodyRot = player.yBodyRot;
        float originalYHeadRot = player.yHeadRot;
        float originalXRot = player.getXRot();
        float originalYRot = player.getYRot();
        float originalYHeadRotO = player.yHeadRotO;
        float originalYRotO = player.yRotO;
        float originalXRotO = player.xRotO;
        float originalYBodyRotO = player.yBodyRotO;

        float targetBodyRot = 180.0F + yLookFactor * 20.0F;
        float targetYRot = 180.0F + yLookFactor * 40.0F;
        float targetXRot = -xLookFactor * 20.0F;
        player.yBodyRot = targetBodyRot;
        player.setYRot(targetYRot);
        player.setXRot(targetXRot);
        player.yHeadRot = targetYRot;
        player.yHeadRotO = targetYRot;
        player.yRotO = targetYRot;
        player.xRotO = targetXRot;
        player.yBodyRotO = targetBodyRot;

        dispatcher.setRenderShadow(false);
        dispatcher.render(player, 0.0, 0.0, 0.0, 0.0F, partialTicks, guiGraphics.pose(), bufferSource, LightTexture.FULL_BRIGHT);
        bufferSource.endBatch();
        bufferSource.endBatch();
        dispatcher.setRenderShadow(true);

        player.yBodyRot = originalYBodyRot;
        player.setYRot(originalYRot);
        player.setXRot(originalXRot);
        player.yHeadRot = originalYHeadRot;
        player.yHeadRotO = originalYHeadRotO;
        player.yRotO = originalYRotO;
        player.xRotO = originalXRotO;
        player.yBodyRotO = originalYBodyRotO;

        Lighting.setupForFlatItems();
        guiGraphics.pose().popPose();
    }

    public static long getRemainingMillisUntilReset() {
        return getRemainingMillisUntilReset(1);
    }

    public static long getRemainingMillisUntilReset(int category) {
        long nextResetTimestampInSeconds = MissionResetHandler.getNextResetTimestamp(category);

        long nextResetTimestampInMillis = nextResetTimestampInSeconds * 1000;

        return nextResetTimestampInMillis - System.currentTimeMillis();
    }

    public static Component getFormattedCountdown(long remainingMillis) {
        if (remainingMillis < 0) remainingMillis = 0;

        long hours = TimeUnit.MILLISECONDS.toHours(remainingMillis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(remainingMillis) % 60;

        MutableComponent component = Component.empty();

        Style valueStyle = Style.EMPTY.withColor(TextColor.fromRgb(MissionUIHelper.DESCRIPTION_AMOUNT_COLOR));
        Style suffixStyle = Style.EMPTY.withColor(TextColor.fromRgb(MissionUIHelper.DESCRIPTION_AMOUNT_SUB_COLOR));

        if (hours > 0) {
            component.append(Component.literal(String.valueOf(hours)).withStyle(valueStyle));
            component.append(Component.literal(Component.translatable("gui.brassworksmissions.ui.timer.hour.singular").getString()).withStyle(suffixStyle));
            component.append(" ");
            component.append(Component.literal(String.valueOf(minutes)).withStyle(valueStyle));
            component.append(Component.literal(Component.translatable("gui.brassworksmissions.ui.timer.minute.singular").getString()).withStyle(suffixStyle));
            component.append(" ");
            component.append(Component.literal(String.valueOf(seconds)).withStyle(valueStyle));
            component.append(Component.literal(Component.translatable("gui.brassworksmissions.ui.timer.second.singular").getString()).withStyle(suffixStyle));
        } else if (minutes > 0) {
            component.append(Component.literal(String.valueOf(minutes)).withStyle(valueStyle));
            component.append(Component.literal(Component.translatable("gui.brassworksmissions.ui.timer.minute.singular").getString()).withStyle(suffixStyle));
            component.append(" ");
            component.append(Component.literal(String.valueOf(seconds)).withStyle(valueStyle));
            component.append(Component.literal(Component.translatable("gui.brassworksmissions.ui.timer.second.singular").getString()).withStyle(suffixStyle));
        } else {
            component.append(Component.literal(String.valueOf(seconds)).withStyle(valueStyle));
            component.append(Component.literal(Component.translatable("gui.brassworksmissions.ui.timer.second.singular").getString()).withStyle(suffixStyle));
        }

        return component;
    }

    public static List<Component> getFormattedTooltipCountdown(long remainingMillis) {
        if (remainingMillis < 0) remainingMillis = 0;

        long hours = TimeUnit.MILLISECONDS.toHours(remainingMillis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(remainingMillis) % 60;

        List<Component> tooltipLines = new ArrayList<>();
        tooltipLines.add(
                Component.translatable("gui.brassworksmissions.ui.reset_tooltip_title")
                        .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(MissionUIHelper.TOOLTIP_HEADER_COLOR)))
        );

        MutableComponent timeComponent = Component.empty();
        boolean needsSeparator = false;

        Style valueStyle = Style.EMPTY.withColor(TextColor.fromRgb(MissionUIHelper.PROGRESS_TEXT_COLOR));
        Style suffixStyle = Style.EMPTY.withColor(TextColor.fromRgb(MissionUIHelper.TOOLTIP_SUBTEXT_COLOR));

        if (hours > 0) {
            timeComponent.append(Component.literal(String.valueOf(hours)).withStyle(valueStyle));
            timeComponent.append(Component.literal("" + Component.translatable("gui.brassworksmissions.ui.timer.hour").getString()).withStyle(suffixStyle));
            needsSeparator = true;
        }

        if (minutes > 0) {
            if (needsSeparator) {

                timeComponent.append(Component.literal(", ").withStyle(suffixStyle));
            }
            timeComponent.append(Component.literal(String.valueOf(minutes)).withStyle(valueStyle));
            timeComponent.append(Component.literal("" + Component.translatable("gui.brassworksmissions.ui.timer.minute").getString()).withStyle(suffixStyle));
            needsSeparator = true;
        }

        if (seconds >= 0) {
            if (needsSeparator) {
                timeComponent.append(Component.literal(", ").withStyle(suffixStyle));
            }
            timeComponent.append(Component.literal(String.valueOf(seconds)).withStyle(valueStyle));
            timeComponent.append(Component.literal("" + Component.translatable("gui.brassworksmissions.ui.timer.second").getString()).withStyle(suffixStyle));
        }

        if (!timeComponent.getContents().equals(Component.empty())) {
            tooltipLines.add(timeComponent);
        }

        return tooltipLines;
    }
}


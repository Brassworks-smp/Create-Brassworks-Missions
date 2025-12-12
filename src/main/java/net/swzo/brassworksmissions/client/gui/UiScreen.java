package net.swzo.brassworksmissions.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Indicator;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.swzo.brassworksmissions.client.gui.widget.CustomIconButton;
import net.swzo.brassworksmissions.init.KeybindingInit;
import net.swzo.brassworksmissions.missions.ActiveMission;
import net.swzo.brassworksmissions.missions.MissionController;
import net.swzo.brassworksmissions.missions.PlayerMissionData;
import net.swzo.brassworksmissions.network.BrassworksmissionsModVariables;
import net.swzo.brassworksmissions.network.UiButtonMessage;
import net.swzo.brassworksmissions.network.UpdateSelectedMissionMessage;
import net.swzo.brassworksmissions.world.inventory.UiMenu;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class UiScreen extends AbstractContainerScreen<UiMenu> {
    private final int x, y, z;
    private final Player entity;

    private IconButton claimRewardsButton;
    private IconButton closeButton;
    private CustomIconButton trackButton;
    private CustomIconButton rerollButton;
    private Indicator trackingIndicator;

    private SelectionScrollInput missionSelector;
    private List<Component> missionsList = new ArrayList<>();

    private Label titleLabel;
    private Component descriptionComponent = Component.empty();
    private Label progressLabel;
    private Label progressTextLabel;
    private Label missionResetTimerLabel;

    private ItemStack rewardStack = ItemStack.EMPTY;

    public UiScreen(UiMenu container, Inventory inventory, Component text) {
        super(container, inventory, text);
        this.x = container.x;
        this.y = container.y;
        this.z = container.z;
        this.entity = container.entity;
        this.imageWidth = 241;
        this.imageHeight = 175;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        Component selectedPrefix = Component.translatable("gui.brassworksmissions.ui.selected_prefix")
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(MissionUIHelper.SELECTOR_PREFIX_COLOR)));
        Component selectedValue = Component.translatable("gui.brassworksmissions.ui.mission_prefix", missionSelector.getState() + 1)
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(MissionUIHelper.SELECTOR_VALUE_COLOR)));
        Component fullSelectorText = selectedPrefix.copy().append(selectedValue);
        guiGraphics.drawString(this.font, fullSelectorText, this.leftPos + 55, this.topPos + 47, 0xFFFFFF, true);

        renderTooltips(guiGraphics, mouseX, mouseY);

        UiScreenRenderHelper.renderPlayerModel(guiGraphics, mouseX, mouseY, this.entity, this.leftPos, this.topPos, partialTicks);

        renderLabels(guiGraphics);

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {

        if (missionSelector != null && missionSelector.isMouseOver(mouseX, mouseY)) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable("gui.brassworksmissions.tooltip.available_missions").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(MissionUIHelper.TOOLTIP_HEADER_COLOR))));
            var playerVariables = entity.getData(BrassworksmissionsModVariables.PLAYER_VARIABLES);

            for (int i = 0; i < missionsList.size(); i++) {
                ActiveMission mission = playerVariables.missionData.getMission(i);
                if (mission == null) continue;

                boolean isComplete = mission.isComplete();
                Component missionLabel = Component.translatable("gui.brassworksmissions.ui.mission_prefix", i + 1);
                Component line;
                int colorHex;
                Component arrow;

                if (i == missionSelector.getState()) {

                    arrow = Component.translatable("gui.brassworksmissions.tooltip.arrow_selected").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(MissionUIHelper.SELECTOR_VALUE_COLOR)));
                    colorHex = isComplete ? MissionUIHelper.SCROLLER_SELECTED_COMPLETED_COLOR : MissionUIHelper.SCROLLER_SELECTED_INCOMPLETE_COLOR;
                } else {

                    arrow = Component.translatable("gui.brassworksmissions.tooltip.arrow_unselected").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(MissionUIHelper.TOOLTIP_TEXT_COLOR)));
                    colorHex = isComplete ? MissionUIHelper.SCROLLER_UNSELECTED_COMPLETED_COLOR : MissionUIHelper.SCROLLER_UNSELECTED_INCOMPLETE_COLOR;
                }

                Component missionComponent = missionLabel.copy()
                        .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colorHex)));
                line = arrow.copy().append(missionComponent);
                tip.add(line);
            }
            tip.add(Component.translatable("gui.brassworksmissions.tooltip.scroll_select").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(MissionUIHelper.TOOLTIP_SUBTEXT_COLOR)).withItalic(true)));
            guiGraphics.renderComponentTooltip(font, tip, mouseX, mouseY);
        }

        if (!rewardStack.isEmpty() && rewardStack.getCount() >= 1) {
            int rewardX = this.leftPos + 26 + 99 - 21;
            int rewardY = this.topPos + 105 - 2 + 12;
            if (mouseX >= rewardX && mouseX < rewardX + 16 && mouseY >= rewardY && mouseY < rewardY + 16){
                guiGraphics.renderTooltip(
                        this.font,
                        rewardStack,
                        mouseX,
                        mouseY
                );
            }
        }

        ItemStack allRewardsIcon = new ItemStack(rewardStack.getItem(), MissionController.getTotalClaimableItemCount(entity));
        int allRewardsX = this.leftPos + 10;
        int allRewardsY = this.topPos + 140+12;

        if (mouseX >= allRewardsX && mouseX < allRewardsX + 16 && mouseY >= allRewardsY && mouseY < allRewardsY + 16) {
            if (allRewardsIcon.getCount() >= 1) {
                guiGraphics.renderTooltip(
                        this.font,
                        allRewardsIcon,
                        mouseX,
                        mouseY
                );
            } else {
                List<Component> tip = new ArrayList<>();
                tip.add(Component.translatable("gui.brassworksmissions.tooltip.no_rewards").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(MissionUIHelper.TOOLTIP_ERROR_COLOR))));
                guiGraphics.renderComponentTooltip(font, tip, mouseX, mouseY);
            }
        }

        if (claimRewardsButton != null && claimRewardsButton.isMouseOver(mouseX, mouseY)) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable("gui.brassworksmissions.tooltip.claim_reward").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(MissionUIHelper.TOOLTIP_HEADER_COLOR))));
            guiGraphics.renderComponentTooltip(font, tip, mouseX, mouseY);
        }

        if (closeButton != null && closeButton.isMouseOver(mouseX, mouseY)) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable("gui.brassworksmissions.tooltip.confirm_close").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(MissionUIHelper.TOOLTIP_HEADER_COLOR))));
            guiGraphics.renderComponentTooltip(font, tip, mouseX, mouseY);
        }

        if (trackButton != null && trackButton.isMouseOver(mouseX, mouseY)) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable("gui.brassworksmissions.tooltip.track_mission").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(MissionUIHelper.TOOLTIP_HEADER_COLOR))));
            guiGraphics.renderComponentTooltip(font, tip, mouseX, mouseY);
        }

        if (rerollButton != null && rerollButton.isMouseOver(mouseX, mouseY)) {
            var playerVariables = entity.getData(BrassworksmissionsModVariables.PLAYER_VARIABLES);
            int rerollCost = playerVariables.reRollAmount * 2;
            int cappedCost = Math.min(rerollCost, 32);
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable("gui.brassworksmissions.tooltip.reroll_mission")
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(MissionUIHelper.TOOLTIP_HEADER_COLOR))));
            Item rerollitem = rewardStack.getItem();
            ItemStack rerollstack = new ItemStack(rerollitem);
            Component costPrefix = Component.translatable(
                    "gui.brassworksmissions.tooltip.cost_prefix", cappedCost
            ).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(MissionUIHelper.TOOLTIP_TEXT_COLOR)));

            Component costItem = rerollstack.getHoverName()
                    .copy()
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(MissionUIHelper.REWARD_ITEM_COLOR)))
                    .append(Component.translatable("gui.brassworksmissions.ui.plural_format"));

            tip.add(costPrefix.copy().append(costItem));

            tip.add(Component.translatable(
                    "gui.brassworksmissions.tooltip.cost_cap", rerollstack.getHoverName()
                            .copy()
                            .append(Component.translatable("gui.brassworksmissions.ui.plural_format"))
            ).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(MissionUIHelper.TOOLTIP_SUBTEXT_COLOR))
                    .withItalic(true)));

            long owned = this.entity.getInventory().countItem(rerollitem);
            if (owned < cappedCost) {
                tip.add(Component.translatable(
                        "gui.brassworksmissions.tooltip.not_enough", rerollstack.getHoverName()
                                .copy()
                                .append(Component.translatable("gui.brassworksmissions.ui.plural_format"))
                ).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(MissionUIHelper.TOOLTIP_ERROR_COLOR))));
            }

            guiGraphics.renderComponentTooltip(font, tip, mouseX, mouseY);
        }

        if (missionResetTimerLabel != null && missionResetTimerLabel.text != null) {
            int timerX = this.leftPos + 124 -36 -8 - 4;
            int timerY = this.topPos + 138 + 18 - 4;
            int textWidth = 77;
            int textHeight = 15;
            if (mouseX >= timerX && mouseX <= timerX + textWidth && mouseY >= timerY && mouseY <= timerY + textHeight) {
                long remainingMillis = UiScreenRenderHelper.getRemainingMillisUntilReset();
                List<Component> tip = UiScreenRenderHelper.getFormattedTooltipCountdown(remainingMillis);
                guiGraphics.renderComponentTooltip(font, tip, mouseX, mouseY);
            }
        }
    }

    private void renderLabels(GuiGraphics guiGraphics) {

        final int descriptionWrapWidth = 140;

        int centerX = this.leftPos + 39 - 3 + (118 / 2);
        int currentY = this.topPos + 72;
        int textX = this.leftPos + 28;

        if (titleLabel != null && titleLabel.text != null && !titleLabel.text.getString().isEmpty()) {
            int titleX = centerX - (this.font.width(titleLabel.text) / 2);
            guiGraphics.drawString(this.font, titleLabel.text, titleX, currentY, 0xFFFFFF, true);
        }

        currentY += 15;

        int descriptionLinesCount = 0;
        if (descriptionComponent != null && !descriptionComponent.getString().isEmpty()) {
            List<FormattedCharSequence> descriptionLines = this.font.split(descriptionComponent, descriptionWrapWidth);
            descriptionLinesCount = descriptionLines.size();
            for (FormattedCharSequence line : descriptionLines) {
                guiGraphics.drawString(this.font, line, textX, currentY, 0xFFFFFF, true);
                currentY += this.font.lineHeight;
            }
        }

        if (descriptionLinesCount == 0) {
            currentY += this.font.lineHeight;
        }

        if (progressTextLabel != null && progressTextLabel.text != null && !progressTextLabel.text.getString().isEmpty()) {
            guiGraphics.drawString(this.font, progressTextLabel.text, textX, this.topPos + 112, 0xFFFFFF, true);
        }

        if (progressLabel != null && progressLabel.text != null && !progressLabel.text.getString().isEmpty()) {
            guiGraphics.drawString(this.font, progressLabel.text, textX, this.topPos + 123, 0xFFFFFF, true);
        }

        if (missionResetTimerLabel != null && missionResetTimerLabel.text != null && !missionResetTimerLabel.text.getString().isEmpty()) {
            int timerX = this.leftPos + 124 -36 -8 +34 +1 - (this.font.width(missionResetTimerLabel.text) / 2);
            int timerY = this.topPos + 138+18;
            guiGraphics.drawString(this.font, missionResetTimerLabel.text, timerX , timerY, 0xFFFFFF, true);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.blit(ResourceLocation.parse("brassworksmissions:textures/screens/missionsui.png"),
                this.leftPos, this.topPos, 0, 0, 256, 175, 256, 175);
        RenderSystem.disableBlend();
        int idx = missionSelector.getState();
        var playerVariables = entity.getData(BrassworksmissionsModVariables.PLAYER_VARIABLES);
        ActiveMission mission = playerVariables.missionData.getMission(idx);

        ItemStack missionIcon = null;
        if (mission != null) {
            missionIcon = new ItemStack(mission.getRequirementItemStack().getItem(), 1);
        }
        if (missionIcon != null) {
            guiGraphics.renderItem(missionIcon, this.leftPos + 25, this.topPos + 43);
        }

        ItemStack allRewardsIcon = new ItemStack(rewardStack.getItem(), MissionController.getTotalClaimableItemCount(entity));
        int allRewardsX = this.leftPos + 10;
        int allRewardsY = this.topPos + 140+12;
        guiGraphics.renderItem(allRewardsIcon, allRewardsX, allRewardsY);
        guiGraphics.renderItemDecorations(this.font, allRewardsIcon, allRewardsX, allRewardsY);

        if (!rewardStack.isEmpty()) {
            int rewardX = this.leftPos + 26;
            int rewardY = this.topPos + 105+12;

            ItemStack rewardIcon = new ItemStack(rewardStack.getItem(), rewardStack.getCount());
            guiGraphics.renderItemDecorations(this.font, rewardIcon, rewardX + 78, rewardY - 2);
            guiGraphics.renderItem(rewardIcon, rewardX + 78, rewardY - 2);
        }
    }

    @Override
    public boolean keyPressed(int key, int b, int c) {
        if (key == 256 || key == KeybindingInit.OPEN_MISSIONS_UI_KEY.getKey().getValue()) {
            if (this.minecraft != null) {
                if (this.minecraft.player != null) {
                    this.minecraft.player.closeContainer();
                }
            }
            int selectedSlot = missionSelector.getState();

            entity.getData(BrassworksmissionsModVariables.PLAYER_VARIABLES).SelectedMission = selectedSlot;

            PacketDistributor.sendToServer(new UpdateSelectedMissionMessage(selectedSlot));
            return true;
        } else if (key == KeybindingInit.TRACK_MISSIONS_UI_KEY.getKey().getValue()) {
            PacketDistributor.sendToServer(new UpdateSelectedMissionMessage(missionSelector.getState()));
            PacketDistributor.sendToServer(new UiButtonMessage(3, x, y, (int) z, missionSelector.getState()));
        }
        return super.keyPressed(key, b, c);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {

    }

    @Override
    public void init() {
        super.init();

        this.missionsList.clear();

        initMissionSelector();

        initLabels();

        initButtons();
    }

    private void initMissionSelector() {
        final int SEL_X = this.leftPos + 50;
        final int SEL_Y = this.topPos + 44;
        final int SEL_W = 118;

        for (int i = 0; i < PlayerMissionData.MISSION_SLOTS; i++) {
            this.missionsList.add(Component.translatable("gui.brassworksmissions.ui.mission_prefix", i + 1));
        }

        missionSelector = new SelectionScrollInput(SEL_X - 3, SEL_Y - 2, SEL_W, 18) {
            @Override
            public void updateTooltip() { this.toolTip.clear(); }
        };
        missionSelector.forOptions(missionsList);
        missionSelector.titled(Component.translatable("gui.brassworksmissions.ui.select_mission"));
        missionSelector.setTooltip(Tooltip.create(Component.empty()));
        BrassworksmissionsModVariables.PlayerVariables playerVariables = entity.getData(BrassworksmissionsModVariables.PLAYER_VARIABLES);
        missionSelector.setState((int) playerVariables.SelectedMission);
        this.addRenderableWidget(missionSelector);
    }

    private void initLabels() {
        int textY = this.topPos + 70;
        titleLabel = new Label(0, textY, Component.empty()).withShadow();
        progressLabel = new Label(0, textY + 24, Component.empty()).withShadow();
        progressTextLabel = new Label(0, textY + 24, Component.empty()).withShadow();
        missionResetTimerLabel = new Label(0, 0, Component.empty()).withShadow();
    }

    private void initButtons() {
        var playerVariables = entity.getData(BrassworksmissionsModVariables.PLAYER_VARIABLES);

        claimRewardsButton = new IconButton(this.leftPos + 30, this.topPos + 139+12, MissionIcons.I_CLAIM)
                .withCallback(() -> {
                    int selectedSlot = missionSelector.getState();
                    playerVariables.SelectedMission = selectedSlot;
                    PacketDistributor.sendToServer(new UpdateSelectedMissionMessage(selectedSlot));
                    PacketDistributor.sendToServer(new UiButtonMessage(0, x, y, (int) z, selectedSlot));
                });
        this.addRenderableWidget(claimRewardsButton);

        closeButton = new IconButton(this.leftPos + 167, this.topPos + 139+12, AllIcons.I_CONFIRM)
                .withCallback(() -> {
                    int selectedSlot = missionSelector.getState();
                    playerVariables.SelectedMission = selectedSlot;
                    PacketDistributor.sendToServer(new UpdateSelectedMissionMessage(selectedSlot));
                    if (UiScreen.this.minecraft != null) {
                        if (UiScreen.this.minecraft.player != null) {
                            UiScreen.this.minecraft.player.closeContainer();
                        }
                    }
                });
        this.addRenderableWidget(closeButton);

        rerollButton = new CustomIconButton(this.leftPos + 124, this.topPos + 102+12, AllIcons.I_REFRESH)
                .withCallback(() -> {
                    int selectedSlot = missionSelector.getState();
                    playerVariables.SelectedMission = selectedSlot;
                    PacketDistributor.sendToServer(new UpdateSelectedMissionMessage(selectedSlot));
                    PacketDistributor.sendToServer(new UiButtonMessage(2, x, y, (int) z, selectedSlot));
                });
        this.addRenderableWidget(rerollButton);

        trackButton = new CustomIconButton(this.leftPos + 145, this.topPos + 102+12, AllIcons.I_WHITELIST)
                .withCallback(() -> {
                    int selectedSlot = missionSelector.getState();
                    playerVariables.SelectedMission = selectedSlot;
                    PacketDistributor.sendToServer(new UpdateSelectedMissionMessage(selectedSlot));
                    PacketDistributor.sendToServer(new UiButtonMessage(3, x, y, (int) z, selectedSlot));
                });
        this.addRenderableWidget(trackButton);

        trackingIndicator = new Indicator(this.leftPos + 145, this.topPos + 96+12, Component.empty());
        this.addRenderableWidget(trackingIndicator);
    }

    @Override
    public void containerTick() {
        super.containerTick();

        if (missionSelector != null) {
            int idx = missionSelector.getState();
            var playerVariables = entity.getData(BrassworksmissionsModVariables.PLAYER_VARIABLES);
            ActiveMission mission = playerVariables.missionData.getMission(idx);

            claimRewardsButton.active = MissionController.getTotalClaimableItemCount(entity) >= 1;

            if (mission != null) {
                rerollButton.active = (playerVariables.reRollAmount * 2 <= 32) && !mission.isComplete();
            }

            if (trackingIndicator != null && trackButton != null) {
                boolean isTracked = playerVariables.trackedMissions.contains(idx);
                boolean isComplete = false;
                if (mission != null) {
                    isComplete = mission.isComplete();
                }

                if (isComplete && !isTracked) {

                    trackButton.active = false;
                    trackingIndicator.state = Indicator.State.OFF;
                } else {

                    trackButton.active = true;

                    trackingIndicator.state = isTracked ? Indicator.State.ON : Indicator.State.OFF;
                }
            }

            if (mission != null) {
                boolean isComplete = mission.isComplete();

                int titleColorHex = isComplete ? MissionUIHelper.COMPLETED_TITLE_COLOR : MissionUIHelper.INCOMPLETE_TITLE_COLOR;
                titleLabel.text = Component.literal(mission.getTitle())
                        .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(titleColorHex)));

                descriptionComponent = MissionUIHelper.getMissionDescription(mission);

                progressTextLabel.text = Component.translatable("gui.brassworksmissions.ui.progress")
                        .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(MissionUIHelper.PROGRESS_TEXT_COLOR)));

                progressLabel.text = MissionUIHelper.getFormattedProgress(mission, null);

                rewardStack = mission.getRewardItemStack();
            } else {
                titleLabel.text = Component.translatable("gui.brassworksmissions.ui.no_mission_data").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(MissionUIHelper.TOOLTIP_ERROR_COLOR)));
                descriptionComponent = Component.empty();
                progressLabel.text = Component.empty();
                progressTextLabel.text = Component.empty();
                rewardStack = ItemStack.EMPTY;
            }
        }

        long remainingMillis = UiScreenRenderHelper.getRemainingMillisUntilReset();
        if (missionResetTimerLabel != null) {
            missionResetTimerLabel.text = UiScreenRenderHelper.getFormattedCountdown(remainingMillis);
        }
    }
}


package net.swzo.brassworksmissions.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.createmod.catnip.gui.element.ScreenElement;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.swzo.brassworksmissions.client.gui.MissionGuiTextures;

public class CustomIconButton extends AbstractSimiWidget {

    protected ScreenElement icon;
    protected boolean isPressed;

    public CustomIconButton(int x, int y, ScreenElement icon) {
        this(x, y, 18, 18, icon);
    }

    public CustomIconButton(int x, int y, int w, int h, ScreenElement icon) {
        super(x, y, w, h);
        this.icon = icon;
    }

    @Override
    public void doRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (!visible) {
            return;
        }

        isHovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;

        MissionGuiTextures button;
        if (!active) {
            button = MissionGuiTextures.BUTTON_DISABLED;
        } else if (isPressed && isHovered) {
            button = MissionGuiTextures.BUTTON_DOWN;
        } else if (isHovered) {
            button = MissionGuiTextures.BUTTON_HOVER;
        } else {
            button = MissionGuiTextures.BUTTON;
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        drawBg(graphics, button);

        if (icon != null) {
            icon.render(graphics, getX() + 1, getY() + 1);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.active && this.visible && this.isHovered && button == 0) {
            this.isPressed = true;
            Minecraft.getInstance()
                    .getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        return false;
    }


    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.isPressed) {
            this.isPressed = false;
            if (this.active && this.isHovered) {
                this.onClick(mouseX, mouseY);
            }
            return true;
        }
        return false;
    }


    protected void drawBg(GuiGraphics graphics, MissionGuiTextures button) {
        graphics.blit(button.location, getX(), getY(), button.getStartX(), button.getStartY(), button.getWidth(),
                button.getHeight());
    }

    public void setToolTip(Component text) {
        toolTip.clear();
        toolTip.add(text);
    }

    public void setIcon(ScreenElement icon) {
        this.icon = icon;
    }
}


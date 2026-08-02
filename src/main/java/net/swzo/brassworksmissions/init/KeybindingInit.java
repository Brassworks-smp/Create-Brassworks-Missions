package net.swzo.brassworksmissions.init;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;


public class KeybindingInit {
    public static final KeyMapping OPEN_MISSIONS_UI_KEY = new KeyMapping(
            "key.brassworksmissions.open_missions_ui",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "key.category.brassworksmissions"
    );
    public static final KeyMapping TRACK_MISSIONS_UI_KEY = new KeyMapping(
            "key.brassworksmissions.track_missions_ui",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_T,
            "key.category.brassworksmissions"
    );
}


package tech.onetap.ui.punch.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import tech.onetap.util.render.builders.Builder;
import tech.onetap.util.render.builders.states.QuadColorState;
import tech.onetap.util.render.builders.states.QuadRadiusState;
import tech.onetap.util.render.builders.states.SizeState;
import tech.onetap.util.render.renderers.IRenderer;
import tech.onetap.util.render.renderers.impl.BuiltTexture;

public final class PlayerHead {
    private static final float HEAD_U = 8.0F / 64.0F;
    private static final float HEAD_V = 8.0F / 64.0F;
    private static final float HEAD_SIZE = 8.0F / 64.0F;

    private PlayerHead() {
    }

    public static void draw(float x, float y, float size, Identifier skin, float radius, int color) {
        if (skin == null) {
            return;
        }
        int textureId = MinecraftClient.getInstance().getTextureManager().getTexture(skin).getGlId();
        BuiltTexture texture = Builder.texture()
                .size(new SizeState(size, size))
                .radius(new QuadRadiusState(radius))
                .color(new QuadColorState(color))
                .texture(HEAD_U, HEAD_V, HEAD_SIZE, HEAD_SIZE, textureId)
                .smoothness(1.0F)
                .build();
        texture.render(IRenderer.DEFAULT_MATRIX, x, y, 0.0F);
    }
}
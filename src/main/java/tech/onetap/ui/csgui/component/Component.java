package tech.onetap.ui.csgui.component;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import tech.onetap.util.render.math.Animation;
import tech.onetap.util.render.math.Easing;

public abstract class Component {
    @Getter
    protected float x;
    @Getter
    protected float y;
    @Getter
    protected float width;
    @Getter
    protected float height;
    @Setter
    protected float alpha = 1.0f;
    private final Animation visibilityAnimation = new Animation(Easing.CUBIC_OUT, 180);

    public void setBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public float updateVisibility(boolean visible) {
        visibilityAnimation.run(visible ? 1f : 0f);
        return (float) visibilityAnimation.getValue();
    }

    public float getVisibilityProgress() {
        return (float) visibilityAnimation.getValue();
    }

    public abstract void render(DrawContext context, int mouseX, int mouseY, float delta);

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
    }
}

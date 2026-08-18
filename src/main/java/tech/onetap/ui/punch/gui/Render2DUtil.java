package tech.onetap.ui.punch.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import tech.onetap.ui.punch.color.ColorUtil;
import tech.onetap.ui.punch.math.MathUtil;
import tech.onetap.util.render.builders.Builder;
import tech.onetap.util.render.builders.states.QuadColorState;
import tech.onetap.util.render.builders.states.QuadRadiusState;
import tech.onetap.util.render.builders.states.SizeState;
import tech.onetap.util.render.math.Scissor;
import tech.onetap.util.render.renderers.impl.BuiltBlur;
import tech.onetap.util.render.renderers.impl.BuiltBorder;
import tech.onetap.util.render.renderers.impl.BuiltRectangle;
import tech.onetap.util.render.renderers.impl.BuiltTexture;

import java.util.ArrayDeque;
import java.util.Deque;

public final class Render2DUtil {
    private Render2DUtil() {
    }

    private static final Deque<float[]> SCISSOR_STACK = new ArrayDeque<>();

    public static void beginFrame() {
        SCISSOR_STACK.clear();
    }

    public static void flush() {
    }

    public static void setBackdropBlurScale(float scale) {
    }

    public static void pushScissor(float x, float y, float width, float height) {
        float[] parent = SCISSOR_STACK.peek();
        float nx = x;
        float ny = y;
        float nw = width;
        float nh = height;
        if (parent != null) {
            nx = Math.max(parent[0], x);
            ny = Math.max(parent[1], y);
            nw = Math.min(parent[0] + parent[2], x + width) - nx;
            nh = Math.min(parent[1] + parent[3], y + height) - ny;
            if (nw < 0.0F) nw = 0.0F;
            if (nh < 0.0F) nh = 0.0F;
        }
        SCISSOR_STACK.push(new float[]{nx, ny, nw, nh});
        Scissor.scissor(MinecraftClient.getInstance().getWindow(), nx, ny, nw, nh);
    }

    public static void popScissor() {
        if (SCISSOR_STACK.isEmpty()) {
            return;
        }
        SCISSOR_STACK.pop();
        float[] top = SCISSOR_STACK.peek();
        if (top == null) {
            Scissor.unset();
        } else {
            Scissor.scissor(MinecraftClient.getInstance().getWindow(), top[0], top[1], top[2], top[3]);
        }
    }

    public static boolean isPointScissored(float mouseX, float mouseY) {
        if (SCISSOR_STACK.isEmpty()) {
            return false;
        }
        float[] top = SCISSOR_STACK.peek();
        return mouseX < top[0] || mouseX > top[0] + top[2] || mouseY < top[1] || mouseY > top[1] + top[3];
    }

    public static void menuBackground(float x, float y, float width, float height, float radius,
                                     float time, int shaderMode, int primary, int secondary) {
        if (shaderMode == 0) {
            return;
        }
        float pulse = (float) (Math.sin(time * 0.4F) * 0.5F + 0.5F);
        int c1 = ColorUtil.lerp(primary, secondary, pulse);
        int c2 = ColorUtil.lerp(secondary, primary, pulse);
        BuiltRectangle rect = Builder.rectangle()
                .size(new SizeState(width, height))
                .color(new QuadColorState(c1, c2, c2, c1))
                .radius(new QuadRadiusState(radius))
                .smoothness(1)
                .build();
        rect.render(x, y);
    }

    public static RectBuilder rect(float x, float y, float width, float height) {
        return new RectBuilder(x, y, width, height);
    }

    public static TextBuilder text(float x, float y, float size, String value) {
        return new TextBuilder(x, y, size, value);
    }

    public static TextureBuilder texture(Identifier texture, float x, float y, float width, float height) {
        return texture(x, y, width, height, texture);
    }

    public static TextureBuilder texture(float x, float y, float width, float height, Identifier texture) {
        return new TextureBuilder(x, y, width, height, texture);
    }

    public static ColorGridBuilder colorGrid(float x, float y, float cellSize, int columns, int[] colors) {
        return new ColorGridBuilder(x, y, cellSize, columns, colors);
    }

    public static final class RectBuilder {
        private final float x;
        private final float y;
        private final float width;
        private final float height;
        private int color = ColorUtil.TRANSPARENT;
        private float radius;
        private Float borderThickness;
        private Integer borderColor;
        private Integer shadowColor;
        private Float shadowBlur;
        private float blur;
        private float blurAlpha = -1.0F;
        private boolean glass;
        private float glassAlpha;
        private float glassBorderThickness;
        private float glassBlur;

        private RectBuilder(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public RectBuilder color(int color) {
            this.color = color;
            return this;
        }

        public RectBuilder radius(float radius) {
            this.radius = radius;
            return this;
        }

        public RectBuilder radius(float topLeft, float topRight, float bottomRight, float bottomLeft) {
            this.radius = -1.0F;
            this.borderRadius = new float[]{topLeft, topRight, bottomRight, bottomLeft};
            return this;
        }

        private float[] borderRadius;

        public RectBuilder border(float thickness, int color) {
            this.borderThickness = thickness;
            this.borderColor = color;
            return this;
        }

        public RectBuilder shadow(int color, float blur) {
            this.shadowColor = color;
            this.shadowBlur = blur;
            return this;
        }

        public RectBuilder blur(float blur) {
            this.blur = blur;
            return this;
        }

        public RectBuilder blur(float blur, float alpha) {
            this.blur = blur;
            this.blurAlpha = alpha;
            return this;
        }

        public RectBuilder glass(float alpha, float borderThickness, float blur) {
            this.glass = true;
            this.glassAlpha = alpha;
            this.glassBorderThickness = borderThickness;
            this.glassBlur = blur;
            return this;
        }

        public void draw() {
            QuadRadiusState radiusState = borderRadius != null
                    ? new QuadRadiusState(borderRadius[0], borderRadius[1], borderRadius[2], borderRadius[3])
                    : new QuadRadiusState(radius);

            Integer shadowColor = this.shadowColor;
            Float shadowBlur = this.shadowBlur;
            if (shadowColor != null) {
                BuiltBlur blur = Builder.blur()
                        .size(new SizeState(width, height))
                        .color(new QuadColorState(shadowColor))
                        .radius(radiusState)
                        .blurRadius(Math.max(1.0F, shadowBlur))
                        .smoothness(1)
                        .build();
                blur.render(x, y);
            }

            if (glass) {
                BuiltBlur glassBlurRender = Builder.blur()
                        .size(new SizeState(width, height))
                        .color(new QuadColorState(ColorUtil.multiplyAlpha(0xFFFFFFFF, glassAlpha)))
                        .radius(radiusState)
                        .blurRadius(Math.max(1.0F, this.glassBlur))
                        .smoothness(1)
                        .build();
                glassBlurRender.render(x, y);
            } else if (blur > 0.0F) {
                int c = blurAlpha >= 0.0F
                        ? ColorUtil.multiplyAlpha(0xFFFFFFFF, MathUtil.clamp01(blurAlpha))
                        : 0xFFFFFFFF;
                BuiltBlur blurRender = Builder.blur()
                        .size(new SizeState(width, height))
                        .color(new QuadColorState(c))
                        .radius(radiusState)
                        .blurRadius(Math.max(1.0F, blur))
                        .smoothness(1)
                        .build();
                blurRender.render(x, y);
            }

            int fillColor = glass ? ColorUtil.multiplyAlpha(0xFF1B1D23, glassAlpha) : color;
            if (fillColor != ColorUtil.TRANSPARENT) {
                BuiltRectangle rect = Builder.rectangle()
                        .size(new SizeState(width, height))
                        .color(new QuadColorState(fillColor))
                        .radius(radiusState)
                        .smoothness(1)
                        .build();
                rect.render(x, y);
            }

            if (borderThickness != null && borderColor != null && borderThickness > 0.0F) {
                BuiltBorder border = Builder.border()
                        .size(new SizeState(width, height))
                        .color(new QuadColorState(borderColor))
                        .radius(radiusState)
                        .thickness(borderThickness)
                        .smoothness(1.0F, 1.0F)
                        .build();
                border.render(x, y);
            }
        }
    }

    public static final class TextBuilder {
        private final float x;
        private final float y;
        private final float size;
        private final String value;
        private int color = -1;
        private UiFontStyle style = UiFontStyle.REGULAR;
        private TextAlign align = TextAlign.LEFT;
        private float scale = 1.0F;

        private TextBuilder(float x, float y, float size, String value) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.value = value;
        }

        public TextBuilder style(UiFontStyle style) {
            this.style = style;
            return this;
        }

        public TextBuilder color(int color) {
            this.color = color;
            return this;
        }

        public TextBuilder align(TextAlign align) {
            this.align = align;
            return this;
        }

        public TextBuilder scale(float scale) {
            this.scale = scale;
            return this;
        }

        public void draw() {
            if (value == null || value.isEmpty()) {
                return;
            }
            MsdfFont font = UiFonts.sfPro(style.weight());
            float textSize = size * scale;
            float letterSpacing = textSize * style.letterSpacingEm();
            float textWidth = font.measureWidth(value, textSize, letterSpacing);
            float drawX = x;
            if (align == TextAlign.CENTER) {
                drawX = x - textWidth / 2.0F;
            } else if (align == TextAlign.RIGHT) {
                drawX = x - textWidth;
            }
            tech.onetap.util.render.msdf.MsdfFont delegate = font.getFont();
            if (letterSpacing > 0.01F) {
                tech.onetap.util.render.msdf.MsdfRenderer.renderText(delegate, value, textSize, color,
                        tech.onetap.util.render.renderers.IRenderer.DEFAULT_MATRIX,
                        drawX, y - (size - textSize) / 2.0F, 0.0F);
            } else {
                tech.onetap.util.render.renderers.DrawUtil.drawText(delegate, value, drawX, y, color, textSize);
            }
        }
    }

    public static final class TextureBuilder {
        private final float x;
        private final float y;
        private final float width;
        private final float height;
        private final Identifier texture;
        private int color = -1;

        private TextureBuilder(float x, float y, float width, float height, Identifier texture) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.texture = texture;
        }

        public TextureBuilder color(int color) {
            this.color = color;
            return this;
        }

        public void draw() {
            Window window = MinecraftClient.getInstance().getWindow();
            if (window == null) {
                return;
            }
            int textureId = MinecraftClient.getInstance().getTextureManager().getTexture(texture).getGlId();
            BuiltTexture built = Builder.texture()
                    .size(new SizeState(width, height))
                    .radius(QuadRadiusState.NO_ROUND)
                    .color(new QuadColorState(color))
                    .texture(0.0F, 0.0F, 1.0F, 1.0F, textureId)
                    .smoothness(1.0F)
                    .build();
            built.render(tech.onetap.util.render.renderers.IRenderer.DEFAULT_MATRIX, x, y, 0.0F);
        }
    }

    public static final class ColorGridBuilder {
        private final float x;
        private final float y;
        private final float cellSize;
        private final int columns;
        private final int[] colors;
        private int color = -1;

        private ColorGridBuilder(float x, float y, float cellSize, int columns, int[] colors) {
            this.x = x;
            this.y = y;
            this.cellSize = cellSize;
            this.columns = Math.max(1, columns);
            this.colors = colors;
        }

        public ColorGridBuilder radius(float radius) {
            return this;
        }

        public ColorGridBuilder color(int color) {
            this.color = color;
            return this;
        }

        public void draw() {
            if (colors == null) {
                return;
            }
            for (int i = 0; i < colors.length; i++) {
                int column = i % columns;
                int row = i / columns;
                DrawRect.color(x + column * cellSize, y + row * cellSize, cellSize, cellSize, colors[i]);
            }
        }
    }

    private static final class DrawRect {
        private DrawRect() {
        }

        private static void color(float x, float y, float width, float height, int color) {
            tech.onetap.util.render.renderers.DrawUtil.drawRound(x, y, width, height, 0.0F, color);
        }
    }
}
package tech.onetap.ui.menu;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import tech.onetap.util.alt.Alt;
import tech.onetap.util.alt.AltManager;
import tech.onetap.util.cursor.CursorManager;
import tech.onetap.util.render.builders.Builder;
import tech.onetap.util.render.builders.states.QuadColorState;
import tech.onetap.util.render.builders.states.QuadRadiusState;
import tech.onetap.util.render.builders.states.SizeState;
import tech.onetap.util.render.helper.HoverUtil;
import tech.onetap.util.render.math.Scissor;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.CustomDrawContext;
import tech.onetap.util.render.renderers.DrawUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AltWidget {
    public final List<Alt> alts = new ArrayList<>();
    public boolean open = false;
    private String altName = "";
    private boolean typing;
    private float scrollOffset = 0f;

    private static final Identifier STAR_ID = Identifier.of("mre", "images/star.png");
    private static final float ROW_HEIGHT = 25f;
    private static final float ROW_SPACE = 30f;
    private static final float STAR_SIZE = 14f;

    private final float mainWidth = 400;
    private final float mainHeight = 350;
    private final float createWidth = 180;
    private final float createHeight = 180;
    private final float[] buttonHoverProgress = new float[3];

    private final MinecraftClient mc = MinecraftClient.getInstance();

    public AltWidget() {
        for (int i = 0; i < buttonHoverProgress.length; i++) {
            buttonHoverProgress[i] = 0f;
        }
    }

    public void close() {
        open = false;
        typing = false;
        altName = "";
        scrollOffset = 0f;
    }

    public void updateScroll(double mouseX, double mouseY, float delta) {
        if (!open || delta == 0) return;
        float mainX = (mc.getWindow().getScaledWidth() - mainWidth) / 2f;
        float mainY = (mc.getWindow().getScaledHeight() - mainHeight) / 2f;
        if (HoverUtil.isHovered(mouseX, mouseY, mainX, mainY + 20, mainWidth, mainHeight - 60)) {
            refresh();
            float contentHeight = alts.size() * ROW_SPACE;
            float maxScroll = Math.max(0, contentHeight - (mainHeight - 80));
            scrollOffset = MathHelper.clamp(scrollOffset - delta * 14f, 0, maxScroll);
        }
    }

    public void render(CustomDrawContext ctx, int mouseX, int mouseY) {
        if (!open) return;

        float mainX = (mc.getWindow().getScaledWidth() - mainWidth) / 2f;
        float mainY = (mc.getWindow().getScaledHeight() - mainHeight) / 2f;
        float createX = mainX + mainWidth + 10;
        float createY = mainY + (mainHeight - createHeight) / 2f;

        int themeA = ColorProvider.getThemeColor();
        int themeB = ColorProvider.getThemeColorTwo();
        float lineHeight = 2f;

        int panelBase = ColorProvider.rgba(6, 14, 34, 255);
        int panelFill = ColorProvider.rgba(9, 20, 46, 255);
        int panelHover = ColorProvider.rgba(22, 34, 66, 255);
        int panelBorder = ColorProvider.rgba(38, 58, 110, 200);

        DrawUtil.drawRect(ctx.getMatrices(), 0, 0, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(), ColorProvider.rgba(0, 0, 0, 178));

        DrawUtil.drawRoundBlur(mainX, mainY, mainWidth, mainHeight, 10f, panelBase, 18f);
        DrawUtil.drawRound(mainX - 0.7f, mainY - 0.7f, mainWidth + 1.4f, mainHeight + 1.4f, 10f, panelBorder);
        DrawUtil.drawRound(mainX, mainY, mainWidth, mainHeight, 10f, panelFill);
        DrawUtil.drawRound(mainX + 8f, mainY + 1f, mainWidth - 16f, lineHeight, 1f, ColorProvider.setAlpha(themeA, 200));

        DrawUtil.drawText(Fonts.SFBOLD.get(), "\u041C\u0435\u043D\u0435\u0434\u0436\u0435\u0440 \u0430\u043A\u043A\u0430\u0443\u043D\u0442\u043E\u0432", mainX + 10, mainY + 8, ColorProvider.rgba(255, 255, 255, 255), 10f);

        float exitButtonX = mainX + 10;
        float exitButtonY = mainY + mainHeight - 35;
        boolean isExitButtonHovered = HoverUtil.isHovered(mouseX, mouseY, exitButtonX, exitButtonY, mainWidth - 20, 25);
        if (isExitButtonHovered) {
            CursorManager.requestHand();
        }
        buttonHoverProgress[0] = MathHelper.lerp(0.2f, buttonHoverProgress[0], isExitButtonHovered ? 1.0f : 0.0f);
        int exitBorder = ColorProvider.rgba(
                (int) MathHelper.lerp(buttonHoverProgress[0], ColorProvider.red(themeA), ColorProvider.red(themeB)),
                (int) MathHelper.lerp(buttonHoverProgress[0], ColorProvider.green(themeA), ColorProvider.green(themeB)),
                (int) MathHelper.lerp(buttonHoverProgress[0], ColorProvider.blue(themeA), ColorProvider.blue(themeB)),
                255);
        if (buttonHoverProgress[0] > 0) {
            DrawUtil.drawRoundBlur(exitButtonX, exitButtonY, mainWidth - 20, 25, 3f, ColorProvider.setAlpha(exitBorder, (int) (255 * buttonHoverProgress[0])), 12f);
        }
        DrawUtil.drawRound(exitButtonX, exitButtonY, mainWidth - 20, 25, 3f, isExitButtonHovered ? panelHover : panelBorder);
        DrawUtil.drawText(Fonts.SFREGULAR.get(), "\u0412\u044B\u0445\u043E\u0434", exitButtonX + 10, exitButtonY + (25f - 8f) / 2f, ColorProvider.rgba(255, 255, 255, 255), 8f);

        Scissor.push();
        Scissor.setFromComponentCoordinates(mainX, mainY + 20, mainWidth, mainHeight - 60);
        refresh();
        float listTop = mainY + 20;
        float listBottom = mainY + mainHeight - 60;
        float contentHeight = alts.size() * ROW_SPACE;
        float maxScroll = Math.max(0, contentHeight - (listBottom - listTop));
        scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);

        float i = 0;
        for (Alt alt : alts) {
            float rowY = mainY + 26 + i * ROW_SPACE - scrollOffset;
            if (rowY + ROW_HEIGHT >= listTop && rowY <= listBottom) {
                boolean isHovered = HoverUtil.isHovered(mouseX, mouseY, mainX + 10, rowY, mainWidth - 20, ROW_HEIGHT);
                boolean isSelected = mc.getSession().getUsername() != null && mc.getSession().getUsername().equals(alt.name());
                boolean pinned = alt.pinned();

                if (isHovered) {
                    CursorManager.requestHand();
                }

                int rowFill;
                if (isSelected) {
                    rowFill = ColorProvider.rgba(214, 216, 224, 232);
                } else {
                    rowFill = isHovered ? panelHover : panelBorder;
                }

                if (isSelected || isHovered) {
                    DrawUtil.drawRoundBlur(mainX + 10, rowY, mainWidth - 20, ROW_HEIGHT, 3f,
                            isSelected ? ColorProvider.setAlpha(themeA, 150) : ColorProvider.setAlpha(themeA, 90), 12f);
                }

                DrawUtil.drawRound(mainX + 10, rowY, mainWidth - 20, ROW_HEIGHT, 3f, rowFill);
                if (isSelected) {
                    DrawUtil.drawRoundBlur(mainX + 10, rowY, mainWidth - 20, ROW_HEIGHT, 3f,
                            ColorProvider.setAlpha(ColorProvider.rgba(86, 156, 255, 255), 130), 10f);
                }

                int nameColor = isSelected
                        ? ColorProvider.rgba(24, 26, 34, 255)
                        : ColorProvider.rgba(255, 255, 255, 255);
                float nameMaxW = mainWidth - 20 - STAR_SIZE - 12;
                DrawUtil.drawText(Fonts.SFREGULAR.get(), alt.name(), mainX + 15, rowY + (ROW_HEIGHT - 8f) / 2f,
                        nameColor, 8f, 0.8f, 1f, nameMaxW);

                float starX = mainX + mainWidth - 10 - STAR_SIZE;
                float starY = rowY + (ROW_HEIGHT - STAR_SIZE) / 2f;
                boolean starHovered = HoverUtil.isHovered(mouseX, mouseY, starX, starY, STAR_SIZE, STAR_SIZE);
                drawStar(ctx, starX, starY, STAR_SIZE, pinned, starHovered || isSelected);
            }
            i++;
        }

        if (maxScroll > 0) {
            float trackX = mainX + mainWidth - 9;
            float trackY = listTop + 2;
            float trackH = listBottom - listTop - 4;
            DrawUtil.drawRound(trackX, trackY, 3f, trackH, 1.5f, ColorProvider.rgba(255, 255, 255, 40));
            float handleH = Math.max(18f, trackH * (listBottom - listTop) / contentHeight);
            float handleY = trackY + (trackH - handleH) * (scrollOffset / maxScroll);
            DrawUtil.drawRound(trackX, handleY, 3f, handleH, 1.5f, ColorProvider.setAlpha(themeA, 210));
        }
        Scissor.unset();
        Scissor.pop();

        DrawUtil.drawRoundBlur(createX, createY, createWidth, createHeight, 10f, panelBase, 18f);
        DrawUtil.drawRound(createX - 0.7f, createY - 0.7f, createWidth + 1.4f, createHeight + 1.4f, 10f, panelBorder);
        DrawUtil.drawRound(createX, createY, createWidth, createHeight, 10f, panelFill);
        DrawUtil.drawRound(createX + 8f, createY + 1f, createWidth - 16f, lineHeight, 1f, ColorProvider.setAlpha(themeA, 200));

        DrawUtil.drawText(Fonts.SFBOLD.get(), "\u0421\u043E\u0437\u0434\u0430\u0442\u044C \u0430\u043A\u043A\u0430\u0443\u043D\u0442", createX + 10, createY + 8, ColorProvider.rgba(255, 255, 255, 255), 10f);

        String textToDraw = altName.isEmpty() && !typing ? "\u0412\u0432\u0435\u0434\u0438\u0442\u0435 \u0438\u043C\u044F" : altName;
        boolean isInputHovered = HoverUtil.isHovered(mouseX, mouseY, createX + 10, createY + 30, createWidth - 20, 25);
        if (isInputHovered || typing) {
            CursorManager.requestIBeam();
        }
        DrawUtil.drawRoundBlur(createX + 10, createY + 30, createWidth - 20, 25, 3f, ColorProvider.setAlpha(themeA, 140), 12f);
        DrawUtil.drawRound(createX + 10, createY + 30, createWidth - 20, 25, 3f, typing || isInputHovered ? panelHover : panelBorder);
        int inputColor = textToDraw.equals("\u0412\u0432\u0435\u0434\u0438\u0442\u0435 \u0438\u043C\u044F")
                ? ColorProvider.rgba(190, 200, 220, 160)
                : ColorProvider.rgba(255, 255, 255, typing ? 255 : 200);
        DrawUtil.drawText(Fonts.SFREGULAR.get(), textToDraw + (typing ? (System.currentTimeMillis() % 1000 > 500 ? "_" : "") : ""),
                createX + 15, createY + 30 + (25f - 8f) / 2f, inputColor, 8f, 0.8f, 1f, createWidth - 30);

        float addButtonX = createX + 10;
        float addButtonY = createY + createHeight - 80;
        boolean isAddButtonHovered = HoverUtil.isHovered(mouseX, mouseY, addButtonX, addButtonY, createWidth - 20, 30);
        if (isAddButtonHovered) {
            CursorManager.requestHand();
        }
        buttonHoverProgress[1] = MathHelper.lerp(0.2f, buttonHoverProgress[1], isAddButtonHovered ? 1.0f : 0.0f);
        if (buttonHoverProgress[1] > 0) {
            DrawUtil.drawRoundBlur(addButtonX, addButtonY, createWidth - 20, 30, 3f, ColorProvider.setAlpha(themeA, (int) (255 * buttonHoverProgress[1])), 12f);
        }
        DrawUtil.drawRound(addButtonX, addButtonY, createWidth - 20, 30, 3f, isAddButtonHovered ? panelHover : panelBorder);
        DrawUtil.drawText(Fonts.SFREGULAR.get(), "\u0414\u043E\u0431\u0430\u0432\u0438\u0442\u044C", addButtonX + 10, addButtonY + (30f - 8f) / 2f, ColorProvider.rgba(255, 255, 255, 255), 8f);

        float randomButtonX = createX + 10;
        float randomButtonY = createY + createHeight - 40;
        boolean isRandomButtonHovered = HoverUtil.isHovered(mouseX, mouseY, randomButtonX, randomButtonY, createWidth - 20, 30);
        if (isRandomButtonHovered) {
            CursorManager.requestHand();
        }
        buttonHoverProgress[2] = MathHelper.lerp(0.2f, buttonHoverProgress[2], isRandomButtonHovered ? 1.0f : 0.0f);
        if (buttonHoverProgress[2] > 0) {
            DrawUtil.drawRoundBlur(randomButtonX, randomButtonY, createWidth - 20, 30, 3f, ColorProvider.setAlpha(themeA, (int) (255 * buttonHoverProgress[2])), 12f);
        }
        DrawUtil.drawRound(randomButtonX, randomButtonY, createWidth - 20, 30, 3f, isRandomButtonHovered ? panelHover : panelBorder);
        DrawUtil.drawText(Fonts.SFREGULAR.get(), "\u0420\u0430\u043D\u0434\u043E\u043C", randomButtonX + 10, randomButtonY + (30f - 8f) / 2f, ColorProvider.rgba(255, 255, 255, 255), 8f);
    }

    private void drawStar(CustomDrawContext ctx, float x, float y, float size, boolean pinned, boolean emphasized) {
        int starTex = mc.getTextureManager().getTexture(STAR_ID).getGlId();
        int color;
        if (pinned) {
            color = ColorProvider.setAlpha(ColorProvider.rgba(255, 215, 0, 255), 250);
        } else {
            color = ColorProvider.rgba(emphasized ? 210 : 150, emphasized ? 220 : 150, 255, 235);
        }
        Builder.texture()
                .size(new SizeState(size, size))
                .radius(QuadRadiusState.NO_ROUND)
                .color(new QuadColorState(color))
                .texture(0f, 0f, 1f, 1f, starTex)
                .smoothness(1f)
                .build()
                .render(ctx.getMatrices().peek().getPositionMatrix(), x, y, 0f);
    }

    public void onChar(char typed) {
        if (typing) {
            if (Fonts.SFREGULAR.get().getWidth(altName, 8f) < createWidth - 50) {
                altName += typed;
            }
        }
    }

    public void onKey(int key) {
        boolean ctrlDown = GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
                GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
        if (typing) {
            if (ctrlDown && key == GLFW.GLFW_KEY_V) {
                try {
                    String clipboard = GLFW.glfwGetClipboardString(mc.getWindow().getHandle());
                    if (clipboard != null && Fonts.SFREGULAR.get().getWidth(altName + clipboard, 8f) < createWidth - 50) {
                        altName += clipboard;
                    }
                } catch (Exception ignored) {
                }
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!altName.isEmpty()) {
                    altName = altName.substring(0, altName.length() - 1);
                }
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                if (AltManager.isValidName(altName)) {
                    AltManager.addAlt(altName);
                    altName = "";
                    typing = false;
                }
            }
        }
    }

    private String generateRandomName() {
        String[] prefixes = {"Cool", "Dark", "Fire", "Ice", "Shadow", "Storm"};
        String[] suffixes = {"Gamer", "Pro", "Ninja", "Wizard", "Knight", "Rider"};
        Random random = new Random();
        return prefixes[random.nextInt(prefixes.length)] + suffixes[random.nextInt(suffixes.length)] + random.nextInt(1000);
    }

    public void click(int mouseX, int mouseY, int button) {
        if (!open) return;

        float mainX = (mc.getWindow().getScaledWidth() - mainWidth) / 2f;
        float mainY = (mc.getWindow().getScaledHeight() - mainHeight) / 2f;
        float createX = mainX + mainWidth + 10;
        float createY = mainY + (mainHeight - createHeight) / 2f;

        float exitButtonX = mainX + 10;
        float exitButtonY = mainY + mainHeight - 35;
        if (HoverUtil.isHovered(mouseX, mouseY, exitButtonX, exitButtonY, mainWidth - 20, 25) && button == 0) {
            close();
            return;
        }

        if (HoverUtil.isHovered(mouseX, mouseY, mainX, mainY + 20, mainWidth, mainHeight - 60)) {
            refresh();
            float maxScroll = Math.max(0, alts.size() * ROW_SPACE - (mainHeight - 80));
            scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);
            float i = 0;
            for (Alt alt : alts) {
                float rowY = mainY + 26 + i * ROW_SPACE - scrollOffset;
                float starX = mainX + mainWidth - 10 - STAR_SIZE;
                float starY = rowY + (ROW_HEIGHT - STAR_SIZE) / 2f;
                if (button == 0 && HoverUtil.isHovered(mouseX, mouseY, starX, starY, STAR_SIZE, STAR_SIZE)) {
                    AltManager.setPinned(alt.name(), !alt.pinned());
                    refresh();
                    return;
                }
                if (HoverUtil.isHovered(mouseX, mouseY, mainX + 10, rowY, mainWidth - 20 - STAR_SIZE - 6, ROW_HEIGHT)) {
                    if (button == 0) {
                        AltManager.login(alt);
                    } else if (button == 1) {
                        AltManager.removeAlt(alt.name());
                        refresh();
                    }
                    return;
                }
                i++;
            }
        }

        if (HoverUtil.isHovered(mouseX, mouseY, createX + 10, createY + 30, createWidth - 20, 25)) {
            typing = !typing;
            if (!typing) {
                altName = "";
            }
        }

        float addButtonX = createX + 10;
        float addButtonY = createY + createHeight - 80;
        if (HoverUtil.isHovered(mouseX, mouseY, addButtonX, addButtonY, createWidth - 20, 30) && button == 0) {
            if (AltManager.isValidName(altName)) {
                AltManager.addAlt(altName);
                altName = "";
                typing = false;
            }
        }

        float randomButtonX = createX + 10;
        float randomButtonY = createY + createHeight - 40;
        if (HoverUtil.isHovered(mouseX, mouseY, randomButtonX, randomButtonY, createWidth - 20, 30) && button == 0) {
            altName = generateRandomName();
            AltManager.addAlt(altName);
            altName = "";
            typing = false;
        }
    }

    private void refresh() {
        alts.clear();
        alts.addAll(AltManager.getAlts());
    }
}
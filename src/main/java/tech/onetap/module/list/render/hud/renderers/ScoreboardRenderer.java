package tech.onetap.module.list.render.hud.renderers;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Formatting;
import tech.onetap.module.list.render.hud.Interface;
import tech.onetap.ui.punch.text.NameProtectUtil;
import tech.onetap.util.draggable.Draggable;
import tech.onetap.util.render.math.Animation;
import tech.onetap.util.render.math.Easing;
import tech.onetap.util.render.math.Scissor;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Кастомный Scoreboard в стиле Hud3/DLC: панель с заголовком и строками,
 * можно перетаскивать. Через NameProtectUtil имена подменяются в любых местах
 * (название заголовка, префиксы команд, сами строки записей).
 */
public class ScoreboardRenderer {
    private static final int MAX_LINES = 15;

    private final Interface owner;
    private final Animation alpha = new Animation(Easing.EXPO_OUT, 200);
    private final Animation widthAnim = new Animation(Easing.CUBIC_OUT, 300);

    public ScoreboardRenderer(Interface owner) {
        this.owner = owner;
    }

    public void render(DrawContext context) {
        if (owner.mc.player == null || owner.mc.world == null) return;

        Draggable drag = owner.getScoreboardDrag();
        if (!owner.getHudStyleSetting().is("DLC")) {
            drag.setWidth(0);
            drag.setHeight(0);
            return;
        }

        Scoreboard scoreboard = owner.mc.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (objective == null) {
            drag.setWidth(0);
            drag.setHeight(0);
            return;
        }

        List<ScoreboardEntry> entries = scoreboard.getScoreboardEntries(objective).stream()
                .filter(entry -> !entry.hidden())
                .sorted(Comparator.comparingInt(ScoreboardEntry::value).reversed()
                        .thenComparing(entry -> entry.owner(), String.CASE_INSENSITIVE_ORDER))
                .limit(MAX_LINES)
                .toList();

        alpha.run(1f);
        float globalAlpha = (float) alpha.getValue();
        if (globalAlpha <= 0.05f) return;

        String title = stripCodes(NameProtectUtil.protect(objective.getDisplayName().getString()));
        if (title == null || title.isEmpty()) title = "Scoreboard";

        List<Row> rows = new ArrayList<>();
        float maxContentWidth = 0f;
        for (ScoreboardEntry entry : entries) {
            String line = stripCodes(NameProtectUtil.protect(buildLine(scoreboard, entry)));
            String number = stripCodes(String.valueOf(entry.value()));
            if (line == null || line.isEmpty()) continue;

            int color = rowColor(scoreboard, entry);
            float textW = Fonts.SFMEDIUM.get().getWidth(line, 6f);
            float numW = Fonts.SFMEDIUM.get().getWidth(number, 6f);
            maxContentWidth = Math.max(maxContentWidth, textW + 14f + numW);
            rows.add(new Row(line, number, textW, numW, color));
        }

        float headerHeight = 15f;
        float itemSpacing = 11f;
        float minWidth = 64f;

        float titleWidth = Fonts.SFMEDIUM.get().getWidth(title, 7f);
        float targetWidth = Math.max(minWidth, Math.max(titleWidth + 20f, maxContentWidth + 16f));
        widthAnim.run(targetWidth);
        float currentWidth = Math.max(minWidth, (float) widthAnim.getValue());

        float rowCount = Math.max(1f, rows.size());
        float totalHeight = Math.max(20f, headerHeight + rowCount * itemSpacing);

        float x = drag.getX();
        float y = drag.getY();

        Hud3Style.drawPanel(x, y, currentWidth, totalHeight, true, globalAlpha);
        Hud3Style.drawHeader(x, y, currentWidth, title, null, globalAlpha);

        float rowY = y + headerHeight;
        Scissor.push();
        Scissor.setFromComponentCoordinates((int) x, (int) rowY, (int) currentWidth, (int) (totalHeight - headerHeight + 2f));

        for (Row row : rows) {
            float blockW = row.textW + 14f + row.numW;
            float blockX = x + Math.max(6f, (currentWidth - blockW) / 2f);
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), row.text, blockX, rowY + 3f,
                    ColorProvider.setAlpha(row.color, (int) (255 * globalAlpha)), 6f);
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), row.number, blockX + row.textW + 14f, rowY + 3f,
                    ColorProvider.setAlpha(ColorProvider.getThemeColor(), (int) (255 * globalAlpha)), 6f);
            rowY += itemSpacing;
        }

        Scissor.unset();
        Scissor.pop();

        drag.setWidth(currentWidth);
        drag.setHeight(totalHeight);
    }

    private String buildLine(Scoreboard scoreboard, ScoreboardEntry entry) {
        Team team = scoreboard.getScoreHolderTeam(entry.owner());
        String name = entry.name().getString();
        if (name == null || name.isEmpty()) name = entry.owner();
        String prefix = team == null ? "" : team.getPrefix().getString();
        String suffix = team == null ? "" : team.getSuffix().getString();
        if (prefix == null) prefix = "";
        if (suffix == null) suffix = "";
        return prefix + name + suffix;
    }

    private int rowColor(Scoreboard scoreboard, ScoreboardEntry entry) {
        Team team = scoreboard.getScoreHolderTeam(entry.owner());
        if (team != null) {
            Formatting color = team.getColor();
            if (color != null && color.getColorValue() != null) {
                return ColorProvider.rgba(
                        (color.getColorValue() >> 16) & 0xFF,
                        (color.getColorValue() >> 8) & 0xFF,
                        color.getColorValue() & 0xFF,
                        255);
            }
        }
        return ColorProvider.rgba(235, 238, 245, 255);
    }

    private String stripCodes(String input) {
        if (input == null) return "";
        return input.replaceAll("\u00A7[0-9A-FK-ORa-fk-orx]", "");
    }

    private record Row(String text, String number, float textW, float numW, int color) {
    }
}
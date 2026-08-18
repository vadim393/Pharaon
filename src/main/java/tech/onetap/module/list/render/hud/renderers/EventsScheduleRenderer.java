package tech.onetap.module.list.render.hud.renderers;

import net.minecraft.client.gui.DrawContext;
import tech.onetap.module.list.render.hud.Interface;
import tech.onetap.util.draggable.Draggable;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Расписание событий в стиле Hud3: панель с заголовком, вкладкой дня и
 * строками событий (время + название). Активное сейчас событие подсвечивается
 * цветом темы.
 */
public class EventsScheduleRenderer {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private static final Map<DayOfWeek, List<ScheduleEvent>> WEEK_SCHEDULE = new EnumMap<>(DayOfWeek.class);

    static {
        addEvent(DayOfWeek.MONDAY, "No Limit", "19:00", "22:00");
        addEvent(DayOfWeek.TUESDAY, "No Limit", "19:00", "22:00");
        addEvent(DayOfWeek.WEDNESDAY, "No Limit", "19:00", "22:00");
        addEvent(DayOfWeek.THURSDAY, "No Limit", "19:00", "22:00");
        addEvent(DayOfWeek.FRIDAY, "No Limit", "19:00", "22:00");
        addEvent(DayOfWeek.SATURDAY, "No Limit", "14:00", "18:00");
        addEvent(DayOfWeek.SATURDAY, "Kill Aura", "18:00", "22:00");
        addEvent(DayOfWeek.SUNDAY, "No Limit", "14:00", "18:00");
        addEvent(DayOfWeek.SUNDAY, "Kill Aura", "18:00", "22:00");
    }

    private static void addEvent(DayOfWeek day, String name, String start, String end) {
        WEEK_SCHEDULE.computeIfAbsent(day, k -> new ArrayList<>())
                .add(new ScheduleEvent(name, parse(start), parse(end)));
    }

    private static LocalTime parse(String text) {
        return LocalTime.parse(text, TIME_FORMAT);
    }

    private final Interface owner;

    public EventsScheduleRenderer(Interface owner) {
        this.owner = owner;
    }

    public void render(DrawContext context) {
        if (owner.mc.player == null) return;

        Draggable drag = owner.getEventsScheduleDrag();
        float x = drag.getX();
        float y = drag.getY();

        if (owner.getHudStyleSetting().is("DLC")) {
            renderPouchOld(context, x, y, drag);
            return;
        }

        renderClassic(context, x, y, drag);
    }

    private void renderPouchOld(DrawContext context, float x, float y, Draggable drag) {
        boolean visible = owner.mc.player != null;
        if (!visible) {
            drag.setWidth(0);
            drag.setHeight(0);
            return;
        }

        float headerHeight = 15f;
        float rowHeight = 11f;
        float padX = 4.5f;
        float width = 88f;
        float totalHeight = headerHeight + 3f * rowHeight + 5f;

        String airDropTime = nextAirDropTime();
        String mascotTime = nextMascotTime();
        String chestTime = nextChestTime();

        int theme = ColorProvider.getThemeColor();
        int bgA = 255;
        int alpha = 255;
        int textColor = ColorProvider.rgba(255, 255, 255, 220);
        int timeColor = ColorProvider.setAlpha(theme, 255);
        int headerTextColor = ColorProvider.setAlpha(theme, 255);
        int brightAccent = ColorProvider.interpolateColor(theme, ColorProvider.rgba(255, 255, 255, 255), 0.3f);
        int headerGradientEnd = ColorProvider.setAlpha(brightAccent, 255);
        int separatorColor = ColorProvider.setAlpha(theme, (int) (0.3f * 255));

        DrawUtil.drawRoundBlur(x, y, width, totalHeight, 5f, ColorProvider.rgba(0, 0, 0, (int) (255 * 0.45f)), 15f);
        DrawUtil.drawRoundBlur(x, y + headerHeight, width, totalHeight - headerHeight, 5f,
                ColorProvider.rgba(0, 0, 0, (int) (255 * 0.45f)), 15f);
        DrawUtil.drawRoundBlur(x, y, width, headerHeight, new org.joml.Vector4f(5f, 5f, 0f, 0f),
                ColorProvider.rgba(0, 0, 0, 255), 15f);

        float titleW = Fonts.SFMEDIUM.get().getWidth("Events", 7f);
        float iconW = Fonts.ICONS_NURIK.get().getWidth("Q", 8f);
        float headerGap = 5f;
        float headBlockW = titleW + headerGap + iconW;
        float headBlockX = x + Math.max(padX, (width - headBlockW) / 2f);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), "Events", headBlockX, y + (headerHeight - 7f) * 0.5f - 0.5f, headerTextColor, 7f);
        DrawUtil.drawText(Fonts.ICONS_NURIK.get(), "Q", headBlockX + titleW + headerGap, y + 4.5f, headerGradientEnd, 8f);

        float baseY = y + headerHeight + 4f;
        DrawUtil.drawRound(x + padX, baseY - 2f, width - padX * 2f, 0.5f, 0f, separatorColor);

        String[] names = {"AirDrop", "Mascot", "Chest"};
        String[] times = {airDropTime, mascotTime, chestTime};
        for (int i = 0; i < names.length; i++) {
            float rowY = baseY + (float) i * rowHeight;
            float nameW = Fonts.SFMEDIUM.get().getWidth(names[i], 6f);
            float timeW = Fonts.SFMEDIUM.get().getWidth(times[i], 6f);
            float gap = 12f;
            float blockW = nameW + gap + timeW;
            float blockX = x + Math.max(padX, (width - blockW) / 2f);
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), names[i], blockX, rowY + 1f, textColor, 6f);
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), times[i], blockX + nameW + gap, rowY + 1f, timeColor, 6f);
        }

        drag.setWidth(width);
        drag.setHeight(totalHeight);
    }

    private String nextAirDropTime() {
        LocalTime now = LocalTime.now().withNano(0);
        LocalTime target = null;
        for (int hour = 9; hour <= 23; hour += 2) {
            LocalTime t = LocalTime.of(hour, 0);
            if (t.isAfter(now)) {
                target = t;
                break;
            }
        }
        if (target == null) target = LocalTime.of(9, 0);
        return formatCountdown(now, target);
    }

    private String nextMascotTime() {
        LocalTime now = LocalTime.now().withNano(0);
        LocalTime target = now.isBefore(LocalTime.of(15, 30)) ? LocalTime.of(15, 30) : null;
        return formatCountdown(now, target);
    }

    private String nextChestTime() {
        LocalTime now = LocalTime.now().withNano(0);
        int bucket = now.getHour() / 6 * 6;
        LocalTime target = LocalTime.of(bucket, 0);
        if (!target.isAfter(now)) target = target.plusHours(6);
        return formatCountdown(now, target);
    }

    private String formatCountdown(LocalTime now, LocalTime target) {
        if (target == null) return "--s";
        long hours = java.time.Duration.between(now, target).toHours();
        long minutes = java.time.Duration.between(now, target).toMinutes() % 60;
        long seconds = java.time.Duration.between(now, target).toSeconds() % 60;
        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0 || hours > 0) sb.append(minutes).append("m ");
        sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    private void renderClassic(DrawContext context, float x, float y, Draggable drag) {
        float width = 100f;
        float height = 16f;

        DrawUtil.drawRound(x, y, width, height, 3f, ColorProvider.rgba(20, 22, 28, 200));
        DrawUtil.drawText(Fonts.SFREGULAR.get(), "Schedule", x + 3f, y + 4.5f, -1, 6.5f);

        drag.setWidth(width);
        drag.setHeight(height);
    }

    private List<ScheduleEvent> getDayEvents(DayOfWeek day) {
        List<ScheduleEvent> list = WEEK_SCHEDULE.getOrDefault(day, Collections.emptyList());
        return new ArrayList<>(list);
    }

    private String dayName(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "ПН";
            case TUESDAY -> "ВТ";
            case WEDNESDAY -> "СР";
            case THURSDAY -> "ЧТ";
            case FRIDAY -> "ПТ";
            case SATURDAY -> "СБ";
            case SUNDAY -> "ВС";
        };
    }

    private enum ScheduleState {
        PAST,
        ACTIVE,
        UPCOMING
    }

    private static class ScheduleEvent implements Comparable<ScheduleEvent> {
        final String name;
        final LocalTime start;
        final LocalTime end;

        ScheduleEvent(String name, LocalTime start, LocalTime end) {
            this.name = name;
            this.start = start;
            this.end = end;
        }

        String toTimeString() {
            return start.format(TIME_FORMAT) + " - " + end.format(TIME_FORMAT);
        }

        ScheduleState state(LocalTime now) {
            if (!now.isBefore(start) && now.isBefore(end)) return ScheduleState.ACTIVE;
            return now.isBefore(start) ? ScheduleState.UPCOMING : ScheduleState.PAST;
        }

        @Override
        public int compareTo(ScheduleEvent other) {
            return this.start.compareTo(other.start);
        }
    }
}
package tech.onetap.module.list.render.hud;

import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import tech.onetap.event.list.EventHUD;
import tech.onetap.event.list.EventPacket;
import tech.onetap.event.list.EventTick;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.list.render.hud.renderers.HotbarRenderer;
import tech.onetap.module.list.render.hud.renderers.ArmorRenderer;
import tech.onetap.module.list.render.hud.renderers.ArrowsRenderer;
import tech.onetap.module.list.render.hud.renderers.CooldownsRenderer;
import tech.onetap.module.list.render.hud.renderers.EventsScheduleRenderer;
import tech.onetap.module.list.render.hud.renderers.InfoRenderer;
import tech.onetap.module.list.render.hud.renderers.InventoryRenderer;
import tech.onetap.module.list.render.hud.renderers.KeyBindsRenderer;
import tech.onetap.module.list.render.hud.renderers.LowHealthAlertRenderer;
import tech.onetap.module.list.render.hud.renderers.MediaPlayerRenderer;
import tech.onetap.module.list.render.hud.renderers.ModuleListRenderer;
import tech.onetap.module.list.render.hud.renderers.PotionsRenderer;
import tech.onetap.module.list.render.hud.renderers.ScoreboardRenderer;
import tech.onetap.module.list.render.hud.renderers.SpeedRenderer;
import tech.onetap.module.list.render.hud.renderers.StaffListRenderer;
import tech.onetap.module.list.render.hud.renderers.TotemCounterRenderer;
import tech.onetap.module.list.render.hud.renderers.WatermarkRenderer;
import tech.onetap.module.list.render.hud.renderers.TargetHudRenderer;
import tech.onetap.module.list.render.hud.renderers.Hud3Style;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeListSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.chat.SpekTracker;
import tech.onetap.util.base.Instance;
import tech.onetap.util.draggable.DragManager;
import tech.onetap.util.draggable.Draggable;
import tech.onetap.util.render.builders.Builder;
import tech.onetap.util.render.builders.states.QuadColorState;
import tech.onetap.util.render.builders.states.QuadRadiusState;
import tech.onetap.util.render.builders.states.SizeState;
import tech.onetap.util.render.helper.HoverUtil;
import tech.onetap.util.render.math.Animation;
import tech.onetap.util.render.math.Easing;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

import java.util.concurrent.CopyOnWriteArrayList;

@ModuleInformation(moduleName = "Interface", moduleCategory = ModuleCategory.RENDER)
public class Interface extends Module {
    private final ModeSetting hudStyle = new ModeSetting("Стиль HUD", "DLC", "DLC", "Nursultan", "Exp4.0", "Funtime");
    private final ModeSetting targetHudStyle = new ModeSetting("Стиль TargetHud", "DLC", "DLC", "Pharaon", "PharaonBETA", "Pharaon2", "Akrein", "Exp4.0", "CelkalOld", "Wex16.5", "Dima");
    private final ModeSetting targetHudColorMode = new ModeSetting("Цвет TargetHud", "От здоровья", "От здоровья", "От темы");
    private final BooleanSetting targetHudShowAbsorption = new BooleanSetting("Поглощение", true);
    private final BooleanSetting targetHudShowItems = new BooleanSetting("Предметы", true);
    private final SliderSetting targetHudHeadRound = new SliderSetting("Скругление головы", 2f, 0f, 8f, 0.5f);
    private final ModeListSetting elements = new ModeListSetting("Элементы",
            new BooleanSetting("Ватермарка", true),
            new BooleanSetting("Таймер-индикатор", true),
            new BooleanSetting("Активный таргет", true),
            new BooleanSetting("Координаты", false),
            new BooleanSetting("Привязанные модули", true),
            new BooleanSetting("Активные модераторы", true),
            new BooleanSetting("Бафы", true),
            new BooleanSetting("Скорость", true),
            new BooleanSetting("Счетчик тотемов", true),
            new BooleanSetting("Нотификации", true),
            new BooleanSetting("СпекТрекер", true),
            new BooleanSetting("MediaPlayer", true),
            new BooleanSetting("Хотбар", true),
            new BooleanSetting("Кулдауны", true),
            new BooleanSetting("Инфо", true),
            new BooleanSetting("Инвентарь", true),
            new BooleanSetting("Броня", true),
            new BooleanSetting("Список модулей", true),
new BooleanSetting("Расписание событий", true),
            new BooleanSetting("Стрелы", true),
            new BooleanSetting("Scoreboard", true)
    );
    private final SliderSetting backgroundIntensityTheme = new SliderSetting("Интенсивность темы", 0.15f, 0.05f, 1.0f, 0.01f);
    private final SliderSetting backgroundIntensityDef = new SliderSetting("Интенсивность основы", 0.15f, 0.05f, 1.0f, 0.01f);
    private final ModeListSetting blurBackgroundElements = new ModeListSetting("Блюр фона",
            new BooleanSetting("Ватермарка", true),
            new BooleanSetting("Активный таргет", true),
            new BooleanSetting("Привязанные модули", true),
            new BooleanSetting("Активные модераторы", true),
            new BooleanSetting("Бафы", true),
            new BooleanSetting("Счетчик тотемов", true),
            new BooleanSetting("Нотификации", true),
            new BooleanSetting("MediaPlayer", true),
            new BooleanSetting("Хотбар", true),
            new BooleanSetting("Оповещение о хп", true),
            new BooleanSetting("Броня", true),
            new BooleanSetting("Список модулей", true),
            new BooleanSetting("Расписание событий", true),
            new BooleanSetting("Стрелы", true)
    ).setVisible(() -> false);
    private final ModeListSetting themedBackgroundElements = new ModeListSetting("Задний фон от темы",
            new BooleanSetting("Ватермарка", false),
            new BooleanSetting("Активный таргет", false),
            new BooleanSetting("Привязанные модули", false),
            new BooleanSetting("Активные модераторы", false),
            new BooleanSetting("Бафы", false),
            new BooleanSetting("Счетчик тотемов", false),
            new BooleanSetting("Нотификации", false),
            new BooleanSetting("MediaPlayer", false),
            new BooleanSetting("Хотбар", false),
            new BooleanSetting("Оповещение о хп", false),
            new BooleanSetting("Броня", false),
            new BooleanSetting("Список модулей", false),
            new BooleanSetting("Расписание событий", false),
            new BooleanSetting("Стрелы", false),
            new BooleanSetting("Scoreboard", false)
    ).setVisible(() -> false);

    private final BooleanSetting nursultanShowLogin = new BooleanSetting("Nursultan Логин", true).setVisible(() -> false);
    private final BooleanSetting nursultanShowFps = new BooleanSetting("Nursultan FPS", true).setVisible(() -> false);
    private final BooleanSetting nursultanShowTime = new BooleanSetting("Nursultan Время", true).setVisible(() -> false);
    private final BooleanSetting nursultanShowCoords = new BooleanSetting("Nursultan Координаты", true).setVisible(() -> false);
    private final BooleanSetting nursultanShowPing = new BooleanSetting("Nursultan Пинг", true).setVisible(() -> false);
    private final BooleanSetting nursultanShowTps = new BooleanSetting("Nursultan TPS", true).setVisible(() -> false);
    private final BooleanSetting nursultanShowBps = new BooleanSetting("Nursultan BPS", true).setVisible(() -> false);
    private final BooleanSetting nursultanShowLogo = new BooleanSetting("Nursultan Лого", true).setVisible(() -> false);
    private final ModeSetting nursultanWatermarkMode = new ModeSetting("Nursultan Режим", "Полное", "Полное", "Компактное").setVisible(() -> false);
    private final BooleanSetting nursultanTransparentBackground = new BooleanSetting("Nursultan Прозрачный фон", false).setVisible(() -> false);

    private final SliderSetting lowHpAlertThreshold = new SliderSetting("Порог ХП оповещения", 8f, 1f, 20f, 0.5f);

    private static final SoundEvent SPEK_SOUND = SoundEvent.of(Identifier.of("mre", "spek"));

    @Getter
    private final Draggable watermarkDrag = DragManager.installDrag(this, "Watermark", 4, 4);
    @Getter
    private final Draggable keyBindsDrag = DragManager.installDrag(this, "HotKeys", 100, 50);
    @Getter
    private final Draggable staffListDrag = DragManager.installDrag(this, "StaffList", 200, 50);
    @Getter
    private final Draggable potionsDrag = DragManager.installDrag(this, "Potions", 300, 50);
    @Getter
    private final Draggable totemCounterDrag = DragManager.installDrag(this, "TotemCounter", 200, 200);
    @Getter
    private final Draggable mediaPlayerDrag = DragManager.installDrag(this, "MediaPlayer", 20, 60);
    @Getter
    private final Draggable cooldownsDrag = DragManager.installDrag(this, "CoolDowns", 10, 160);
    @Getter
    private final Draggable infoDrag = DragManager.installDrag(this, "Info", 10, 200);
    @Getter
    private final Draggable inventoryDrag = DragManager.installDrag(this, "Inventory", 10, 260);
    @Getter
    private final Draggable armorDrag = DragManager.installDrag(this, "Armor", 10, 320);
    @Getter
    private final Draggable moduleListDrag = DragManager.installDrag(this, "ModuleList", 200, 120);
    @Getter
    private final Draggable eventsScheduleDrag = DragManager.installDrag(this, "EventsSchedule", 300, 120);
    @Getter
    private final Draggable arrowsDrag = DragManager.installDrag(this, "Arrows", 200, 320);
    @Getter
    private final Draggable scoreboardDrag = DragManager.installDrag(this, "Scoreboard", 250, 250);
    private final Draggable targetHudDrag = DragManager.installDrag(this, "TargetHud", 10, 150);

    private final KeyBindsRenderer keyBindsRenderer = new KeyBindsRenderer(this);
    private final LowHealthAlertRenderer lowHealthAlertRenderer = new LowHealthAlertRenderer(this);
    private final TotemCounterRenderer totemCounterRenderer = new TotemCounterRenderer(this);
    private final WatermarkRenderer watermarkRenderer = new WatermarkRenderer(this);
    private final StaffListRenderer staffListRenderer = new StaffListRenderer(this);
    private final PotionsRenderer potionsRenderer = new PotionsRenderer(this);
    private final SpeedRenderer speedRenderer = new SpeedRenderer(this);
    private final MediaPlayerRenderer mediaPlayerRenderer = new MediaPlayerRenderer(this);
    private final HotbarRenderer hotbarRenderer = new HotbarRenderer(this);
    private final CooldownsRenderer cooldownsRenderer = new CooldownsRenderer(this);
    private final InfoRenderer infoRenderer = new InfoRenderer(this);
    private final InventoryRenderer inventoryRenderer = new InventoryRenderer(this);
    private final ArmorRenderer armorRenderer = new ArmorRenderer(this);
    private final ModuleListRenderer moduleListRenderer = new ModuleListRenderer(this);
    private final EventsScheduleRenderer eventsScheduleRenderer = new EventsScheduleRenderer(this);
    private final ArrowsRenderer arrowsRenderer = new ArrowsRenderer(this);
    private final ScoreboardRenderer scoreboardRenderer = new ScoreboardRenderer(this);
    private final TargetHudRenderer targetHudRenderer = new TargetHudRenderer(this);

    private String activeBackgroundElement;
    private boolean elementContextMenuOpen;
    private String elementContextMenuElement;
    private float elementContextMenuX;
    private float elementContextMenuY;
    private float elementContextMenuWidth;
    private float elementContextMenuHeight;
    private float elementContextMenuAnchorX = Float.NaN;
    private float elementContextMenuAnchorY = Float.NaN;
    private float elementContextMenuBlurProgress;
    private float elementContextMenuThemeProgress;
    private float elementContextMenuTargetModeProgress;
    private float notificationPreviewX;
    private float notificationPreviewY;
    private float notificationPreviewWidth;
    private float notificationPreviewHeight;
    private final Animation notificationPreviewAnimation = new Animation(Easing.BACK_OUT, 300);

    private static final float ELEMENT_CONTEXT_MENU_WIDTH = 94.0f;
    private static final float ELEMENT_CONTEXT_MENU_WIDTH_COMPACT = 88.0f;
    private static final float ELEMENT_CONTEXT_MENU_PADDING = 4.0f;
    private static final float ELEMENT_CONTEXT_MENU_ROW_HEIGHT = 10.0f;
    private static final float ELEMENT_CONTEXT_MENU_TOGGLE_WIDTH = 22.0f;
    private static final float ELEMENT_CONTEXT_MENU_TOGGLE_HEIGHT = 9.5f;
    private static final float ELEMENT_CONTEXT_MENU_TOGGLE_RADIUS = 3.2f;
    private static final float ELEMENT_CONTEXT_MENU_TOGGLE_KNOB_PADDING = 0.55f;
    private static final float ELEMENT_CONTEXT_MENU_TOGGLE_KNOB_REDUCTION = 0.5f;
    private static final float ELEMENT_CONTEXT_MENU_TOGGLE_KNOB_RADIUS_DIVISOR = 2.5f;
    private static final float ELEMENT_CONTEXT_MENU_MODE_TOP = 1.5f;
    private static final float ELEMENT_CONTEXT_MENU_MODE_HEIGHT = 11.5f;
    private static final float ELEMENT_CONTEXT_MENU_MODE_GAP = 3.0f;
    private static final float ELEMENT_CONTEXT_MENU_MODE_BOTTOM = 0.5f;

    public void drawBackground(float x, float y, float w, float h, float radius, int alpha) {
        boolean useBlur = isBackgroundOptionEnabled(blurBackgroundElements, true);
        boolean useThemeBackground = isBackgroundOptionEnabled(themedBackgroundElements, false);

        if (isAxiomStyle()) {
            drawAxiomBackground(x, y, w, h, radius, alpha, useBlur, useThemeBackground);
            return;
        }

        if (useBlur) {
            DrawUtil.drawRoundBlur(x, y, w, h, radius - 0.5f, ColorProvider.rgba(255, 255, 255, alpha), 45);
        }
        int baseColor = ColorProvider.rgba(25, 25, 25, (int) (alpha * backgroundIntensityDef.getFloatValue()));
        int themedBaseColor = ColorProvider.rgba(0, 0, 0, (int) (alpha * backgroundIntensityDef.getFloatValue()));

        if (useThemeBackground) {
            DrawUtil.drawRound(x, y, w, h, radius - 0.5f, themedBaseColor);

            DrawUtil.drawRound(x, y, w, h, radius - 0.5f, getThemeTint(alpha));
        }
        else{
            DrawUtil.drawRound(x, y, w, h, radius - 0.5f, baseColor);
        }
    }

    private void drawAxiomBackground(float x, float y, float w, float h, float radius, int alpha, boolean useBlur, boolean useThemeBackground) {
        float safeWidth = Math.max(1.0f, w);
        float safeHeight = Math.max(1.0f, h);
        float safeRadius = Math.max(0.5f, radius);

        int themeColor = ColorProvider.getThemeColor();
        int themeColorTwo = ColorProvider.getThemeColorTwo();
        float themeIntensity = backgroundIntensityTheme.getFloatValue();
        float baseIntensity = backgroundIntensityDef.getFloatValue();

        if (useBlur) {
            int blurAlphaPrimary = Math.min(140, Math.max(24, (int) (alpha * (0.18f + themeIntensity * 0.24f))));
            int blurAlphaSecondary = Math.min(120, Math.max(18, (int) (alpha * (0.12f + themeIntensity * 0.2f))));
            DrawUtil.drawRoundBlur(
                    x - 0.8f,
                    y - 0.8f,
                    safeWidth + 1.6f,
                    safeHeight + 1.6f,
                    safeRadius + 0.8f,
                    ColorProvider.setAlpha(themeColor, blurAlphaPrimary),
                    ColorProvider.setAlpha(themeColorTwo, blurAlphaSecondary),
                    24.0f
            );
        }

        int outlineTop = ColorProvider.rgba(255, 255, 255, Math.max(12, (int) (alpha * 0.12f)));
        int outlineBottomLeft = ColorProvider.setAlpha(themeColor, Math.max(10, (int) (alpha * (0.06f + themeIntensity * 0.12f))));
        int outlineBottomRight = ColorProvider.setAlpha(themeColorTwo, Math.max(10, (int) (alpha * (0.06f + themeIntensity * 0.12f))));
        DrawUtil.drawRound(
                x - 0.6f,
                y - 0.6f,
                safeWidth + 1.2f,
                safeHeight + 1.2f,
                safeRadius + 0.5f,
                outlineTop,
                outlineTop,
                outlineBottomRight,
                outlineBottomLeft
        );

        int baseAlpha = Math.max(32, (int) (alpha * (0.5f + baseIntensity * 0.22f)));
        DrawUtil.drawRound(
                x,
                y,
                safeWidth,
                safeHeight,
                safeRadius,
                ColorProvider.rgba(9, 12, 18, baseAlpha),
                ColorProvider.rgba(11, 15, 22, baseAlpha),
                ColorProvider.rgba(5, 7, 11, baseAlpha),
                ColorProvider.rgba(8, 10, 15, baseAlpha)
        );

        float innerX = x + 1.0f;
        float innerY = y + 1.0f;
        float innerWidth = Math.max(1.0f, safeWidth - 2.0f);
        float innerHeight = Math.max(1.0f, safeHeight - 2.0f);
        float innerRadius = Math.max(0.8f, safeRadius - 0.9f);
        int innerAlpha = Math.max(14, (int) (alpha * (0.08f + baseIntensity * 0.08f)));
        DrawUtil.drawRound(
                innerX,
                innerY,
                innerWidth,
                innerHeight,
                innerRadius,
                ColorProvider.rgba(25, 31, 44, innerAlpha),
                ColorProvider.rgba(22, 28, 40, innerAlpha),
                ColorProvider.rgba(5, 7, 11, innerAlpha),
                ColorProvider.rgba(13, 16, 24, innerAlpha)
        );

        if (useThemeBackground) {
            int tintAlpha = Math.max(10, (int) (alpha * (0.05f + themeIntensity * 0.18f)));
            DrawUtil.drawRound(
                    innerX,
                    innerY,
                    innerWidth,
                    innerHeight,
                    innerRadius,
                    ColorProvider.setAlpha(themeColor, tintAlpha),
                    ColorProvider.setAlpha(themeColorTwo, tintAlpha),
                    ColorProvider.setAlpha(themeColorTwo, Math.max(6, tintAlpha / 2)),
                    ColorProvider.setAlpha(themeColor, Math.max(6, tintAlpha / 2))
            );
        }

        if (safeWidth > 10.0f && safeHeight > 5.0f) {
            float accentWidth = Math.max(8.0f, safeWidth - 8.0f);
            float accentHeight = Math.min(2.2f, Math.max(1.0f, safeHeight * 0.11f));
            DrawUtil.drawRound(
                    x + 4.0f,
                    y + 2.3f,
                    accentWidth,
                    accentHeight,
                    accentHeight / 2.0f,
                    ColorProvider.setAlpha(themeColor, Math.max(16, (int) (alpha * (0.1f + themeIntensity * 0.16f)))),
                    ColorProvider.setAlpha(themeColorTwo, Math.max(14, (int) (alpha * (0.08f + themeIntensity * 0.14f))))
            );
        }

        if (safeWidth > 8.0f && safeHeight > 4.0f) {
            DrawUtil.drawRound(
                    x + 2.0f,
                    y + safeHeight - 2.0f,
                    Math.max(4.0f, safeWidth - 4.0f),
                    0.9f,
                    0.45f,
                    ColorProvider.rgba(255, 255, 255, Math.max(5, (int) (alpha * 0.05f)))
            );
        }
    }

    private boolean isBackgroundOptionEnabled(ModeListSetting setting, boolean defaultValue) {
        if (activeBackgroundElement == null) {
            return defaultValue;
        }
        BooleanSetting option = setting.getOption(activeBackgroundElement);
        return option != null ? option.getValue() : defaultValue;
    }

    private void withBackgroundElement(String elementName, Runnable renderTask) {
        String previousElement = activeBackgroundElement;
        activeBackgroundElement = elementName;
        try {
            renderTask.run();
        } finally {
            activeBackgroundElement = previousElement;
        }
    }

    @Subscribe
    public void onEventHUD(EventHUD e) {
        if (mc.player == null || mc.options.hudHidden || mc.getDebugHud().shouldShowDebugHud()) return;

        DrawContext context = e.getDrawContext();

        if (elements.isEnabled("Нотификации")) {
            withBackgroundElement("Нотификации", () -> {
                NotificationManager.render(context);
                renderNotificationPreview(context);
            });
        } else {
            resetNotificationPreviewBounds();
        }
        withBackgroundElement("Оповещение о хп", () -> lowHealthAlertRenderer.render(context));
        if (elements.isEnabled("Счетчик тотемов")) {
            withBackgroundElement("Счетчик тотемов", () -> totemCounterRenderer.render(context));
        }
        if (elements.isEnabled("Ватермарка")) {
            withBackgroundElement("Ватермарка", () -> watermarkRenderer.render(context));
        }
        if (elements.isEnabled("Координаты")) renderCoordsInfo(context);
        if (elements.isEnabled("Активный таргет")) {
            try {
                withBackgroundElement("Активный таргет", () -> targetHudRenderer.render(context));
            } catch (Throwable ignored) {
            }
        }
        if (elements.isEnabled("Привязанные модули")) {
            withBackgroundElement("Привязанные модули", () -> keyBindsRenderer.render(context));
        }
        if (elements.isEnabled("Активные модераторы")) {
            withBackgroundElement("Активные модераторы", () -> staffListRenderer.render(context));
        }
        if (elements.isEnabled("Бафы")) {
            withBackgroundElement("Бафы", () -> potionsRenderer.render(context));
        }
        if (elements.isEnabled("Скорость")) speedRenderer.render(context);
        if (elements.isEnabled("MediaPlayer")) {
            try {
                withBackgroundElement("MediaPlayer", () -> mediaPlayerRenderer.render(context));
            } catch (Throwable ignored) { }
        }
        if (elements.isEnabled("Кулдауны")) {
            withBackgroundElement("Кулдауны", () -> cooldownsRenderer.render(context));
        }
        if (elements.isEnabled("Инфо")) {
            withBackgroundElement("Инфо", () -> infoRenderer.render(context));
        }
        if (elements.isEnabled("Инвентарь")) {
            withBackgroundElement("Инвентарь", () -> inventoryRenderer.render(context));
        }
        if (elements.isEnabled("Броня")) {
            withBackgroundElement("Броня", () -> armorRenderer.render(context));
        }
        if (elements.isEnabled("Список модулей")) {
            withBackgroundElement("Список модулей", () -> moduleListRenderer.render(context));
        }
        if (elements.isEnabled("Расписание событий")) {
            withBackgroundElement("Расписание событий", () -> eventsScheduleRenderer.render(context));
        }
        if (elements.isEnabled("Стрелы")) {
            withBackgroundElement("Стрелы", () -> arrowsRenderer.render(context));
        }
        if (elements.isEnabled("Scoreboard")) {
            withBackgroundElement("Scoreboard", () -> scoreboardRenderer.render(context));
        }
        if (isHotbarEnabled()) {
            withBackgroundElement("Хотбар", () -> hotbarRenderer.render(context));
        }

        renderElementContextMenu(context);
    }

    @Subscribe
    private void onUpdate(EventTick e) {
        if (mc.player == null || mc.world == null) return;

        if (elements.isEnabled("СпекТрекер")) {
            SpekTracker.tick(mc);
        }

        if (elements.isEnabled("Активные модераторы")) staffListRenderer.update();
        if (elements.isEnabled("Бафы")) potionsRenderer.updatePotions();
    }

    @Subscribe
    private void onPacket(EventPacket e) {
        if (!elements.isEnabled("СпекТрекер") || mc.player == null) return;
        if (e.getPacket() instanceof GameMessageS2CPacket packet) {
            String rawContent = packet.content().getString();
            String triggerKeyword = SpekTracker.findTriggerKeyword(rawContent);
            String trackedSuspect = SpekTracker.findMatchingSuspect(rawContent);
            String suffix = triggerKeyword != null ? " [" + triggerKeyword + "]" : "";
            if (trackedSuspect != null) trackedSuspect += suffix;
            if (trackedSuspect != null) {
                NotificationManager.postWarning("Просят наблюдение: " + trackedSuspect);
                playSpekSound();
                return;
            }
            if (SpekTracker.isOwnCallout(rawContent)) {
                NotificationManager.postWarning("Пишут про тебя: " + mc.player.getName().getString());
                playSpekSound();
            }
        }
    }

    private void playSpekSound() {
        if (mc.getSoundManager() != null) {
            mc.getSoundManager().play(PositionedSoundInstance.master(SPEK_SOUND, 1.0f));
        }
    }

    private void renderCoordsInfo(DrawContext context) {
        if (mc.player == null) return;

        int xCoord = (int) Math.floor(mc.player.getX());
        int yCoord = (int) Math.floor(mc.player.getY());
        int zCoord = (int) Math.floor(mc.player.getZ());
        String coordsText = "X: " + xCoord + " Y: " + yCoord + " Z: " + zCoord;

        float x = 4f;
        float height = isAxiomStyle() ? 16.5f : 20f;
        float y = mc.getWindow().getScaledHeight() - height - 4f;

        if (isAxiomStyle()) {
            float accentWidth = 18.0f;
            float textX = x + 4.0f + accentWidth + 6.0f;
            float textSize = 7.05f;
            float width = textX - x + Fonts.SFSEMIBOLD.get().getWidth(coordsText, textSize) + 7.0f;

            drawBackground(x, y, width, height, 5.0f, 235);
            drawAxiomAccent(x + 4.0f, y + 3.0f, accentWidth, height - 6.0f, 3.5f, 255);
            DrawUtil.drawText(Fonts.RUBIK.get(), "XYZ", x + 6.25f, y + 3.8f, getAxiomPrimaryTextColor(255), 7.0f);
            DrawUtil.drawText(Fonts.SFSEMIBOLD.get(), coordsText, textX, y + 4.05f, getAxiomPrimaryTextColor(255), textSize);
            return;
        }

        if (hudStyle.is("Pharaon")) {
            float iconSize = 10.5f;
            float textSize = 8.6f;
            float iconBackgroundX = x + 4f;
            float iconBackgroundY = y + 4f;
            float iconBackgroundSize = 12f;
            float iconX = iconBackgroundX + 2.25f;
            float textX = iconBackgroundX + iconBackgroundSize + 3f;
            float width = textX - x + Fonts.SFBOLD.get().getWidth(coordsText, textSize) + 7f;

            DrawUtil.drawRound(x, y, width, height, 3f, 2f, ColorProvider.rgba(13, 16, 23, 255));
            DrawUtil.drawRound(iconBackgroundX, iconBackgroundY, iconBackgroundSize, iconBackgroundSize, 2.5f,
                    ColorProvider.setAlpha(ColorProvider.getThemeColor(), 255));
            DrawUtil.drawText(Fonts.MOONWARD_ICONS.get(), "\ue912", iconX, y + 5.65f, ColorProvider.rgba(222, 222, 222, 255), iconSize);
            DrawUtil.drawText(Fonts.SFBOLD.get(), coordsText, textX, y + 6.1f, ColorProvider.rgba(200, 200, 200, 255), textSize);
        } else {
            float textSize = 7f;
            float width = Fonts.SFMEDIUM.get().getWidth(coordsText, textSize) + 16f;
            int accent = ColorProvider.getThemeColor();
            DrawUtil.drawRound(x, y, width, height, 3f, ColorProvider.rgba(13, 16, 23, 255));
            DrawUtil.drawRound(x, y, 2.5f, height, 3f, ColorProvider.setAlpha(accent, 200));
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), coordsText, x + 8f, y + 6f,
                    ColorProvider.rgba(200, 200, 200, 255), textSize);
        }
    }

    public int getThemeTint(int alpha) {
        int themeColor = ColorProvider.getThemeColor();
        return ColorProvider.setAlpha(themeColor, (int) (100 * (alpha / 255f) * backgroundIntensityTheme.getFloatValue()));
    }

    public boolean isAxiomStyle() {
        return hudStyle.is("Axiom");
    }

    public int getAxiomPrimaryTextColor(int alpha) {
        return ColorProvider.rgba(236, 242, 248, alpha);
    }

    public int getAxiomSecondaryTextColor(int alpha) {
        return ColorProvider.rgba(154, 166, 184, alpha);
    }

    public void drawExp4Border(float x, float y, float w, float h, float radius, int alpha) {
        Builder.border()
                .size(new SizeState(w + 0.5f, h + 0.25f))
                .radius(new QuadRadiusState(radius))
                .color(new QuadColorState(ColorProvider.rgba(45, 45, 45, alpha)))
                .thickness(0.35f)
                .smoothness(0.5f, 1f)
                .build()
                .render(x, y);
    }

    public void drawAxiomAccent(float x, float y, float w, float h, float radius, int alpha) {
        float safeWidth = Math.max(1.0f, w);
        float safeHeight = Math.max(1.0f, h);
        float safeRadius = Math.max(0.5f, radius);

        int themeColor = ColorProvider.getThemeColor();
        int themeColorTwo = ColorProvider.getThemeColorTwo();
        int topLeft = ColorProvider.setAlpha(themeColor, Math.min(255, Math.max(24, (int) (alpha * 0.95f))));
        int topRight = ColorProvider.setAlpha(themeColorTwo, Math.min(255, Math.max(24, (int) (alpha * 0.88f))));
        int bottomRight = ColorProvider.setAlpha(
                ColorProvider.interpolateColor(themeColorTwo, ColorProvider.rgba(255, 255, 255, 255), 0.18f),
                Math.min(255, Math.max(20, (int) (alpha * 0.68f)))
        );
        int bottomLeft = ColorProvider.setAlpha(
                ColorProvider.interpolateColor(themeColor, ColorProvider.rgba(255, 255, 255, 255), 0.12f),
                Math.min(255, Math.max(20, (int) (alpha * 0.72f)))
        );

        DrawUtil.drawRound(x, y, safeWidth, safeHeight, safeRadius, topLeft, topRight, bottomRight, bottomLeft);
        if (safeWidth > 3.0f && safeHeight > 3.0f) {
            DrawUtil.drawRound(
                    x + 1.0f,
                    y + 1.0f,
                    Math.max(1.0f, safeWidth - 2.0f),
                    Math.min(1.05f, safeHeight - 2.0f),
                    0.55f,
                    ColorProvider.rgba(255, 255, 255, Math.max(10, (int) (alpha * 0.18f)))
            );
        }
    }

    public boolean handleChatOverlayClick(double mouseX, double mouseY, int button) {
        if (!isEnabled()) {
            return false;
        }
        if (watermarkRenderer.handleChatClick(mouseX, mouseY, button)) {
            if (button == 1 && isDraggableHovered(elements.isEnabled("Ватермарка"), watermarkDrag, mouseX, mouseY)) {
                closeElementContextMenu();
            }
            return true;
        }
        return handleElementContextMenuClick(mouseX, mouseY, button);
    }

    public void onChatClosed() {
        watermarkRenderer.onChatClosed();
        closeElementContextMenu();
        resetNotificationPreviewBounds();
    }

    public BooleanSetting getBlurBackgroundOption(String elementName) {
        return blurBackgroundElements.getOption(elementName);
    }

    public BooleanSetting getThemeBackgroundOption(String elementName) {
        return themedBackgroundElements.getOption(elementName);
    }

    private boolean handleElementContextMenuClick(double mouseX, double mouseY, int button) {
        if (!(mc.currentScreen instanceof ChatScreen)) {
            closeElementContextMenu();
            return false;
        }

        String hoveredElement = getHoveredContextElement(mouseX, mouseY);
        if (button == 1 && hoveredElement != null) {
            if (elementContextMenuOpen && hoveredElement.equals(elementContextMenuElement)) {
                closeElementContextMenu();
            } else {
                openElementContextMenu(hoveredElement, (float) mouseX, (float) mouseY);
            }
            return true;
        }

        if (!elementContextMenuOpen) {
            return false;
        }

        updateElementContextMenuBounds();
        if (button == 1) {
            return HoverUtil.isHovered(mouseX, mouseY, elementContextMenuX, elementContextMenuY, elementContextMenuWidth, elementContextMenuHeight);
        }

        if (button != 0 || !HoverUtil.isHovered(mouseX, mouseY, elementContextMenuX, elementContextMenuY, elementContextMenuWidth, elementContextMenuHeight)) {
            return false;
        }

        float toggleX = elementContextMenuX + elementContextMenuWidth - ELEMENT_CONTEXT_MENU_PADDING - ELEMENT_CONTEXT_MENU_TOGGLE_WIDTH;

        BooleanSetting blurOption = getBlurBackgroundOption(elementContextMenuElement);
        float blurRowY = elementContextMenuY + ELEMENT_CONTEXT_MENU_PADDING;
        float blurToggleY = blurRowY + (ELEMENT_CONTEXT_MENU_ROW_HEIGHT - ELEMENT_CONTEXT_MENU_TOGGLE_HEIGHT) / 2.0f;
        if (blurOption != null && (
                HoverUtil.isHovered(mouseX, mouseY, elementContextMenuX + ELEMENT_CONTEXT_MENU_PADDING, blurRowY, elementContextMenuWidth - ELEMENT_CONTEXT_MENU_PADDING * 2.0f, ELEMENT_CONTEXT_MENU_ROW_HEIGHT) ||
                        HoverUtil.isHovered(mouseX, mouseY, toggleX, blurToggleY, ELEMENT_CONTEXT_MENU_TOGGLE_WIDTH, ELEMENT_CONTEXT_MENU_TOGGLE_HEIGHT))) {
            blurOption.toggle();
            return true;
        }

        BooleanSetting themeOption = getThemeBackgroundOption(elementContextMenuElement);
        float themeRowY = blurRowY + ELEMENT_CONTEXT_MENU_ROW_HEIGHT;
        float themeToggleY = themeRowY + (ELEMENT_CONTEXT_MENU_ROW_HEIGHT - ELEMENT_CONTEXT_MENU_TOGGLE_HEIGHT) / 2.0f;
        if (themeOption != null && (
                HoverUtil.isHovered(mouseX, mouseY, elementContextMenuX + ELEMENT_CONTEXT_MENU_PADDING, themeRowY, elementContextMenuWidth - ELEMENT_CONTEXT_MENU_PADDING * 2.0f, ELEMENT_CONTEXT_MENU_ROW_HEIGHT) ||
                        HoverUtil.isHovered(mouseX, mouseY, toggleX, themeToggleY, ELEMENT_CONTEXT_MENU_TOGGLE_WIDTH, ELEMENT_CONTEXT_MENU_TOGGLE_HEIGHT))) {
            themeOption.toggle();
            return true;
        }

        if ("Активный таргет".equals(elementContextMenuElement)) {
            float modeY = themeRowY + ELEMENT_CONTEXT_MENU_ROW_HEIGHT + ELEMENT_CONTEXT_MENU_MODE_TOP;
            float modeHeight = ELEMENT_CONTEXT_MENU_MODE_HEIGHT;
            float modeButtonWidth = (elementContextMenuWidth - ELEMENT_CONTEXT_MENU_PADDING * 2.0f - ELEMENT_CONTEXT_MENU_MODE_GAP) / 2.0f;
            float healthModeX = elementContextMenuX + ELEMENT_CONTEXT_MENU_PADDING;
            float themeModeX = healthModeX + modeButtonWidth + ELEMENT_CONTEXT_MENU_MODE_GAP;
            if (HoverUtil.isHovered(mouseX, mouseY, healthModeX, modeY, modeButtonWidth, modeHeight)) {
                targetHudColorMode.setValue("От здоровья");
                return true;
            }
            if (HoverUtil.isHovered(mouseX, mouseY, themeModeX, modeY, modeButtonWidth, modeHeight)) {
                targetHudColorMode.setValue("От темы");
                return true;
            }
        }

        return true;
    }

    private void renderElementContextMenu(DrawContext context) {
        if (!elementContextMenuOpen) {
            return;
        }
        if (!(mc.currentScreen instanceof ChatScreen) || !isEnabled() || elementContextMenuElement == null || !elements.isEnabled(elementContextMenuElement)) {
            closeElementContextMenu();
            return;
        }

        updateElementContextMenuBounds();

        int textColor = ColorProvider.rgba(225, 225, 225, 255);
        int rowTextColorOn = ColorProvider.rgba(210, 210, 210, 255);
        int rowTextColorOff = ColorProvider.rgba(210, 210, 210, 195);
        int themeColor = ColorProvider.getThemeColor();

        withBackgroundElement(elementContextMenuElement, () ->
                drawBackground(elementContextMenuX, elementContextMenuY, elementContextMenuWidth, elementContextMenuHeight, 3.2f, 210)
        );

        float toggleX = elementContextMenuX + elementContextMenuWidth - ELEMENT_CONTEXT_MENU_PADDING - ELEMENT_CONTEXT_MENU_TOGGLE_WIDTH;

        BooleanSetting blurOption = getBlurBackgroundOption(elementContextMenuElement);
        float blurRowY = elementContextMenuY + ELEMENT_CONTEXT_MENU_PADDING;
        int blurTextColor = blurOption != null && blurOption.getValue() ? rowTextColorOn : rowTextColorOff;
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), "Блюр фона", elementContextMenuX + ELEMENT_CONTEXT_MENU_PADDING, blurRowY + 2.6f, blurTextColor, 6.7f);
        if (blurOption != null) {
            elementContextMenuBlurProgress = animateMenuProgress(elementContextMenuBlurProgress, blurOption.getValue());
            float blurToggleY = blurRowY + (ELEMENT_CONTEXT_MENU_ROW_HEIGHT - ELEMENT_CONTEXT_MENU_TOGGLE_HEIGHT) / 2.0f;
            drawMenuToggle(toggleX, blurToggleY, ELEMENT_CONTEXT_MENU_TOGGLE_WIDTH, ELEMENT_CONTEXT_MENU_TOGGLE_HEIGHT, elementContextMenuBlurProgress, themeColor);
        }

        BooleanSetting themeOption = getThemeBackgroundOption(elementContextMenuElement);
        float themeRowY = blurRowY + ELEMENT_CONTEXT_MENU_ROW_HEIGHT;
        int themeTextColor = themeOption != null && themeOption.getValue() ? rowTextColorOn : rowTextColorOff;
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), "Фон от темы", elementContextMenuX + ELEMENT_CONTEXT_MENU_PADDING, themeRowY + 2.6f, themeTextColor, 6.7f);
        if (themeOption != null) {
            elementContextMenuThemeProgress = animateMenuProgress(elementContextMenuThemeProgress, themeOption.getValue());
            float themeToggleY = themeRowY + (ELEMENT_CONTEXT_MENU_ROW_HEIGHT - ELEMENT_CONTEXT_MENU_TOGGLE_HEIGHT) / 2.0f;
            drawMenuToggle(toggleX, themeToggleY, ELEMENT_CONTEXT_MENU_TOGGLE_WIDTH, ELEMENT_CONTEXT_MENU_TOGGLE_HEIGHT, elementContextMenuThemeProgress, themeColor);
        }

        if ("Активный таргет".equals(elementContextMenuElement)) {
            float modeY = themeRowY + ELEMENT_CONTEXT_MENU_ROW_HEIGHT + ELEMENT_CONTEXT_MENU_MODE_TOP;
            float modeHeight = ELEMENT_CONTEXT_MENU_MODE_HEIGHT;
            float modeButtonWidth = (elementContextMenuWidth - ELEMENT_CONTEXT_MENU_PADDING * 2.0f - ELEMENT_CONTEXT_MENU_MODE_GAP) / 2.0f;
            float healthModeX = elementContextMenuX + ELEMENT_CONTEXT_MENU_PADDING;
            float themeModeX = healthModeX + modeButtonWidth + ELEMENT_CONTEXT_MENU_MODE_GAP;
            boolean healthActive = targetHudColorMode.is("От здоровья");
            boolean themeActive = targetHudColorMode.is("От темы");

            int activeButtonColor = ColorProvider.setAlpha(themeColor, 95);
            withBackgroundElement(elementContextMenuElement, () -> {
                drawBackground(healthModeX, modeY, modeButtonWidth, modeHeight, 3.0f, 190);
                drawBackground(themeModeX, modeY, modeButtonWidth, modeHeight, 3.0f, 190);
            });
            elementContextMenuTargetModeProgress = animateMenuProgress(elementContextMenuTargetModeProgress, themeActive);
            float modeHighlightX = healthModeX + (themeModeX - healthModeX) * elementContextMenuTargetModeProgress;
            DrawUtil.drawRound(modeHighlightX, modeY, modeButtonWidth, modeHeight, 3.0f, activeButtonColor);

            String healthText = "От хп";
            String themeText = "От темы";
            float modeTextY = modeY + (modeHeight - 6.7f) / 2.0f - 0.3f;
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), healthText, healthModeX + (modeButtonWidth - Fonts.SFMEDIUM.get().getWidth(healthText, 6.7f)) / 2.0f, modeTextY, textColor, 6.7f);
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), themeText, themeModeX + (modeButtonWidth - Fonts.SFMEDIUM.get().getWidth(themeText, 6.7f)) / 2.0f, modeTextY, textColor, 6.7f);
        }
    }

    private float animateMenuProgress(float currentValue, boolean enabled) {
        float targetValue = enabled ? 1.0f : 0.0f;
        float nextValue = currentValue + (targetValue - currentValue) * 0.24f;
        if (Math.abs(targetValue - nextValue) < 0.001f) {
            nextValue = targetValue;
        }
        return nextValue;
    }

    private void drawMenuToggle(float x, float y, float width, float height, float progress, int themeColor) {
        float clampedProgress = Math.max(0.0f, Math.min(1.0f, progress));
        int offColor = ColorProvider.rgba(58, 62, 78, 210);
        int onColor = ColorProvider.setAlpha(themeColor, 165);
        int bgColor = ColorProvider.interpolateColor(offColor, onColor, clampedProgress);
        DrawUtil.drawRound(x, y, width, height, ELEMENT_CONTEXT_MENU_TOGGLE_RADIUS, bgColor);

        float knobSize = Math.max(
                1.0f,
                height - (ELEMENT_CONTEXT_MENU_TOGGLE_KNOB_PADDING * 2.0f) - ELEMENT_CONTEXT_MENU_TOGGLE_KNOB_REDUCTION
        );
        float minKnobX = x + ELEMENT_CONTEXT_MENU_TOGGLE_KNOB_PADDING;
        float maxKnobX = x + width - knobSize - ELEMENT_CONTEXT_MENU_TOGGLE_KNOB_PADDING;
        float knobX = minKnobX + (maxKnobX - minKnobX) * clampedProgress;
        float knobY = y + (height - knobSize) / 2.0f;
        int knobOffColor = ColorProvider.rgba(206, 206, 206, 255);
        int knobOnColor = ColorProvider.rgba(232, 232, 232, 255);
        int knobColor = ColorProvider.interpolateColor(knobOffColor, knobOnColor, clampedProgress);
        DrawUtil.drawRound(knobX, knobY, knobSize, knobSize, knobSize / ELEMENT_CONTEXT_MENU_TOGGLE_KNOB_RADIUS_DIVISOR, knobColor);
    }

    private String getHoveredContextElement(double mouseX, double mouseY) {
        if (isNotificationPreviewHovered(mouseX, mouseY)) return "Нотификации";
        if (isDraggableHovered(elements.isEnabled("Активный таргет"), targetHudDrag, mouseX, mouseY)) return "Активный таргет";
        if (isDraggableHovered(elements.isEnabled("Привязанные модули"), keyBindsDrag, mouseX, mouseY)) return "Привязанные модули";
        if (isDraggableHovered(elements.isEnabled("Активные модераторы"), staffListDrag, mouseX, mouseY)) return "Активные модераторы";
        if (isDraggableHovered(elements.isEnabled("Бафы"), potionsDrag, mouseX, mouseY)) return "Бафы";
        if (isDraggableHovered(elements.isEnabled("Счетчик тотемов"), totemCounterDrag, mouseX, mouseY)) return "Счетчик тотемов";
        if (isDraggableHovered(elements.isEnabled("MediaPlayer"), mediaPlayerDrag, mouseX, mouseY)) return "MediaPlayer";
        if (isDraggableHovered(elements.isEnabled("Кулдауны"), cooldownsDrag, mouseX, mouseY)) return "Кулдауны";
        if (isDraggableHovered(elements.isEnabled("Инфо"), infoDrag, mouseX, mouseY)) return "Инфо";
        if (isDraggableHovered(elements.isEnabled("Инвентарь"), inventoryDrag, mouseX, mouseY)) return "Инвентарь";
        if (isDraggableHovered(elements.isEnabled("Броня"), armorDrag, mouseX, mouseY)) return "Броня";
        if (isDraggableHovered(elements.isEnabled("Список модулей"), moduleListDrag, mouseX, mouseY)) return "Список модулей";
        if (isDraggableHovered(elements.isEnabled("Расписание событий"), eventsScheduleDrag, mouseX, mouseY)) return "Расписание событий";
        if (isDraggableHovered(elements.isEnabled("Стрелы"), arrowsDrag, mouseX, mouseY)) return "Стрелы";
        if (isDraggableHovered(elements.isEnabled("Scoreboard"), scoreboardDrag, mouseX, mouseY)) return "Scoreboard";
        return null;
    }

    private boolean isDraggableHovered(boolean enabled, Draggable drag, double mouseX, double mouseY) {
        return enabled && drag.getWidth() > 1.0f && drag.getHeight() > 1.0f &&
                HoverUtil.isHovered(mouseX, mouseY, drag.getX(), drag.getY(), drag.getWidth(), drag.getHeight());
    }

    private boolean isNotificationPreviewHovered(double mouseX, double mouseY) {
        return elements.isEnabled("Нотификации")
                && notificationPreviewWidth > 1.0f
                && notificationPreviewHeight > 1.0f
                && HoverUtil.isHovered(mouseX, mouseY, notificationPreviewX, notificationPreviewY, notificationPreviewWidth, notificationPreviewHeight);
    }

    private void renderNotificationPreview(DrawContext context) {
        boolean shouldShow = mc.currentScreen instanceof ChatScreen && !NotificationManager.hasActiveNotifications();
        float scale = Math.max(0.0f, Math.min(1.0f, notificationPreviewAnimation.run(shouldShow ? 1.0f : 0.0f)));
        if (scale <= 0.01f) {
            resetNotificationPreviewBounds();
            return;
        }

        String previewText = "Пример уведомления";
        float centerX = mc.getWindow().getScaledWidth() / 2.0f;
        float y = mc.getWindow().getScaledHeight() / 2.0f + 20.0f;
        boolean moonwardStyle = getHudStyleSetting().is("Pharaon");

        float width;
        float height;
        float x;

        if (moonwardStyle) {
            float textSize = 8.6f;
            height = 20.0f;
            width = 20.0f + Fonts.SFBOLD.get().getWidth(previewText, textSize) + 7.0f;
            x = centerX - width / 2.0f;

            context.getMatrices().push();
            context.getMatrices().translate(centerX, y + height / 2.0f, 0.0f);
            context.getMatrices().scale(scale, scale, 1.0f);
            context.getMatrices().translate(-centerX, -(y + height / 2.0f), 0.0f);

            int alphaInt = (int) (255 * scale);
            DrawUtil.drawRoundBlur(x, y, width, height, 3.0f, ColorProvider.rgba(155, 155, 155, alphaInt), 45.0f);
            DrawUtil.drawRound(x + 3.5f, y + 4.0f, 12.5f, 12.0f, 2.5f, ColorProvider.rgba(55, 165, 55, alphaInt));
            DrawUtil.drawText(Fonts.MOONWARD_ICONS.get(), "\ue90c", x + 5.0f, y + 5.65f, ColorProvider.rgba(222, 222, 222, alphaInt), 10.5f);
            DrawUtil.drawText(Fonts.SFBOLD.get(), previewText, x + 19.0f, y + 5.5f, ColorProvider.rgba(200, 200, 200, alphaInt), textSize);
            context.getMatrices().pop();
        } else if (getHudStyleSetting().is("Exp4.0")) {
            float textSize = 6.0f;
            float iconSize = 15.0f;
            height = 16.0f;
            width = iconSize + Fonts.SFBOLD.get().getWidth(previewText, textSize) + 6.0f;
            x = centerX - width / 2.0f;

            context.getMatrices().push();
            context.getMatrices().translate(centerX, y + height / 2.0f, 0.0f);
            context.getMatrices().scale(scale, scale, 1.0f);
            context.getMatrices().translate(-centerX, -(y + height / 2.0f), 0.0f);

            int alphaInt = (int) (255 * scale);
            int bgAlpha = (int) (230 * scale);
            DrawUtil.drawRound(x, y, width, height, 4.0f,
                    ColorProvider.rgba(20, 20, 20, bgAlpha),
                    ColorProvider.rgba(15, 15, 15, bgAlpha),
                    ColorProvider.rgba(20, 20, 20, bgAlpha),
                    ColorProvider.rgba(15, 15, 15, bgAlpha));
            drawExp4Border(x, y, width, height, 4.0f, bgAlpha);
            DrawUtil.drawText(Fonts.ICONS2.get(), "H", x + 1.0f, y + (height - iconSize) / 2.0f - 1.5f,
                    ColorProvider.rgba(130, 140, 255, alphaInt), iconSize);
            DrawUtil.drawText(Fonts.SFBOLD.get(), previewText, x + iconSize + 1.0f, y + (height - textSize) / 2.0f + 0.5f,
                    ColorProvider.rgba(255, 255, 255, alphaInt), textSize);
            context.getMatrices().pop();
        } else {
            float textSize = 7.5f;
            float iconWidth = 12.0f;
            height = 14.5f;
            width = iconWidth + Fonts.SFMEDIUM.get().getWidth(previewText, textSize) + 22.0f;
            x = centerX - width / 2.0f;

            context.getMatrices().push();
            context.getMatrices().translate(centerX, y + height / 2.0f, 0.0f);
            context.getMatrices().scale(scale, scale, 1.0f);
            context.getMatrices().translate(-centerX, -(y + height / 2.0f), 0.0f);

            int alphaInt = (int) (255 * scale);
            Hud3Style.drawChip(x, y, width, height, 5f, scale);
            DrawUtil.drawText(Fonts.ICONS_NURIK.get(), "J", x + 5.0f, y + 4.0f, ColorProvider.setAlpha(ColorProvider.getThemeColor(), alphaInt), 9.0f);
            DrawUtil.drawRound(x + 18.0f, y + 2.5f, 0.5f, height - 5.0f, 0.0f, ColorProvider.setAlpha(Hud3Style.BORDER, (int) (140 * scale)));
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), previewText, x + 23.0f, y + 3.0f, ColorProvider.setAlpha(Hud3Style.TEXT, alphaInt), textSize);
            context.getMatrices().pop();
        }

        float scaledWidth = width * scale;
        float scaledHeight = height * scale;
        notificationPreviewX = centerX - scaledWidth / 2.0f;
        notificationPreviewY = y + (height - scaledHeight) / 2.0f;
        notificationPreviewWidth = scaledWidth;
        notificationPreviewHeight = scaledHeight;
    }

    private void resetNotificationPreviewBounds() {
        notificationPreviewX = 0.0f;
        notificationPreviewY = 0.0f;
        notificationPreviewWidth = 0.0f;
        notificationPreviewHeight = 0.0f;
    }

    private void openElementContextMenu(String elementName, float anchorX, float anchorY) {
        elementContextMenuOpen = true;
        elementContextMenuElement = elementName;
        elementContextMenuAnchorX = anchorX;
        elementContextMenuAnchorY = anchorY;
        BooleanSetting blurOption = getBlurBackgroundOption(elementName);
        elementContextMenuBlurProgress = blurOption != null && blurOption.getValue() ? 1.0f : 0.0f;
        BooleanSetting themeOption = getThemeBackgroundOption(elementName);
        elementContextMenuThemeProgress = themeOption != null && themeOption.getValue() ? 1.0f : 0.0f;
        elementContextMenuTargetModeProgress = targetHudColorMode.is("От темы") ? 1.0f : 0.0f;
        updateElementContextMenuBounds();
    }

    private void closeElementContextMenu() {
        elementContextMenuOpen = false;
        elementContextMenuElement = null;
        elementContextMenuAnchorX = Float.NaN;
        elementContextMenuAnchorY = Float.NaN;
    }

    private void updateElementContextMenuBounds() {
        elementContextMenuWidth = getElementContextMenuWidth(elementContextMenuElement);
        float baseHeight = ELEMENT_CONTEXT_MENU_PADDING
                + (ELEMENT_CONTEXT_MENU_ROW_HEIGHT * 2.0f)
                + ELEMENT_CONTEXT_MENU_PADDING;
        if ("Активный таргет".equals(elementContextMenuElement)) {
            baseHeight += ELEMENT_CONTEXT_MENU_MODE_TOP + ELEMENT_CONTEXT_MENU_MODE_HEIGHT + ELEMENT_CONTEXT_MENU_MODE_BOTTOM;
        }
        elementContextMenuHeight = baseHeight;

        float baseX = Float.isNaN(elementContextMenuAnchorX) ? 8.0f : elementContextMenuAnchorX + 4.0f;
        float baseY = Float.isNaN(elementContextMenuAnchorY) ? 8.0f : elementContextMenuAnchorY + 4.0f;
        float screenWidth = mc.getWindow().getScaledWidth();
        float screenHeight = mc.getWindow().getScaledHeight();
        elementContextMenuX = Math.max(2.0f, Math.min(baseX, screenWidth - elementContextMenuWidth - 2.0f));
        elementContextMenuY = Math.max(2.0f, Math.min(baseY, screenHeight - elementContextMenuHeight - 2.0f));
    }

    private float getElementContextMenuWidth(String elementName) {
        if ("Бафы".equals(elementName) || "Привязанные модули".equals(elementName)) {
            return ELEMENT_CONTEXT_MENU_WIDTH_COMPACT;
        }
        return ELEMENT_CONTEXT_MENU_WIDTH;
    }

    public ModeSetting getHudStyleSetting() { return hudStyle; }
    public ModeSetting getTargetHudStyleSetting() { return targetHudStyle; }
    public ModeSetting getTargetHudColorModeSetting() { return targetHudColorMode; }
    public boolean isTargetHudThemeMode() { return targetHudColorMode.is("От темы"); }
    public BooleanSetting getShowAbsorptionSetting() { return targetHudShowAbsorption; }
    public BooleanSetting getShowItemsSetting() { return targetHudShowItems; }
    public SliderSetting getHeadRoundSetting() { return targetHudHeadRound; }
    public Draggable getTargetHUDDrag() { return targetHudDrag; }
    public TargetHudRenderer getRenderer() { return targetHudRenderer; }
    public ModeListSetting getElementsSetting() { return elements; }
    public boolean isScoreboardElementEnabled() { return elements.isEnabled("Scoreboard"); }
    public SliderSetting getLowHpAlertThresholdSetting() { return lowHpAlertThreshold; }
    public boolean isHotbarEnabled() { return elements.isEnabled("Хотбар"); }
    public BooleanSetting getNursultanShowLoginSetting() { return nursultanShowLogin; }
    public BooleanSetting getNursultanShowFpsSetting() { return nursultanShowFps; }
    public BooleanSetting getNursultanShowTimeSetting() { return nursultanShowTime; }
    public BooleanSetting getNursultanShowCoordsSetting() { return nursultanShowCoords; }
    public BooleanSetting getNursultanShowPingSetting() { return nursultanShowPing; }
    public BooleanSetting getNursultanShowTpsSetting() { return nursultanShowTps; }
    public BooleanSetting getNursultanShowBpsSetting() { return nursultanShowBps; }
    public BooleanSetting getNursultanShowLogoSetting() { return nursultanShowLogo; }
    public ModeSetting getNursultanWatermarkModeSetting() { return nursultanWatermarkMode; }
    public BooleanSetting getNursultanTransparentBackgroundSetting() { return nursultanTransparentBackground; }

    public static class NotificationManager {
        private static final CopyOnWriteArrayList<Notification> notifications = new CopyOnWriteArrayList<>();

        public static void post(String name, boolean enabled) {
            notifications.add(0, new Notification(name, enabled));
        }

        public static void postCheckbox(String name, boolean enabled) {
            Notification notification = new Notification(name, enabled);
            notification.isCheckbox = true;
            notifications.add(0, notification);
        }

        public static void postWarning(String text) {
            notifications.add(0, new Notification(text, true, true));
        }

        public static void postItemWarning(String text, ItemStack stack) {
            notifications.add(0, new Notification(text, true, true, stack));
        }

        public static void postItemWarning(Text text, ItemStack stack) {
            notifications.add(0, new Notification(text, true, true, stack));
        }

        public static void postItemInfo(String text, ItemStack stack) {
            notifications.add(0, new Notification(text, true, false, stack));
        }

        public static void postItemInfo(Text text, ItemStack stack) {
            notifications.add(0, new Notification(text, true, false, stack));
        }

        public static void postAutoSwapInfo(String text, ItemStack stack) {
            Notification notification = new Notification(text, true, false, stack);
            notification.style = NotificationStyle.AUTO_SWAP;
            notifications.add(0, notification);
        }

        public static void postAutoSwapInfo(Text text, ItemStack stack) {
            Notification notification = new Notification(text, true, false, stack);
            notification.style = NotificationStyle.AUTO_SWAP;
            notifications.add(0, notification);
        }

        public static void render(DrawContext context) {
            float centerX = MinecraftClient.getInstance().getWindow().getScaledWidth() / 2f;
            float startY = (MinecraftClient.getInstance().getWindow().getScaledHeight() / 2f) + 20f;
            float offset = 0;
            Interface interfaceModule = Instance.get(Interface.class);
            boolean axiomStyle = interfaceModule != null && interfaceModule.isAxiomStyle();
            boolean moonwardStyle = interfaceModule != null && interfaceModule.getHudStyleSetting().is("Pharaon");
            boolean exp4Style = interfaceModule != null && interfaceModule.getHudStyleSetting().is("Exp4.0");
            boolean pouchStyle = interfaceModule != null && interfaceModule.getHudStyleSetting().is("DLC");

            for (Notification n : notifications) {
                if (System.currentTimeMillis() - n.time > n.duration && n.anim.getValue() <= 0.01) {
                    notifications.remove(n);
                    continue;
                }

                boolean expiring = System.currentTimeMillis() - n.time > n.duration;
                n.anim.run(expiring ? 0 : 1);
                float clampedScale = (float) Math.max(0.0, Math.min(1.0, n.anim.getValue()));
                if (clampedScale <= 0.01f) continue;

                int alphaInt = (int) (255 * clampedScale);
                float textSize = 7f;
                String fullText;
                Text fullTextComponent;
                boolean drawItemIcon = n.iconStack != null && !n.iconStack.isEmpty();

                if (n.hasCustomText()) {
                    fullText = n.customText;
                    fullTextComponent = n.customTextComponent;
                } else {
                    fullTextComponent = null;
                    fullText =  "«" +n.name + "» " + buildModuleStateText(n);
                }

                if (axiomStyle) {
                    float height = 16.5f;
                    float axiomTextSize = 7.2f;
                    float textWidth = fullTextComponent != null
                            ? Fonts.RUBIK.get().getWidth(fullTextComponent, axiomTextSize)
                            : Fonts.RUBIK.get().getWidth(fullText, axiomTextSize);
                    float width = drawItemIcon ? 24f + textWidth + 8f : 20f + textWidth + 8f;
                    float x = centerX - (width / 2f);
                    float y = startY + offset;

                    context.getMatrices().push();
                    context.getMatrices().translate(centerX, y + height / 2f, 0);
                    context.getMatrices().scale(clampedScale, clampedScale, 1f);
                    context.getMatrices().translate(-centerX, -(y + height / 2f), 0);

                    interfaceModule.drawBackground(x, y, width, height, 5.0f, alphaInt);
                    interfaceModule.drawAxiomAccent(x + 4.0f, y + 3.25f, 13.5f, height - 6.5f, 3.6f, alphaInt);

                    if (drawItemIcon) {
                        context.getMatrices().push();
                        context.getMatrices().translate(x + 4.35f, y + 3.55f, 0f);
                        context.getMatrices().scale(0.67f, 0.67f, 1f);
                        context.drawItem(n.iconStack, 0, 0);
                        context.getMatrices().pop();
                    } else {
                        String iconCode = n.hasCustomText() ? (n.isWarning ? "G" : "J") : (n.enabled ? "J" : "K");
                        int iconColor = n.isWarning
                                ? ColorProvider.rgba(255, 228, 166, alphaInt)
                                : n.enabled
                                ? interfaceModule.getAxiomPrimaryTextColor(alphaInt)
                                : ColorProvider.rgba(255, 190, 190, alphaInt);
                        DrawUtil.drawText(Fonts.ICONS_NURIK.get(), iconCode, x + 6.0f, y + 4.35f, iconColor, 8.5f);
                    }

                    if (fullTextComponent != null) {
                        DrawUtil.drawText(Fonts.RUBIK.get(), fullTextComponent, x + 22.0f, y + 4.0f, axiomTextSize, alphaInt);
                    } else {
                        DrawUtil.drawText(Fonts.RUBIK.get(), fullText, x + 22.0f, y + 3.95f, interfaceModule.getAxiomPrimaryTextColor(alphaInt), axiomTextSize);
                    }

                    context.getMatrices().pop();
                    offset += (height + 3f) * clampedScale;
                } else if (moonwardStyle) {
                    float height = 20f;
                    float moonwardTextSize = 8.6f;
                    float textWidth = fullTextComponent != null
                            ? Fonts.SFBOLD.get().getWidth(fullTextComponent, moonwardTextSize)
                            : Fonts.SFBOLD.get().getWidth(fullText, moonwardTextSize);
                    float width = drawItemIcon ? 24f + textWidth + 7f : 20f + textWidth + 7f;
                    float x = centerX - (width / 2f);
                    float y = startY + offset;

                    context.getMatrices().push();
                    context.getMatrices().translate(centerX, y + height / 2f, 0);
                    context.getMatrices().scale(clampedScale, clampedScale, 1f);
                    context.getMatrices().translate(-centerX, -(y + height / 2f), 0);

                    DrawUtil.drawRoundBlur(x, y, width, height, 3f, ColorProvider.rgba(155, 155, 155, alphaInt),45f);

                    if (drawItemIcon) {
                        DrawUtil.drawRound(x + 4f, y + 4f, 12f, 12f, 2.5f, ColorProvider.setAlpha(ColorProvider.getThemeColor(), alphaInt));
                        context.getMatrices().push();
                        context.getMatrices().translate(x + 4f, y + 4f, 0f);
                        context.getMatrices().scale(0.75f, 0.75f, 1f);
                        context.drawItem(n.iconStack, 0, 0);
                        context.getMatrices().pop();
                    } else {
                        String iconGlyph = n.isWarning ? "\ue90e" : n.enabled ? "\ue90c" : "\ue90d";
                        float color = n.enabled ? ColorProvider.rgba(55,165,55,alphaInt) : ColorProvider.rgba(165,55,55,alphaInt);
                        DrawUtil.drawRound(x + 3.5f, y + 4f, 12.5f, 12f, 2.5f, (int) color);
                        DrawUtil.drawText(Fonts.MOONWARD_ICONS.get(), iconGlyph, x + 5f, y + 5.65f, ColorProvider.rgba(222, 222, 222, alphaInt), 10.5f);
                    }

                    if (fullTextComponent != null) {
                        DrawUtil.drawText(Fonts.SFBOLD.get(), fullTextComponent, x + 19f, y + 6.1f, moonwardTextSize, alphaInt);
                    } else {
                        DrawUtil.drawText(Fonts.SFBOLD.get(), fullText, x + 19f, y + 5.5f, ColorProvider.rgba(200, 200, 200, alphaInt), moonwardTextSize);
                    }

                    context.getMatrices().pop();
                    offset += (height + 3f) * clampedScale;
                } else if (exp4Style) {
                    float height = 16f;
                    float exp4TextSize = 6f;
                    float iconSize = 15f;
                    float textWidth = fullTextComponent != null
                            ? Fonts.SFBOLD.get().getWidth(fullTextComponent, exp4TextSize)
                            : Fonts.SFBOLD.get().getWidth(fullText, exp4TextSize);
                    float width = drawItemIcon ? iconSize + textWidth + 6f : iconSize + textWidth + 6f;
                    float x = centerX - (width / 2f);
                    float y = startY + offset;

                    context.getMatrices().push();
                    context.getMatrices().translate(centerX, y + height / 2f, 0);
                    context.getMatrices().scale(clampedScale, clampedScale, 1f);
                    context.getMatrices().translate(-centerX, -(y + height / 2f), 0);

                    int exp4BgAlpha = (int) (230 * clampedScale);
                    DrawUtil.drawRound(x, y, width, height, 4f,
                            ColorProvider.rgba(20, 20, 20, exp4BgAlpha),
                            ColorProvider.rgba(15, 15, 15, exp4BgAlpha),
                            ColorProvider.rgba(20, 20, 20, exp4BgAlpha),
                            ColorProvider.rgba(15, 15, 15, exp4BgAlpha));
                    if (interfaceModule != null) {
                        interfaceModule.drawExp4Border(x, y, width, height, 4f, exp4BgAlpha);
                    }

                    int exp4IconColor;
                    if (n.isWarning) {
                        exp4IconColor = ColorProvider.rgba(230, 170, 50, alphaInt);
                    } else {
                        exp4IconColor = n.enabled
                                ? ColorProvider.rgba(45, 190, 100, alphaInt)
                                : ColorProvider.rgba(220, 60, 60, alphaInt);
                    }
                    float iconX = x + 1f;
                    float iconY = y + (height - iconSize) / 2f - 1.5f;
                    DrawUtil.drawText(Fonts.ICONS2.get(), "H", iconX, iconY, exp4IconColor, iconSize);

                    float textY = y + (height - exp4TextSize) / 2f + 0.5f;
                    if (fullTextComponent != null) {
                        DrawUtil.drawText(Fonts.SFBOLD.get(), fullTextComponent, iconX + iconSize + 1f, textY, exp4TextSize, alphaInt);
                    } else {
                        DrawUtil.drawText(Fonts.SFBOLD.get(), fullText, iconX + iconSize + 1f, textY, ColorProvider.rgba(255, 255, 255, alphaInt), exp4TextSize);
                    }

                    context.getMatrices().pop();
                    offset += (height + 3f) * clampedScale;
                } else if (pouchStyle) {
                    float height = 15f;
                    float pouchTextSize = 7f;
                    float textWidth = fullTextComponent != null
                            ? Fonts.SFMEDIUM.get().getWidth(fullTextComponent, pouchTextSize)
                            : Fonts.SFMEDIUM.get().getWidth(fullText, pouchTextSize);
                    float iconSize = 8.5f;
                    float width = drawItemIcon ? iconSize + textWidth + 18f : iconSize + textWidth + 18f;
                    float x = centerX - (width / 2f);
                    float y = startY + offset;

                    context.getMatrices().push();
                    context.getMatrices().translate(centerX, y + height / 2f, 0);
                    context.getMatrices().scale(clampedScale, clampedScale, 1f);
                    context.getMatrices().translate(-centerX, -(y + height / 2f), 0);

                    Hud3Style.drawChip(x, y, width, height, 5f, clampedScale);

                    int pouchIconColor;
                    if (n.isWarning) {
                        pouchIconColor = ColorProvider.rgba(255, 210, 130, alphaInt);
                    } else {
                        pouchIconColor = n.enabled
                                ? ColorProvider.setAlpha(ColorProvider.getThemeColor(), alphaInt)
                                : ColorProvider.rgba(220, 110, 110, alphaInt);
                    }

                    float iconX = x + 5f;
                    float iconY = y + (height - iconSize) / 2f - 0.5f;
                    if (drawItemIcon) {
                        context.getMatrices().push();
                        context.getMatrices().translate(iconX - 2f, iconY - 2f, 0f);
                        context.getMatrices().scale(0.55f, 0.55f, 1f);
                        context.drawItem(n.iconStack, 0, 0);
                        context.getMatrices().pop();
                    } else {
                        DrawUtil.drawText(Fonts.ICONS2.get(), n.enabled ? "K" : "I", iconX, iconY, pouchIconColor, iconSize);
                    }

                    float textY = y + (height - pouchTextSize) / 2f + 0f;
                    if (fullTextComponent != null) {
                        DrawUtil.drawText(Fonts.SFMEDIUM.get(), fullTextComponent, iconX + iconSize + 5f, textY, pouchTextSize, alphaInt);
                    } else {
                        DrawUtil.drawText(Fonts.SFMEDIUM.get(), fullText, iconX + iconSize + 5f, textY, ColorProvider.setAlpha(Hud3Style.TEXT, alphaInt), pouchTextSize);
                    }

                    context.getMatrices().pop();
                    offset += (height + 3f) * clampedScale;
                } else {
                    String iconCode = n.hasCustomText() ? (n.isWarning ? "G" : "J") : (n.enabled ? "J" : "K");
                    int iconColor = n.enabled ? ColorProvider.rgba(125, 255, 125, alphaInt) : ColorProvider.rgba(255, 125, 125, alphaInt);
                    float height = 14.5f;
                    float textWidth = fullTextComponent != null
                            ? Fonts.SFMEDIUM.get().getWidth(fullTextComponent, textSize)
                            : Fonts.SFMEDIUM.get().getWidth(fullText, textSize);
                    float iconWidth = drawItemIcon ? 12f : Fonts.ICONS_NURIK.get().getWidth(iconCode, 9f);
                    float width = iconWidth + textWidth + n.getRightPadding();

                    float x = centerX - (width / 2f);
                    float y = startY + offset;

                    context.getMatrices().push();
                    context.getMatrices().translate(centerX, y + height / 2f, 0);
                    context.getMatrices().scale(clampedScale, clampedScale, 1f);
                    context.getMatrices().translate(-centerX, -(y + height / 2f), 0);

                    interfaceModule.drawBackground(x, y, width, height, 4, alphaInt);

                    if (drawItemIcon) {
                        context.getMatrices().push();
                        context.getMatrices().translate(x + n.getItemIconOffsetX(), y + n.getItemIconOffsetY(), 0f);
                        context.getMatrices().scale(0.58f, 0.58f, 1f);
                        context.drawItem(n.iconStack, 0, 0);
                        context.getMatrices().pop();
                    } else {
                        DrawUtil.drawText(Fonts.ICONS_NURIK.get(), iconCode, x + 5, y + 4, iconColor, 9f);
                    }

                    DrawUtil.drawRound(x + 18f, y + 2.5f, 0.5f, height - 5f, 0, ColorProvider.rgba(255, 255, 255, (int) (120 * clampedScale)));
                    float textY = drawItemIcon ? y + 3.2f : y + 3f;
                    if (fullTextComponent != null) {
                        DrawUtil.drawText(Fonts.SFMEDIUM.get(), fullTextComponent, x + 23f, textY, textSize, alphaInt);
                    } else {
                        DrawUtil.drawText(Fonts.SFMEDIUM.get(), fullText, x + 23f, textY, ColorProvider.rgba(255, 255, 255, alphaInt), textSize);
                    }

                    context.getMatrices().pop();
                    offset += (height + 3) * clampedScale;
                }
            }
        }

        public static boolean hasActiveNotifications() {
            return !notifications.isEmpty();
        }

        private static String buildToggleText(Notification notification) {
            String prefix = notification.isCheckbox ? "Чекбокс " : "Модуль ";
            String state = notification.enabled ? "Включен!" : "Выключен!";
            return prefix + notification.name + " " + state;
        }

        private static String buildToggleTextStable(Notification notification) {
            String prefix = notification.isCheckbox ? "Чекбокс " : "Модуль ";
            String state = notification.enabled ? "Включен!" : "Выключен!";
            return prefix + notification.name + " " + state;
        }

        private static String buildModuleStateText(Notification notification) {
            return notification.enabled ? "включен!" : "выключен!";
        }

        private enum NotificationStyle {
            DEFAULT,
            AUTO_SWAP
        }

        private static class Notification {
            String name;
            boolean enabled;
            long time;
            long duration = 1000;
            Animation anim = new Animation(Easing.BACK_OUT, 300);
            boolean isWarning = false;
            String customText;
            Text customTextComponent;
            ItemStack iconStack;
            boolean isCheckbox = false;
            NotificationStyle style = NotificationStyle.DEFAULT;

            Notification(String name, boolean enabled) {
                this.name = name;
                this.enabled = enabled;
                this.time = System.currentTimeMillis();
            }

            Notification(String customText, boolean enabled, boolean isWarning) {
                this(customText, enabled, isWarning, null);
            }

            Notification(String customText, boolean enabled, boolean isWarning, ItemStack iconStack) {
                this.customText = customText;
                this.enabled = enabled;
                this.isWarning = isWarning;
                this.iconStack = iconStack == null ? null : iconStack.copy();
                this.time = System.currentTimeMillis();
                this.duration = 2000;
            }

            Notification(Text customTextComponent, boolean enabled, boolean isWarning, ItemStack iconStack) {
                this.customText = customTextComponent.getString();
                this.customTextComponent = customTextComponent.copy();
                this.enabled = enabled;
                this.isWarning = isWarning;
                this.iconStack = iconStack == null ? null : iconStack.copy();
                this.time = System.currentTimeMillis();
                this.duration = 2000;
            }

            boolean hasCustomText() {
                return customText != null && !customText.isEmpty();
            }

            float getItemIconOffsetX() {
                return style == NotificationStyle.AUTO_SWAP ? 4.8f : 4.0f;
            }

            float getItemIconOffsetY() {
                return style == NotificationStyle.AUTO_SWAP ? 2.1f : 1.0f;
            }

            float getRightPadding() {
                return style == NotificationStyle.AUTO_SWAP ? 19f : 22f;
            }
        }
    }
}

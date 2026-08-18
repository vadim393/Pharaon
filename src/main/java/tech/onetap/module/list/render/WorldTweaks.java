package tech.onetap.module.list.render;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import tech.onetap.event.list.EventPlayerUpdate;
import tech.onetap.event.list.EventTick;
import tech.onetap.event.list.EventWorldRender;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ColorSetting;
import tech.onetap.module.settings.ModeListSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.friend.FriendRepository;
import tech.onetap.util.render.providers.ColorProvider;

@ModuleInformation(
        moduleName = "WorldTweaks",
        moduleDesc = "\u0422\u0432\u0438\u043A\u0438 \u043C\u0438\u0440\u0430 \u0438 \u043F\u0430\u0440\u0442\u0438\u043A\u043B\u044B",
        moduleCategory = ModuleCategory.RENDER
)
public class WorldTweaks extends Module {

    private static final String OPTION_SMALL_PLAYER = "\u041C\u0430\u043B\u0435\u043D\u044C\u043A\u0438\u0439 \u0438\u0433\u0440\u043E\u043A";
    private static final String OPTION_CUSTOM_FOG = "\u0421\u0432\u043E\u0439 \u0442\u0443\u043C\u0430\u043D";
    private static final String OPTION_CUSTOM_FOG_DISTANCE = "\u0421\u0432\u043E\u044F \u0434\u0438\u0441\u0442\u0430\u043D\u0446\u0438\u044F \u0442\u0443\u043C\u0430\u043D\u0430";
    private static final String OPTION_TIME_OF_DAY = "\u0412\u0440\u0435\u043C\u044F \u0441\u0443\u0442\u043E\u043A";
    private static final String OPTION_SHADER_SKY = "\u0428\u0435\u0439\u0434\u0435\u0440\u043D\u043E\u0435 \u041D\u0435\u0431\u043E";
    private static final String OPTION_WORLD_PARTICLES = "\u041F\u0430\u0440\u0442\u0438\u043A\u043B\u044B \u0432 \u043C\u0438\u0440\u0435";
    private static final String SMALL_PLAYER_SELF = "\u0421\u0435\u0431\u044F";
    private static final String SMALL_PLAYER_FRIENDS = "\u0414\u0440\u0443\u0437\u0435\u0439";
    private static final String SMALL_PLAYER_PLAYERS = "\u0418\u0433\u0440\u043E\u043A\u043E\u0432";

    private static final String FOG_MODE_THEME = "\u0422\u0435\u043C\u0430";
    private static final String FOG_MODE_CUSTOM = "\u0421\u0432\u043E\u0439";

    private static final String TIME_DAWN = "\u0420\u0430\u0441\u0441\u0432\u0435\u0442";
    private static final String TIME_MORNING = "\u0423\u0442\u0440\u043E";
    private static final String TIME_NOON = "\u041F\u043E\u043B\u0434\u0435\u043D\u044C";
    private static final String TIME_SUNSET = "\u0417\u0430\u043A\u0430\u0442";
    private static final String TIME_NIGHT = "\u041D\u043E\u0447\u044C";
    private static final String SKY_SHADER_PLASMA = "Plasma";
    private static final String SKY_SHADER_COSMOS = "Cosmos";
    static final String WORLD_PARTICLE_MODE_FIREFLY = "\u0421\u0432\u0435\u0442\u043B\u044F\u0447\u043A\u0438";
    static final String WORLD_PARTICLE_MODE_CLASSIC = "\u041E\u0431\u044B\u0447\u043D\u044B\u0435";

    public final ModeListSetting options = new ModeListSetting(
            "\u041E\u043F\u0446\u0438\u0438",
            new BooleanSetting(OPTION_SMALL_PLAYER, false),
            new BooleanSetting(OPTION_CUSTOM_FOG, true),
            new BooleanSetting(OPTION_CUSTOM_FOG_DISTANCE, true),
            new BooleanSetting(OPTION_TIME_OF_DAY, true),
            new BooleanSetting(OPTION_SHADER_SKY, false),
            new BooleanSetting(OPTION_WORLD_PARTICLES, false)
    );

    public final ModeListSetting smallPlayerTargets = new ModeListSetting(
            "\u041D\u0430 \u043A\u043E\u043C",
            new BooleanSetting(SMALL_PLAYER_SELF, true),
            new BooleanSetting(SMALL_PLAYER_FRIENDS, false),
            new BooleanSetting(SMALL_PLAYER_PLAYERS, false)
    ).setVisible(() -> isEnabled(OPTION_SMALL_PLAYER));

    public final SliderSetting fogDistance = new SliderSetting(
            "\u0414\u0438\u0441\u0442\u0430\u043D\u0446\u0438\u044F \u0442\u0443\u043C\u0430\u043D\u0430",
            0.8f,
            0.1f,
            1.0f,
            0.1f
    ).setVisible(this::isCustomFogDistanceEnabled);

    public final ModeSetting fogMode = new ModeSetting(
            "\u0412\u0438\u0434",
            FOG_MODE_THEME,
            FOG_MODE_THEME,
            FOG_MODE_CUSTOM
    ).setVisible(this::isFogEnabled);

    public final ColorSetting fogColor = new ColorSetting(
            "\u0426\u0432\u0435\u0442",
            ColorProvider.rgb(255, 255, 255)
    ).setVisible(() -> isFogEnabled() && fogMode.is(FOG_MODE_CUSTOM));

    public final ModeSetting time = new ModeSetting(
            "\u0412\u0440\u0435\u043C\u044F",
            TIME_NIGHT,
            TIME_DAWN,
            TIME_MORNING,
            TIME_NOON,
            TIME_SUNSET,
            TIME_NIGHT
    ).setVisible(this::shouldOverrideTime);

    public final ModeSetting skyShader = new ModeSetting(
            "\u0428\u0435\u0439\u0434\u0435\u0440 \u043D\u0435\u0431\u0430",
            SKY_SHADER_PLASMA,
            SKY_SHADER_PLASMA,
            SKY_SHADER_COSMOS
    ).setVisible(this::isShaderSkyEnabled);

    public final SliderSetting skyShaderScale = new SliderSetting(
            "\u041C\u0430\u0441\u0448\u0442\u0430\u0431 \u0448\u0435\u0439\u0434\u0435\u0440\u0430",
            1.2f,
            0.25f,
            4.0f,
            0.05f
    ).setVisible(this::isShaderSkyEnabled);

    public final SliderSetting skyShaderSpeed = new SliderSetting(
            "\u0421\u043A\u043E\u0440\u043E\u0441\u0442\u044C \u0448\u0435\u0439\u0434\u0435\u0440\u0430",
            1.0f,
            0.1f,
            4.0f,
            0.05f
    ).setVisible(this::isShaderSkyEnabled);

    public final ModeSetting worldParticleMode = new ModeSetting(
            "\u0420\u0435\u0436\u0438\u043C",
            WORLD_PARTICLE_MODE_FIREFLY,
            WORLD_PARTICLE_MODE_FIREFLY,
            WORLD_PARTICLE_MODE_CLASSIC
    ).setVisible(this::isWorldParticlesEnabled);

    public final SliderSetting worldParticleCount = new SliderSetting(
            "\u041A\u043E\u043B\u0438\u0447\u0435\u0441\u0442\u0432\u043E",
            20.0f,
            10.0f,
            300.0f,
            1.0f
    ).setVisible(this::isWorldParticlesEnabled);

    public final SliderSetting worldParticleSpeed = new SliderSetting(
            "\u0421\u043A\u043E\u0440\u043E\u0441\u0442\u044C",
            0.15f,
            0.05f,
            0.5f,
            0.05f
    ).setVisible(() -> isWorldParticlesEnabled() && isWorldParticleMode(WORLD_PARTICLE_MODE_FIREFLY));

    public final SliderSetting worldParticleRadius = new SliderSetting(
            "\u0420\u0430\u0434\u0438\u0443\u0441",
            25.0f,
            10.0f,
            50.0f,
            5.0f
    ).setVisible(() -> isWorldParticlesEnabled() && isWorldParticleMode(WORLD_PARTICLE_MODE_FIREFLY));

    public final SliderSetting worldParticleTrail = new SliderSetting(
            "\u0414\u043B\u0438\u043D\u0430 \u0445\u0432\u043E\u0441\u0442\u0430",
            20.0f,
            5.0f,
            40.0f,
            5.0f
    ).setVisible(() -> isWorldParticlesEnabled() && isWorldParticleMode(WORLD_PARTICLE_MODE_FIREFLY));

    public final SliderSetting worldParticleSize = new SliderSetting(
            "\u0420\u0430\u0437\u043C\u0435\u0440",
            0.22f,
            0.08f,
            0.6f,
            0.01f
    ).setVisible(this::isWorldParticlesEnabled);

    public final BooleanSetting worldParticleRandomColor = new BooleanSetting(
            "\u0420\u0430\u043D\u0434\u043E\u043C\u043D\u044B\u0439 \u0446\u0432\u0435\u0442",
            true
    ).setVisible(() -> isWorldParticlesEnabled() && isWorldParticleMode(WORLD_PARTICLE_MODE_FIREFLY));

    public final BooleanSetting worldParticleThemeColor = new BooleanSetting(
            "\u0426\u0432\u0435\u0442 \u043E\u0442 \u0442\u0435\u043C\u044B",
            true
    ).setVisible(() -> isWorldParticlesEnabled()
            && (!isWorldParticleMode(WORLD_PARTICLE_MODE_FIREFLY) || !worldParticleRandomColor.getValue()));

    public static boolean child;
    public static boolean childSelf;
    public static boolean childFriends;
    public static boolean childPlayers;

    private final WorldTweaksParticleController worldParticleController = new WorldTweaksParticleController(this);

    @Subscribe
    private void onUpdate(EventPlayerUpdate e) {
        child = isEnabled(OPTION_SMALL_PLAYER);
        childSelf = child && smallPlayerTargets.isEnabled(SMALL_PLAYER_SELF);
        childFriends = child && smallPlayerTargets.isEnabled(SMALL_PLAYER_FRIENDS);
        childPlayers = child && smallPlayerTargets.isEnabled(SMALL_PLAYER_PLAYERS);
    }

    @Subscribe
    private void onTick(EventTick e) {
        worldParticleController.onTick();
    }

    @Subscribe
    private void onWorldRender(EventWorldRender event) {
        worldParticleController.onWorldRender(event);
    }

    public static boolean shouldRenderSmallPlayer(PlayerEntity player) {
        if (!child || player == null) {
            return false;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return false;
        }

        if (player == client.player) {
            return childSelf;
        }

        if (FriendRepository.isFriend(player.getNameForScoreboard())) {
            return childFriends;
        }

        return childPlayers;
    }

    public boolean isOptionEnabled(String name) {
        return options.isEnabled(name);
    }

    public boolean isEnabled(String name) {
        return isOptionEnabled(name);
    }

    public boolean isFogEnabled() {
        return isEnabled(OPTION_CUSTOM_FOG);
    }

    public boolean isCustomFogDistanceEnabled() {
        return isEnabled(OPTION_CUSTOM_FOG_DISTANCE);
    }

    public boolean shouldOverrideTime() {
        return isEnabled(OPTION_TIME_OF_DAY);
    }

    public boolean isShaderSkyEnabled() {
        return isEnabled(OPTION_SHADER_SKY);
    }

    public boolean isWorldParticlesEnabled() {
        return isEnabled(OPTION_WORLD_PARTICLES);
    }

    public int getFogColor() {
        if (fogMode.is(FOG_MODE_CUSTOM)) {
            return fogColor.getValue();
        }
        return ColorProvider.getThemeColor();
    }

    public long getCustomTime() {
        return switch (time.getValue()) {
            case TIME_DAWN -> 0L;
            case TIME_MORNING -> 1000L;
            case TIME_NOON -> 6000L;
            case TIME_SUNSET -> 12700L;
            case TIME_NIGHT -> 18000L;
            default -> 18000L;
        };
    }

    public float getFogDistance() {
        return fogDistance.getFloatValue();
    }

    public boolean isThemeFogColorEnabled() {
        return fogMode.is(FOG_MODE_THEME);
    }

    public String getSkyShaderMode() {
        return SKY_SHADER_COSMOS.equalsIgnoreCase(skyShader.getValue()) ? SKY_SHADER_COSMOS : SKY_SHADER_PLASMA;
    }

    public boolean isSkyShaderMode(String mode) {
        return mode != null && mode.equalsIgnoreCase(getSkyShaderMode());
    }

    public boolean isWorldParticleMode(String mode) {
        return mode != null && mode.equalsIgnoreCase(worldParticleMode.getValue());
    }

    public float getSkyShaderScale() {
        return skyShaderScale.getFloatValue();
    }

    public float getSkyShaderSpeed() {
        return skyShaderSpeed.getFloatValue();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        worldParticleController.reset();
    }

    @Override
    public void onDisable() {
        worldParticleController.reset();
        child = false;
        childSelf = false;
        childFriends = false;
        childPlayers = false;
        super.onDisable();
    }
}

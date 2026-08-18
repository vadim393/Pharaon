package tech.onetap.module;

import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonObject;
import lombok.Getter;
import tech.onetap.Onetap;
import tech.onetap.event.EventGameUpdate;
import tech.onetap.event.list.EventHUD;
import tech.onetap.event.list.EventKeyInput;
import tech.onetap.event.list.EventTick;
import tech.onetap.module.list.combat.*;
import tech.onetap.module.list.misc.*;
import tech.onetap.module.list.movement.*;
import tech.onetap.module.list.player.*;
import tech.onetap.module.list.render.*;
import tech.onetap.module.list.render.Projectile;
import tech.onetap.module.list.render.hud.Interface;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ThemeSetting;
import tech.onetap.util.IMinecraft;
import tech.onetap.util.base.Instance;
import tech.onetap.util.party.connection.PartyApiClient;
import tech.onetap.util.player.other.SlownessManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class ModuleStorage implements IMinecraft {
    private static final long aura_return_time = 100L;

    private final List<Module> modules = new ArrayList<>();
    private final Map<Class<? extends Module>, Module> modulesByClass = new HashMap<>();
    private final Map<String, Module> modulesByName = new HashMap<>();
    private boolean auraHadTarget;
    private boolean auraWasEnabled;
    private boolean auraReturningToCamera;
    private long auraReturnStartTime;

    public void injectRegisterModules() {
        modules.addAll(List.of(
                new FullBright(), new ClickGui(), new CsGui(), new Sprint(),
                new TriggerBot(),
                new NoRender(), new KillAura(), new ElytraTarget(), new Arrows(),
                new ItemPilot(),
                new AttackAura(),
                new NameTags(), new TargetESP(), new GlowESP(), new NoPush(), new SoulESP(),
                new NoJumpDelay(),  new ElytraSwap(), new PickBlowup(),
                new AutoTotem(), new ClickPearl(), new UseTracker(),
                new ClientSounds(), new NoFriendDamage(), new ElytraBooster(), new Flight(),
                new AutoPrimer(),
                new FreeCamera(), new SwingAnimations(), new Predictions(), new Projectile(), new ShulkerViewer(),
                new DogFly(), new AutoTpaccept(), new RPSpoofer(), new Particles(),
                new AntiBot(), new AutoExplosion(), new DeathCoords(), new CrystalOptimizer(),
                new GuiMove(), new Okkk(), new Tracers(), new JumpCircle(), new ElytraMotion(), new Velocity(), new ElytraFlight(),
                new GlideFly(), new Disabler(), new AntiStop(), new ElytraExploit(),
                new AntiThorns(), new AutoPotion(), new DamageSwap(),
                new ViewModel(), new AspectRatio(), new ItemPhysics(), new KillEffect(), new LonyHelper(), new Speed(),
                new AutoTool(), new TapeMouse(), new WorldTweaks(), new BlockOverlay(), new FreeLook(),
                new Trails(), new FastExp(), new NameProtect(), new CrystalSpammer(), new ChinaHat(), new ProjectileHelper(),
                new AirStuck(), new AutoSwap(), new AutoWeb(), new NoSlow(), new NoWeb(), new DiscordRPC(),
                new FakePlayer(), new Interface(), new AutoEat(), new AutoLeave(), new WellJoiner(), new InvSaver(),
                new BlockESP(), new CordsDropper(), new Crosshair(),  new ElytraJump(), new FireWorkESP(), new ItemScroller(), new PacketCriticals(), new RgExploitTest(),
                new Panic(), new ScoreboardHealth(), new TargetPearl(),
                 new BravoStrafe(),
                 new tech.onetap.module.list.player.RWHelper()
        ));

        modulesByClass.clear();
        modulesByName.clear();
        for (Module module : modules) {
            modulesByClass.put(module.getClass(), module);
            modulesByName.put(module.getName().toLowerCase(Locale.ROOT), module);
        }

        Onetap.getInstance().getEventBus().register(this);
    }

    public <T extends Module> T get(final String name) {
        Module cachedModule = modulesByName.get(name.toLowerCase(Locale.ROOT));
        if (cachedModule != null) {
            return (T) cachedModule;
        }

        T resolved = this.modules.stream()
                .filter(entry -> entry.getName().equalsIgnoreCase(name))
                .map(entry -> (T) entry)
                .findFirst()
                .orElse(null);
        if (resolved != null) {
            modulesByName.put(name.toLowerCase(Locale.ROOT), resolved);
        }
        return resolved;
    }

    public <T extends Module> T get(final Class<T> clazz) {
        Module cachedModule = modulesByClass.get(clazz);
        if (cachedModule != null) {
            return clazz.cast(cachedModule);
        }

        T resolved = this.modules.stream()
                .filter(entry -> clazz.isAssignableFrom(entry.getClass()))
                .map(clazz::cast)
                .findFirst()
                .orElse(null);
        if (resolved != null) {
            modulesByClass.put(clazz, resolved);
        }
        return resolved;
    }

    public List<Module> get(final ModuleCategory category) {
        return this.modules.stream()
                .filter(module -> module.getCategory() == category)
                .collect(Collectors.toList());
    }

    @Subscribe
    private void onGameUpdate(EventGameUpdate e) {
        if (mc.player == null) return;

        if (!SlownessManager.slowTasksIsEmpty()) SlownessManager.updateSlowTasks();
        if (!SlownessManager.timeTasksIsEmpty()) SlownessManager.updateTimeTasks(false);

        var aura = get(KillAura.class);

        if (!aura.isEnabled() || aura.getTarget() == null) {
            boolean targetJustLost = auraHadTarget;
            boolean auraJustDisabled = auraWasEnabled && !aura.isEnabled();
            auraHadTarget = false;
            auraWasEnabled = aura.isEnabled();

            if (targetJustLost || auraJustDisabled) {
                auraReturningToCamera = true;
                auraReturnStartTime = System.currentTimeMillis();
                aura.resetCameraReturnRotationState();
            }

            if (aura.rotation.is("Vanilla") || Instance.get(FreeLook.class).isActive()) {
                auraReturningToCamera = false;
                auraReturnStartTime = 0L;
                aura.resetCameraReturnRotationState();
                aura.syncRotationStateToCamera();
                return;
            }

            if (auraReturningToCamera) {
                aura.setReturningToCamera(true);
                if (System.currentTimeMillis() - auraReturnStartTime < aura_return_time) {
                    aura.rotateToCamera();
                    return;
                }

                if (aura.updateRotationToCamera(3.5f)) {
                    auraReturningToCamera = false;
                    auraReturnStartTime = 0L;
                    aura.setReturningToCamera(false);
                    aura.syncRotationStateToCamera();
                }
                return;
            }

            aura.syncRotationStateToCamera();
            return;
        }

        auraReturningToCamera = false;
        auraReturnStartTime = 0L;
        aura.setReturningToCamera(false);
        aura.resetCameraReturnRotationState();
        auraHadTarget = true;
        auraWasEnabled = true;
    }

    @Subscribe
    private void onRender(EventHUD ignored) {
        for (var module : getModules()) {
            module.getAnimation().run(module.isEnabled());
            for (var setting : module.getSettings()) {
                if (setting instanceof BooleanSetting b) b.getAnimation().run(b.getValue());
                if (setting instanceof ThemeSetting t) t.getValue().animation.run(1);
            }
        }
    }

    @Subscribe
    private void onKey(EventKeyInput e) {
        if (e.getAction() == 0) return;
        if (e.getKey() >= 0 && e.getKey() <= org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT) return;
        for (var module : getModules()) {
            module.getSettings().stream()
                    .filter(setting -> setting instanceof BooleanSetting b && e.getKey() == b.getKey())
                    .forEach(setting -> {
                        BooleanSetting boolSetting = (BooleanSetting) setting;
                        boolSetting.toggle();
                        Interface.NotificationManager.postCheckbox(boolSetting.getName(), boolSetting.getValue());
                    });
        }
    }

    @Subscribe
    private void onUpdate(EventTick ignored) {
        if (!SlownessManager.timeTasksIsEmpty()) SlownessManager.updateTimeTasks(true);

        if (mc.player == null || true) return;

        PartyApiClient.fetchPartyStateAsync();
        PartyApiClient.fetchInvitesAsync();

        JsonObject j = new JsonObject();
        j.addProperty("player", mc.player.getNameForScoreboard());
        j.addProperty("x", mc.player.getX());
        j.addProperty("y", mc.player.getY());
        j.addProperty("z", mc.player.getZ());

        PartyApiClient.postAsync("/party/pos", j, json -> {});
    }

}

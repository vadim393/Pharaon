package tech.onetap;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import tech.onetap.event.list.EventKeyInput;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleStorage;
import tech.onetap.ui.punch.PunchMenu;
import tech.onetap.util.auth.AuthManager;
import tech.onetap.util.alt.AltManager;
import tech.onetap.util.commands.CommandDispatcher;
import tech.onetap.util.commands.manager.CommandRepository;
import tech.onetap.util.config.ConfigManager;
import tech.onetap.util.config.ThemeBootstrap;
import tech.onetap.util.draggable.DragManager;
import tech.onetap.util.friend.FriendRepository;
import tech.onetap.util.macro.MacroRepository;
import tech.onetap.util.math.TPSGetter;
import tech.onetap.util.player.combat.IdealHitUtils;
import tech.onetap.util.player.other.ServerManager;
import tech.onetap.util.rotation.ComponentManager;
import tech.onetap.util.script.ScriptManager;
import tech.onetap.util.staff.StaffManager;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class Onetap implements ModInitializer {

    private static Onetap instance;

    @Getter
    private final EventBus eventBus;

    //допустим тут мега бупас


    @Getter
    private final ModuleStorage moduleStorage;
    @Getter
    private final ComponentManager componentManager;
    @Getter
    private final DragManager dragManager;
    @Getter
    private final CommandRepository commandRepository;
    @Getter
    private final MacroRepository macroRepository;
    @Getter
    private final ConfigManager configManager;
    @Getter
    private final CommandDispatcher commandDispatcher;
    @Getter
    private final StaffManager staffManager;
    @Getter
    private final ServerManager serverManager;
    @Getter
    private final TPSGetter tpsGetter;
    @Getter
    private final IdealHitUtils idealHitUtils;
    @Getter
    private final ScriptManager scriptManager;
    @Getter
    private final AuthManager authManager;
    private final AtomicBoolean clientStatePersisted = new AtomicBoolean(false);


    public Onetap() {
        instance = this;

        eventBus = new EventBus();
        eventBus.register(this);

        System.out.println("[OneTap] Starting authentication...");
        authManager = new AuthManager();

        if (!authManager.authenticate()) {
            System.err.println("[OneTap] Authentication failed! Exiting...");
            Runtime.getRuntime().halt(1);
        }

        System.out.println("[OneTap] Authentication successful! Initializing client...");

        moduleStorage = new ModuleStorage();
        componentManager = new ComponentManager();
        dragManager = new DragManager();
        macroRepository = new MacroRepository();
        configManager = new ConfigManager();
        staffManager = new StaffManager();
        staffManager.load();
        commandRepository = new CommandRepository();
        commandDispatcher = new CommandDispatcher();
        serverManager = new ServerManager();
        tpsGetter = new TPSGetter();
        idealHitUtils = new IdealHitUtils();
        scriptManager = new ScriptManager();
        Runtime.getRuntime().addShutdownHook(new Thread(this::persistClientState));
        File dir = new File("onetap/configs/");
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public static Onetap getInstance() {
        return instance == null ? new Onetap() : instance;
    }

    @Override
    public void onInitialize() {
        runInitStep("theme bootstrap", ThemeBootstrap::applySavedThemeIfPresent);
        runInitStep("module registration", () -> getModuleStorage().injectRegisterModules());
        runInitStep("punch menu init", PunchMenu::init);
        runInitStep("component manager init", componentManager::init);
        runInitStep("drag manager load", dragManager::load);
        runInitStep("macro repository load", macroRepository::load);
        runInitStep("friend repository load", FriendRepository::load);
        runInitStep("alt persist load", AltManager::applyPersistedAlt);
        runInitStep("config load", () -> ConfigManager.load("autocfg"));
    }

    public void persistClientState() {
        if (!clientStatePersisted.compareAndSet(false, true)) {
            return;
        }

        runPersistenceStep("config save", () -> ConfigManager.save("autocfg"));
        runPersistenceStep("drag manager save", () -> getDragManager().saveDraggables());
        runPersistenceStep("macro repository save", () -> getMacroRepository().save());
        runPersistenceStep("friend repository save", FriendRepository::save);
        runPersistenceStep("staff manager save", staffManager::save);
    }

    private void runInitStep(String step, Runnable action) {
        try {
            action.run();
        } catch (Throwable t) {
            System.err.println("[OneTap] Initialization step failed: " + step + " -> " + t.getClass().getSimpleName() + ": " + t.getMessage());
            t.printStackTrace();
        }
    }

    private void runPersistenceStep(String step, Runnable action) {
        try {
            action.run();
        } catch (Throwable t) {
            System.err.println("[OneTap] Persistence step failed: " + step + " -> " + t.getClass().getSimpleName() + ": " + t.getMessage());
            t.printStackTrace();
        }
    }

    @Subscribe
    private void onModuleKeyPressed(EventKeyInput event) {
        if (event.getAction() != 1 || MinecraftClient.getInstance().currentScreen != null) {
            return;
        }
        if (event.getKey() >= 0 && event.getKey() <= org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return;
        }
        for (Module module : getModuleStorage().getModules()) {
            if (module.getKey() == event.getKey()) {
                module.toggle();
            }
        }
    }
}

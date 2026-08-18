package tech.onetap.module.list.render;

import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;

import java.util.Map;
import java.util.WeakHashMap;

@ModuleInformation(moduleName = "ItemPhysics", moduleDesc = "Добавляет физику выпавшим предметам", moduleCategory = ModuleCategory.RENDER)
public class ItemPhysics extends Module {
    private static final Map<ItemEntityRenderState, PhysicsState> STATE_CACHE = new WeakHashMap<>();

    public static void capture(ItemEntityRenderState state, boolean onGround) {
        if (state != null) {
            STATE_CACHE.put(state, new PhysicsState(onGround));
        }
    }

    public static PhysicsState getState(ItemEntityRenderState state) {
        return state == null ? null : STATE_CACHE.get(state);
    }

    public record PhysicsState(boolean onGround) {
    }
}

package tech.onetap.ui.punch.context;

import net.minecraft.client.MinecraftClient;

public interface MinecraftContext {
    MinecraftClient mc = MinecraftClient.getInstance();
}
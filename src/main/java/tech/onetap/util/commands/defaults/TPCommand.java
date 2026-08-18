package tech.onetap.util.commands.defaults;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import tech.onetap.util.commands.api.Command;
import tech.onetap.util.commands.api.argument.IArgConsumer;
import tech.onetap.util.commands.api.exception.CommandException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class TPCommand extends Command {
    public TPCommand() {
        super("tp");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMin(1);
        String targetName = args.getString();

        ClientPlayerEntity player = Objects.requireNonNull(MinecraftClient.getInstance().player);
        ClientWorld world = MinecraftClient.getInstance().world;

        AbstractClientPlayerEntity target = findPlayer(world, targetName);
        if (target == null) {
            logDirect(Formatting.RED + "Игрок " + Formatting.GOLD + targetName + Formatting.RED + " не найден.");
            return;
        }

        double distance = player.distanceTo(target);
        double targetX = target.getX();
        double targetZ = target.getZ();
        double targetY = findSolidBlockY(world, target);

        if (targetY == Double.MIN_VALUE) {
            logDirect(Formatting.RED + "Не удалось найти твердый блок рядом с игроком "
                    + Formatting.GOLD + targetName + Formatting.RED + ".");
            return;
        }

        int packetsCount = Math.max((int) (distance / 1000), 3);
        boolean onGround = player.isOnGround();
        boolean horizontalCollision = player.horizontalCollision;

        for (int i = 0; i < packetsCount; i++) {
            player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(onGround, horizontalCollision));
        }

        player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(targetX, targetY, targetZ, false, horizontalCollision));
        player.setPosition(targetX, targetY, targetZ);

        logDirect(Formatting.GRAY + "Телепортация к " + Formatting.GOLD + targetName + Formatting.GRAY + " выполнена.");
        logDirect(String.format(Formatting.GRAY + "Координаты: %.1f %.1f %.1f", targetX, targetY, targetZ));
    }

    private double findSolidBlockY(ClientWorld world, AbstractClientPlayerEntity target) {
        if (world == null) {
            return Double.MIN_VALUE;
        }

        BlockPos targetPos = target.getBlockPos();

        for (int y = targetPos.getY() - 1; y >= 0; y--) {
            BlockPos pos = new BlockPos(targetPos.getX(), y, targetPos.getZ());
            if (world.getBlockState(pos).isSolid()) {
                return y + 0.25;
            }
        }

        for (int y = targetPos.getY() + 1; y < 256; y++) {
            BlockPos pos = new BlockPos(targetPos.getX(), y, targetPos.getZ());
            if (world.getBlockState(pos).isSolid()) {
                return y + 0.25;
            }
        }

        return Double.MIN_VALUE;
    }

    private AbstractClientPlayerEntity findPlayer(ClientWorld world, String name) {
        if (world == null || world.getPlayers() == null) {
            return null;
        }

        for (AbstractClientPlayerEntity player : world.getPlayers()) {
            if (player != null && player.getGameProfile() != null
                    && player.getGameProfile().getName().equalsIgnoreCase(name)) {
                return player;
            }
        }
        return null;
    }

    @Override
    public String getShortDesc() {
        return "Телепорт к игроку";
    }

    @Override
    public List<String> getLongDesc() {
        return List.of(
                "Телепортирует к указанному игроку",
                "",
                "> tp <ник> — Телепортация к игроку",
                "Пример: > tp Notch"
        );
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) {
            return Stream.empty();
        }
        return world.getPlayers().stream()
                .map(p -> p.getGameProfile().getName())
                .distinct();
    }
}
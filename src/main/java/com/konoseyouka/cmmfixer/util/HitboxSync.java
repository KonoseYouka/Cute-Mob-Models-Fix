package com.konoseyouka.cmmfixer.util;

import com.konoseyouka.cmmfixer.CMMFix;
import com.konoseyouka.cmmfixer.CMMFixConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.server.ServerLifecycleHooks;
import java.util.*;
import java.util.function.Supplier;

public final class HitboxSync {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        CMMFix.resourceLocation("hitboxes"), () -> "6", "6"::equals, "6"::equals);

    private static Set<String> last = Set.of();
    public static Runnable clientReset = () -> {};
    private static final Map<UUID,Integer> awaitingPack = new HashMap<>();
    private static long sizeMessages, sizePayloadBytes;
    private static int resetGeneration;

    public static void init() {
        CHANNEL.registerMessage(
                0,
                Snapshot.class, Snapshot::encode, Snapshot::decode, Snapshot::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(
                1,
                SizePacket.class, SizePacket::encode, SizePacket::decode, SizePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(
                2,
                Reset.class, Reset::encode, Reset::decode, Reset::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(
                3,
                Ready.class, Ready::encode, Ready::decode, Ready::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static Set<String> activeServerModels() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null
                || !CMMFixConfig.SPEC.isLoaded()
                || !CMMFixConfig.HITBOXES.get())
            return Set.of();

        if (!server.isDedicatedServer()) return Hitboxes.clientModels;

        var configured = CMMFixConfig.SERVER_MODELS.get();
        return configured.isEmpty()
                ? BaselineDimensions.ids()
                : Set.copyOf(configured);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            awaitingPack.put(player.getUUID(), player.server.getTickCount() + 200);
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Snapshot(activeServerModels()));
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server=ServerLifecycleHooks.getCurrentServer();

        if (server != null) awaitingPack.entrySet().removeIf(entry -> {
            if (server.getTickCount() < entry.getValue()) return false;
            var player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null)
                player.connection.disconnect(
                        net.minecraft.network.chat.Component.literal(
                                "Required Cute Mob Models pack was not confirmed. Restart with Cute Mob Models Fix 1.1."));
            return true;
        });

        Set<String> active = activeServerModels();
        if (!active.equals(last)) {
            last = active;
            CHANNEL.send(PacketDistributor.ALL.noArg(), new Snapshot(active));
        }
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) {
        last = Set.of();
        awaitingPack.clear();
        sizeMessages = 0;
        sizePayloadBytes = 0;
        Hitboxes.serverModels = Set.of();
    }

    public static void sendSize(net.minecraft.world.entity.LivingEntity entity, ModelSize size) {
        sizeMessages++;
        sizePayloadBytes += size == null ? 17 : 49;
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), new SizePacket(entity.getUUID(), size));
    }

    @SubscribeEvent
    public static void tracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getTarget() instanceof net.minecraft.world.entity.LivingEntity entity) {
            // トラッキングは、エンティティのはじめの移動tickが発生する前に、サイズ拡大を許可しない。
            // 新しいエンティティはtick()またはsendSize()から、はじめてサイズ情報を受け取る。
            ModelSize size = Hitboxes.enforced(entity);
            if (size != null) {
                sizeMessages++;
                sizePayloadBytes += 49;
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SizePacket(entity.getUUID(), size));
            }
        }
    }

    @SubscribeEvent
    public static void commands(net.minecraftforge.event.RegisterCommandsEvent event) {
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("cmmfix")
                .requires(source -> source.hasPermission(2))
            .then(net.minecraft.commands.Commands.literal("remeasure")
                    .executes(ctx -> {
                CHANNEL.send(PacketDistributor.ALL.noArg(), new Reset(++resetGeneration));
                ctx.getSource().sendSuccess(
                        () -> net.minecraft.network.chat.Component.literal(
                                "Remeasurement requested." +
                                        "Existing collision sizes remain until new measurements are accepted." +
                                        "Dedicated servers continue using configured profiles."), true);
                return 1;
            }))
            .then(net.minecraft.commands.Commands.literal("status")
                    .executes(ctx -> {
                ctx.getSource().sendSuccess(
                        () -> net.minecraft.network.chat.Component.literal(
                                "CMM sizes: "+Hitboxes.observed.size() +
                                        " cached host samples; " + sizeMessages +
                                        " size messages / " + sizePayloadBytes +
                                        " payload bytes scheduled (before tracking fan-out and transport overhead)."), false);
                return 1;
            })));
    }

    public static void reportReady(boolean ready) {
        CHANNEL.sendToServer(
                new Ready(ready
                        ? com.konoseyouka.cmmfixer.pack.PackBootstrap.REVISION
                        : "missing"));
    }

    public record Ready(String revision) {
        void encode(FriendlyByteBuf b) { b.writeUtf(revision); }

        static Ready decode(FriendlyByteBuf b) { return new Ready(b.readUtf(64)); }

        static void handle(Ready packet, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                var player = ctx.get().getSender();
                if (player == null) return;

                if (!packet.revision().equals(com.konoseyouka.cmmfixer.pack.PackBootstrap.REVISION))
                    player.connection.disconnect(
                            net.minecraft.network.chat.Component.literal(
                                    "The required Cute Mob Models resource pack is not active."));
                else awaitingPack.remove(player.getUUID());
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public record Reset(int generation) {
        void encode(FriendlyByteBuf b) { b.writeVarInt(generation); }

        static Reset decode(FriendlyByteBuf b) { return new Reset(b.readVarInt()); }

        static void handle(Reset packet, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(clientReset);
            ctx.get().setPacketHandled(true);
        }
    }

    public record SizePacket(UUID uuid, ModelSize size) {
        void encode(FriendlyByteBuf b) {
            b.writeUUID(uuid);
            b.writeBoolean(size != null);

            if (size != null) {
                b.writeFloat(size.width()); b.writeFloat(size.height());
                b.writeFloat(size.minX()); b.writeFloat(size.minY()); b.writeFloat(size.minZ());
                b.writeFloat(size.maxX()); b.writeFloat(size.maxY()); b.writeFloat(size.maxZ());
            }
        }

        static SizePacket decode(FriendlyByteBuf b) {
            UUID id = b.readUUID();
            ModelSize size = b.readBoolean()
                    ? new ModelSize(
                            b.readFloat(), b.readFloat(),
                            b.readFloat(), b.readFloat(), b.readFloat(),
                            b.readFloat(), b.readFloat(), b.readFloat())
                    : null;

            if (size != null && !size.valid())
                throw new IllegalArgumentException("Invalid model dimensions");

            return new SizePacket(id, size);
        }

        static void handle(SizePacket message, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                if (message.size() == null) Hitboxes.received.remove(message.uuid());
                else Hitboxes.received.put(message.uuid(), message.size());
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public record Snapshot(Set<String> models) {
        void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(models.size());
            models.forEach(buffer::writeUtf);
        }

        static Snapshot decode(FriendlyByteBuf buffer) {
            int count = buffer.readVarInt();
            if (count < 0 || count > 4096) throw new IllegalArgumentException("Invalid CMM model count");

            Set<String> models = new HashSet<>();
            for (int i = 0; i < count; i++) models.add(buffer.readUtf(256));

            return new Snapshot(Set.copyOf(models));
        }

        static void handle(Snapshot message, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> Hitboxes.serverModels = message.models());
            context.get().setPacketHandled(true);
        }
    }

    private HitboxSync() {}
}

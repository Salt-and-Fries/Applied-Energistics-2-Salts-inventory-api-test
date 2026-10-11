package appeng.client.gui;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import appeng.core.network.ClientboundPacket;

/** Lifetime-addressed AE packet receivers. Contains no Salt API references. */
public final class DesktopContentRegistry {
    private static final Map<UUID, Consumer<ClientboundPacket>> RECEIVERS = new HashMap<>();
    private static final Map<UUID, ArrayDeque<PendingUpdate>> PENDING = new LinkedHashMap<>();
    private static final LinkedHashSet<UUID> CLOSED = new LinkedHashSet<>();

    private record PendingUpdate(ClientboundPacket packet, int bytes) {
    }

    private DesktopContentRegistry() {
    }

    public static void register(UUID owner, Consumer<ClientboundPacket> receiver) {
        if (RECEIVERS.putIfAbsent(owner, receiver) != null) {
            throw new IllegalStateException("Duplicate desktop menu identity");
        }
        var pending = PENDING.remove(owner);
        if (pending != null) {
            pending.forEach(update -> receiver.accept(update.packet()));
        }
    }

    public static void remove(UUID owner) {
        RECEIVERS.remove(owner);
        PENDING.remove(owner);
        CLOSED.add(owner);
        if (CLOSED.size() > 64) {
            CLOSED.remove(CLOSED.iterator().next());
        }
    }

    public static boolean receive(UUID owner, ClientboundPacket packet) {
        var receiver = RECEIVERS.get(owner);
        if (receiver == null) {
            return false;
        }
        receiver.accept(packet);
        return true;
    }

    /** Preserve bounded updates arriving before the opening snapshot creates the window state. */
    public static void awaitOpening(UUID owner, ClientboundPacket packet, int encodedBytes) {
        if (CLOSED.contains(owner)) {
            return;
        }
        if (!PENDING.containsKey(owner) && PENDING.size() >= 16) {
            PENDING.remove(PENDING.keySet().iterator().next());
        }
        var queue = PENDING.computeIfAbsent(owner, ignored -> new ArrayDeque<>());
        int bytes = queue.stream().mapToInt(PendingUpdate::bytes).sum();
        if (queue.size() >= 64 || bytes + encodedBytes > 4 * 1024 * 1024) {
            throw new IllegalStateException("AE desktop updates arrived before a window could be initialized");
        }
        queue.addLast(new PendingUpdate(packet, encodedBytes));
    }
}

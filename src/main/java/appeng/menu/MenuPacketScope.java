package appeng.menu;

import java.util.function.Supplier;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import appeng.core.network.ServerboundPacket;

/** Main-thread scope for reusable AE widgets. It contains no optional-mod types. */
public final class MenuPacketScope {
    private static final ThreadLocal<AEBaseMenu> CURRENT = new ThreadLocal<>();

    private MenuPacketScope() {
    }

    public static <T> T call(AEBaseMenu menu, Supplier<T> callback) {
        var previous = CURRENT.get();
        CURRENT.set(menu);
        try {
            return callback.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    public static void run(AEBaseMenu menu, Runnable callback) {
        call(menu, () -> {
            callback.run();
            return null;
        });
    }

    public static void send(ServerboundPacket packet) {
        var menu = CURRENT.get();
        if (menu == null) {
            ClientPacketDistributor.sendToServer(packet);
        } else {
            menu.sendPacketToServer(packet);
        }
    }
}

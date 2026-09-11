package net.susu609.royalarena.client.state;

import net.susu609.royalarena.network.s2c.S2CQueueStatusPacket;

public final class ClientQueueState {
    private static boolean searching;
    private static int position;
    private static String message = "Not searching";

    private ClientQueueState() {
    }

    public static void apply(S2CQueueStatusPacket packet) {
        searching = packet.searching();
        position = packet.position();
        message = packet.message();
    }

    public static void reset() {
        searching = false;
        position = 0;
        message = "Not searching";
    }

    public static boolean searching() {
        return searching;
    }
    public static int position() {
        return position;
    }
    public static String message() {
        return message;
    }
}

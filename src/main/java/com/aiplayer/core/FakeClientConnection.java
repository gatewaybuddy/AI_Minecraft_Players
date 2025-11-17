package com.aiplayer.core;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;

/**
 * Fake client connection for AI players.
 *
 * AI players don't have a real network connection, but the server's
 * PlayerManager expects one. This class provides a dummy connection
 * that doesn't actually send or receive packets.
 *
 * Based on the FakePlayer pattern used in various Minecraft mods.
 */
public class FakeClientConnection extends ClientConnection {

    private final AIPlayerEntity aiPlayer;

    public FakeClientConnection(AIPlayerEntity aiPlayer) {
        super(NetworkSide.SERVERBOUND);
        this.aiPlayer = aiPlayer;
    }

    @Override
    public void send(Packet<?> packet) {
        // AI players don't need to receive packets
        // Silently ignore
    }

    @Override
    public void send(Packet<?> packet, @Nullable PacketSendListener listener) {
        // AI players don't need to receive packets
        // Silently ignore
    }

    @Override
    public void disconnect(Text disconnectReason) {
        // AI players handle cleanup differently
        // Silently ignore
    }

    @Override
    public boolean isOpen() {
        // Always return true - AI players are always "connected"
        return true;
    }

    @Override
    public InetSocketAddress getAddress() {
        // Return a dummy address
        return new InetSocketAddress("127.0.0.1", 0);
    }

    @Override
    public void tick() {
        // No network operations needed
    }

    @Override
    public void setPacketListener(PacketListener listener) {
        // No packet handling needed
    }
}

package com.aiplayer.mixin;

import com.aiplayer.AIPlayerMod;
import com.aiplayer.core.AIPlayerEntity;
import com.aiplayer.core.AIPlayerManager;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to intercept chat messages and route them to AI players.
 */
@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("AIPlayer-ChatMixin");

    @Shadow
    public ServerPlayerEntity player;

    /**
     * Intercept chat messages and send to AI players.
     */
    @Inject(method = "handleDecoratedMessage", at = @At("TAIL"))
    private void onChatMessage(SignedMessage message, CallbackInfo ci) {
        try {
            String messageText = message.getContent().getString();
            LOGGER.debug("[CHAT-MIXIN] Intercepted message from {}: '{}'",
                player.getName().getString(), messageText);

            // Get AI player manager
            AIPlayerManager manager = AIPlayerMod.getPlayerManager();
            if (manager == null) {
                return;
            }

            // Send message to all AI players with chat systems
            for (AIPlayerEntity aiPlayer : manager.getAllPlayers()) {
                if (aiPlayer.getChatSystem() != null && aiPlayer.getChatSystem().isAvailable()) {
                    LOGGER.debug("[CHAT-MIXIN] Routing message to AI player: {}", aiPlayer.getName().getString());
                    aiPlayer.getChatSystem().onChatMessage(player, messageText);
                }
            }
        } catch (Exception e) {
            LOGGER.error("[CHAT-MIXIN] Error processing chat message", e);
        }
    }
}

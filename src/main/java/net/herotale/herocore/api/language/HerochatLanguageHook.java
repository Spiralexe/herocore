package net.herotale.herocore.api.language;

import java.util.UUID;

/**
 * Hook for Herochat integration.
 * Herochat should call these methods during message processing.
 */
public interface HerochatLanguageHook {
    
    /**
     * Process a message before it's sent to recipients.
     * Called in the message pipeline for each sender-receiver pair.
     * 
     * @param senderUuid UUID of the message sender
     * @param receiverUuid UUID of the message receiver (or null for broadcast)
     * @param originalMessage The original message text
     * @return The processed message (possibly distorted based on receiver's proficiency)
     */
    String processMessageForReceiver(UUID senderUuid, UUID receiverUuid, String originalMessage);
    
    /**
     * Check if the sender and receiver can understand each other's language.
     * Used for filtering recipient lists (e.g., faction channels).
     * 
     * @param senderUuid UUID of the sender
     * @param receiverUuid UUID of the receiver
     * @return true if the receiver can understand the sender's active language
     */
    boolean canReceiverUnderstand(UUID senderUuid, UUID receiverUuid);
}

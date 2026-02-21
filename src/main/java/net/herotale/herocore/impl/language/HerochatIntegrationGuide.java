package net.herotale.herocore.impl.language;

/**
 * Herochat Integration Guide
 * 
 * HeroCore provides a language system that integrates with Herochat via the HerochatLanguageHook.
 * 
 * INTEGRATION POINTS:
 * 
 * 1. **Message Processing Hook**
 *    When a player sends a message to a channel, call:
 *    
 *    ```java
 *    String originalMessage = ...;
 *    String processedMessage = herochatLanguageHook.processMessageForReceiver(
 *        senderUuid, receiverUuid, originalMessage
 *    );
 *    // Send processedMessage to receiver
 *    ```
 *    
 *    This distorts the message based on the receiver's proficiency in the sender's language.
 * 
 * 2. **Recipient Filtering (Optional)**
 *    To prevent faction channels from showing messages to hostile factions:
 *    
 *    ```java
 *    if (!herochatLanguageHook.canReceiverUnderstand(senderUuid, receiverUuid)) {
 *        // Don't add receiverUuid to recipient list
 *    }
 *    ```
 *    
 *    This is OPTIONAL — you can also show garbled text instead of hiding.
 * 
 * 3. **Herochat API Method Call Location**
 *    In Herochat's ChatListener.broadcastToChannel() or similar:
 *    
 *    ```java
 *    // Inside the loop for each recipient
 *    for (UUID recipient : channel.getMembers()) {
 *        String message = originalMessage;
 *        
 *        // Apply language distortion
 *        if (herochatLanguageHook != null) {
 *            message = herochatLanguageHook.processMessageForReceiver(sender, recipient, message);
 *        }
 *        
 *        // Send message to recipient...
 *    }
 *    ```
 * 
 * SETUP IN HEROCHAT:
 * 
 * 1. Add HeroCore as a dependency in manifest.json
 * 2. In Herochat.setup():
 *    ```java
 *    Herochat plugin = ... // your plugin instance
 *    HeroCore hc = (HeroCore) server.getPluginManager().getPlugin("Herocore");
 *    if (hc != null) {
 *        HerochatLanguageHook hook = hc.getLanguageService().getHerochatHook();
 *        plugin.getChatListener().setLanguageHook(hook);
 *    }
 *    ```
 * 3. In ChatListener, inject the hook and use it in message processing
 * 
 * PERFORMANCE NOTES:
 * 
 * - Language processing is pure function (no DB access)
 * - String distortion is very cheap (100-200us per message)
 * - Can be called per-recipient without performance concerns
 * - No async needed — purely synchronous
 */
public class HerochatIntegrationGuide {
}

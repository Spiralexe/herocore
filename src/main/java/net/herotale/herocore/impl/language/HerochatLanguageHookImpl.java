package net.herotale.herocore.impl.language;

import net.herotale.herocore.api.language.*;

import java.util.UUID;

/**
 * Implementation of HerochatLanguageHook.
 * Integrates with Herochat's message pipeline.
 */
public class HerochatLanguageHookImpl implements HerochatLanguageHook {
    
    private final LanguageService languageService;
    
    public HerochatLanguageHookImpl(LanguageService languageService) {
        this.languageService = languageService;
    }
    
    @Override
    public String processMessageForReceiver(UUID senderUuid, UUID receiverUuid, String originalMessage) {
        // Get sender's active language
        var activeLanguage = languageService.getActiveLanguage(senderUuid);
        
        if (activeLanguage.isEmpty()) {
            // No active language set, return original
            return originalMessage;
        }
        
        LanguageId senderLanguage = activeLanguage.get();
        
        // Get receiver's proficiency in that language
        int receiverProficiency = languageService.getProficiency(receiverUuid, senderLanguage);
        
        // Process the message
        return languageService.processMessage(originalMessage, senderLanguage, receiverProficiency);
    }
    
    @Override
    public boolean canReceiverUnderstand(UUID senderUuid, UUID receiverUuid) {
        var activeLanguage = languageService.getActiveLanguage(senderUuid);
        
        if (activeLanguage.isEmpty()) {
            // No language set, everyone can "understand" (it's not distorted)
            return true;
        }
        
        LanguageId senderLanguage = activeLanguage.get();
        
        // Receiver can understand if they have ANY proficiency in the sender's language
        // Or if they have perfect fluency (200)
        int receiverProficiency = languageService.getProficiency(receiverUuid, senderLanguage);
        return receiverProficiency > 0;
    }
}

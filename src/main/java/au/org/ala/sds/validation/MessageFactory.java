package au.org.ala.sds.validation;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import au.org.ala.sds.model.Message;
import au.org.ala.sds.model.SdsMessage;

/**
 * Message factory for creating formatted messages. The text of messages are
 * loaded as a ResourceBundle from a properties file. Lazy initialisation is
 * achieved using the holder class idiom.
 *
 * <p>
 * Message key constants are defined here.
 *
 * @author Peter Flemming
 */
public class MessageFactory {

	// Message key constants
    public static final String PLANT_PEST_MSG_CAT1_01 = "PPC1-01";
    public static final String PLANT_PEST_MSG_CAT1_02 = "PPC1-02";
    public static final String PLANT_PEST_MSG_CAT1_03 = "PPC1-03";
    public static final String PLANT_PEST_MSG_CAT1_04 = "PPC1-04";

    public static final String PLANT_PEST_MSG_CAT2_01 = "PPC2-01";
    public static final String PLANT_PEST_MSG_CAT2_02 = "PPC2-02";
    public static final String PLANT_PEST_MSG_CAT2_03 = "PPC2-03";
    public static final String PLANT_PEST_MSG_CAT2_04 = "PPC2-04";

    public static final String PLANT_PEST_MSG_CAT3_01 = "PPC3-01";
    public static final String PLANT_PEST_MSG_CAT3_02 = "PPC3-02";
    public static final String PLANT_PEST_MSG_CAT3_03 = "PPC3-03";
    public static final String PLANT_PEST_MSG_CAT3_04 = "PPC3-04";

    public static final String PLANT_PEST_MSG_CAT4_01 = "PPC4-01";
    public static final String PLANT_PEST_MSG_CAT4_02 = "PPC4-02";
    public static final String PLANT_PEST_MSG_CAT4_03 = "PPC4-03";
    public static final String PLANT_PEST_MSG_CAT4_04 = "PPC4-04";

    // Resource file is sds-messages.properties
    private static String BUNDLE_NAME = "sds-messages";

    // Holder class for lazy initialisation
    private static class MessagesHolder {
    	// Create PropertyResourceBundle from properties file
        public static ResourceBundle messages = ResourceBundle.getBundle(BUNDLE_NAME);
    }

    /**
     * Create a message using the message key to get the message text while substituting
     * any provided context parameters.
     *
     * @param type The message type.
     * @param messageKey The message key for the text definition.
     * @param context Varargs of context parameters.
     * @return The created message.
     */
    public static Message createMessage(Message.Type type, String messageKey, Object... context) {
        return new SdsMessage(type, getMessageText(messageKey, context));
    }

    /**
     * Get message text by keyed lookup on <code>ResourceBundle</code> and substituting
     * any provided parameters.  Parameter substitution is done with the
     * <code>MessageFormat</code> class, where parameter placeholders
     * are of the form {n} where n = 0..9 and parameters are passed as an array of
     * <code>Object</code>.
     *
     * <p>See <code>java.text.MessageFormat</code> for formatting options.
     *
     * @param messageKey The key that identifies the message.
     * @param context An array of <code>Object</code> containing substitution parameters. May be null.
     * @return The constructed message text.
     */
    private static String getMessageText(String messageKey, Object... context) {
        String msgTemplate;

        try {
        	msgTemplate = MessagesHolder.messages.getString(messageKey);
        } catch(MissingResourceException mre) {
            msgTemplate = "";
        }

        if (msgTemplate.equals("")) {
        	msgTemplate = "Message definition not found for supplied message key.";
        }

        return messageKey + " - " + MessageFormat.format(msgTemplate, context);
    }

    /**
     * Override default message bundle name.  For testing purposes only.
     *
     * @param MessageBundle
     */
    protected static void setMessageBundleName(String MessageBundle) {
    	BUNDLE_NAME = MessageBundle;
    }
}

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
    public static final String LOCATION_GENERALISED = "LocGen";
    public static final String LOCATION_NOT_GENERALISED = "LocNotGen";
    public static final String LOCATION_ALREADY_GENERALISED = "LocAlreadyGen";
    public static final String LOCATION_WITHHELD = "LocWithheld";

    public static final String LOCATION_INVALID = "LatLongInvalid";
    public static final String LOCATION_MISSING = "LatLongMissing";
    public static final String STATE_INVALID = "StateInvalid";
    public static final String NOT_AUSTRALIA = "NotAus";

    public static final String PLANT_PEST_MSG_CAT1_A0 = "PPC1";
    public static final String PLANT_PEST_MSG_CAT1_A1 = "PPC1-A1";
    public static final String PLANT_PEST_MSG_CAT1_A2 = "PPC1-A2";
    public static final String PLANT_PEST_MSG_CAT1_A3 = "PPC1-A3";
    public static final String PLANT_PEST_MSG_CAT1_A4 = "PPC1-A4";
    public static final String PLANT_PEST_MSG_CAT1_B1 = "PPC1-B1";
    public static final String PLANT_PEST_MSG_CAT1_D1 = "PPC1-D1";

    public static final String PLANT_PEST_MSG_CAT2_A1 = "PPC2-A1";
    public static final String PLANT_PEST_MSG_CAT2_B1 = "PPC2-B1";
    public static final String PLANT_PEST_MSG_CAT2_B2 = "PPC2-B2";

    public static final String PLANT_PEST_MSG_CAT3_A1 = "PPC3-A1";
    public static final String PLANT_PEST_MSG_CAT3_B1 = "PPC3-B1";
    public static final String PLANT_PEST_MSG_CAT3_B2 = "PPC3-B2";

    public static final String PLANT_PEST_MSG_CAT4_A1 = "PPC4-A1";
    public static final String PLANT_PEST_MSG_CAT4_B1 = "PPC4-B1";
    public static final String PLANT_PEST_MSG_CAT4_B2 = "PPC4-B2";
    public static final String PLANT_PEST_MSG_CAT4_B3 = "PPC4-B3";

    public static final String PLANT_PEST_MSG_CAT5A_A1 = "PPC5a-A1";
    public static final String PLANT_PEST_MSG_CAT5A_A2 = "PPC5a-A2";
    public static final String PLANT_PEST_MSG_CAT5A_B1 = "PPC5a-B1";
    public static final String PLANT_PEST_MSG_CAT5A_B2 = "PPC5a-B2";
    public static final String PLANT_PEST_MSG_CAT5A_C1 = "PPC5a-C1";
    public static final String PLANT_PEST_MSG_CAT5A_C2 = "PPC5a-C2";

    public static final String PLANT_PEST_MSG_CAT5B_A1 = "PPC5b-A1";
    public static final String PLANT_PEST_MSG_CAT5B_A2 = "PPC5b-A2";

    public static final String PLANT_PEST_MSG_CAT6_A1 = "PPC6-A1";
    public static final String PLANT_PEST_MSG_CAT6_A2 = "PPC6-A2";

    public static final String PLANT_PEST_MSG_CAT7 = "PBC7";
    public static final String PLANT_PEST_MSG_CAT8 = "PBC8";
    public static final String PLANT_PEST_MSG_CAT9 = "PBC9";
    public static final String PLANT_PEST_MSG_CAT10 = "PBC10";

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
        return new SdsMessage(type, getMessageText(messageKey, context) + ". (" + messageKey + ")", messageKey);
    }

    /**
     * Convenience methods.
     *
     * @param messageKey The message key for the text definition.
     * @param context Varargs of context parameters.
     * @return The created message.
     */
    public static Message createInfoMessage(String messageKey, Object... context) {
        return createMessage(Message.Type.INFO, messageKey, context);
    }

    public static Message createErrorMessage(String messageKey, Object... context) {
        return createMessage(Message.Type.ERROR, messageKey, context);
    }

    public static Message createAlertMessage(String messageKey, Object... context) {
        return createMessage(Message.Type.ALERT, messageKey, context);
    }

    public static Message createWarningMessage(String messageKey, Object... context) {
        return createMessage(Message.Type.WARNING, messageKey, context);
    }

    /**
     * Create a message using the message key to get the message text.
     * No context parameters are provided.
     *
     * @param type The message type.
     * @param messageKey The message key for the text definition.
     * @return The created message.
     */
    public static Message createMessage(Message.Type type, String messageKey) {
        return new SdsMessage(type, getMessageText(messageKey, new Object [] {}) + ". (" + messageKey + ")", messageKey);
    }

    /**
     * Convenience methods.
     *
     * @param type The message type.
     * @param messageKey The message key for the text definition.
     * @return The created message.
     */
    public static Message createInfoMessage(String messageKey) {
        return createMessage(Message.Type.INFO, messageKey);
    }

    public static Message createErrorMessage(String messageKey) {
        return createMessage(Message.Type.ERROR, messageKey);
    }

    public static Message createAlertMessage(String messageKey) {
        return createMessage(Message.Type.ALERT, messageKey);
    }

    public static Message createWarningMessage(String messageKey) {
        return createMessage(Message.Type.WARNING, messageKey);
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
     * @param context Varargs of context parameters.
     * @return The constructed message text.
     */
    public static String getMessageText(String messageKey, Object... context) {
        String msgTemplate;

        try {
        	msgTemplate = MessagesHolder.messages.getString(messageKey);
        } catch(MissingResourceException mre) {
            msgTemplate = "Message definition not found for supplied message key (" + messageKey + ")";
        }

        return MessageFormat.format(msgTemplate, context);
    }

    /**
     * Get message text by keyed lookup on <code>ResourceBundle</code>.
     *
     * @param messageKey The key that identifies the message.
     * @return The message text.
     */
    public static String getMessageText(String messageKey) {
        String msgTemplate;

        try {
            msgTemplate = MessagesHolder.messages.getString(messageKey);
        } catch(MissingResourceException mre) {
            msgTemplate = "Message definition not found for supplied message key (" + messageKey + ")";
        }

        return msgTemplate;
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

package au.org.ala.sds.validation;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import au.org.ala.sds.model.Message;


public class MessageFactoryTest {

    @Test
    public void createInfoMessages() {
        Message msg = MessageFactory.createMessage(Message.Type.INFO, MessageFactory.PLANT_PEST_MSG_CAT1_A1);
        assertNotNull(msg);

        msg = MessageFactory.createInfoMessage(MessageFactory.PLANT_PEST_MSG_CAT1_A1);
        assertNotNull(msg);
        System.out.println(msg);
    }
}

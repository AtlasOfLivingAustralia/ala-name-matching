package au.org.ala.sds.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.HashMap;

import org.junit.Test;

import au.org.ala.sds.validation.FactCollection;


public class DateHelperTest {

    @Test
    public void validateDateTest() {
        FactCollection facts = new FactCollection(new HashMap<String, String>() {
            {
                put(FactCollection.YEAR_KEY, "1846");
                put(FactCollection.DAY_KEY, "15");
                put(FactCollection.EVENT_DATE_KEY, "1954-01-29");
            }
        });

        Date date = DateHelper.validateDate(facts);
        assertEquals(DateHelper.formattedIso8601Date(date), "1846-01-01");

        facts.add(FactCollection.MONTH_KEY, "06");
        date = DateHelper.validateDate(facts);
        assertEquals(DateHelper.formattedIso8601Date(date), "1846-06-15");

        facts.add(FactCollection.YEAR_KEY, "");
        date = DateHelper.validateDate(facts);
        assertEquals(DateHelper.formattedIso8601Date(date), "1954-01-29");

        facts.add(FactCollection.EVENT_DATE_KEY, "");
        date = DateHelper.validateDate(facts);
        assertNull(date);
    }

    @Test
    public void parseDateTest() {
        Date date = DateHelper.parseDate("1846");
        assertEquals(DateHelper.formattedIso8601Date(date), "1846-01-01");

        date = DateHelper.parseDate("1905-06");
        assertEquals(DateHelper.formattedIso8601Date(date), "1905-06-01");
        String sdate="2011-11-31";
        try {
            date = DateHelper.parseDate(sdate);
            fail("parseDate() should have thrown an exception");
        } catch (Exception e) {
            assert(e instanceof IllegalArgumentException && e.getMessage().equals("Date " + sdate + " cannot be parsed"));
        }
    }

}

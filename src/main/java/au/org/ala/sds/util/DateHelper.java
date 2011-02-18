package au.org.ala.sds.util;

import java.text.ParseException;
import java.util.Date;

import org.apache.commons.lang.time.DateUtils;

public class DateHelper {

    public static Date parseDate(String date) {
        Date parsedDate = null;
        try {
            parsedDate = DateUtils.parseDateStrictly(date, new String[] {"yyyy-MM-dd"});
        } catch (ParseException pe) {
            throw new IllegalArgumentException("Date " + date + " cannot be parsed", pe);
        }
        return parsedDate;
    }
}

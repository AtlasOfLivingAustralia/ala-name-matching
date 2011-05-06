package au.org.ala.sds.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.time.DateUtils;

public class DateHelper {

    public static Date parseDate(String date) {
        Date parsedDate = null;
        try {
            parsedDate = DateUtils.parseDateStrictly(date, new String[] {"yyyy-MM-dd", "dd/mm/yy"});
        } catch (ParseException pe) {
            throw new IllegalArgumentException("Date " + date + " cannot be parsed", pe);
        }
        return parsedDate;
    }

    public static String formattedIso8601Date(Date date) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        return df.format(date);
    }

    public static String formattedNiceDate(Date date) {
        DateFormat df = new SimpleDateFormat("dd MMM yyyy");
        return df.format(date);
    }

    public static String getYear(Date date) {
        DateFormat df = new SimpleDateFormat("yyyy");
        return df.format(date);
    }

    public static boolean dateBefore(Date date, String dateStr) {
        return date.before(parseDate(dateStr));
    }

    public static boolean dateOnOrAfter(Date date, String dateStr) {
        Date refDate = parseDate(dateStr);
        return date.equals(refDate) || date.after(refDate);
    }
}

package au.org.ala.sds.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;

import au.org.ala.sds.validation.FactCollection;

public class DateHelper {

    public static Date parseDate(String date) {
        Date parsedDate = null;
        try {
            parsedDate = DateUtils.parseDateStrictly(date, new String[] {"yyyy-MM-dd", "yyyy", "yyyy-MM", "dd/mm/yy"});
        } catch (ParseException pe) {
            throw new IllegalArgumentException("Date " + date + " cannot be parsed", pe);
        }
        return parsedDate;
    }

    public static Date validateDate(FactCollection facts) {
        String year = facts.get(FactCollection.YEAR_KEY);
        String month = facts.get(FactCollection.MONTH_KEY);
        String day = facts.get(FactCollection.DAY_KEY);
        String eventDate = facts.get(FactCollection.EVENT_DATE_KEY);

        if (StringUtils.isNotBlank(year)) {
            String date = year;
            if (StringUtils.isNotBlank(month)) {
                date = date + "-" + month;
                if (StringUtils.isNotBlank(day)) {
                    date = date + "-" + day;
                }
            }
            return parseDate(date);
        } else {
            if (StringUtils.isNotBlank(eventDate)) {
                return parseDate(eventDate);
            } else {
                return null;
            }
        }
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

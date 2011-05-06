/**
 *
 */
package au.org.ala.sds.util;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class ExcelUtils {

    public static int getOrdinal(String value) {
        int ordinal = 0;
        for (int i = 0; i < value.length(); i++) {
            int charVal = value.charAt(value.length() - (i + 1)) - 'A' + 1;
            int exp = (int) Math.pow(26, i);
            ordinal = ordinal + exp * charVal;
        }

        return ordinal;
    }
}

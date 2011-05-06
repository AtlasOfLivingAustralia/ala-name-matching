/**
 *
 */
package au.org.ala.sds.dto;

import org.apache.commons.lang.StringUtils;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class DataStreamProperties {

    private final int startRow;
    private final int endRow;
    private final int sheetIndex;

    public DataStreamProperties(int startRow, int endRow, int sheetIndex) {
        super();
        this.startRow = startRow;
        this.endRow = endRow;
        this.sheetIndex = sheetIndex;
    }

    public DataStreamProperties(int startRow, int endRow) {
        super();
        this.startRow = startRow;
        this.endRow = endRow;
        this.sheetIndex = 0;
    }

    public int getSheetIndex() {
        return sheetIndex;
    }

    public int getStartRow() {
        return startRow;
    }

    public int getEndRow() {
        return endRow;
    }

    public static int validateStartRow(String row) {
        if (StringUtils.isBlank(row) || row.equals("0") || !StringUtils.isNumeric(row)) {
            return 1;
        } else {
            return Integer.parseInt(row);
        }
    }

    public static int validateEndRow(String row) {
        if (StringUtils.isBlank(row) || !StringUtils.isNumeric(row)) {
            return 0;
        } else {
            return Integer.parseInt(row);
        }
    }

    public static int validateSheetIndex(String index) {
        if (StringUtils.isBlank(index) || !StringUtils.isNumeric(index)) {
            return 0;
        } else {
            return Integer.parseInt(index);
        }
    }

}

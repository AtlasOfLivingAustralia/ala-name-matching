/**
 *
 */
package au.org.ala.sds.dao;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class DataStreamDaoFactory {

    public static DataStreamDao createDao(String fileName) throws Exception {
        return createDao(fileName, new FileInputStream(fileName));
    }

    public static DataStreamDao createDao(String fileName, InputStream inputStream) {
        if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) {
            return new DataStreamExcelDao(fileName, inputStream);
        } else if (fileName.endsWith(".csv")) {
            return new DataStreamCsvDao(fileName, inputStream);
        } else if (fileName.endsWith(".tsv")) {
            return new DataStreamCsvDao(fileName, inputStream, '\t');
        } else {
            throw new IllegalArgumentException("Data file extension not supported - use .xls, .xlsx, .csv or .tsv");
        }

    }
}

/**
 *
 */
package au.org.ala.sds.dao;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import au.org.ala.sds.dto.DataColumnMapper;
import au.org.ala.sds.dto.DataStreamProperties;
import au.org.ala.sds.util.DateHelper;
import au.org.ala.sds.util.ExcelUtils;
import au.org.ala.sds.validation.FactCollection;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class DataStreamExcelDao implements DataStreamDao {

    protected static final Logger logger = Logger.getLogger(DataStreamExcelDao.class);

    private final String fileName;
    private final InputStream stream;

    public DataStreamExcelDao(String fileName, InputStream stream) {
        this.fileName = fileName;
        this.stream = stream;
    }

    /**
     * @throws IOException
     *
     */
    @Override
    public void processStream(
        DataColumnMapper mapper,
        DataStreamProperties properties,
        DataRowHandler rowHandler) throws IOException {

        Workbook wb = null;
        if (fileName.endsWith(".xls")) {
            wb = new HSSFWorkbook(stream);
        } else if (fileName.endsWith(".xlsx")) {
            wb = new XSSFWorkbook(stream);
        } else {
            throw new IllegalStateException("File extension not supported");
        }

        Sheet sheet = wb.getSheetAt(properties.getSheetIndex() - 1);
        logger.debug("Reading Excel workbook = " + fileName);
        logger.debug("Sheet = " + sheet.getSheetName());

        Map<String, Integer> columnMap = new HashMap<String, Integer>();
        for (String key : mapper.getKeySet()) {
            String value = mapper.get(key);
            if (StringUtils.isNumeric(value)) {
                columnMap.put(key, Integer.parseInt(value) - 1);
            } else {
                columnMap.put(key, ExcelUtils.getOrdinal(value) - 1);
            }
        }

        int rowCount = 0;
        int start = properties.getStartRow() - 1;
        int end = properties.getEndRow() == 0 ? sheet.getLastRowNum() : properties.getEndRow() -1;
        for (int i = start; i <= end; i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                Map<String, String> facts = new HashMap<String, String>();
                for (String key : columnMap.keySet()) {
                    Cell cell = row.getCell(columnMap.get(key));
                    if (cell != null) {
                        switch (cell.getCellType()) {
                            case Cell.CELL_TYPE_STRING:
                                facts.put(key, cell.getStringCellValue().trim());
                                break;
                            case Cell.CELL_TYPE_NUMERIC:
                            case Cell.CELL_TYPE_FORMULA:
                                double d = cell.getNumericCellValue();
                                try {
                                    if (HSSFDateUtil.isCellDateFormatted(cell)) {
                                        facts.put(key, DateHelper.formattedIso8601Date(HSSFDateUtil.getJavaDate(d)));
                                    } else {
                                        facts.put(key, Double.toString(d));
                                    }
                                } catch (Exception e) {
                                    logger.warn("Exception caught trying to check if cell '" + key + "' is a date - " + e.getMessage());
                                    facts.put(key, Double.toString(d));
                                }
                                break;
                            default:
                                facts.put(key, "");
                                break;
                        }
                    }
                }
                if (!facts.isEmpty()) {
                    facts.put(FactCollection.ROW_KEY, Integer.toString(i + 1));
                    rowCount++;
                    rowHandler.handleRow(facts);
                }
            }
        }

        System.out.println("Total no of rows = " + rowCount);
    }

}

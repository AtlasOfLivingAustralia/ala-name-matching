package au.org.ala.sds;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;

import au.com.bytecode.opencsv.CSVReader;


public class FileInputTest {

    @Test
    public void readExcelWorkbook() throws IOException {
        String inputFileName = "/Users/peterflemming/Documents/workspaces/sds/sensitive-species/src/test/resources/workbook.xlsx";
        InputStream is = new FileInputStream(inputFileName);
        Workbook wb = null;
        if (inputFileName.endsWith(".xls")) {
            wb = new HSSFWorkbook(is);
        } else if (inputFileName.endsWith(".xlsx")) {
            wb = new XSSFWorkbook(is);
        }

        Sheet sheet = wb.getSheetAt(1);
        System.out.println("Read Excel worksheet test");
        System.out.println("Sheet - " + sheet.getSheetName());
        int rowCount = 0;
        for (Row row : sheet) {

            if (row.getRowNum() == 0) {
                System.out.println("Column headings,");
                for (Cell cell : row) {
                    System.out.println(cell.getStringCellValue() + " " + cell.getColumnIndex());
                }
            }
            rowCount++;
        }
        System.out.println("Total no of rows - " + rowCount);

    }

    @Test
    public void readCsvFile() throws IOException {
        String inputFileName = "/Users/peterflemming/Documents/workspaces/sds/sensitive-species/src/test/resources/sds-data.csv";
        CSVReader reader = new CSVReader(new FileReader(inputFileName));
        String [] row;
        int i;
        System.out.println("\nRead CSV data test");
        for (i = 0; (row = reader.readNext()) != null; i++) {
            if (i == 0) {
                System.out.println("Column headings,");
                for (String cell : row) {
                    System.out.println(cell);
                }
            }
        }
        System.out.println("Total no of rows - " + i);

    }

    @Test
    public void readTsvFile() throws IOException {
        String inputFileName = "/Users/peterflemming/Documents/workspaces/sds/sensitive-species/src/test/resources/workbook.txt";
        CSVReader reader = new CSVReader(new FileReader(inputFileName), '\t');
        String [] row;
        int i;
        System.out.println("\nRead TSV data test");
        for (i = 0; (row = reader.readNext()) != null; i++) {
            if (i == 0) {
                System.out.println("Column headings,");
                for (String cell : row) {
                    System.out.println(cell);
                }
            }
        }
        System.out.println("Total no of rows - " + i);

    }

}

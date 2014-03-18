package au.org.ala.sds.dao;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.sds.dto.DataColumnMapper;
import au.org.ala.sds.dto.DataStreamProperties;
import au.org.ala.sds.util.ExcelUtils;
import au.org.ala.sds.validation.FactCollection;

public class DataStreamCsvDao implements DataStreamDao {

    protected static final Logger logger = Logger.getLogger(DataStreamCsvDao.class);

    private final String fileName;
    private final InputStream stream;
    private char delimiter = ',';

    public DataStreamCsvDao(String fileName, InputStream stream) {
        this.fileName = fileName;
        this.stream = stream;
    }

    public DataStreamCsvDao(String fileName, InputStream stream, char delimiter) {
        this.fileName = fileName;
        this.stream = stream;
        this.delimiter = delimiter;
    }

    @Override
    public void processStream(
            DataColumnMapper mapper,
            DataStreamProperties properties,
            DataRowHandler rowHandler) throws IOException {

        CSVReader reader = new CSVReader(new BufferedReader(new InputStreamReader(stream)), delimiter);
        logger.debug("Reading data file - " + fileName);

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
        int start = properties.getStartRow();
        int end = properties.getEndRow() == 0 ? Integer.MAX_VALUE : properties.getEndRow();
        int i = 1;
        boolean eof = false;

        // position at start row
        while (i < start) {
            if (reader.readNext() == null) {
                eof = true;
                break;
            } else {
                i++;
            }
        }

        // read data rows
        while (!eof && i <= end) {
            String [] row;
            if ((row = reader.readNext()) == null) {
                eof = true;
            } else {
                Map<String, String> facts = new HashMap<String, String>();
                for (String key : columnMap.keySet()) {
                    Integer idx = columnMap.get(key);
                    if (idx < row.length) {
                        String value = row[idx].trim();
                        if (StringUtils.isNotBlank(value)) {
                            facts.put(key, value);
                        }
                    }
                }

                if (!facts.isEmpty()) {
                    facts.put(FactCollection.ROW_KEY, Integer.toString(i));
                    rowCount++;
                    rowHandler.handleRow(facts);
                }
            }
            i++;
        }

        System.out.println("Total no of rows = " + rowCount);
    }

}

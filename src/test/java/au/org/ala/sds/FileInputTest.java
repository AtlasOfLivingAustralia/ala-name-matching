package au.org.ala.sds;

import java.util.Map;

import au.org.ala.names.search.ALANameSearcher;
import org.apache.commons.lang.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import au.org.ala.names.search.ALANameIndexer;
import au.org.ala.sds.dao.DataRowHandler;
import au.org.ala.sds.dao.DataStreamDao;
import au.org.ala.sds.dao.DataStreamDaoFactory;
import au.org.ala.sds.dto.DataColumnMapper;
import au.org.ala.sds.dto.DataStreamProperties;
import au.org.ala.sds.model.Message;
import au.org.ala.sds.model.SensitiveTaxon;
import au.org.ala.sds.util.Configuration;
import au.org.ala.sds.validation.FactCollection;
import au.org.ala.sds.validation.ServiceFactory;
import au.org.ala.sds.validation.ValidationOutcome;
import au.org.ala.sds.validation.ValidationReport;
import au.org.ala.sds.validation.ValidationService;

public class FileInputTest {

    static ALANameSearcher nameSearcher;
    static SensitiveSpeciesFinder finder;

//    @BeforeClass
//    public static void runOnce() throws Exception {
//        cbIndexSearch = new CBIndexSearch(Configuration.getInstance().getNameMatchingIndex());
//        finder = SensitiveSpeciesFinderFactory.getSensitiveSpeciesFinder("file:///data/sds/sensitive-species.xml", cbIndexSearch);
//    }
//
//    @Test
//    public void readExcelXmlWorkbook() throws Exception {
//        String inputFileName = "/Users/peterflemming/Documents/workspaces/sds/sensitive-species/src/test/resources/workbook.xlsx";
//        DataStreamDao dao = DataStreamDaoFactory.createDao(inputFileName);
//        DataColumnMapper mapper = new DataColumnMapper();
//        mapper.add("family", "E");
//        mapper.add("genus", "F");
//        mapper.add("specificEpithet", "G");
//        mapper.add("intraspecificEpithet", "H");
//        mapper.add("municipality", "Q");
//        mapper.add("stateProvince", "R");
//        mapper.add("country", "S");
//        mapper.add("decimalLatitude", "T");
//        mapper.add("decimalLongitude", "U");
//        mapper.add("eventDate", "W");
//        mapper.add("year", "X");
//
//        DataStreamProperties properties = new DataStreamProperties(2, 11, 2);
//
//        dao.processStream(
//                mapper,
//                properties,
//                new DataRowHandler() {
//                    public void handleRow(Map<String, String> facts) {
//                        processRow(facts);
//                    }
//                });
//    }
//
//    @Test
//    public void readExcelWorkbook() throws Exception {
//        String inputFileName = "/Users/peterflemming/Documents/workspaces/sds/sensitive-species/src/test/resources/workbook.xls";
//        DataStreamDao dao = DataStreamDaoFactory.createDao(inputFileName);
//        DataColumnMapper mapper = new DataColumnMapper();
//        mapper.add("family", "E");
//        mapper.add("genus", "F");
//        mapper.add("specificEpithet", "G");
//        mapper.add("intraspecificEpithet", "H");
//        mapper.add("municipality", "Q");
//        mapper.add("stateProvince", "R");
//        mapper.add("country", "S");
//        mapper.add("decimalLatitude", "T");
//        mapper.add("decimalLongitude", "U");
//        mapper.add("eventDate", "W");
//        mapper.add("year", "X");
//
//        DataStreamProperties properties = new DataStreamProperties(2, 0, 3);
//
//        dao.processStream(
//                mapper,
//                properties,
//                new DataRowHandler() {
//                    public void handleRow(Map<String, String> facts) {
//                        processRow(facts);
//                    }
//                });
//    }

//    @Test
//    public void readCsvFile() throws Exception {
//        String inputFileName = "/Users/peterflemming/Documents/workspaces/sds/sensitive-species/src/test/resources/workbook.csv";
//        DataStreamDao dao = DataStreamDaoFactory.createDao(inputFileName);
//        DataColumnMapper mapper = new DataColumnMapper();
//        mapper.add("family", "5");
//        mapper.add("genus", "6");
//        mapper.add("specificEpithet", "7");
//        mapper.add("intraspecificEpithet", "8");
//        mapper.add("municipality", "17");
//        mapper.add("stateProvince", "18");
//        mapper.add("country", "19");
//        mapper.add("decimalLatitude", "20");
//        mapper.add("decimalLongitude", "21");
//        mapper.add("eventDate", "23");
//        mapper.add("year", "24");
//
//        DataStreamProperties properties = new DataStreamProperties(12, 12);
//
//        dao.processStream(
//                mapper,
//                properties,
//                new DataRowHandler() {
//                    public void handleRow(FactCollection facts) {
//                        processRow(facts);
//                    }
//                });
//    }

//    @Test
//    public void readTsvFile() throws Exception {
//        String inputFileName = "/Users/peterflemming/Documents/workspaces/sds/sensitive-species/src/test/resources/workbook.tsv";
//        DataStreamDao dao = DataStreamDaoFactory.createDao(inputFileName);
//        DataColumnMapper mapper = new DataColumnMapper();
//        mapper.add("family", "5");
//        mapper.add("genus", "6");
//        mapper.add("specificEpithet", "7");
//        mapper.add("intraspecificEpithet", "8");
//        mapper.add("municipality", "17");
//        mapper.add("stateProvince", "18");
//        mapper.add("country", "19");
//        mapper.add("decimalLatitude", "20");
//        mapper.add("decimalLongitude", "21");
//        mapper.add("eventDate", "23");
//        mapper.add("year", "24");
//
//        DataStreamProperties properties = new DataStreamProperties(2, 0);
//
//        dao.processStream(
//                mapper,
//                properties,
//                new DataRowHandler() {
//                    public void handleRow(FactCollection facts) {
//                        processRow(facts);
//                    }
//                });
//    }

    private void processRow(Map<String, String> facts) {
        System.out.println("Row data - " + facts);
        StringBuilder msgOut = new StringBuilder();
        String name = facts.get(FactCollection.GENUS_KEY) + " " + facts.get(FactCollection.SPECIFIC_EPITHET_KEY);
        if (StringUtils.isNotBlank(facts.get(FactCollection.INTRA_SPECIFIC_EPITHET_KEY))) {
            name = name + " " + facts.get(FactCollection.INTRA_SPECIFIC_EPITHET_KEY);
        }

        SensitiveTaxon st = finder.findSensitiveSpecies(name);
        if (st != null) {
            ValidationService service = ServiceFactory.createValidationService(st);
            ValidationOutcome outcome = service.validate(facts);

            msgOut.append(st.getTaxonName());
            if (st.getAcceptedName() != null && !name.equalsIgnoreCase(st.getAcceptedName())) {
                msgOut.append("  (Matched on " + st.getAcceptedName() + ")");
            }
            if (StringUtils.isNotBlank(st.getCommonName())) {
                msgOut.append(" [" + st.getCommonName() + "]");
            }
            if (StringUtils.isNotBlank(facts.get(FactCollection.ROW_KEY))) {
                msgOut.append(" (row ").append(facts.get(FactCollection.ROW_KEY)).append(")");
            }
            msgOut.append("\n");

            ValidationReport report = outcome.getReport();
            for (Message message : report.getMessages()) {
                msgOut.append("  ").append(message.getType()).append(" - ").append(message.getMessageText()).append("\n");
            }

        } else {
            msgOut.append("Species '" + name + "' (row " + facts.get(FactCollection.ROW_KEY) + ") not found in list of sensitive species.\n");
        }
        System.out.print(msgOut.toString());
    }

}

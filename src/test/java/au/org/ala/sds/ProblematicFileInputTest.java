package au.org.ala.sds;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import au.org.ala.names.search.ALANameSearcher;
import au.org.ala.sds.dao.DataRowHandler;
import au.org.ala.sds.dao.DataStreamDao;
import au.org.ala.sds.dao.DataStreamDaoFactory;
import au.org.ala.sds.dto.DataColumnMapper;
import au.org.ala.sds.dto.DataStreamProperties;
import au.org.ala.sds.model.SensitiveTaxon;
import au.org.ala.sds.util.Configuration;
import au.org.ala.sds.validation.FactCollection;
import au.org.ala.sds.validation.ServiceFactory;
import au.org.ala.sds.validation.ValidationOutcome;
import au.org.ala.sds.validation.ValidationService;

public class ProblematicFileInputTest {

    static ALANameSearcher nameSearcher;
    static SensitiveSpeciesFinder finder;

    //@BeforeClass
    public static void runOnce() throws Exception {
        nameSearcher = new ALANameSearcher(Configuration.getInstance().getNameMatchingIndex());
        finder = SensitiveSpeciesFinderFactory.getSensitiveSpeciesFinder("file:///data/sds/sensitive-species.xml", nameSearcher);
    }

   // @Test
    public void readExcelXmlWorkbook() throws Exception {
        String inputFileName = "/Users/peterflemming/Dropbox/SDS/SDStest1.xls";
        DataStreamDao dao = DataStreamDaoFactory.createDao(inputFileName);
        DataColumnMapper mapper = new DataColumnMapper();
        mapper.add("scientificName", "E");
        mapper.add("decimalLatitude", "H");
        mapper.add("decimalLongitude", "I");
        mapper.add("eventDate", "K");
        mapper.add("year", "K");

        DataStreamProperties properties = new DataStreamProperties(2, 0, 1);

        dao.processStream(
                mapper,
                properties,
                new DataRowHandler() {
                    public void handleRow(Map<String, String> facts) {
                        processRow(facts);
                    }
                });
    }

    private void processRow(Map<String, String> facts) {
        System.out.println("Row data - " + facts);
        StringBuilder msgOut = new StringBuilder();
        String name = facts.get(FactCollection.SCIENTIFIC_NAME_KEY);

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

        } else {
            msgOut.append("Species '" + name + "' (row " + facts.get(FactCollection.ROW_KEY) + ") not found in list of sensitive species.\n");
        }
        System.out.print(msgOut.toString());
    }

}

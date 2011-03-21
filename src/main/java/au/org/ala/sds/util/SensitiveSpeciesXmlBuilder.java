/**
 *
 */
package au.org.ala.sds.util;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import au.org.ala.sds.dao.SensitiveSpeciesDao;
import au.org.ala.sds.dao.SensitiveSpeciesMySqlDao;
import au.org.ala.sds.model.ConservationInstance;
import au.org.ala.sds.model.PlantPestInstance;
import au.org.ala.sds.model.SensitiveSpecies;
import au.org.ala.sds.model.SensitivityInstance;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class SensitiveSpeciesXmlBuilder {

    /**
     * @param args
     */
    public static void main(String[] args) {
        Document doc = new Document();
        Element root = new Element("sensitiveSpeciesList");
        doc.setRootElement(root);

        SensitiveSpeciesDao dao = null;
        try {
            dao = new SensitiveSpeciesMySqlDao(getDataSource());
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<SensitiveSpecies> species = dao.getAll();

        String currentName = "";
        Element sensitiveSpecies = null;
        for (SensitiveSpecies ss : species) {
            if (!ss.getScientificName().equalsIgnoreCase(currentName)) {
                sensitiveSpecies = new Element("sensitiveSpecies");
                sensitiveSpecies.setAttribute("name", ss.getScientificName());
                root.addContent(sensitiveSpecies);
                currentName = ss.getScientificName();
            }
            Element instances = new Element("instances");
            List<SensitivityInstance> sis = ss.getInstances();
            for (SensitivityInstance si : sis) {
                Element instance = null;
                if (si instanceof ConservationInstance) {
                    instance = new Element("conservationInstance");
                } else if (si instanceof PlantPestInstance) {
                    instance = new Element("plantPestInstance");
                }
                instance.setAttribute("category", si.getCategory().getValue());
                instance.setAttribute("authority", si.getAuthority());
                instance.setAttribute("zone", si.getZone().name());
                if (si instanceof ConservationInstance) {
                    instance.setAttribute("generalisation", ((ConservationInstance) si).getLocationGeneralisation());
                } else if (si instanceof PlantPestInstance) {
                    String fromDate = "";
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    if (((PlantPestInstance) si).getFromDate() != null) {
                        fromDate = dateFormat.format(((PlantPestInstance) si).getFromDate());
                    }
                    instance.setAttribute("fromDate", fromDate);

                    String toDate = "";
                    if (((PlantPestInstance) si).getToDate() != null) {
                        toDate = dateFormat.format(((PlantPestInstance) si).getToDate());
                    }
                    instance.setAttribute("toDate", toDate);
                }
                instances.addContent(instance);
            }
            sensitiveSpecies.addContent(instances);
        }

        XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
        try {
            xmlOutputter.output(doc, System.out);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static DataSource getDataSource() {
        DataSource dataSource = new BasicDataSource();
        ((BasicDataSource) dataSource).setDriverClassName("com.mysql.jdbc.Driver");
        ((BasicDataSource) dataSource).setUrl("jdbc:mysql://localhost/portal");
        ((BasicDataSource) dataSource).setUsername("root");
        ((BasicDataSource) dataSource).setPassword("password");
        return dataSource;
    }
}

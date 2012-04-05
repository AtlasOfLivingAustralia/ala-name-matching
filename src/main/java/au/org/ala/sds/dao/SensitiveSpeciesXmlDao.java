/**
 *
 */
package au.org.ala.sds.dao;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import au.org.ala.sds.model.ConservationInstance;
import au.org.ala.sds.model.PlantPestInstance;
import au.org.ala.sds.model.SensitiveTaxon;
import au.org.ala.sds.model.SensitivityCategoryFactory;
import au.org.ala.sds.model.SensitivityInstance;
import au.org.ala.sds.model.SensitivityZoneFactory;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class SensitiveSpeciesXmlDao implements SensitiveSpeciesDao {

    protected static final Logger logger = Logger.getLogger(SensitiveSpeciesXmlDao.class);

    private final InputStream stream;

    public SensitiveSpeciesXmlDao(InputStream stream) throws Exception {
        this.stream = stream;
    }
    /**
     * @throws IOException
     * @throws JDOMException
     * @see au.org.ala.sds.dao.SensitiveSpeciesDao#getAll()
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<SensitiveTaxon> getAll() throws Exception {
        List<SensitiveTaxon> species = new ArrayList<SensitiveTaxon>();
        SAXBuilder builder = new SAXBuilder();
        Document doc = null;

        doc = builder.build(this.stream);

        Element root = doc.getRootElement();
        List speciesList = root.getChildren();

        for (Iterator sli = speciesList.iterator(); sli.hasNext(); ) {
            Element sse = (Element) sli.next();
            String name = sse.getAttributeValue("name");
            String family = sse.getAttributeValue("family");
            SensitiveTaxon.Rank rank = SensitiveTaxon.Rank.valueOf(sse.getAttributeValue("rank"));
            String commonName = sse.getAttributeValue("commonName");
            SensitiveTaxon ss = new SensitiveTaxon(name, rank);
            ss.setFamily(family);
            ss.setCommonName(commonName);

            Element instances = sse.getChild("instances");
            List instanceList = instances.getChildren();

            for (Iterator ili = instanceList.iterator(); ili.hasNext(); ) {
                Element ie = (Element) ili.next();
                SensitivityInstance instance = null;
                if (ie.getName().equalsIgnoreCase("conservationInstance")) {
                    instance = new ConservationInstance(
                            SensitivityCategoryFactory.getCategory(ie.getAttributeValue("category")),
                            ie.getAttributeValue("authority"),
                            SensitivityZoneFactory.getZone(ie.getAttributeValue("zone")),
                            ie.getAttributeValue("reason"),
                            ie.getAttributeValue("remarks"),
                            ie.getAttributeValue("generalisation"));
                } else if (ie.getName().equalsIgnoreCase("plantPestInstance")) {
                    instance = new PlantPestInstance(
                            SensitivityCategoryFactory.getCategory(ie.getAttributeValue("category")),
                            ie.getAttributeValue("authority"),
                            SensitivityZoneFactory.getZone(ie.getAttributeValue("zone")),
                            ie.getAttributeValue("reason"),
                            ie.getAttributeValue("remarks"),
                            ie.getAttributeValue("fromDate"),
                            ie.getAttributeValue("toDate"));
                }
                ss.getInstances().add(instance);
            }
            species.add(ss);
        }

        // Sort list since MySQL sort order is not the same as Java's
        Collections.sort(species);

        return species;
    }

}

/**
 *
 */
package au.org.ala.sds.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.sql.DataSource;

import au.org.ala.names.parser.PhraseNameParser;
import au.org.ala.sds.model.*;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.gbif.ecat.model.ParsedName;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import au.org.ala.sds.dao.SensitiveSpeciesDao;
import au.org.ala.sds.dao.SensitiveSpeciesMySqlDao;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class SensitiveSpeciesXmlBuilder {
    final static Logger logger = Logger.getLogger(SensitiveSpeciesXmlBuilder.class);
    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        //generateFromDatabase();
        //System.out.println(generateFromWebservices(System.out, DateHelper.parseDate("2013-06-22T06:14:55Z")));
        if(args.length>0){
            if(args[0].equals("-db")){
                generateFromDatabase();
            } else if(args[0].equals("-ws")){
                OutputStream outputStream = args.length>1? new FileOutputStream(new java.io.File(args[1])):System.out;
                generateFromWebservices(outputStream,null);
            }
        } else {
            System.out.println("Usage: \n-db : build from database\n-ws : build from ws and write to standard out\n-ws <filename> build from ws and write to supplied file");
        }
    }

    private static boolean hasListsChanged(Collection<SDSSpeciesListDTO> lists, Date date){
        for(SDSSpeciesListDTO list : lists){
            if(list.getDateUpdated().after(date))
                return true;
        }
        return false;
    }

    /**
     * NC 2013-05-13:  Generate the XML list using webservice calls to the list app and collectory
     *
     * @param out the output stream to write the XML to
     * @param lastGenerateDate the date on which the last generation was performed.  The list will only be regenerated
     *                         if this is null or one of the lists have changes since the last generation
     * @return true when the list was generated correctly false otherwise
     */
    public static boolean generateFromWebservices(OutputStream out, Date lastGenerateDate){
        Document doc = new Document();
        Element root = new Element("sensitiveSpeciesList");
        doc.setRootElement(root);
        PhraseNameParser parser = new PhraseNameParser();
        //Step 0: Get all the lists that are considered "isSDS" so that we can get the global properties
        Map<String, SDSSpeciesListDTO> sdsLists = SpeciesListUtil.getSDSLists();
        //now check to see if there have been any changes to the lists since the last generated date before getting the items
        if(lastGenerateDate != null &&!hasListsChanged(sdsLists.values(), lastGenerateDate))
            return false;
        //Step 1: get all of the items that have a guid
        List<SDSSpeciesListItemDTO> guidItems = SpeciesListUtil.getSDSListItems(true);
        if(sdsLists.isEmpty() || guidItems == null || guidItems.isEmpty())
            return false;

        String currentGuid = "";
        List<String> resources= new ArrayList<String>();
        Element sensitiveSpecies = null;
        Element instances =null;

        for(SDSSpeciesListItemDTO item : guidItems){
            //if it si a new guid add a new sensitive species
            if(!currentGuid.equals(item.getGuid())){
                if(instances != null){
                    sensitiveSpecies.addContent(instances);
                }
                instances = new Element("instances");
                sensitiveSpecies = new Element("sensitiveSpecies");
                sensitiveSpecies.setAttribute("name", item.getName());
                sensitiveSpecies.setAttribute("family", item.getFamily() != null ? item.getFamily() : "");
                String rank ="UNKNOWN";
                try{

                    ParsedName<?> pn = parser.parse(item.getName());
                    if(pn != null && pn.getRank() != null) {
                        rank = pn.getRank().toString().toUpperCase();
                    }
                } catch(Exception e){
                    logger.error("Unable to get rank for " + item.getName(), e);
                }
                sensitiveSpecies.setAttribute("guid", item.getGuid());
                sensitiveSpecies.setAttribute("rank", rank);
                String commonName = item.getKVPValue(item.commonNameLabels);
                sensitiveSpecies.setAttribute("commonName", commonName != null ? commonName : "");
                //sensitiveSpecies.setAttribute("commonName", st.getCommonName() != null ? st.getCommonName() : "");
                root.addContent(sensitiveSpecies);
                currentGuid = item.getGuid();
                resources.clear();
            }
            //NQ 2014-03-14 - Ensure that each data resource only has one inclusion for each species
            if(!resources.contains(item.getDataResourceUid())){
                resources.add(item.getDataResourceUid());
                addInstanceInformation(sdsLists, item,instances);
            }
        }
        sensitiveSpecies.addContent(instances);
        //Step 2: get all the items that could NOT be matched to the current species list
        List<SDSSpeciesListItemDTO> unmatchedItems = SpeciesListUtil.getSDSListItems(false);
        String currentName = "";
        sensitiveSpecies = null;
        instances =null;
        resources.clear();
        for(SDSSpeciesListItemDTO item : unmatchedItems){
            //if it si a new guid add a new sensitive species
            if(!currentName.equals(item.getName())){
                if(instances != null && sensitiveSpecies != null){
                    sensitiveSpecies.addContent(instances);
                }
                instances = new Element("instances");
                sensitiveSpecies = new Element("sensitiveSpecies");
                sensitiveSpecies.setAttribute("name", item.getName());
                sensitiveSpecies.setAttribute("family", item.getFamily() != null ? item.getFamily() : "");
                String rank ="UNKNOWN";
                try{
                    rank = parser.parse(item.getName()).getRank().toString().toUpperCase();
                } catch(Exception e){
                    logger.error("Unable to get rank for " + item.getName(), e);
                }
                sensitiveSpecies.setAttribute("rank", rank);
                String commonName = item.getKVPValue(item.commonNameLabels);
                sensitiveSpecies.setAttribute("commonName", commonName != null ? commonName : "");
                //sensitiveSpecies.setAttribute("commonName", st.getCommonName() != null ? st.getCommonName() : "");
                root.addContent(sensitiveSpecies);
                currentName = item.getName();
                resources.clear();
            }
            if(!resources.contains(item.getDataResourceUid())){
                resources.add(item.getDataResourceUid());
                addInstanceInformation(sdsLists, item,instances);
            }
        }
        sensitiveSpecies.addContent(instances);

        XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
        try {
            xmlOutputter.output(doc, out);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private static void addInstanceInformation(Map<String,SDSSpeciesListDTO> sdsLists,SDSSpeciesListItemDTO item, Element instances){
        //now add the instance
        SDSSpeciesListDTO list = sdsLists.get(item.getDataResourceUid());
        boolean isConservation ="CONSERVATION".equals(list.getSdsType());
        boolean isStateBased = "PBC6".equals(list.getCategory());
        Element instance = isConservation?new Element("conservationInstance"):new Element("plantPestInstance");
        String category = item.getKVPValue("category");
        if(category == null){
            category = list.getCategory();
        }
        instance.setAttribute("category", category != null ? category : "");
        instance.setAttribute("authority", list.getAuthority() != null ? list.getAuthority() : "");
        instance.setAttribute("dataResourceId", list.getDataResourceUid());

        String reason = item.getKVPValue("reason");
        if (reason != null) {
            instance.setAttribute("reason", reason);
        }
        String remarks = item.getKVPValue("remarks");
        if (remarks != null) {
            instance.setAttribute("remarks", remarks);
        }
        if (isConservation) {
            String generalisation = item.getKVPValue("generalisation");
            if(generalisation == null) {
                generalisation = list.getGeneralisation();
            }
            instance.setAttribute("generalisation", generalisation);
        } else  {
            String fromDate = item.getKVPValue("fromDate");

            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            if (fromDate != null) {
                fromDate = dateFormat.format(fromDate);
            } else{
                fromDate="";
            }
            instance.setAttribute("fromDate", fromDate);

            String toDate = item.getKVPValue("toDate");
            if (toDate != null) {
                toDate = dateFormat.format(toDate);
            } else{
                toDate = "";
            }
            instance.setAttribute("toDate", toDate);
        }
        if(isStateBased && StringUtils.isBlank(list.getRegion())){
            //create multiple instance from this for each state that is enabled.
            //This is based on the State based being provided in one list with a checkbox for each state

            for(Map<String,String> kvp : item.getKvpValues()){
                SensitivityZone zone = SensitivityZoneFactory.getZone(kvp.get("key").toUpperCase());
                if(zone != null){
                    //now add a new instance for this state
                    //TODO work out where the state notification details are store and accessed - they may need to be properties of the data resource in which case each state will need to be a separate list.
                    Element newInstance = (Element)instance.clone();
                    newInstance.setAttribute("zone", zone.getId());
                    instances.addContent(newInstance);
                }
            }

        } else{
            //add the zone for the sensitive species
            instance.setAttribute("zone", list.getRegion() != null ? list.getRegion() : "AUS");
            instances.addContent(instance);
        }
    }

    public static void generateFromDatabase() throws Exception{
        Document doc = new Document();
        Element root = new Element("sensitiveSpeciesList");
        doc.setRootElement(root);

        SensitiveSpeciesDao dao = null;
        try {
            dao = new SensitiveSpeciesMySqlDao(getDataSource());
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<SensitiveTaxon> species = dao.getAll();
        Collections.sort(species);

        String currentName = "";
        Element sensitiveSpecies = null;
        for (SensitiveTaxon st : species) {
            if (!st.getTaxonName().equalsIgnoreCase(currentName)) {
                sensitiveSpecies = new Element("sensitiveSpecies");
                sensitiveSpecies.setAttribute("name", st.getTaxonName());
                sensitiveSpecies.setAttribute("family", st.getFamily());
                sensitiveSpecies.setAttribute("rank", st.getRank().name());
                sensitiveSpecies.setAttribute("commonName", st.getCommonName() != null ? st.getCommonName() : "");
                root.addContent(sensitiveSpecies);
                currentName = st.getTaxonName();
            }
            Element instances = new Element("instances");
            List<SensitivityInstance> sis = st.getInstances();
            for (SensitivityInstance si : sis) {
                Element instance = null;
                if (si instanceof ConservationInstance) {
                    instance = new Element("conservationInstance");
                } else if (si instanceof PlantPestInstance) {
                    instance = new Element("plantPestInstance");
                }
                instance.setAttribute("category", si.getCategory().getId());
                instance.setAttribute("authority", si.getAuthority());
                instance.setAttribute("dataResourceId", si.getDataResourceId());
                instance.setAttribute("zone", si.getZone().getId());
                if (si.getReason() != null) {
                    instance.setAttribute("reason", si.getReason());
                }
                if (si.getRemarks() != null) {
                    instance.setAttribute("remarks", si.getRemarks());
                }
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

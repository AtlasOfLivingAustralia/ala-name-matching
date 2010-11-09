/***************************************************************************
 * Copyright (C) 2010 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 ***************************************************************************/
package au.org.ala.checklist;
import au.org.ala.data.util.RankType;
import gnu.trove.TIntObjectHashMap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbcp.BasicDataSource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.gbif.checklistbank.utils.SqlUtils;
import org.postgresql.PGConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Exports a tab delimited file of the content of the name_usage table.
 * Optionally exporting a denormalised version.
 *
 * @author Natasha Carter
 */
public class ChecklistBankExporter {
    private final int pageSize = 10000;
    protected Log log = LogFactory.getLog(ChecklistBankExporter.class);
    protected ApplicationContext context;
    protected BasicDataSource dataSource;
    protected JdbcTemplate dTemplate;
    //Use view
    //private String denormaliseSql = "SELECT * from ala_dwc_classification where id >? order by id limit ?";
    //Use materialised view (tmp table) - should speed up queries
    private String denormaliseSql = "SELECT * from tmp_export_name_usage where id >? order by id limit ?";
    //private String rankSQL = "SELECT min(portal_rank) FROM term_gbif_portal_rank WHERE term_fk =? ";
    private String identifierSql = "SELECT identifier, checklist_fk FROM tmp_identifiers WHERE lexical_group_fk =? and name_fk = ? ORDER BY id";
    private String rankMapSql = "SELECT term_fk, portal_rank FROM term_gbif_portal_rank ORDER BY term_fk, portal_rank";
    private String nameLexicalSql = "SELECT COALESCE(ns.canonical_name_fk, ns.id) as name_fk, lexical_group_fk from name_usage nu JOIN name_string ns ON nu.name_fk = ns.id where nu.id = ?";
    private String lexGroupSql = "copy ( select nu.id,ns.scientific_name from name_usage nu JOIN name_in_lexical_group nlg ON nu.lexical_group_fk = nlg.lexical_group_fk JOIN name_string ns on nlg.name_fk = ns.id where checklist_fk = 1 order by nu.id ) TO STDOUT WITH NULL '' ";
    private String nameSql = "copy (select * from export_ala_taxon_name) TO STDOUT WITH NULL '' ";
    private OutputStreamWriter fileOut;
    private FileOutputStream idFileOut, lexOut, nameOut;
    private int nameCounter;
    private String nullString = ""; //"\\N";
    private HashMap<Integer, Integer> rankMappings;
    private TIntObjectHashMap<String> idToLsidMap = new TIntObjectHashMap<String>();
    private List<Integer> exportRanks = new ArrayList<Integer>();
    private PGConnection pgCon;
    
    /**
     * Intialise DB connections and set up mappings.
     */
	private void init(String fileName) throws Exception {
		        exportRanks.add(25);
        exportRanks.add(75);
        exportRanks.add(150);
        exportRanks.add(225);
        exportRanks.add(325);
        exportRanks.add(350);
        exportRanks.add(425);
        exportRanks.add(450);
        exportRanks.add(600);
        exportRanks.add(475);
        exportRanks.add(650);
        exportRanks.add(700);
        exportRanks.add(750);
        exportRanks.add(625);
        exportRanks.add(675);
        exportRanks.add(875);
        exportRanks.add(825);
               
		log.info("Initialising DB connections and output files...");
		String[] locations = {"classpath*:au/org/ala/**/applicationContext-cb*.xml"};
		context = new ClassPathXmlApplicationContext(locations);
		dataSource = (BasicDataSource) context.getBean("cbDataSource");
		dTemplate = new JdbcTemplate(dataSource);


		
        if(fileName != null){

            File exportFile = new File(fileName);
            exportFile.mkdirs();
            if(exportFile.exists()){
            	FileUtils.forceDelete(exportFile);
            }
            fileOut = new OutputStreamWriter(new FileOutputStream(exportFile), "UTF-8");
            log.info("Exporting to " + fileName);

            String directory = exportFile.getParent();
            File exportIdsFile = new File(directory+File.separator+"cb_identifiers.txt");
            if(exportIdsFile.exists()){
            	FileUtils.forceDelete(exportIdsFile);
            }
            idFileOut = new FileOutputStream(exportIdsFile);
            log.info("Exporting identifiers to " + exportIdsFile);

            File lexFile = new File(directory + File.separator +"cb_lex_names.txt");
            if(lexFile.exists())
                FileUtils.forceDelete(lexFile);
            lexOut = FileUtils.openOutputStream(lexFile);
            File nameFile = new File(directory + File.separator + "cb_name_string.txt");
            if(nameFile.exists())
                FileUtils.forceDelete(nameFile);
            nameOut = FileUtils.openOutputStream(nameFile);
            nameCounter = 0;

        }
        log.info("Initialised.");
	}
	
    private String replaceNull(String in){
        return StringUtils.isEmpty(in) ? nullString : in;
    }
    private String quoteReplaceNull(String in){
        return quoteField(replaceNull(in));
    }
    /**
     * runs the script to set up the supporting items
     * @throws Exception
     */
    public void setUpDatabase() throws Exception{
        //run the checklist_bank_model_additions_ala.sql script
        long start = System.currentTimeMillis();
        InputStream script = this.getClass().getClassLoader().getResourceAsStream("au/org/ala/db/checklist_bank_model_additions_ala.sql");

        if(script != null){
            log.info("Creating the necessary items to export checklist.");
            SqlUtils.executeSqlScript(dataSource.getConnection(), script);
            log.info("Finished setup in " + (System.currentTimeMillis() - start) + " ms");
        }
    }
    private boolean isExportRank(Integer cbRank){
        return exportRanks.contains(cbRank);
    }


    /**
     * export the name_usage items from the checklist bank in a tab delimitted file
     * @param checklistId
     * @param useCanonicalName
     * @param idOrderPreference
     * @throws Exception
     */
    public long export(int checklistId, boolean denormalise, boolean major)throws Exception {//, int[] idOrderPreference) throws Exception{

          //initialise the port mappings
            rankMappings = new HashMap<Integer,Integer>();
            List<Map<String, Object>> ranks = dTemplate.queryForList(rankMapSql);
            for(Map<String, Object> rank: ranks){
                Integer rankfk = (Integer)rank.get("term_fk");
                Integer portal = (Integer)rank.get("portal_rank");
                if(!rankMappings.containsKey(rankfk))
                    rankMappings.put(rankfk, portal);
            }

        //get a list of the id, parent_id, name_fk, scientific names
        long startTime = System.currentTimeMillis();
        log.info("Starting export for checklist " + checklistId);
        Connection conn = dataSource.getConnection();
        //String query = denormalise ? denormaliseSql: normaliseSql;
        String query =denormaliseSql;
        //System.out.println(query);
        java.sql.PreparedStatement stmt = conn.prepareStatement(query,ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        boolean hasMore = true;
        int min = 0;
        //insert the header line for the file
        fileOut.write("nub id\tparent nub id\tlsid\taccepted id\taccepted lsid\tsci_name_id\tcanonical_name id\tscientific_name\tcanonical name\tauthor\tportal rank id\trank\tlft\trgt\tkingdom id\tkingdom\tphylum id\tphylum\tclass id\tclass\torder id\torder\tfamily id\tfamily\tgenus id\tgenus\tspecies id\tspecies\tsource\n");
        //perform a paged export of the name_usage table
        while(hasMore){
            int numProcessed =0;
            stmt.setInt(1, min);
            stmt.setInt(2, pageSize);
            long localStart = System.currentTimeMillis();
            ResultSet rs = stmt.executeQuery();
            log.info("Finished executing query in " + (System.currentTimeMillis() - localStart) + " ms");

            while (rs.next()) {
                Integer id = rs.getInt("id");
                Integer cbNubRankId = rs.getInt("rank_fk");
                //Integer portalRankId = rankMappings.get(cbNubRankId);
                //Export all records except for the "incertae sedis" (id = 9)

                if(id != 9 && (!major || isExportRank(cbNubRankId))){
                    min = id;
                    String parentFk = replaceNull(rs.getString("parent_fk"));
                    //remove all the parentFKs that refer back to the "incertae sedis" record.
                   // parentFk = parentFk.equals("9")?nullString:parentFk; this is handled by the view now
                    Integer nameFk = rs.getInt("name_fk");
                    Integer lexicalGroupFk = rs.getInt("lexical_group_fk");

                    //now get the value for the scientific name
                    //String scientificName = getScientificName(nameFk, useCanonicalName);
                    String sciNameID = rs.getString("sci_name_id");
                    String canId = rs.getString("can_id");
                    String canonicalName = rs.getString("canonical_name");
                    String scientificName = rs.getString("scientific_name");
                    //String portalRank = rs.getString("portal_rank");
                    
                    String rank = replaceNull(rs.getString("rank"));
                    boolean isSynonym = rs.getBoolean("is_synonym");
                    
                    String acceptedId = nullString;
                    String acceptedLsid = nullString;
                   
   

                    String portalRank = cbNubRankId == null ? nullString : getRank(cbNubRankId);

   
                    //now lookup the appropriate LSID from the identifier table
                    String[] lsidAndClid = getLSID(id,Integer.parseInt(canId), lexicalGroupFk);
                    String lsid = lsidAndClid[0];
                    String source = getSource(lsidAndClid[1]);
                    if(isSynonym && !parentFk.equals(nullString)){
                        //when the records is a synonym the parent id represents the taxon concept that it is a synonym of.
                    //when the parentFk is "9" then we can not identify the record as a synonym (this is handled from the view
                        if(!parentFk.equals(nullString)){
                            int pid = Integer.parseInt(parentFk);
                            acceptedId = parentFk;
                            parentFk = nullString;
                            acceptedLsid = getLSID(pid);
                        }
                    }

   

                    String line = "\""+id +"\"\t"+quoteField(parentFk) +"\t"+quoteField(lsid) +"\t" + quoteField(acceptedId) + "\t" + quoteField(acceptedLsid)
                            +"\t"+ quoteField(sciNameID)+"\t"+quoteField(canId)+"\t"+ quoteField(scientificName) +"\t" + quoteField(canonicalName) + "\t"
                            + quoteReplaceNull(rs.getString("authorship"))+"\t" + quoteField(portalRank) +"\t" +quoteField(rank) + "\t" + quoteReplaceNull(rs.getString("lft")) + "\t" + quoteReplaceNull(rs.getString("rgt"));
                    if(denormalise){

                        line+= "\t" + quoteReplaceNull(rs.getString("kingdom_fk")) +"\t" + quoteReplaceNull(rs.getString("kingdom"))+
                                "\t" + quoteReplaceNull(rs.getString("phylum_fk")) + "\t" + quoteReplaceNull(rs.getString("phylum"))+
                                "\t"+ quoteReplaceNull(rs.getString("class_fk")) +"\t" + quoteReplaceNull(rs.getString("class"))+
                                "\t"+quoteReplaceNull(rs.getString("order_fk"))+ "\t" + quoteReplaceNull(rs.getString("order"))+
                                "\t"+ quoteReplaceNull(rs.getString("family_fk")) + "\t" + quoteReplaceNull(rs.getString("family"))+
                                "\t" + quoteReplaceNull(rs.getString("genus_fk")) + "\t" + quoteReplaceNull(rs.getString("genus"))+
                                "\t" + quoteReplaceNull(rs.getString("species_fk")) + "\t" + quoteReplaceNull(rs.getString("species"));
                    }
                    line+="\t" +quoteReplaceNull(source)+ "\n";

                    nameCounter++;
                    fileOut.write(line);
                }
                if (nameCounter % 10000 == 0) {
                    log.info("Exported " + nameCounter + " in " + (System.currentTimeMillis() - startTime) + " ms");
                }
                numProcessed++;
            }
            hasMore = numProcessed>=pageSize;
        }
        
        long timeTaken = System.currentTimeMillis() - startTime;
        log.info("Finished exporting " + nameCounter+" names in  " + (System.currentTimeMillis() - startTime)+ " ms.");
        fileOut.flush();
        fileOut.close();
        idFileOut.flush();
        idFileOut.close();
        exportLexicalNames();
        exportNames();
        return timeTaken;
    }
    private String quoteField(String value){
        //escape the embedded quote characters with a / character
        return "\"" + value.replaceAll("\"", "/\"") + "\"";
    }
    /**
     * Converts the checklist id into a
     * source string to be used in the export file.
     *
     * When the taxon concept was automatically derived
     * by CB from a binomial/trinomial etc the source will be
     * empty.
     *
     * @param id
     * @return
     */
    private String getSource(String id){
        try{
        switch(Integer.parseInt(id)){
            case 1001:
                return "AFD";
            case 1002:
                return "APC";
            case 1003:
                return "APNI";
            case 1004:
                return "CoL";
            default:
                return nullString;
        }
        }
        catch(NumberFormatException e){
            return nullString;
        }
    }
    /**
     * Exports the all the names that belong to the same lexical group for each id.
     * Uses a bulk export method 
     *
     * This export file is used to create the lucene index used in the name matching API
     * 
     * @throws Exception
     */
    private void exportLexicalNames() throws Exception{
        long startTime = System.currentTimeMillis();
        log.info("Exporting the lexical names...");
        PGConnection con = getPGConnection();
        con.getCopyAPI().copyOut(lexGroupSql,lexOut);
        lexOut.flush();
        lexOut.close();
        log.info("Finished exporting the lexical names in " + (System.currentTimeMillis() - startTime) + " ms");
    }
    private PGConnection getPGConnection() throws Exception{
        if(pgCon == null){
            pgCon = (PGConnection)DriverManager.getConnection(dataSource.getUrl(), dataSource.getUsername(),dataSource.getPassword());
        }
        return pgCon;
    }
    /**
     * Exports the names in the name_string table that are associated with the NUB checklist
     * @throws Exception
     */
    private void exportNames() throws Exception{
        long startTime = System.currentTimeMillis();
        log.info("Exporting the name strings ....");
        PGConnection con = getPGConnection();
        con.getCopyAPI().copyOut(nameSql, nameOut);
        nameOut.flush();
        nameOut.close();
        log.info("Finished exporting the name strings in " + (System.currentTimeMillis() - startTime) + " ms");
    }
   
    /**
     * Retrieve a string value for a rank.
     * 
     * @param id
     * @return
     */
    private String getRank(Integer id) {
        Integer rank = rankMappings.get(id);
        if(rank != null)
            return Integer.toString(rankMappings.get(id));
        log.warn("A mapping for rank [" + id + "] does not exist.");
        return nullString;
        //return dTemplate.queryForInt(rankSQL, new Object[]{id});
    }
    /**
     * Returns the LSID for the specified cb id.
     * @param cbId
     * @return
     */
    private String getLSID(int cbId) throws Exception{
        //check to see if the LSID already exists
//        if(idToLsidMap.contains(cbId))
//            return idToLsidMap.get(cbId);
        //else we need to get the nameFK and lexGrpFK for the cbId
        Map<String,Object> cbusage = dTemplate.queryForMap(nameLexicalSql, new Object[]{cbId});
        
        return getLSID((Integer)cbusage.get("name_fk"), (Integer)cbusage.get("lexical_group_fk"))[0];
    }

    private String[] getLSID(int cbId, Integer nameFK, Integer lexGrpFK) throws Exception{
        //check to see if the LSID already exists
//        if(idToLsidMap.contains(cbId))
//            return idToLsidMap.get(cbId);
        return getLSID(nameFK, lexGrpFK);
    }
    /**
     * Gets the identifier for the name based on order of preference for the source of the id.
     * 
     * DM - AFD, APC and APNI identifiers take precedence over other GUIDs. Where we have an AFD, APC or APNI
     * identifier, we should return this and then serialise other GUIDs to the secondary identifiers file. (NC
     * This is already handled in the view)
     *
     * 
     * APC identifiers *should* take precedence over APNI identifiers.
     * 
     * FIXME We need to separate APC LSIDs from APNI into a separate checklist on import. We can then use
     * the associated checklist_fk on the tmp_identifiers view to allow us to select which should be used.
     * 
	 * 1001 | Australian Faunal Directory
	 * 1002 | Australian Plant Census
     * 1003 | Australian Plant Names Index
     * 1004 | Catalogue of Life
     * 
     * @param id the id of the name usage to export
     * @param nameFK
     * @param clOrderPref
     * @return
     * @throws Exception
     */
    private String[] getLSID(Integer nameFK, Integer lexGrpFK) throws Exception{

        //List<String> identifiers = dTemplate.queryForList(identifierSql, new Object[]{lexGrpFK, nameFK, new Integer(lsidType)}, String.class);
        List<Map<String,Object>> identifiers = dTemplate.queryForList(identifierSql, new Object[]{lexGrpFK, nameFK});
        //just take the value of the first one for now (it should be the one that has come from the checklist with the highest priority)
        String[] preferredLsid = null;
        if (identifiers != null && identifiers.size() > 0) {
            for (Map<String, Object> identifier : identifiers) {
                String guid = (String) identifier.get("identifier");
                guid = StringUtils.trimToNull(guid);
                if (preferredLsid == null) {
                    preferredLsid = new String[]{replaceNull(guid), identifier.get("checklist_fk").toString()};
                } else {
                    //add it to the identifiers file
                    if (guid != null) {
                        idFileOut.write((nameFK + "\t" + preferredLsid[0] + "\t" + guid + "\n").getBytes());
                    }
                }
            }
            /* THE CODE BELOW SHOULD NOT BE NECESSARY because the view is ordering the
             * LSIDs by AFP, APC,APNI then CoL.
             * Thus the first LSID should be the correct one.
             */

//        	//select AFD and APC first
//        	List<Integer> preferredIds = new ArrayList<Integer>();
//        	preferredIds.add(new Integer(1001));
//        	preferredIds.add(new Integer(1002));
//        	String[] preferredLsid = getPreferredGuid(identifiers, preferredIds);
//
//    		//allow APNI if we havent found an AFD, or APC GUID
//        	if(preferredLsid==null){
//        		preferredIds.clear();
//        		preferredIds.add(new Integer(1003));
//        		preferredLsid = getPreferredGuid(identifiers, preferredIds);
//        	}
//
//        	//select the first one if still null
//        	if(preferredLsid==null){
//	    		//if this hasnt been set, choose the first one
//	    		Map<String,Object> result = (Map<String,Object>) identifiers.get(0);
//	    		preferredLsid = new String[]{(String) result.get("identifier"), result.get("checklist_fk").toString()};
//        	}

//        	for(Map<String, Object> identifier : identifiers){
//                //remove the first record and then process the other records into the additional identifiers file
//        		String guid = (String) identifier.get("identifier");
//        		guid = StringUtils.trimToNull(guid);
//
//        		if(guid!=null && preferredLsid[0]!=null && !guid.equals(preferredLsid[0]) ){
//        			idFileOut.write((nameFK + "\t" + preferredLsid[0] + "\t" + guid + "\n").getBytes());
//        		}
//            }
            
            return preferredLsid;
        }
//        else{
//            System.out.println("The following name and lexical group does not have any identifiers: "+ nameFK + " " + lexGrpFK);
//        }
        return new String[]{nullString, nullString};
    }

    /**
     * Retrieve a preferred GUID using the preferred list of checklists.
     * 
     * @param identifiers
     * @param preferredChecklistIds
     * @return
     */
	private String[] getPreferredGuid(List<Map<String, Object>> identifiers, List<Integer> preferredChecklistIds) {
		if(identifiers.size()>1){
			//chose the LSID provided by AFD or APC
			for(Map<String, Object> identifier : identifiers){
				Integer checklistId = (Integer) identifier.get("checklist_fk");
				if(preferredChecklistIds.contains(checklistId)){
					String guid =  (String) identifier.get("identifier");
					guid = StringUtils.trimToNull(guid);
					if(guid!=null){
						return new String[]{replaceNull(guid), checklistId.toString()};
					}
				}
			}
		}
		return null;
	}

    /**
     * Runner for the checklistbank exporter. Includes two options:
     * 
     * <ul>
     * <li> -norm : avoids running denormalisation SQL</li>
     * <li> -noset : avoids running the database setup</li>
     * <li> -major : only export the major ranks </li>
     * </ul>
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args)throws Exception{

        ChecklistBankExporter cbe = new ChecklistBankExporter();
        
        if(args.length ==0){
            //DEFAULT options assume that the database has already been setup
            System.out.println("Using default options...");
            cbe.init("/data/checklistbank/exported/cb_name_usages.txt");
            cbe.setUpDatabase();
            cbe.export(1, true, false);
        }
        else if(args.length >=1){
            //filename is the first argument
            String filename = args[0];
            cbe.init(filename);

            boolean denormalise = true;
            boolean setup = true;
            boolean major = false;
            //the remaining arguments indicate whether or not to use canonical and denormalised structure
            for(int i = 1; i<args.length;i++){
                String arg = args[i];

                if(arg.equals("-norm"))
                    denormalise = false;
                else if(arg.equals("-noset"))
                    setup = false;
                else if(arg.equals("-major"))
                    major = true;
            }
            //set up the database if necessary
            if(setup)
                cbe.setUpDatabase();
            cbe.export(1,  denormalise, major);//, clbPriorities);
        }
        else {
            //System.out.println("ChecklistBankExporter <filename> <comma separated list of checklist priority> [-norm]");
            System.out.println("ChecklistBankExporter <filename> [-norm] [-noset]");
        }
        System.exit(1);
    }
}

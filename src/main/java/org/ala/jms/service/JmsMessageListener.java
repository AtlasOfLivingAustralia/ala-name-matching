/**************************************************************************
 *  Copyright (C) 2011 Atlas of Living Australia
 *  All Rights Reserved.
 *
 *  The contents of this file are subject to the Mozilla Public
 *  License Version 1.1 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of
 *  the License at http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  rights and limitations under the License.
 ***************************************************************************/

package org.ala.jms.service;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.ala.jms.dto.CitizenScience;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import antlr.StringUtils;
import au.org.ala.biocache.FullRecord;
import au.org.ala.biocache.Store;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ActiveMq Listerner 
 * 
 * @author mok011
 */
public class JmsMessageListener implements MessageListener {
	private static final Logger logger = Logger.getLogger(JmsMessageListener.class);
	public static final String MESSAGE_METHOD = "messageMethod";

	protected ObjectMapper mapper = new ObjectMapper();

	public static final String CITIZEN_SCIENCE_DRUID = "dr364";
	public static final List<String> ID_LIST = new ArrayList<String>();
	public List<Map<String,String>> upsertList;
	public List<String> deleteList;
	private long lastMessage=0;
	private int secondsBeforeBatch =5;
	private int batchSize=100;

	private boolean hasAssociatedMedia = true;
	
	public enum Method {
		CREATE, UPDATE, DELETE
	}

	public JmsMessageListener() {
		// initialise the object mapper
		mapper.getDeserializationConfig().set(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES,	false);
		ID_LIST.add("OccurrenceID");
		upsertList = new ArrayList<Map<String,String>>();
		deleteList = new ArrayList<String>();		
		new BatchThread().start();
	}

	public boolean isHasAssociatedMedia() {
		return hasAssociatedMedia;
	}

	public void setHasAssociatedMedia(boolean hasAssociatedMedia) {
		this.hasAssociatedMedia = hasAssociatedMedia;
	}
	
	
	/**
     * @return the secondsBeforeBatch
     */
    public int getSecondsBeforeBatch() {
        return secondsBeforeBatch;
    }

    /**
     * @param secondsBeforeBatch the secondsBeforeBatch to set
     */
    public void setSecondsBeforeBatch(int secondsBeforeBatch) {
        this.secondsBeforeBatch = secondsBeforeBatch;
    }

    /**
     * @return the batchSize
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * @param batchSize the batchSize to set
     */
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    /**
	 * Implementation of <code>MessageListener</code>.
	 * 
	 * JSON Message:
	 * ==============
	 * {
	 * "guid" : "urn:lsid:cs.ala.org.au:Record:51",
	 * "scientificName" : "Trichoglossus haematodus eyrei",
	 * "vernacularName" : "Rainbow Lorikeet",
	 * "locality" : "Emmet Yaraka Rd, Isisford QLD 4731, Australia",
	 * "decimalLatitude" : "-24.729292",
	 * "decimalLongitude" : "144.234375",
	 * "individualCount" : "345",
	 * "coordinateUncertaintyInMeters" : "222.0",
	 * "occurrenceRemarks" : "rwe",
	 * "eventDate" : "2011-07-11",
	 * "eventTime" : "13:50:00EST",
	 * "taxonID" : "urn:lsid:biodiversity.org.au:afd.taxon:13a00712-95cb-475b-88a5-f1c7917e10e3",
	 * "family" : "Psittacidae",
	 * "kingdom" : "Animalia",
	 * "associatedMedia" : ["http://cs.ala.org.au/bdrs-ala/files/download.htm?className=au.com.gaiaresources.bdrs.model.taxa.AttributeValue&id=63&fileName=Argentina.gif"]
	 * }
	 */
	public void onMessage(Message message) {
    	String occId = "";
    	String json = "";
    	Method messageMethod = null;    	
        logger.info("Message received from the queue...");
        logger.debug(message.toString());
        
        try {
        	if(message.getStringProperty(MESSAGE_METHOD) != null && !"".equals(message.getStringProperty(MESSAGE_METHOD))){
        		messageMethod = Method.valueOf(message.getStringProperty(MESSAGE_METHOD));	
        		//CitizenScience sighting = null;
        		Map<String,String> map = new java.util.HashMap<String,String>();
        		Map<String,Object>omap = null;
	            if (message instanceof TextMessage) {
	                lastMessage = System.currentTimeMillis();
	            	// prepare data
	                TextMessage tm = (TextMessage)message;
	                json = tm.getText();	                
	                if(json != null && !"".equals(json)){
	                    logger.debug("creating map : " + json);
	                    try{	                        
	                        omap = mapper.readValue(json, Map.class);
	                        
	                        for(String key: omap.keySet()){
	                            if("associatedMedia".equals(key)){
	                                if(hasAssociatedMedia){
    	                                Object value = omap.get(key);
    	                                String newValue = org.apache.commons.lang.StringUtils.join((List)value, ";");
    	                                map.put("associatedMedia", newValue);
	                                }
	                            }
	                            else if("userID".equals(key)){
	                                map.put("recordedBy", omap.get(key).toString());
	                            }
	                            else if("guid".equals(key)){
	                                map.put("occurrenceID", omap.get(key).toString());
	                            }
	                            else{
	                                map.put(key, omap.get(key).toString());
	                            }
	                        }
	                        
	                    }
	                    catch(Exception e){
	                        e.printStackTrace();
	                    }
	                    catch(Error e){
	                        e.printStackTrace();
	                    }
	                    logger.debug("finished creating map " + map);
	                	//sighting = (CitizenScience) mapper.readValue(json, CitizenScience.class);
	                }
	                else{
	                	logger.error("Empty Json Message!  Method: " + message.getStringProperty(MESSAGE_METHOD));
	                	return;
	                }
	                
	                //process request
	                switch(messageMethod){
	                	case CREATE:
	                	case UPDATE:
	                		//addUpdateOccRecord(sighting);
	                	    if(map != null && map.size()>0){	                	        
	                            synchronized(upsertList){
	                                upsertList.add(map);
	                            }
	                        }
	                		break;
	                		
	                	case DELETE:
	                	    if(map != null){
	                	        occId = CITIZEN_SCIENCE_DRUID + "|" + map.get("OccurrenceID");
	                	        synchronized(deleteList){
	                	            deleteList.add(occId);
	                	        }
	                    	//deleteOccRecord(occId);
	                	    }
	                		break;
	                		
	                	default:
	                		logger.error("Invalid method! Method: " + message.getStringProperty(MESSAGE_METHOD) + ".  json= " + json); 
	                		break;
	                }
	                logger.debug("Method = " + message.getStringProperty(MESSAGE_METHOD) + " : Processed message " + json);  
	            }
        	}
        	else{
        		logger.error("Invalid method! Method: " + message.getStringProperty(MESSAGE_METHOD));
        	}
        } 
        catch (Exception e) {
        	logger.error("Error Message: " + json + " Method :"  + messageMethod);
            logger.error(e.getMessage(), e);
        }
    }
	/**
	 * 
	 * @param sighting
	 * @return
	 * @deprecated Batch loading is based on Maps rather than FullRecords.
	 */
	@Deprecated
	private FullRecord populateFullRecord(CitizenScience sighting){
    	FullRecord fullRecord = new FullRecord();
    	if(sighting == null){
    		return fullRecord;
    	}

    	fullRecord.getOccurrence().setOccurrenceID(sighting.getGuid());
    	fullRecord.getOccurrence().setRecordedBy(sighting.getUserID());
    	fullRecord.getClassification().setKingdom(sighting.getKingdom());
    	fullRecord.getClassification().setFamily(sighting.getFamily());
    	fullRecord.getClassification().setScientificName(sighting.getScientificName());
    	fullRecord.getClassification().setVernacularName(sighting.getVernacularName());
    	fullRecord.getClassification().setTaxonConceptID(sighting.getTaxonConceptGuid());
    	
    	if(isHasAssociatedMedia() && sighting.getAssociatedMedia() != null){
    		StringBuffer urls = new StringBuffer();
    		for(int i = 0; i < sighting.getAssociatedMedia().length; i++){               
                //download the media
    			urls.append(sighting.getAssociatedMedia()[i]);
    			if(i < (sighting.getAssociatedMedia().length - 1)){
    				urls.append("; ");
    			}
    		}
    		fullRecord.occurrence().setAssociatedMedia(urls.toString());
    	}
    	fullRecord.occurrence().setIndividualCount("" + sighting.getIndividualCount());
    	fullRecord.occurrence().setOccurrenceRemarks(sighting.getOccurrenceRemarks());
    	
     	fullRecord.event().setEventDate(sighting.getEventDate());
    	fullRecord.event().setEventTime(sighting.getEventTime());
    	
    	fullRecord.location().setDecimalLatitude("" + sighting.getDecimalLatitude());
    	fullRecord.location().setDecimalLongitude("" + sighting.getDecimalLongitude());
    	fullRecord.location().setLocality(sighting.getLocality());
    	if(sighting.getCoordinateUncertaintyInMeters() != null){
    		fullRecord.location().setCoordinateUncertaintyInMeters("" + sighting.getCoordinateUncertaintyInMeters());
    	}    	
     	
    	fullRecord.attribution().setDataResourceUid(CITIZEN_SCIENCE_DRUID);
    	    	        	
    	return fullRecord;
	}
	/**
	 * 
	 * @param sighting
	 * @deprecated Batch processing occurs on List of Maps
	 */
	@Deprecated
	private void addUpdateOccRecord(CitizenScience sighting) {
		FullRecord fullRecord = populateFullRecord(sighting);
        List<String> identifyingTerms = new ArrayList<String>();
        identifyingTerms.add(sighting.getGuid());
		Store.loadRecord(CITIZEN_SCIENCE_DRUID, fullRecord, identifyingTerms, true);
	}
	/**
	 * 
	 * @param occId
	 * @deprecated Batch deleting occurs on a list of 
	 */
	@Deprecated	
	private void deleteOccRecord(String occId) {
		Store.deleteRecord(occId);
	}
	
	private class BatchThread extends Thread {
	    public void run(){
	        while(true){
    	        long current = System.currentTimeMillis();
    	        if(lastMessage != 0 && ((current-lastMessage)/1000 >secondsBeforeBatch || upsertList.size() == batchSize || deleteList.size() == batchSize)){
    	            //send the batch off to the biocache-store
    	            logger.debug("Sending " + upsertList.size() + " records for update and " + deleteList.size() + " records to be deleted.");
    	            synchronized(upsertList){
        	            if(upsertList.size()>0){
        	                Store.loadRecords(CITIZEN_SCIENCE_DRUID, upsertList, ID_LIST, true);
        	                upsertList.clear();
        	            }
    	            }
    	            synchronized(deleteList){
        	            if(deleteList.size()>0){
        	                //delete the list of records...
        	                Store.deleteRecords(deleteList);
        	                deleteList.clear();
        	            }
    	            }
    	            lastMessage=0;
    	        }
    	        try{
//    	            logger.debug("Sleeping to wait for a batch");
    	        sleep(secondsBeforeBatch*1000);
    	        }
    	        catch(Exception e){
    	            //don't care if we are interrupted.
    	        }
	        }
	    }
	}
}

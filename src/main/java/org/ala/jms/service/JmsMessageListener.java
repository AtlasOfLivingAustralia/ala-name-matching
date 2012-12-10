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

import org.ala.jms.dto.Sighting;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import au.org.ala.biocache.FullRecord;
import au.org.ala.biocache.Store;
import org.codehaus.jackson.type.TypeReference;

import java.util.*;

/**
 * JMS listener for CRUD operations on sightings supplied by citizen science projects
 * such as the BDRS.
 * 
 * @author mok011
 */
public class JmsMessageListener implements MessageListener {

    private static final Logger logger = Logger.getLogger(JmsMessageListener.class);
    public static final String MESSAGE_METHOD = "messageMethod";
    public static final String TEST_MESSAGE = "testMessage";
	protected ObjectMapper mapper = new ObjectMapper();
	public static final List<String> ID_LIST = new ArrayList<String>();
	public Map<String,List<Map<String,String>>> upsertList;
	public List<String> deleteList;
	protected long lastMessage = 0;
	protected int secondsBeforeBatch = 5;
	protected int batchSize = 100;
    /**
     * The default data resource Uid to use if not supplied in the original
     * message. Note this is configurable with spring config.
     */
    protected String defaultDataResourceUid = "dr364";

    protected boolean hasAssociatedMedia = true;
	
	public enum Method { CREATE, UPDATE, DELETE }

	public JmsMessageListener() {
		// initialise the object mapper
        logger.info("JMS Listener initialising....");
		mapper.getDeserializationConfig().set(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES,	false);
		ID_LIST.add("occurrenceID");
		upsertList = new HashMap<String,List<Map<String,String>>>();
		deleteList = new ArrayList<String>();
        logger.info("JMS Listener about to start batch thread....");
		new BatchThread().start();
        logger.info("JMS Listener initialised.");
	}

    private int getListSize(Map<String,List<Map<String,String>>> listToProcess){
        int count = 0;
        Iterator it = listToProcess.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String,List<Map<String,String>>> resourceList = (Map.Entry) it.next();
            count += resourceList.getValue().size();
        }
        return count;
    }

    public Method getMethod(Message message){
        try {
            logger.debug("Message type: " + message.getClass().getName() + ", TextMessage: " + (message instanceof TextMessage));
            if( message.getStringProperty(MESSAGE_METHOD) != null && !"".equals(message.getStringProperty(MESSAGE_METHOD))){
                return Method.valueOf(message.getStringProperty(MESSAGE_METHOD));
            }
            if(message instanceof TextMessage){
                //parse the message
                TextMessage tm = (TextMessage) message;
                String json = tm.getText();
                if(StringUtils.isNotEmpty(json)){
                    Map<String,Object> omap = mapper.readValue(json, new TypeReference<HashMap<String,Object>>(){});
                    String messageMethod = (String) omap.get("messageMethod");
                    if(StringUtils.isNotEmpty(messageMethod)){
                        return Method.valueOf(messageMethod);
                    }
                }
            }
        } catch (Exception e){
            e.printStackTrace();
            logger.error(e.getMessage(),e);
        }
        return null;
    }

    /**
	 * Implementation of <code>MessageListener</code>.
	 * 
	 * JSON Message:
	 * ==============
	 * {
	 * "guid" : "urn:lsid:cs.ala.org.au:Record:51",
     * "dataResourceUid" : "dr364",
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

    	Method messageMethod = getMethod(message);
        logger.info("Message received from the queue..." + messageMethod);
        logger.debug(message.toString());
        
        try {
        	if(messageMethod != null){
        		Map<String,String> map = new java.util.HashMap<String,String>();

	            if (message instanceof TextMessage) {
	                lastMessage = System.currentTimeMillis();
	            	// prepare data
	                TextMessage tm = (TextMessage) message;
	                json = tm.getText();	                
	                if(json != null && !"".equals(json)){
	                    logger.debug("creating map : " + json);
	                    try{	                        
	                        Map<String,Object> omap  = mapper.readValue(json, new TypeReference<HashMap<String,Object>>(){});
	                        for(String key: omap.keySet()){
	                            Object value = omap.get(key);
	                            if("associatedMedia".equalsIgnoreCase(key)){
	                                if(hasAssociatedMedia && value != null){    	                                
    	                                String newValue = org.apache.commons.lang.StringUtils.join((List)value, ";");
    	                                map.put("associatedMedia", newValue);
	                                }
	                            }
//	                            else if("userID".equalsIgnoreCase(key)){
//	                                if(value != null)
//	                                    map.put("recordedBy", value.toString());
//	                            }
	                            else if("guid".equalsIgnoreCase(key)){
	                                if(value != null)
	                                    map.put("occurrenceID", value.toString());
	                            }
	                            else if(value != null){
	                                map.put(key, omap.get(key).toString());
	                            }
	                        }
	                    } catch(Exception e){
	                        logger.error(e.getMessage(),e);
	                    } catch(Throwable e){
                            e.printStackTrace();
	                        logger.error(e.getMessage(),e);
	                    }
	                    logger.debug("finished creating map " + map);

	                } else {
	                	logger.error("Empty Json Message!  Method: " + message.getStringProperty(MESSAGE_METHOD));
	                	return;
	                }

                    if(map.get(TEST_MESSAGE) != null){
                        //log it and return.
                        logger.info("Test message received. Will not proceed with commit to biocache");
                        return;
                    }

                    //remove transport info from payload if supplied
                    map.remove("messageMethod");

	                //process request
	                switch(messageMethod){
	                	case CREATE:
	                	    createOrUpdate(map);
	                		break;
	                	case UPDATE:
	                	    createOrUpdate(map);
	                		break;
	                	case DELETE:
	                	    if(map != null){
                                //TODO deletes for when the data resource UID is supplied
	                	        occId = getDefaultDataResourceUid() + "|" + map.get("occurrenceID");
                                logger.info("Delete request received for ID: " + occId);
	                	        synchronized(deleteList){
	                	            deleteList.add(occId);
	                	        }
	                	    }
	                		break;
	                	default:
	                		logger.error("Invalid method! Method: " + message.getStringProperty(MESSAGE_METHOD) + ".  json= " + json); 
	                		break;
	                }
	                logger.debug("Method = " + messageMethod + " : Processed message " + json);
	            }
        	} else {
        		logger.error("Invalid method! Method: " + messageMethod);
        	}
        } catch (Exception e) {
        	logger.error("Error processing message: " + json + " Method :"  + messageMethod);
            logger.error(e.getMessage(), e);
        }
    }

    private void createOrUpdate(Map<String, String> map){
        if(map != null && !map.isEmpty()){
            synchronized(upsertList){
                String dataResourceUid = getDataResourceUidForSighting(map);
                List<Map<String,String>> recordList = upsertList.get(dataResourceUid);
                if(recordList == null){
                   recordList = new ArrayList<Map<String, String>>();
                }
                recordList.add(map);
                upsertList.put(dataResourceUid, recordList);
                if(logger.isDebugEnabled()){
                    logger.debug("Message added to queue for CREATE: " + dataResourceUid + ", current size: " + upsertList.get(dataResourceUid).size());
                }
            }
        } else {
            logger.info("Empty map supplied.");
        }
    }

	/**
	 * 
	 * @param sighting
	 * @return
	 * @deprecated Batch loading is based on Maps rather than FullRecords.
	 */
	@Deprecated
	private FullRecord populateFullRecord(Sighting sighting){
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

        if(sighting.getGeodeticDatum() != null)
            fullRecord.location().setGeodeticDatum(sighting.getGeodeticDatum());

    	fullRecord.location().setLocality(sighting.getLocality());
    	if(sighting.getCoordinateUncertaintyInMeters() != null){
    		fullRecord.location().setCoordinateUncertaintyInMeters("" + sighting.getCoordinateUncertaintyInMeters());
    	}    	
     	
    	fullRecord.attribution().setDataResourceUid(getDataResourceUidForSighting(sighting));
    	    	        	
    	return fullRecord;
	}

    protected String getDataResourceUidForSighting(Map<String,String> propertyMap){
        if(propertyMap.get("dataResourceUid") !=null){
          return (String) propertyMap.get("dataResourceUid");
        }
        return defaultDataResourceUid;
    }

    protected String getDataResourceUidForSighting(Sighting sighting){
        if(sighting.getDataResourceUid() !=null){
          return sighting.getDataResourceUid();
        }
        return defaultDataResourceUid;
    }

	/**
	 * 
	 * @param sighting
	 * @deprecated Batch processing occurs on List of Maps
	 */
	@Deprecated
	private void addUpdateOccRecord(Sighting sighting) {
		FullRecord fullRecord = populateFullRecord(sighting);
        List<String> identifyingTerms = new ArrayList<String>();
        identifyingTerms.add(sighting.getGuid());
		Store.loadRecord(getDataResourceUidForSighting(sighting), fullRecord, identifyingTerms, true);
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

        private long lastCheck = 0;


	    public void run(){
            logger.info("JMS Listener starting batch thread....");

	        while(true){
                long now = System.currentTimeMillis();
                if(lastCheck == 0 || now - lastCheck > 60000){
                    logger.debug("Last message: " + lastMessage + ", Upsert size " + getListSize(upsertList) + ", delete size " + deleteList.size());
                    lastCheck = now;
                }

    	        long current = System.currentTimeMillis();
    	        if(lastMessage != 0 && ((current-lastMessage)/1000 > secondsBeforeBatch || getListSize(upsertList) >= batchSize || deleteList.size() >= batchSize)){
    	            //send the batch off to the biocache-store
    	            logger.debug("Sending " + getListSize(upsertList) + " records for update and " + deleteList.size() + " records to be deleted.");
    	            synchronized(upsertList){
        	            if(!upsertList.isEmpty()){
        	                try{
                                Iterator it = upsertList.entrySet().iterator();
                                while (it.hasNext()) {
                                    Map.Entry<String,List<Map<String,String>>> resourceList = (Map.Entry) it.next();
                                    logger.debug("Sending records for resource:" + resourceList.getKey() + " records for update and " + deleteList.size() + " records to be deleted.");
                                    Store.loadRecords(resourceList.getKey(), resourceList.getValue(), ID_LIST, true);
                                    it.remove(); // avoids a ConcurrentModificationException
                                }
                                upsertList.clear();
                                lastMessage = 0;
        	                }
        	                catch(Exception e){
        	                    //leave the upsert list identical 
        	                    logger.error("Error loading CS recsords",e);
        	                }
        	            }
    	            }
    	            synchronized(deleteList){
        	            if(!deleteList.isEmpty()){
        	                //delete the list of records...
        	                try{
        	                    Store.deleteRecords(deleteList);
        	                    deleteList.clear();
        	                    lastMessage = 0;
        	                }
        	                catch(Exception e){
        	                    //leave the delete list identical
        	                    logger.error("Error deleting CS records,", e);
        	                }
        	            }
    	            }
    	        }

    	        try {
//    	            logger.debug("Sleeping to wait for a batch");
    	            sleep(secondsBeforeBatch*1000);
    	        }
    	        catch(Exception e){
    	            //don't care if we are interrupted.
    	        }
	        }
	    }
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

    public String getDefaultDataResourceUid() {
        return defaultDataResourceUid;
    }

    public void setDefaultDataResourceUid(String defaultDataResourceUid) {
        this.defaultDataResourceUid = defaultDataResourceUid;
    }
}

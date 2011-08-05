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
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import au.org.ala.biocache.FullRecord;
import au.org.ala.biocache.Config;
import au.org.ala.biocache.Store;

/**
 * ActiveMq Listerner 
 * 
 * @author mok011
 * 
 */

//@Component
public class JmsMessageListener implements MessageListener {
	private static final Logger logger = Logger.getLogger(JmsMessageListener.class);
	public static final String MESSAGE_METHOD = "messageMethod";

	protected ObjectMapper mapper = new ObjectMapper();

	public static final String CITIZEN_SCIENCE_DRUID = "dr364";
	public static final String CITIZEN_SCIENCE_DPUID = "dp31";
	
	public enum Method {
		CREATE, UPDATE, DELETE
	}

	public JmsMessageListener() {
		// initialise the object mapper
		mapper.getDeserializationConfig().set(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES,	false);
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
    	
        try {
        	if(message.getStringProperty(MESSAGE_METHOD) != null && !"".equals(message.getStringProperty(MESSAGE_METHOD))){
        		messageMethod = Method.valueOf(message.getStringProperty(MESSAGE_METHOD));	
        		CitizenScience sighting = null;   	 
	            if (message instanceof TextMessage) {
	            	// prepare data
	                TextMessage tm = (TextMessage)message;
	                json = tm.getText();	                
	                if(json != null && !"".equals(json)){
	                	sighting = (CitizenScience)mapper.readValue(json, CitizenScience.class);	
	                }
	                else{
	                	logger.error("Empty Json Message!  Method: " + message.getStringProperty(MESSAGE_METHOD));
	                	return;
	                }
	                
	                //process request
	                switch(messageMethod){
	                	case CREATE:
	                	case UPDATE:
	                		addUpdateOccRecord(sighting);
	                		break;
	                		
	                	case DELETE:
	                		occId = CITIZEN_SCIENCE_DRUID + "|" + sighting.getGuid();
	                    	deleteOccRecord(occId);
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
	
	private FullRecord populateFullRecord(CitizenScience sighting){
    	FullRecord fullRecord = new FullRecord();
    	if(sighting == null){
    		return fullRecord;
    	}
    	
    	//keys
    	fullRecord.setRowKey(CITIZEN_SCIENCE_DRUID + "|" + sighting.getGuid());    	
    	fullRecord.occurrence().setOccurrenceID(sighting.getGuid());
    	
    	fullRecord.classification().setKingdom(sighting.getKingdom());
    	fullRecord.classification().setFamily(sighting.getFamily());
    	fullRecord.classification().setScientificName(sighting.getScientificName());
    	fullRecord.classification().setVernacularName(sighting.getVernacularName());
    	fullRecord.classification().setTaxonConceptID(sighting.getTaxonConceptGuid());
    	
    	if(sighting.getAssociatedMedia() != null){
    		StringBuffer urls = new StringBuffer();
    		for(int i = 0; i < sighting.getAssociatedMedia().length; i++){
    			urls.append(sighting.getAssociatedMedia()[i]);
    			if(i < (sighting.getAssociatedMedia().length - 1)){
    				urls.append(", ");
    			}
    		}
    		fullRecord.occurrence().setAssociatedMedia(urls.toString());
    	}
    	fullRecord.occurrence().setIndividualCount("" + sighting.getIndividualCount());
    	fullRecord.occurrence().setOccurrenceRemarks(sighting.getOccurrenceRemarks());
    	
    	fullRecord.event().setEventDate(sighting.getEventDate().toString());
    	fullRecord.event().setEventTime(sighting.getEventTime());
    	
    	fullRecord.location().setDecimalLatitude("" + sighting.getDecimalLatitude());
    	fullRecord.location().setDecimalLongitude("" + sighting.getDecimalLongitude());
    	fullRecord.location().setLocality(sighting.getLocality());
    	fullRecord.location().setCoordinateUncertaintyInMeters("" + sighting.getCoordinateUncertaintyInMeters());
    	fullRecord.location().setStateProvince(sighting.getStateProvince());
    	fullRecord.location().setCountry(sighting.getCountry());
    	
    	fullRecord.attribution().setDataResourceUid(CITIZEN_SCIENCE_DRUID);
    	        	
    	return fullRecord;
	}
	
	private void addUpdateOccRecord(CitizenScience sighting) {
		FullRecord fullRecord = populateFullRecord(sighting);
		fullRecord.setUuid(Config.occurrenceDAO().createUuid());
		Store.upsertRecord(fullRecord, true);
	}
		
	private void deleteOccRecord(String occId) {
		Store.deleteRecord(occId);
	}
}

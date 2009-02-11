/***************************************************************************
 * Copyright (C) 2005 Global Biodiversity Information Facility Secretariat.
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
package org.ala.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.gbif.portal.dto.Predicate;
import org.gbif.portal.dto.PropertyStoreTripletDTO;
import org.gbif.portal.service.util.UnableToBuildQueryException;
import org.gbif.portal.util.propertystore.PropertyNotFoundException;
import org.gbif.portal.util.propertystore.PropertyStore;

/**
 * A class that uses the property store to generate queries
 * 
 * For example, a List<PropertyStorePredicateDTO> can be used to created 
 * queries based on the Property Store Keys found in the DTO
 * 
 * This is an extension to <code>org.gbif.portal.service.util.PropertyStoreQueryBuilder</code>
 * that supports JoinConditions. This is a requirement if using hibernate 3.3 or above.
 * 
 * @author dmartin
 */
public class PropertyStoreQueryBuilder extends org.gbif.portal.service.util.PropertyStoreQueryBuilder{
	
	protected static Log logger = LogFactory.getLog(PropertyStoreQueryBuilder.class);	
	
	/**
	 * simple map/cache of compiled regex pattern's
	 * todo - replace with better impl.
	 */
	protected static Map<String, Pattern> patternMap = new HashMap<String, Pattern>();
	
	/** The property store to use */
	protected PropertyStore propertyStore;
	/** The key to pull out the preamble for the query */
	protected String preambleKey;
	/** The key to pull out the postamble for the query */
	protected String postambleKey;
	/** The key to pull out the postamble for the query */
	protected String postambleDescKey;	
	/** Start of where clause */
	protected String whereClauseKey;
	/** The namespace to use for the main query parts */
	protected String namespace;
	/** The greater than predicate used for paging */
	protected String greaterThanPredicate;
	/** The less than predicate used for paging */
	protected String lessThanPredicate;	
	/** The subject key for fields to select in the query. */
	protected String selectFieldKey;
	/** The "select" key word or the configured equivalent for the underlying ORM */
	protected String selectKey;		
	/** The "group by" key word or the configured equivalent for the underlying ORM */
	protected String groupByKeyword;		
	/** The default multiple condition keyword. e.g. "or" */
	protected String defaultMultipleConditionKeyword;
	/** The key for retrieving the groupings */
	protected String tripletGroupingsKey;
	/** The key for retrieving the groupings */
	protected String tripletIndexesKey;
	
	/** triplet grouping - internally initialised */
	protected Map<String, List<String>> tripletGroupings; 
	/** triplet indexes - internally initialised */
	protected List<List<String>> tripletIndexes;
	
	/** The key for retrieving the groupings */
	protected String joinConditionsKey = "SERVICE.OCCURRENCE.QUERY.JOINCONDITIONS";
	/** Join conditions */
	protected Map<String, String> joinConditions;
	
	/**
	 * Builds a query based on the criteria provided. The syntax for the query is retrieved
	 * from the property store using the keys in the criteria provided.
	 * @param triplets
	 * @param namespace
	 * @return
	 * @throws UnableToBuildQueryException
	 */
	public String buildQuery(List<PropertyStoreTripletDTO> triplets, boolean matchAll, boolean descending) throws UnableToBuildQueryException {
		try {
			//order by groupings
			sortUsingGroupings(triplets);
			
			//order for indexes
			orderForIndexes(triplets);
			
			StringBuffer sb = new StringBuffer();
			//check for select fields
			// subject=SERVICE.QUERY.SELECT, predicate is ignored, Object=SERVICE.OCCURRENCE.QUERY.RETURNFIELDS.DISTINCTDEGREEPOINTS for example
			List<PropertyStoreTripletDTO> selectTriplets = getSelectFieldTriplets(triplets);
			if(selectTriplets.size()>0){
				//Add the "select" key word
				sb.append(propertyStore.getProperty(namespace, selectKey));
				sb.append(' ');
				//Add the distinct field choices
				for (Iterator<PropertyStoreTripletDTO> iter = selectTriplets.iterator(); iter.hasNext();) {
					PropertyStoreTripletDTO triplet = iter.next();
					sb.append(propertyStore.getProperty(namespace, (String)triplet.getObject()));						
					if(iter.hasNext())
						sb.append(", ");
					else 
						sb.append(' ');
					triplets.remove(triplet);
				}				
			}

			//where (name ="dave" or name="dave2") and ( country="UK" )
			sb.append(propertyStore.getProperty(namespace, preambleKey));
			//get join conditions
			addJoinConditions(sb, triplets);
			
			if(triplets!=null && !triplets.isEmpty()){
				sb.append(propertyStore.getProperty(namespace, whereClauseKey));
			}
			// be safe throughout and ensure spaces (might be overkill)
			sb.append(" ");
			String currentGrouping = null;
			
			for (int i=0; i<triplets.size(); i++) {
				PropertyStoreTripletDTO triplet = triplets.get(i);
				
				//if grouping or subject has changed close the query portion and start a new one
				if (!triplet.getGrouping().equals(currentGrouping)){
					//add a bracket
					if(i>0){
						sb.append(") " );
						if(matchAll)						
							sb.append(" and ");
						else 
							sb.append(" or ");
					}	
					sb.append(" (" );
				}
				
				//if a condition for this has already been set append an OR or an AND instead
				if(triplet.getGrouping().equals(currentGrouping)){
					String multipleCondition = getMultipleCondition(triplet.getSubject());
					sb.append(multipleCondition);
				} 

				currentGrouping = triplet.getGrouping();
				
				if(logger.isTraceEnabled())
					logger.trace("Adding triplet: "+triplet.toString());
				sb.append(propertyStore.getProperty(triplet.getNamespace(), triplet.getSubject()));
				sb.append(' ');
				Predicate predicate = (Predicate) propertyStore.getProperty(triplet.getNamespace(), triplet.getPredicate());
				sb.append(predicate.getPrefix());
				if(triplet.getObject() instanceof Collection){
					Collection collection = (Collection) triplet.getObject();
					Iterator iter = collection.iterator();
					while(iter.hasNext()){
						sb.append("?");
						iter.next();
						if(iter.hasNext())
							sb.append(",");
					}
				} else if(triplet.getObject()==null){
					//append nothing
				} else {
					sb.append("?");
				}
				sb.append(predicate.getPostfix());				
			}
			sb.append(" ) ");

			//add group clauses if they exist
			if(selectTriplets.size()>0){
				boolean groupByAdded = false;
				for (Iterator<PropertyStoreTripletDTO> iter = selectTriplets.iterator(); iter.hasNext();) {
					PropertyStoreTripletDTO triplet = (PropertyStoreTripletDTO) iter.next();
					String groupByValue = (String) propertyStore.getProperty(namespace, ((String)triplet.getObject())+".GROUPBY");						
					if(StringUtils.isNotEmpty(groupByValue) && !groupByAdded){
						sb.append(' ');
						sb.append(groupByKeyword);
						groupByAdded=true;
					}
					sb.append(' ');
					sb.append(groupByValue);
					if(iter.hasNext())
						sb.append(", ");
					else 
						sb.append(' ');
				}	
			}
			
			sb.append(" ");
			if (descending)
				sb.append(propertyStore.getProperty(namespace, postambleDescKey));
			else
				sb.append(propertyStore.getProperty(namespace, postambleKey));
			return sb.toString();
		} 
		catch (PropertyNotFoundException e) {
			throw new UnableToBuildQueryException("Property store is not configured to build this query: " + e.getMessage(), e);
		}
	}
	
	private void addJoinConditions(StringBuffer sb, List<PropertyStoreTripletDTO> triplets) {
		if(joinConditions==null && joinConditionsKey!=null && propertyStore.propertySupported(namespace, joinConditionsKey)){
			joinConditions = (Map<String,String>) propertyStore.getProperty(namespace, joinConditionsKey);
		}
		if(joinConditions!=null){
			//go through each triplet, checking the subjects
			//add a join condition if required
			for(PropertyStoreTripletDTO triplet: triplets){
				String subject = triplet.getSubject();
				String condition = joinConditions.get(subject);
				if(condition!=null){
					sb.append(' ');
					sb.append(condition);
					sb.append(' ');
				}
			}
		}
	}

	public boolean nullSafeEquals(Object original, Object comparedTo){
		if (original==null && comparedTo==null)
			return true;
		if (original!=null && comparedTo==null)
			return false;		
		if (original==null && comparedTo!=null)
			return false;			
		return original.equals(comparedTo);
	}
	
	/**
	 * Sort using groupings. if a grouping is unavailable for a triplet, the subject is used.
	 * 
	 * @param tripletGroupings
	 * @param triplets
	 */
	private void sortUsingGroupings( List<PropertyStoreTripletDTO> triplets) {
		for(PropertyStoreTripletDTO pstDTO: triplets){
			pstDTO.setGrouping(getGroupingName(pstDTO.getSubject()));
		}
		if(triplets.size()>0)
			Collections.sort(triplets, triplets.get(0).new GroupingSubjectComparator());
	}

	/**
	 * Order using indexes. Takes the indexes configuration, finds an index that is applicable for the query
	 * and then orders the triplets so that these triplets are in the correct sequence and are at the
	 * beginning of the list.
	 * 
	 * Note: If a triplet isn't a associated with a group, then the group is the subject for that triplet.
	 * This has been set in a previous step <code>sortUsingGroupings</code>
	 * 
	 * @param triplets
	 */
	private void orderForIndexes(List<PropertyStoreTripletDTO> triplets) {
		
		logger.debug("Ordering for indexes");
		List<List<String>> tripletIndexes = getTripletIndexes();
		if(tripletIndexes!=null){
			
			List<String> tripletIndexToUse = null;
			
			// iterate through indexes looking for a match 
			for(List<String> tripletIndex: tripletIndexes){
				if(logger.isDebugEnabled()){
					logger.debug("Checking for index :"+tripletIndex);
				}
				
				boolean containsCorrectGrouping = true;
				
				for(String tripletIndexGrouping: tripletIndex){
					if(!queryContainsGrouping(tripletIndexGrouping, triplets)){
						containsCorrectGrouping=false;
						break;
					}
				}
				if(containsCorrectGrouping){
					tripletIndexToUse = tripletIndex;
					if(logger.isDebugEnabled()){
						logger.debug("Matched index :"+tripletIndexToUse);
					}
					break;
				}
			}
			
			if(tripletIndexToUse!=null){
				
				if(logger.isDebugEnabled()){
					logger.debug("Unordered triplets :"+triplets);
				}				
				
				//order the triplets associated with the index, placing them in the order specified
				//in the index, and at the beginning of the query
				List<PropertyStoreTripletDTO> tripletsToOrder = new ArrayList<PropertyStoreTripletDTO>();
				for(String indexGroupingName: tripletIndexToUse){
					//pull out these triplets in the grouping and add to tripletsToOrder
					for(PropertyStoreTripletDTO triplet: triplets){
						//if its the correct index subject add to the front
						if(triplet.getGrouping().equals(indexGroupingName)){
							tripletsToOrder.add(triplet);
						}
					}
				}
				
				//remove these triplets
				triplets.removeAll(tripletsToOrder);
				//add to the front of the list
				triplets.addAll(0, tripletsToOrder);
				if(logger.isDebugEnabled()){
					logger.debug("Ordered triplets :"+triplets);
				}
				
			}
		}
	}
	
	/**
	 * Returns true if the supplied list of triplets contains the supplied grouping.
	 * @param subject
	 * @param triplets
	 * @return
	 */
	private boolean queryContainsGrouping(String grouping, List<PropertyStoreTripletDTO> triplets){
		for(PropertyStoreTripletDTO triplet: triplets){
			if(triplet.getGrouping().equals(grouping))
				return true;
		}
		return false;
	}
	
	/**
	 * Returns true if the supplied list of triplets contains the correct grouping.
	 * @param subject
	 * @param triplets
	 * @return
	 */
	private boolean queryContainsSubject(String subject, List<PropertyStoreTripletDTO> triplets){
		for(PropertyStoreTripletDTO triplet: triplets){
			if(triplet.getSubject().equals(subject))
				return true;
		}
		return false;
	}	
	
	/**
	 * Retrieves a grouping name for this triplet.
	 * 
	 * @param tripletGroupings
	 * @param triplet
	 * @return
	 */
	private String getGroupingName(String subject){
		Map<String, List<String>> tripletGroupings = getTripletGroupings();
		if(tripletGroupings!=null){
			Iterator<String> iter = tripletGroupings.keySet().iterator();
			while(iter.hasNext()){
				String groupingName = iter.next();
				List<String> grouping = (List<String>) tripletGroupings.get(groupingName);
				if(grouping.contains(subject))
					return groupingName;
			}
		}
		return subject;
	}
	
	/**
	 * Retrieves the triplet groupings from the property store.
	 * 
	 * @return
	 */
	public Map<String, List<String>> getTripletGroupings(){
		if(tripletGroupings==null && tripletGroupingsKey!=null && propertyStore.propertySupported(namespace, tripletGroupingsKey))
			tripletGroupings = (Map<String, List<String>>) propertyStore.getProperty(namespace, tripletGroupingsKey);
		return tripletGroupings;
	}

	/**
	 * Retrieves the triplet indexes from the property store.
	 * 
	 * @return
	 */
	public List<List<String>> getTripletIndexes(){
		if(tripletIndexes==null && tripletIndexesKey!=null && propertyStore.propertySupported(namespace, tripletIndexesKey))
			tripletIndexes = (List<List<String>>) propertyStore.getProperty(namespace, tripletIndexesKey);
		return tripletIndexes;
	}	
	
	/**
	 * todo this needs to change - either every subject needs a multiplecondition key or the propertystore api
	 * needs to provide a method or retrieving a key without the risk of a PropertyNotFoundException.
	 * @param subject
	 * @return
	 */
	private String getMultipleCondition(String subject) {
		try{
			return (String) propertyStore.getProperty(namespace, subject+".MULTIPLECONDITION");		
		} catch(PropertyNotFoundException e){
			//todo Does the PropertyStore need to throw this exception??
			return defaultMultipleConditionKeyword;
		}
	}

	/**
	 * Retrieves a sublist of select field criteria
	 * @param triplets
	 * @return
	 */
	public  List<PropertyStoreTripletDTO> getSelectFieldTriplets(List<PropertyStoreTripletDTO> triplets) {
		List<PropertyStoreTripletDTO> selectFieldTriplets = new ArrayList<PropertyStoreTripletDTO>();
		for (PropertyStoreTripletDTO triplet : triplets)
			if (triplet.getSubject().equals(selectFieldKey))
				selectFieldTriplets.add(triplet);
		return selectFieldTriplets;
	}
	
	/**
	 * @return the greaterThanPredicate
	 */
	public String getGreaterThanPredicate() {
		return greaterThanPredicate;
	}

	/**
	 * @param greaterThanPredicate the greaterThanPredicate to set
	 */
	public void setGreaterThanPredicate(String greaterThanPredicate) {
		this.greaterThanPredicate = greaterThanPredicate;
	}

	/**
	 * @return the lessThanPredicate
	 */
	public String getLessThanPredicate() {
		return lessThanPredicate;
	}

	/**
	 * @param lessThanPredicate the lessThanPredicate to set
	 */
	public void setLessThanPredicate(String lessThanPredicate) {
		this.lessThanPredicate = lessThanPredicate;
	}

	/**
	 * @return the namespace
	 */
	public String getNamespace() {
		return namespace;
	}

	/**
	 * @param namespace the namespace to set
	 */
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	/**
	 * @return the postambleDescKey
	 */
	public String getPostambleDescKey() {
		return postambleDescKey;
	}

	/**
	 * @param postambleDescKey the postambleDescKey to set
	 */
	public void setPostambleDescKey(String postambleDescKey) {
		this.postambleDescKey = postambleDescKey;
	}

	/**
	 * @return the postambleKey
	 */
	public String getPostambleKey() {
		return postambleKey;
	}

	/**
	 * @param postambleKey the postambleKey to set
	 */
	public void setPostambleKey(String postambleKey) {
		this.postambleKey = postambleKey;
	}

	/**
	 * @return the preambleKey
	 */
	public String getPreambleKey() {
		return preambleKey;
	}

	/**
	 * @param preambleKey the preambleKey to set
	 */
	public void setPreambleKey(String preambleKey) {
		this.preambleKey = preambleKey;
	}

	/**
	 * @return the propertyStore
	 */
	public PropertyStore getPropertyStore() {
		return propertyStore;
	}

	/**
	 * @param propertyStore the propertyStore to set
	 */
	public void setPropertyStore(PropertyStore propertyStore) {
		this.propertyStore = propertyStore;
	}

	/**
	 * @return the selectFieldKey
	 */
	public String getSelectFieldKey() {
		return selectFieldKey;
	}

	/**
	 * @param selectFieldKey the selectFieldKey to set
	 */
	public void setSelectFieldKey(String selectFieldKey) {
		this.selectFieldKey = selectFieldKey;
	}

	/**
	 * @return the selectKey
	 */
	public String getSelectKey() {
		return selectKey;
	}

	/**
	 * @param selectKey the selectKey to set
	 */
	public void setSelectKey(String selectKey) {
		this.selectKey = selectKey;
	}

	/**
	 * @return the groupByKeyword
	 */
	public String getGroupByKeyword() {
		return groupByKeyword;
	}

	/**
	 * @param groupByKeyword the groupByKeyword to set
	 */
	public void setGroupByKeyword(String groupByKeyword) {
		this.groupByKeyword = groupByKeyword;
	}

	/**
	 * @return the defaultMultipleConditionKeyword
	 */
	public String getDefaultMultipleConditionKeyword() {
		return defaultMultipleConditionKeyword;
	}

	/**
	 * @param defaultMultipleConditionKeyword the defaultMultipleConditionKeyword to set
	 */
	public void setDefaultMultipleConditionKeyword(
			String defaultMultipleConditionKeyword) {
		this.defaultMultipleConditionKeyword = defaultMultipleConditionKeyword;
	}

	/**
	 * @param patternMap the patternMap to set
	 */
	public static void setPatternMap(Map<String, Pattern> patternMap) {
		PropertyStoreQueryBuilder.patternMap = patternMap;
	}

	/**
	 * @param tripletGroupingsKey the tripletGroupingsKey to set
	 */
	public void setTripletGroupingsKey(String tripletGroupingsKey) {
		this.tripletGroupingsKey = tripletGroupingsKey;
	}

	/**
	 * @param tripletIndexesKey the tripletIndexesKey to set
	 */
	public void setTripletIndexesKey(String tripletIndexesKey) {
		this.tripletIndexesKey = tripletIndexesKey;
	}

	/**
	 * @param whereClauseKey the whereClauseKey to set
	 */
	public void setWhereClauseKey(String whereClauseKey) {
		this.whereClauseKey = whereClauseKey;
	}
}
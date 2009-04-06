/***************************************************************************
 * Copyright (C) 2008 Global Biodiversity Information Facility Secretariat.  
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
package org.ala.filter;

import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.gbif.portal.dto.PropertyStoreTripletDTO;
import org.gbif.portal.util.geospatial.CellIdUtils;
import org.gbif.portal.web.content.filter.FilterHelper;
import org.gbif.portal.web.filter.CriterionDTO;
import org.gbif.portal.web.util.QueryHelper;
import org.springframework.web.servlet.ModelAndView;

/**
 * A FilterHelper for the Longitude filter.
 * 
 * Taken from the GBIF codebase and refactored to use a range for equals queries.
 * This is a workaround for floating point comparison problems with MySQL.
 *
 * @author davejmartin
 */
public class LongitudeFilterHelper implements FilterHelper {
	
	protected Log logger = LogFactory.getLog(LongitudeFilterHelper.class);

	protected String longitudeSubject="SERVICE.OCCURRENCE.QUERY.SUBJECT.LONGITUDE";
	
	protected String latitudeSubject="SERVICE.OCCURRENCE.QUERY.SUBJECT.LATITUDE";
	
	protected String cellIdSubject="SERVICE.OCCURRENCE.QUERY.SUBJECT.CELLID";
	
	protected String cellIdMod360Subject="SERVICE.OCCURRENCE.QUERY.SUBJECT.CELLID.MOD360";
	
	protected String equalsPredicate = "SERVICE.QUERY.PREDICATE.EQUAL";
	protected String lessThanOrEqualPredicate = "SERVICE.QUERY.PREDICATE.LE";	
	protected String greaterThanOrEqualPredicate = "SERVICE.QUERY.PREDICATE.GE";
	protected String lessThanPredicate = "SERVICE.QUERY.PREDICATE.L";	
	protected String greaterThanPredicate = "SERVICE.QUERY.PREDICATE.G";
	
	
	/**
	 * @see org.gbif.portal.web.content.filter.FilterHelper#preProcess(java.util.List, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public void preProcess(List<PropertyStoreTripletDTO> triplets, HttpServletRequest request,
	    HttpServletResponse response) {
		
		List<PropertyStoreTripletDTO> longitudeTriplets = QueryHelper.getTripletsBySubject(triplets, longitudeSubject);
		List<PropertyStoreTripletDTO> latitudeTriplets = QueryHelper.getTripletsBySubject(triplets, latitudeSubject);
		
		
		for(PropertyStoreTripletDTO longitudeTriplet:longitudeTriplets){
			
			//if equals predicate - add a cell triplet if possible (i.e there is an latitude triplet)
			if(equalsPredicate.equals(longitudeTriplet.getPredicate())){
	
				//is there an equals latitude?
				PropertyStoreTripletDTO equalsLat = getEqualsLatitude(latitudeTriplets);
				if(equalsLat!=null){
					
					Float latitude = (Float) equalsLat.getObject();
					Float longitude = (Float) longitudeTriplet.getObject();
					
					//add an equals cell id and return
					try { 
						int cellId = CellIdUtils.toCellId(latitude, longitude);
						PropertyStoreTripletDTO cellIdTriplet = new PropertyStoreTripletDTO(longitudeTriplet.getNamespace(),
								cellIdSubject,equalsPredicate,cellId);
						triplets.add(cellIdTriplet);
					}catch(Exception e){
						logger.error(e.getMessage(), e);
						return;
					}
				} 
				
				//remove the longitude equals
				triplets.remove(longitudeTriplet);
				
				//determine the number of decimal places
				String longitudeAsString = longitudeTriplet.getObject().toString();
				int noOfDecimalPlaces = longitudeAsString.length() - (longitudeAsString.indexOf('.')+1);
				if(noOfDecimalPlaces>0){
					//so if we have a value of 36.12, the boundary should be 36.119 and 36.121
					float boundary = (float) Math.pow(10d, (double)-(noOfDecimalPlaces+1));
					
					float lowerLimit = ((Float) longitudeTriplet.getObject()).floatValue() - boundary;
					float upperLimit = ((Float) longitudeTriplet.getObject()).floatValue() + boundary;
					
					//add two triplets for boundary
					triplets.add(new PropertyStoreTripletDTO(longitudeTriplet.getNamespace(),longitudeSubject, greaterThanPredicate, lowerLimit));
					triplets.add(new PropertyStoreTripletDTO(longitudeTriplet.getNamespace(),longitudeSubject, lessThanPredicate, upperLimit));
				}
				return;
			} 
			
			
			Float longitude = (Float) longitudeTriplet.getObject();
			int mod360CellId = CellIdUtils.getMod360CellIdFor(longitude);
			PropertyStoreTripletDTO cellIdTriplet = new PropertyStoreTripletDTO(longitudeTriplet.getNamespace(),
					cellIdMod360Subject,longitudeTriplet.getPredicate(),mod360CellId);
			
			triplets.add(cellIdTriplet);
			
			//need to add max/min cell id to use index
			if(latitudeTriplets.isEmpty()){
				PropertyStoreTripletDTO catchAllCellIdTriplet = new PropertyStoreTripletDTO(longitudeTriplet.getNamespace(),
						cellIdSubject,greaterThanOrEqualPredicate,0);
				triplets.add(catchAllCellIdTriplet);
			}
			
		}
	}
	
	private PropertyStoreTripletDTO getEqualsLatitude(List<PropertyStoreTripletDTO> latitudeTriplets) {
	  for(PropertyStoreTripletDTO triplet:latitudeTriplets){
	  	if(equalsPredicate.equals(triplet.getPredicate())){
	  		return triplet;
	  	}
	  }
	  return null;
	}

	/**
	 * @see org.gbif.portal.web.content.filter.FilterHelper#addCriterion2Request(org.gbif.portal.web.filter.CriterionDTO, org.springframework.web.servlet.ModelAndView, javax.servlet.http.HttpServletRequest)
	 */
	public void addCriterion2Request(CriterionDTO criterionDTO, ModelAndView mav, HttpServletRequest request) {}

	/**
	 * @see org.gbif.portal.web.content.filter.FilterHelper#getDefaultDisplayValue(javax.servlet.http.HttpServletRequest)
	 */
	public String getDefaultDisplayValue(HttpServletRequest request) {
		return null;
	}

	/**
	 * @see org.gbif.portal.web.content.filter.FilterHelper#getDefaultValue(javax.servlet.http.HttpServletRequest)
	 */
	public String getDefaultValue(HttpServletRequest request) {
		return null;
	}

	/**
	 * @see org.gbif.portal.web.content.filter.FilterHelper#getDisplayValue(java.lang.String, java.util.Locale)
	 */
	public String getDisplayValue(String value, Locale locale) {
		StringBuffer sb = new StringBuffer();
		if(value!=null){
			Float longitude = Float.parseFloat(value);
			sb.append(Math.abs(longitude));
			sb.append("&deg;");
			if(longitude>=0){
				sb.append('E');
			} else {
				sb.append('W');
			}
			return sb.toString();
		}
		return value;
	}
}
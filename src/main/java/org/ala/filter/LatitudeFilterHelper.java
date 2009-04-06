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
 * A FilterHelper for the Latitude filter.
 * 
 * Taken from the GBIF codebase and refactored to use a range for equals queries.
 * This is a workaround for floating point comparison problems with MySQL.
 * 
 * @author davejmartin
 */
public class LatitudeFilterHelper implements FilterHelper {
	
	protected Log logger = LogFactory.getLog(LatitudeFilterHelper.class);

	protected String subject="SERVICE.OCCURRENCE.QUERY.SUBJECT.LATITUDE";
	
	protected String cellIdSubject="SERVICE.OCCURRENCE.QUERY.SUBJECT.CELLID";
	
	protected String lessThanOrEqualPredicate = "SERVICE.QUERY.PREDICATE.LE";
	
	protected String lessThanPredicate = "SERVICE.QUERY.PREDICATE.L";
	
	protected String greaterThanPredicate = "SERVICE.QUERY.PREDICATE.G";
	
	protected String equalsPredicate = "SERVICE.QUERY.PREDICATE.EQUAL";
		
	protected String greaterThanOrEqualPredicate = "SERVICE.QUERY.PREDICATE.GE";
	
	/**
	 * @see org.gbif.portal.web.content.filter.FilterHelper#preProcess(java.util.List, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public void preProcess(List<PropertyStoreTripletDTO> triplets, HttpServletRequest request,
	    HttpServletResponse response) {
		
		List<PropertyStoreTripletDTO> latitudeTriplets = QueryHelper.getTripletsBySubject(triplets, subject);
		for(PropertyStoreTripletDTO latitudeTriplet:latitudeTriplets){
			
			Float latitude = (Float) latitudeTriplet.getObject();
			
			//get the first cell for the row with this latitude
			int cellId = CellIdUtils.getCellIdFor(latitude);
			
			if(lessThanOrEqualPredicate.equals(latitudeTriplet.getPredicate())){
				
				//get the first cell in the row above
				int firstCellId = 360 + cellId;
				PropertyStoreTripletDTO cellIdTriplet = new PropertyStoreTripletDTO(latitudeTriplet.getNamespace(),
						cellIdSubject,latitudeTriplet.getPredicate(),firstCellId);
				if(logger.isDebugEnabled()){
					logger.debug("Adding GE triplet:"+ cellIdSubject);
				}
				triplets.add(cellIdTriplet);
				
			} else if(greaterThanOrEqualPredicate.equals(latitudeTriplet.getPredicate())){
				
				//get the first cell in the row below
				int firstCellId = cellId - 1;
				PropertyStoreTripletDTO cellIdTriplet = new PropertyStoreTripletDTO(latitudeTriplet.getNamespace(),
						cellIdSubject,latitudeTriplet.getPredicate(),firstCellId);
				if(logger.isDebugEnabled()){
					logger.debug("Adding GE triplet:"+ cellIdSubject);
				}
				triplets.add(cellIdTriplet);
				
			} else if(equalsPredicate.equals(latitudeTriplet.getPredicate()) ){
				
				int topCellId = 360 + cellId;
				PropertyStoreTripletDTO topCellIdTriplet = new PropertyStoreTripletDTO(latitudeTriplet.getNamespace(),
						cellIdSubject,lessThanPredicate,topCellId);
				triplets.add(topCellIdTriplet);

				int bottomCellId = cellId - 1;
				PropertyStoreTripletDTO bottomCellIdTriplet = new PropertyStoreTripletDTO(latitudeTriplet.getNamespace(),
						cellIdSubject,greaterThanPredicate,bottomCellId);
				
				if(logger.isDebugEnabled()){
					logger.debug("Adding top triplet:"+ topCellIdTriplet);
					logger.debug("Adding bottom triplet:"+ bottomCellIdTriplet);
				}
				triplets.add(bottomCellIdTriplet);
				
				
				//This is a workaround for MySQL float point comparison problems
				//determine the number of decimal places
				String latitudeAsString = latitudeTriplet.getObject().toString();
				int noOfDecimalPlaces = latitudeAsString.length() - (latitudeAsString.indexOf('.')+1);
				if(noOfDecimalPlaces>0){
					float boundary = (float) Math.pow(10d, (double)-(noOfDecimalPlaces+1));
					//remove the equals triplet and replace with range
					triplets.remove(latitudeTriplet);
					
					float lowerLimit = ((Float) latitudeTriplet.getObject()).floatValue() - boundary;
					float upperLimit = ((Float) latitudeTriplet.getObject()).floatValue() + boundary;
					
					//add two triplets for boundary
					triplets.add(new PropertyStoreTripletDTO(latitudeTriplet.getNamespace(),subject, greaterThanPredicate, lowerLimit));
					triplets.add(new PropertyStoreTripletDTO(latitudeTriplet.getNamespace(),subject, lessThanPredicate, upperLimit));
				}
			}
		}
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
			Float latitude = Float.parseFloat(value);
			sb.append(Math.abs(latitude));
			sb.append("&deg;");
			if(latitude>=0){
				sb.append('N');
			} else {
				sb.append('S');
			}
			return sb.toString();
		}
		return value;
	}
}
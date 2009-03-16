/***************************************************************************
 * Copyright (C) 2009 Atlas of Living Australia
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
package org.ala.web.controller;

import java.io.OutputStreamWriter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ala.dao.OccurrenceRecordDAO;
import org.ala.web.util.WebUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.gbif.portal.model.occurrence.OccurrenceRecord;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

/**
 * Controller for rendering occurrence records in a cell in kml.
 * 
 * @author Dave Martin (David.Martin@csiro.au)
 */
public class OccurrenceCellController implements Controller {

	protected OccurrenceRecordDAO occurrenceRecordDAO;
	
	/**
	 * @see org.springframework.web.servlet.mvc.Controller#handleRequest(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public ModelAndView handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		int entityId = ServletRequestUtils.getIntParameter(request, "id", 0);
		int entityType = ServletRequestUtils.getIntParameter(request, "type", 0);
		int cellId = ServletRequestUtils.getIntParameter(request, "cellId", 0);
		int centiCellId = ServletRequestUtils.getIntParameter(request, "centiCellId", -1);
		
		if(centiCellId>=0){
			List<OccurrenceRecord> ors = occurrenceRecordDAO.getOccurrenceRecordsForCentiCell(entityType, entityId, cellId, centiCellId);
			renderOccurrences(ors, request, response);
		} 
		
		return null;
	}

	/**
	 * Render the retrieved occurrences.
	 * 
	 * @param ors
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	private void renderOccurrences(List<OccurrenceRecord> ors, HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		response.setContentType("application/vnd.google-earth.kml+xml");
		Template hdr = Velocity.getTemplate("org/ala/io/occurrenceDetailsKmlHeader.vm");
		Template bdy = Velocity.getTemplate("org/ala/io/occurrenceDetailsKml.vm");
		Template ftr = Velocity.getTemplate("org/ala/io/occurrenceDetailsKmlFooter.vm");
		VelocityContext vc = new VelocityContext();
		vc.put("hostUrl", WebUtils.getHostUrl(request));
		OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream());
		hdr.merge(vc, writer);
		writer.flush();
		
		for(OccurrenceRecord oc:ors){
			vc.put("occurrenceRecord", oc);
			vc.put("taxonName", oc.getTaxonName().getCanonical());
			bdy.merge(vc, writer);
			writer.flush();
		}
		ftr.merge(vc, writer);
		writer.flush();
	}

	/**
	 * @param occurrenceRecordDAO the occurrenceRecordDAO to set
	 */
	public void setOccurrenceRecordDAO(OccurrenceRecordDAO occurrenceRecordDAO) {
		this.occurrenceRecordDAO = occurrenceRecordDAO;
	}
}
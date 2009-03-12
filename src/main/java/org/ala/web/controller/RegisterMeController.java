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

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ala.dao.WebServiceDAO;
import org.ala.model.WebService;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

/**
 * 
 *
 * @author Dave Martin (David.Martin@csiro.au)
 */
public class RegisterMeController extends MultiActionController{

	protected WebServiceDAO  webServiceDAO;
	
	public ModelAndView start(HttpServletRequest request, HttpServletResponse response) throws Exception{
		return new ModelAndView("webservices.start");
	}
	
	public ModelAndView list(HttpServletRequest request, HttpServletResponse response) throws Exception{
		ModelAndView mav = new ModelAndView("webservices.list");
		
		List<WebService> raps = null;
		String iso = StringUtils.trimToNull(request.getParameter("iso"));
		if(iso==null){
			raps = webServiceDAO.getAll();
		} else {
			raps = webServiceDAO.getForIsoCountryCode(iso);
		}
		
		mav.addObject("resourceAccessPoints",raps);
		return mav;
	}	
	
	/**
	 * @param webServiceDAO the webServiceDAO to set
	 */
	public void setWebServiceDAO(WebServiceDAO webServiceDAO) {
		this.webServiceDAO = webServiceDAO;
	}
}
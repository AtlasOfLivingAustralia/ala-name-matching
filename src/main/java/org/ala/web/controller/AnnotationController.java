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

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

/**
 * A multi action controller aimed at supporting the creation of annotations.
 *
 * @author Dave Martin (David.Martin@csiro.au)
 */
public class AnnotationController extends MultiActionController {
	
	protected String annoteaServerUrl = "http://localhost:8080/danno/annotea";
	
	protected String annotationTemplate = "org/ala/io/annotation.vm";
	
	//popup the correct form - urlFileNameController -> JSP
	//on submit save annotation
	
	public ModelAndView retrieveAnnotations(HttpServletRequest request, HttpServletResponse response){
		
		//look up via URL
		return null;
	}
	
	/**
	 * Save an annotation to the annotea compliant server.
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	public ModelAndView saveAnnotation(HttpServletRequest request, HttpServletResponse response) throws Exception {

		StringBuffer annotationBody = new StringBuffer();
		
		String url = request.getParameter("url");
		String xpath = request.getParameter("xpath");
		//retrieve old values
		List<String> paramNames = getParamsWithPrefix(request, "old.");
		annotationBody.append("<ala:fieldUpdates>");
		for(String paramName: paramNames){
			String oldValue = request.getParameter("old."+paramName);
			String newValue = request.getParameter("new."+paramName);
			if(StringUtils.isNotEmpty(newValue)){
				addFieldUpdate(annotationBody, paramName, oldValue, newValue);
			}
		}
		annotationBody.append("</ala:fieldUpdates>");
		
		//retrieve comment
		String comment = request.getParameter("comment");
		
		//create an annotea RDF message
		VelocityContext ctx = new VelocityContext();
		Template t = Velocity.getTemplate(annotationTemplate);
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		PrintWriter ow = new PrintWriter(bout, true);
		ctx.put("type", "change");
		ctx.put("url", url);
		ctx.put("xpath", xpath);
		ctx.put("creator", "creator");
		ctx.put("title", "title");
		ctx.put("lang", "en");
		ctx.put("comment", comment);
		ctx.put("contentLength", annotationBody.length());
		ctx.put("body", annotationBody.toString());
		t.merge(ctx, ow);
		ow.flush();
		
		//submit to Danno
		HttpClient httpClient  = new HttpClient();
		PostMethod postMethod = new PostMethod(annoteaServerUrl);
		String requestBody = bout.toString();
		logger.debug(requestBody);
		postMethod.setRequestBody(requestBody);
		int status = httpClient.executeMethod(postMethod);
		logger.debug(postMethod.getResponseBodyAsString());
		logger.debug(status);
		postMethod.releaseConnection();
		return null;
	}

	private void addFieldUpdate(StringBuffer fieldUpdates, String paramName,
			String oldValue, String newValue) {
		fieldUpdates.append("<ala:fieldUpdate><ala:field>");
		fieldUpdates.append(paramName);
		fieldUpdates.append("</ala:field>");
		fieldUpdates.append("<ala:old>");
		fieldUpdates.append(oldValue);
		fieldUpdates.append("</ala:old>");
		fieldUpdates.append("<ala:new>");
		fieldUpdates.append(newValue);
		fieldUpdates.append("</ala:new>");
		fieldUpdates.append("</ala:fieldUpdate>");
	}

	private List<String> getParamsWithPrefix(HttpServletRequest request, String prefix) {
		Enumeration<String> paramNames = request.getParameterNames();
		List<String> params = new ArrayList<String>();
		while(paramNames.hasMoreElements()){
			String paramName = paramNames.nextElement();
			if(paramName.startsWith(prefix)){
				params.add(paramName.substring(prefix.length()));
			}
		}
		return params;
	}
	
	/**
	 * @param annoteaServerUrl the annoteaServerUrl to set
	 */
	public void setAnnoteaServerUrl(String annoteaServerUrl) {
		this.annoteaServerUrl = annoteaServerUrl;
	}

	/**
	 * @param annotationTemplate the annotationTemplate to set
	 */
	public void setAnnotationTemplate(String annotationTemplate) {
		this.annotationTemplate = annotationTemplate;
	}
}
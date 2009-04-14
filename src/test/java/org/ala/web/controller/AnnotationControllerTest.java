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

import java.io.FileInputStream;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.velocity.app.Velocity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * 
 * 
 * @author Dave Martin (David.Martin@csiro.au)
 */
public class AnnotationControllerTest extends TestCase {

	/**
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testSaveAnnotation() throws Exception{
		MockHttpServletRequest req = new MockHttpServletRequest();
		MockHttpServletResponse rep = new MockHttpServletResponse();
//		org.apache.velocity.app.Velocity.init("file:///Users/davejmartin2/dev/src/test/java/velocity.properties");
		
		Properties p = new Properties();
		p.load(new FileInputStream("/Users/davejmartin2/dev/ala-portal-web/src/test/java/velocity.properties"));
		Velocity.init(p);
		req.addParameter("old.latitude", "122");
		req.addParameter("new.latitude", "12");
		req.addParameter("old.longitude", "22");
		req.addParameter("new.longitude", "42");
		
		req.addParameter("type", "change");
		req.addParameter("url", "http://localhost:8080/ala-web/occurrences/123");
		req.addParameter("xpath", "xpointer(string-range(id(\"occurrenceRecord-34588533-dataset\"),\"\",0,0)");
		req.addParameter("creator", "Dave");
		req.addParameter("title", "My Annotation Test");
		req.addParameter("lang", "en");
		req.addParameter("comment", "comment");
		
		AnnotationController ac = new AnnotationController();
		ac.saveAnnotation(req, rep);
	}
}

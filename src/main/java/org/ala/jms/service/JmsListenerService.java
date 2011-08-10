/**************************************************************************
 *  Copyright (C) 2010 Atlas of Living Australia
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

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 
 * @author mok011
 *
 */

public class JmsListenerService {

	public static void main(String[] args) throws Exception {
		long t = System.currentTimeMillis() + 5000;
		
		System.out.println("Loading ClassPathXmlApplicationContext....... ");
		
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath*:activemq-context.xml");
		
		System.out.println("ClassPathXmlApplicationContext: " + context);
		// delay for pelpop client stating up
		if(context != null){
			
			while(true){
				if(System.currentTimeMillis() > t){
					break;
				}
			}
			context.start();
			System.out.println("JMS listener is started!!!");
		}
	}
}

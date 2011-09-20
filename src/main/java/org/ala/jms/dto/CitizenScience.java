package org.ala.jms.dto;

import org.ala.jms.dto.Sighting;

public class CitizenScience extends Sighting {
	String[] associatedMedia;

	public String[] getAssociatedMedia() {
/**
 * Date: 20 Sept 2010.
 * temperory remove/comment out the code that retrieves the "associatedMedia" from the message in the JMS queue.		
 */
//		return this.associatedMedia;
		return  new String[]{};
	}

	public void setAssociatedMedia(String[] associatedMedia) {
		/**
		 * Date: 20 Sept 2010.
		 * temperory remove/comment out the code that retrieves the "associatedMedia" from the message in the JMS queue.		
		 */		
//		this.associatedMedia = associatedMedia;
	}
}

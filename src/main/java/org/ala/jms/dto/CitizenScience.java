package org.ala.jms.dto;

import org.ala.jms.dto.Sighting;

public class CitizenScience extends Sighting {
	String[] associatedMedia;

	public String[] getAssociatedMedia() {
		return this.associatedMedia;
	}

	public void setAssociatedMedia(String[] associatedMedia) {
		this.associatedMedia = associatedMedia;
	}
}

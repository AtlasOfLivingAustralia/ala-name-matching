/***************************************************************************
 * Copyright (C) 2010 Atlas of Living Australia
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
package au.org.ala.sensitiveData.model;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public enum SensitivityCategory {

	EXTREME("X", -1, Integer.MAX_VALUE),
	HIGH("H", 1, 10000),
	MEDIUM("M", 2, 1000),
	LOW("L", 3, 100),
	NOT_SENSITIVE("N", 10, 0);
	
	private String value;
	private int generalisationDecimalPlaces;
	private int generalisationInMetres;
	
	private SensitivityCategory(String value, int decimalPlaces, int metres) {
		this.value = value;
		this.generalisationDecimalPlaces = decimalPlaces;
		this.generalisationInMetres = metres;
	}
	
	public static SensitivityCategory getCategory(String value) {
		for (SensitivityCategory cat : SensitivityCategory.values()) {
			if (cat.getValue().equals(value)) {
				return cat;
			}
		}
		return null;
	}
	
	public String getValue() {
		return value;
	}

	public int getGeneralisationDecimalPlaces() {
		return generalisationDecimalPlaces;
	}

	public int getGeneralisationInMetres() {
		return generalisationInMetres;
	}
}

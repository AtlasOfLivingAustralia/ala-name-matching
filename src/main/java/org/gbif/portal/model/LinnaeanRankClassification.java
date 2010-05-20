/***************************************************************************
 * Copyright (C) 2005 Global Biodiversity Information Facility Secretariat.
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
package org.gbif.portal.model;


import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * @author trobertson
 * Copied from the gbif portal index project. Commented out the sections that are
 * not necessary.
 *
 * Used to specify the
 */
public class LinnaeanRankClassification {
	protected String kingdom;
	protected String phylum;
	protected String klass;
	protected String order;
	protected String family;
	protected String genus;
	protected String scientificName;
	/**
	 * @param kingdom
	 * @param phylum
	 * @param klass
	 * @param order
	 * @param family
	 * @param genus
	 * @param scientificName
	 */
	public LinnaeanRankClassification(String kingdom, String phylum, String klass, String order, String family, String genus, String scientificName) {
		this.kingdom = kingdom;
		this.phylum = phylum;
		this.klass = klass;
		this.order = order;
		this.family = family;
		this.genus = genus;
		this.scientificName = scientificName;
	}
	public LinnaeanRankClassification(String kingdom, String genus){
            this(kingdom, null, null, null, null, genus,null);
        }
	/**
	 * @param classification to build from
	 */
//	public LinnaeanRankClassification(List<TaxonName> classification) {
//		for (TaxonName tn : classification) {
//			if (tn.rank==1000) this.kingdom=tn.getCanonical();
//			if (tn.rank==2000) this.phylum=tn.getCanonical();
//			if (tn.rank==3000) this.klass=tn.getCanonical();
//			if (tn.rank==4000) this.order=tn.getCanonical();
//			if (tn.rank==5000) this.family=tn.getCanonical();
//			if (tn.rank==6000) this.genus=tn.getCanonical();
//			if (tn.rank==7000) this.kingdom=tn.getCanonical();
//		}
//	}
	
	
	/**
	 * @return Returns the family.
	 */
	public String getFamily() {
		return family;
	}
	/**
	 * @param family The family to set.
	 */
	public void setFamily(String family) {
		this.family = family;
	}
	/**
	 * @return Returns the genus.
	 */
	public String getGenus() {
		return genus;
	}
	/**
	 * @param genus The genus to set.
	 */
	public void setGenus(String genus) {
		this.genus = genus;
	}
	/**
	 * @return Returns the kingdom.
	 */
	public String getKingdom() {
		return kingdom;
	}
	/**
	 * @param kingdom The kingdom to set.
	 */
	public void setKingdom(String kingdom) {
		this.kingdom = kingdom;
	}
	/**
	 * @return Returns the klass.
	 */
	public String getKlass() {
		return klass;
	}
	/**
	 * @param klass The klass to set.
	 */
	public void setKlass(String klass) {
		this.klass = klass;
	}
	/**
	 * @return Returns the order.
	 */
	public String getOrder() {
		return order;
	}
	/**
	 * @param order The order to set.
	 */
	public void setOrder(String order) {
		this.order = order;
	}
	/**
	 * @return Returns the phylum.
	 */
	public String getPhylum() {
		return phylum;
	}
	/**
	 * @param phylum The phylum to set.
	 */
	public void setPhylum(String phylum) {
		this.phylum = phylum;
	}
	/**
	 * @return Returns the scientificName.
	 */
	public String getScientificName() {
		return scientificName;
	}
	/**
	 * @param scientificName The scientificName to set.
	 */
	public void setScientificName(String scientificName) {
		this.scientificName = scientificName;
	}



	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return new ToStringBuilder(this)
				.append("kingdom", this.kingdom)
				.append("phylum", this.phylum)
				.append("klass", this.klass)
				.append("order", this.order)
				.append("family", this.family)
				.append("genus", this.genus)
				.append("scientificName",this.scientificName)				
				.toString();
	}



	/**
	 * @see java.lang.Object#equals(Object)
	 */
	public boolean equals(Object object) {
		if (!(object instanceof LinnaeanRankClassification)) {
			return false;
		}
		LinnaeanRankClassification rhs = (LinnaeanRankClassification) object;
		
		return new EqualsBuilder().append(
				this.scientificName, rhs.scientificName).append(this.phylum,
				rhs.phylum).append(this.kingdom, rhs.kingdom).append(
				this.family, rhs.family).append(this.order, rhs.order).append(
				this.klass, rhs.klass).append(this.genus, rhs.genus).isEquals();
	}
	
	public static void main(String[] args) {
		LinnaeanRankClassification a = new LinnaeanRankClassification(null, null,null,null,null,null,"Aus bus"); 
		LinnaeanRankClassification b = new LinnaeanRankClassification(null, null,null,null,null,null,new String("Aus bus"));
		System.out.println("a=b: " + a.equals(b));
		System.out.println("a.hc=b.hc: " + (a.hashCode() == b.hashCode()));
	}



	/**
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return new HashCodeBuilder(1497136033, 448920019).append(this.scientificName).append(
				this.phylum).append(this.kingdom).append(this.family).append(
				this.order).append(this.klass).append(this.genus).toHashCode();
	}
	
	
	
}

package org.ala.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;

/**
 * TaxonName impl using Hibernate Search Annotations
 * 
 * @author "Dave Martin (David.Martin@csiro.au)"
 */
@Entity
@Indexed(index="TaxonNames")
@javax.persistence.Table(name="taxon_name", schema="portal")
public class TaxonName {

	/** The primary key */
	@Id
	@GeneratedValue
	protected Long id;

	/** The canonical name **/
	@Field(index=Index.TOKENIZED, store=Store.YES)
	@IndexedEmbedded(prefix="canonical")
	protected String canonical;
	
	/** The author of this name */
	@Field(index=Index.TOKENIZED, store=Store.YES)
	@IndexedEmbedded(prefix="author")
	protected String author;

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}
	/**
	 * @return the canonical
	 */
	public String getCanonical() {
		return canonical;
	}
	/**
	 * @param canonical the canonical to set
	 */
	public void setCanonical(String canonical) {
		this.canonical = canonical;
	}
	/**
	 * @return the author
	 */
	public String getAuthor() {
		return author;
	}
	/**
	 * @param author the author to set
	 */
	public void setAuthor(String author) {
		this.author = author;
	}
}
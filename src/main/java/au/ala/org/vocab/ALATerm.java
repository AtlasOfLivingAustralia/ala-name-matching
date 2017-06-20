package au.ala.org.vocab;

import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.TermFactory;

/**
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;

 * Copyright (c) 2016 CSIRO
 */
public enum ALATerm implements Term {
    /** The name and authorship, with the author correctly placed */
    nameComplete,
    /** The name and authorship, formatted in some way, usually HTML */
    nameFormatted,
    /** An identifier for non-scientific names */
    nameID,
    /** The status of a piece of information (current, superseeded, etc.) */
    status,
    /** A taxon identifier for the kingdom */
    kingdomID,
    /** A taxon identifier for the phylum */
    phylumID,
    /** A taxon identifier for the class */
    classID,
    /** A taxon identifier for the order */
    orderID,
    /** A taxon identifier for the family */
    familyID,
    /** A taxon identifier for the genus */
    genusID,
    /** A taxon identifier for the species */
    speciesID,
    /** Record type describing a variant (different source, spelling etc.) of a taxon */
    TaxonVariant;

    public static final String NS = "http://ala.org.au/terms/1.0/";
    public static final String PREFIX = "ala:";

    public String qualifiedName() {
        return NS + this.simpleName();
    }

    public String simpleName() {
        return this.name();
    }

    public String toString() {
         return PREFIX + name();
    }

    static {
        TermFactory factory = TermFactory.instance();
        for (Term term : ALATerm.values()) {
            factory.addTerm(term.simpleName(), term, true);
            factory.addTerm(term.qualifiedName(), term);
            factory.addTerm(PREFIX + term.simpleName(), term);
        }
    }

}

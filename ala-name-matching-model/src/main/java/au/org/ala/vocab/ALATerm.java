/*
 * Copyright (c) 2021 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 */

package au.org.ala.vocab;

import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.TermFactory;

/**
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;

 * Copyright (c) 2016 CSIRO
 */
public enum ALATerm implements Term {
    /** The supplied nomenclatural code */
    verbatimNomenclaturalCode,
    /** The supplied taxonomicStatus */
    verbatimTaxonomicStatus,
    /** The supplied nomenclatural status */
    verbatimNomenclaturalStatus,
    /** The supplied taxon remarks */
    verbatimTaxonRemarks,
    /** The name and authorship, with the author correctly placed */
    nameComplete,
    /** The name and authorship, formatted in some way, usually HTML */
    nameFormatted,
    /** An identifier for non-scientific names */
    nameID,
    /** The status of a piece of information (current, superseeded, etc.) */
    status,
    /** A score for taxon/name priority */
    priority,
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
    /** The subphylum classification */
    subphylum,
    /** The subclass classification */
    subclass,
    /** The suborder classification */
    suborder,
    /** The infraorder classification */
    infraorder,
    /** Context labels for names. See http://localcontexts.org/ */
    labels,
    /** A value */
    value,
    /** The principal taxon identifier, for taxa that may have been re-assigned */
    principalTaxonID,
    /** The principal scientific name, for taxa that may have been re-assigned */
    principalScientificName,
    /** Any taxonomic flags */
    taxonomicFlags,
    /** The parent location ID */
    parentLocationID,
    /** The geogreaphy type */
    geographyType,
    /** The distribution, if present as a bar separated list of localities */
    distribution,
    /** The publication DOI of a document */
    doi,
    /** Weight for scoring */
    weight("bayesian:", "http://ala.org.au/bayesian/1.0/"),
    /** Record type describing a yet unplaced reference */
    UnplacedReference,
    /** Record type describing a yet unplaced identifier */
    UnplacedIdentifier,
    /** Record type describing a variant (different source, spelling etc.) of a taxon */
    TaxonVariant,
    /** Record type describing a problem or note about a taxon */
    TaxonomicIssue,
    /** Location information */
    Location("dwc:", "http://rs.tdwg.org/dwc/terms/"),
    /** Location names */
    LocationName,
    /** Location identifier */
    LocationIdentifier;

    public static final String NS = "http://ala.org.au/terms/1.0/";
    public static final String PREFIX = "ala:";

    private String prefix;
    private String namespace;

    ALATerm(String prefix, String namespace) {
        this.prefix = prefix;
        this.namespace = namespace;
    }

    ALATerm() {
        this(PREFIX, NS);
    }

    public String qualifiedName() {
        return this.namespace + this.simpleName();
    }

    public String simpleName() {
        return this.name();
    }

    public String toString() {
         return this.prefix + name();
    }

    static {
        TermFactory factory = TermFactory.instance();
        for (Term term : ALATerm.values()) {
            factory.addTerm(term.simpleName(), term, true);
            factory.addTerm(term.qualifiedName(), term);
            factory.addTerm(term.toString(), term);
        }
    }

}

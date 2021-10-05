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

package au.org.ala.names.index;

/**
 * An exception that indicates a non-fudgeable error in taxon resolution.
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class TaxonResolutionException extends IndexBuilderException {
    /** The offending taxon of resolution */
    private TaxonomicElement taxon;

    public TaxonResolutionException(TaxonomicElement taxon) {
        super();
        this.taxon = this.taxon;
    }

    public TaxonResolutionException(TaxonomicElement taxon, String message) {
        super(message);
        this.taxon = taxon;
    }

    public TaxonResolutionException(TaxonomicElement taxon, String message, Throwable cause) {
        super(message, cause);
        this.taxon = taxon;
    }

    public TaxonResolutionException(TaxonomicElement taxon, Throwable cause) {
        super(cause);
        this.taxon = taxon;
    }

    protected TaxonResolutionException(TaxonomicElement taxon, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.taxon = taxon;
    }

    public TaxonomicElement getTaxon() {
        return taxon;
    }
}

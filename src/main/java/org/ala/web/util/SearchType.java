/*
 *  Copyright (C) 2009 Atlas of Living Australia
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
 */

package org.ala.web.util;

import org.ala.model.*;

/**
 * Enum class for the various search pages, e.g. scientific names, common names,
 * geo regions, localities, etc. Public methods output the type-specific data required
 * for Hibernate search parameters.
 * Used by @see org.ala.web.controller.PagingSearchController
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
public enum SearchType {
    SCIENTIFIC_NAMES("scientificNames", TaxonConcept.class,
            new String[]{"taxonName.canonical", "taxonName.author"},
            new String[]{"kingdomConcept.taxonName.canonical"}),
    COMMON_NAMES("commonNames", CommonName.class,
            new String[]{"name"},
            new String[]{"taxonConcept.taxonName.canonical","taxonConcept.rank","taxonConcept.kingdomConcept.taxonName.canonical"}),
    GEOGRAPHIC_REGIONS("geoRegions", GeoRegion.class,
            new String[]{"name","acronym","geoRegionType.name"},
            new String[]{"geoRegionType.name"}),
    LOCALITIES("localities", Locality.class,
            new String[]{"name","state","postcode"},
            new String[]{"geoRegion.id","geoRegion.name"}),
    DATA_RESOURCES("dataResources", DataResource.class,
            new String[]{"name","description"},
            new String[]{}),
    DATA_PROVIDERS("dataProviders", DataProvider.class,
            new String[]{"name","description"},
            new String[]{}),
    INSTITUTIONS("institutions", Institution.class,
            new String[]{"name","code"},
            new String[]{});

    /** Name of the search page type */
    private String name;
    /** Hibernate Search annotated bean (model) name (class) */
    private Class bean;
    /** String array list of search fields */
    private String[] searchFields;
    /** String array list of display fields */
    private String[] additionalDisplayFields;

    /**
     * Contructor (private)
     *
     * @param name the name to set
     * @param beanName the beanName to set
     * @param searchFields the searchField to set
     * @param displayFields the displayFields to set
     */
    private SearchType(String name, Class beanName, String[] searchFields, String[] displayFields) {
        this.name = name;
        this.bean = beanName;
        this.searchFields = searchFields;
        this.additionalDisplayFields = displayFields;
    }

    /**
     * @return String displayType
     */
    public String[] getAdditionalDisplayFields() {
        return additionalDisplayFields;
    }

    /**
     * @return String name
     */
    public String getName() {
        return name;
    }

    /**
     * @return String searchField
     */
    public String[] getSearchFields() {
        return searchFields;
    }

    /**
     * @return String resultsTotalParam (name + "Total")
     */
    public String getResultTotalParam() {
        return this.name + "Total";
    }

    /**
     * @return String resultsParam (name)
     */
    public String getResultsParam() {
        return this.name;
    }

    /**
     * @return Class bean name (Hiberate Search annotated bean)
     */
    public Class getBean() {
        return bean;
    }
}

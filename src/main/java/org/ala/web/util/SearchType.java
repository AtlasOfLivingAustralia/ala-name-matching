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

import java.util.Arrays;
import java.util.List;
import org.ala.model.*;

/**
 * Enum class for the various search pages, e.g. scientific names, common names,
 * geo regions, localities, etc. Public methods output the type-specific data required
 * for Hibernate search parameters.
 * Used by org.ala.web.controller.PagingSearchController.java.
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
public enum SearchType {
    SCIENTIFIC_NAME("scientificNames", TaxonConcept.class,
            new String[]{"taxonName.canonical", "taxonName.author"},
            new String[]{"taxonName.canonical", "kingdomConcept.taxonName.canonical"}),
    COMMON_NAME("commonNames", CommonName.class,
            new String[]{"name"},
            new String[]{"taxonConcept.taxonName.canonical", "taxonConcept.kingdomConcept.taxonName.canonical"}),
    GEOGRAPHIC_REGIONS("geoRegions", GeoRegion.class,
            new String[]{"name","acronym","geoRegionType.name"},
            new String[]{"geoRegionType.name"}),
    LOCALITIES("localities", Locality.class,
            new String[]{"name","state","postcode"},
            new String[]{"geoRegion.name"}),
    DATA_RESOURCES("dataResources", DataResource.class,
            new String[]{"name","description"},
            new String[]{}),
    DATA_PROVIDERS("dataProviders", DataProvider.class,
            new String[]{"name","description"},
            new String[]{}),
    INSTITUTIONS("institutions", Institution.class,
            new String[]{"name","code"},
            new String[]{});
    
    private String name;
    private Class bean;
    private String[] searchFields;
    private String[] displayFields;
    
    private SearchType(String name, Class beanName, String[] searchFields, String[] displayFields) {
        this.name = name;
        this.bean = beanName;
        this.searchFields = searchFields;
        this.displayFields = displayFields;
    }

    public String[] getDisplayFields() {
        return displayFields;
    }

    public String getName() {
        return name;
    }

    public String[] getSearchFields() {
        return searchFields;
    }

    public String getResultTotalParam() {
        return this.name + "Total";
    }

    public String getResultsParam() {
        return this.name;
    }

    public Class getBean() {
        return bean;
    }
}

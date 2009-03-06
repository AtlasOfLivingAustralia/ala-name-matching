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

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Enum class for selecting the Solr facet for breakdown by
 * taxon rank.
 * 
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
public enum RankFacet {
    KINGDOM (1000, "kingdom"),
    PHYLUM  (2000, "phylum"),
    CLASS   (3000, "class"),
    ORDER   (4000, "order"),
    FAMILY  (5000, "family"),
    GENUS   (6000, "genus"),
    SPECIES (7000, "species");
    
    // Allow reverse-lookup (based on http://www.ajaxonomy.com/2007/java/making-the-most-of-java-50-enum-tricks)
    private static final Map<String,RankFacet> fieldLookup
          = new HashMap<String,RankFacet>();
    private static final Map<Integer,RankFacet> idLookup
          = new HashMap<Integer,RankFacet>();
    
    static {
         for (RankFacet rf : EnumSet.allOf(RankFacet.class)) {
             fieldLookup.put(rf.getRank(), rf);
             idLookup.put(rf.getId(), rf);
         }        
    }

    private Integer id;
    private String field;
    
    /**
     * Constructor for setting the 'value'
     * @param field value as String
     */
    private RankFacet(Integer id, String field) {
        this.id = id;
        this.field = field;
    }

    /**
     * @return id the id
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * @return field the field
     */
    public String getRank() {
        return this.field;
    }

    /**
     * Get the field value
     * @return the field
     */
    public String getFacetField() {
        return field + "_concept_id";
    }

    /**
     * @param field
     * @return RankFacet the RankFacet
     */
    public static RankFacet getForField(String field) {
          return fieldLookup.get(field); 
     }

    /**
     * @param id
     * @return RankFacet the RankFacet
     */
    public static RankFacet getForId(Integer id) {
          return idLookup.get(id); 
     }
}

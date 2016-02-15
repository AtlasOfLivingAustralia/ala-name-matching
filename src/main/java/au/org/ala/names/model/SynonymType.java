/*
 * Copyright (C) 2014 Atlas of Living Australia
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
 */
package au.org.ala.names.model;

/**
 * Stores the Synonym type information.  These synonyms types are based on the ones supplied in NSL relationship CSV.
 * The numeric values come from the dictionary_relationship table of the ala_names as setup by
 * http://code.google.com/p/ala-portal/source/browse/trunk/ala-names-generator/src/main/resources/ala-names-setup.sql
 * <p>
 * Note that this link no longer functions. Instead, synonym labels have been included in the enumeration.
 * TODO: Get a correct description for the synonym types.
 *
 * @author Natasha Carter
 * @author Doug Palmer
 */
public enum SynonymType {

    HOMONYM(2, "Concept is a homonym to another concept", "homonym"),
    INVALID_PUBLICATION(3, "This name was invalid in a publication", "invalid publication"),
    INVALID(4, "", "invalid"),
    MISAPPLIED(5, "This name has been misapplied to the accepted concept in the past", "misapplied"),
    REPLACED(6, "This name has been replaced with the accepted concept", "replaced", "replaced synonym"),
    TRADE_NAME(7, "This name is a trade name for the accepted concept",  "trade name"),
    VARIANT(8, "This name is a variant of the accepted concept",  "variant"),
    EXCLUDES(9, "Name is excluded from the NSL",  "excluded"),
    GENERIC_COMBINATION(10, "A generic combination of the accepted concept.", "generic combination"),
    GENERIC_COMB_UNPLACED(11, "An unplaced generic combination of the accepted concept", "generic combination unplaced"),
    LEGISLATIVE_NAME(12, "This name is a legislative name of the accepted concept", "legislative name"),
    MISC_LITERATURE(13, "This name is a miscellaneous literature name for the accepted concept",  "miscellaneous literature"),
    SYNONYM(14, "", "synonym", "unknown", "[unknown]"),
    SYNONYM_EMEDATION(15, "", "synonym emedation"),
    OBJECTIVE_SYNONYM(16, "", "objective synonym", "nomenclatural synonym", "homotypic synonym", "objectiveSynonym", "homotypicSynonym"),
    ORIGINAL_SPELLING(17, "",  "original spelling"),
    REPLACEMENT_NAME(18, "", "replacement name"),
    SYNONYM_SENS_LAT(19, "", "synonym sens lat"),
    SUBJECTIVE_SYNONYM(20, "", "subjective synonym", "taxonomic synonym", "heterotypic synonym", "subjectiveSynonym", "heterotypicSynonym"),
    SUBSEQUENT_MISSPELLING(21, "", "subsequent misspelling", "orthographic variant"),
    SYNONYM_SYNONYM(22, "", "synonym synonym"),
    INCLUDES_INCERTAE_SEDIS(24, "", "incertae sedis"),
    INCLUDES_NOMENCLATURAL(25, ""),
    INCLUDES_SP_INQUIRENDA(26, "", "species inqurenda"),
    INCLUDES_TAXONOMIC(27, ""),
    INCLDUES_UNPLACED(28, ""),
    CONGRUENT(33, "", "congruent"),
    CONGRUENT_EMENDATION(34, "", "congruent emendation"),
    CONGRUENT_ORIGINAL_SPELLING(35, "", "congruent original spelling"),
    CONGRUENT_REPLACEMENT_NAME(36, "", "congruent replacement name"),
    CONGRUENT_SUBJECTIVE(37, "", "congruent subjective"),
    CONGRUENT_SYNONYM(38, "", "congruent synonym"),
    DOUBTFUL_MISAPPLIED(39, "", "doubtful misapplied"),
    DOUBTFUL_NOMENCLATURAL_SYNONYM(40, "", "doubtful nomenclatural synonym"),
    DOUBTFUL_PRO_PARTE_MISAPPLIED(41, "", "doubtful pro parte misapplied"),
    DOUBTFUL_PRO_PARTE_TAXONOMIC_SYNONYM(42, "", "doubtful pro parte taxonomic synonym"),
    DOUBTFUL_SYNONYM(43, "", "doubtful synonym"),
    DOUBTFUL_TAXONOMIC_SYNONYM(44, "", "doubtful taxonomic synonym"),
    PRO_PARTE_MISAPPLIED(45, "", "pro parte misapplied"),
    PRO_PARTE_NOMENCLATURAL_SYNONYM(46, "", "pro parte nomenclatural synonym"),
    PRO_PARTE_TAXONOMIC_SYNONYM(46, "", "pro parte taxonomic synonym"),
    PRO_PARTE_SYNONYM(47, "", "pro parte synonym", "proParteSynonym"),
    BASIONYM(48, "", "basionym"),
    ISONYM(49, "", "isonym"),
    UNPLACED(50, "", "unplaced"),
    COL_SYNONYM(52, "A synonym that has come from CoL", "col synonym");
    private static final java.util.Map<String, SynonymType> idLookup = new java.util.HashMap<String, SynonymType>();

    static {
        for (SynonymType st : SynonymType.values()) {
            if (st.id != null)
                idLookup.put(st.id.toString(), st);
            for (String label: st.labels)
                idLookup.put(label, st);
        }
    }

    private Integer id;
    private String[] labels;
    private String description;

    SynonymType(int id, String description, String... labels) {
        this.id = id;
        this.labels = labels;
        this.description = description;
    }

    public Integer getId() {
        return id;
    }

    public String[] getLabels() {
        return labels;
    }

    public String getDescription() {
        return description;
    }

    public static SynonymType getTypeFor(String value) {
        return value != null ? idLookup.get(value) : null;
    }
}

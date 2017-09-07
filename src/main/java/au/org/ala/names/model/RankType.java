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

import org.gbif.api.vocabulary.Rank;

import java.util.*;


/**
 * An enumeration of the rank types available in ALA, with some text vocabulary that can be used to match to a RankType.
 * <p/>
 * This is based on org.ala.web.util.RankFacet
 *
 * @author Natasha
 */
public enum RankType {

    DOMAIN(800, "kingdom", Rank.DOMAIN, null, 800, false, "http://rs.tdwg.org/ontology/voc/TaxonRank#Domain", "Domain", "Superkingdom", "Empire"),
    KINGDOM(1000, "kingdom", Rank.KINGDOM, 2f, 1000, false, "http://rs.tdwg.org/ontology/voc/TaxonRank#Kingdom", "Kingdom"),
    SUBKINGDOM(1200, "subkingdom", Rank.SUBKINGDOM, null, 1200, false, "http://rs.tdwg.org/ontology/voc/TaxonRank#Subkingdom"),
    SUPERPHYLUM(1800, "superphylum", Rank.SUPERPHYLUM, null, 2800, false, "http://rs.tdwg.org/ontology/voc/TaxonRank#Superphylum"),
    PHYLUM(2000, "phylum", Rank.PHYLUM, 2f, 2000, false, "http://rs.tdwg.org/ontology/voc/TaxonRank#Phylum", "Phylum", "division botany", "Division Botany"),
    SUBPHYLUM(2200, "subphylum", Rank.SUBPHYLUM, null, 2200, false, "http://rs.tdwg.org/ontology/voc/TaxonRank#Subphylum", "subdivision botany"),
    SUPERCLASS(2800, "superclass", Rank.SUPERCLASS, null, 2800, false, "http://rs.tdwg.org/ontology/voc/TaxonRank#Superclass"),
    CLASS(3000, "class", Rank.CLASS, 2f, 3000, false, "http://rs.tdwg.org/ontology/voc/TaxonRank#Class", "Class"),
    SUBCLASS(3200, "subclass", Rank.SUBCLASS, null, 3200, false, "http://rs.tdwg.org/ontology/voc/TaxonRank#Subclass"),
    INFRACLASS(3350, "infraclass", Rank.INFRACLASS, null, 3350, false),
    SUBINFRACLASS(3370, "subinfraclass", null, null, 3370, false),
    SUPERDIVISION_ZOOLOGY(3450, "superdivision zoology", null, null, 3450, false),
    DIVISION_ZOOLOGY(3500, "division zoology", null, null, 3500, false),
    SUBDIVISION_ZOOLOGY(3550, "subdivision zoology", null, null, 3550, false),
    SUPERCOHORT(3650, "supercohort", null, null, 3650, false),
    COHORT(3700, "cohort", Rank.COHORT, null, 3700, false),
    SUBCOHORT(3750, "subcohort", Rank.SUBCOHORT, null, 3750, false),
    SUPERORDER(3800, "superorder", Rank.SUPERORDER, null, 3800, false, "http://rs.tdwg.org/ontology/voc/TaxonRank#Superorder"),
    ORDER(4000, "order", Rank.ORDER, 2f, 4000, false, "http://rs.tdwg.org/ontology/voc/TaxonRank#Order", "Order"),
    SUBORDER(4200, "suborder", Rank.SUBORDER, null, 4200, false, "http://rs.tdwg.org/ontology/voc/TaxonRank#Suborder"),
    INFRAORDER(4350, "infraorder", Rank.INFRAORDER, null, 4350, false),
    PARVORDER(4400, "parvorder", Rank.PARVORDER, null, 4400, false),
    SUPERSERIES_ZOOLOGY(4445, "superseries zoology", null, null, 4445, false),
    SERIES_ZOOLOGY(4450, "series zoology", null, null, 4450, false),
    SUBSERIES_ZOOLOGY(4455, "subseries zoology", null, null, 4455, false),
    SUPERSECTION_ZOOLOGY(4465, "supersection zoology", null, null, 4465, false),
    SECTION_ZOOLOGY(4470, "section zoology", null, null, 4470, false),
    SUBSECTION_ZOOLOGY(4475, "subsection zoology", null, null, 4475, false),
    SUPERFAMILY(4500, "superfamily", Rank.SUPERFAMILY, null, 4500, false, "Superfamily"),
    FAMILY(5000, "family", Rank.FAMILY, 2f, 5000, false, "http://rs.tdwg.org/ontology/voc/TaxonRank#Family", "Family"),
    SUBFAMILY(5500, "subfamily", Rank.SUBFAMILY, null, 5500, true, "http://rs.tdwg.org/ontology/voc/TaxonRank#Subfamily"),
    INFRAFAMILY(5525, "infrafamily", Rank.INFRAFAMILY, null, 5525, false, "http://rs.tdwg.org/ontology/voc/TaxonRank#Infrafamily"),
    SUPERTRIBE(5550, "supertribe", Rank.SUPERTRIBE, null, 5550, false, "http://rs.tdwg.org/ontology/voc/TaxonRank#TSupertribe"),
    TRIBE(5600, "tribe", Rank.TRIBE, null, 5600, false, "http://rs.tdwg.org/ontology/voc/TaxonRank#Tribe"),
    SUBTRIBE(5700, "subtribe", Rank.SUBTRIBE, null, 5700, false, "http://rs.tdwg.org/ontology/voc/TaxonRank#Subtribe"),
    SUPERGENUS(5900, "genus", null, null, 5900, false, "http://rs.tdwg.org/ontology/voc/TaxonRank#Supergenus", "Supergenus"),
    GENUS_GROUP(5950, "genus group", null, null, 5950, true, "aggregate genera", "Aggregate Genera", "Genus Group"),
    GENUS(6000, "genus", Rank.GENUS, 3f, 6000, false, "http://rs.tdwg.org/ontology/voc/TaxonRank#Genus", "Genus"),
    SUBGENUS(6500, "subgenus", Rank.SUBGENUS, null, 6500, false, "http://rs.tdwg.org/ontology/voc/TaxonRank#Subgenus"),
    SUPERSECTION_BOTANY(6550, "supersection botany", Rank.SECTION, null, 6550, false),
    SECTION_BOTANY(6600, "section botany", Rank.SECTION, null, 6600, false, "http://rs.tdwg.org/ontology/voc/TaxonRank#Section"),
    SUBSECTION_BOTANY(6700, "subsection botany", Rank.SUBSECTION, null, 6700, false, "http://rs.tdwg.org/ontology/voc/TaxonRank#Subsection"),
    SUPERSERIES_BOTANY(6750, "superseries botany", null, null, 6750, false),
    SERIES_BOTANY(6800, "series botany", Rank.SERIES, null, 6800, false, "http://rs.tdwg.org/ontology/voc/TaxonRank#Series"),
    SUBSERIES_BOTANY(6900, "subseries botany", Rank.SUBSERIES, null, 6900, false, "http://rs.tdwg.org/ontology/voc/TaxonRank#Subseries"),
    INFRAGENERICNNAME(6925, "infragenericname", Rank.INFRAGENERIC_NAME, null, 6925, false),
    SPECIES_GROUP(6950, "species group", Rank.SPECIES_AGGREGATE, null, 6950, true, "aggregate species", "Aggregate Species", "Species Group"),
    SUPERSPECIES(6960, "superspecies", null, null, 6960, false, "http://rs.tdwg.org/ontology/voc/TaxonRank#Species", "Superspecies"),
    SPECIES_SUBGROUP(6975, "species subgroup", null, null, 6975, true, "Species Subroup"),
    SPECIES(7000, "species", Rank.SPECIES, 2f, 7000, false, "http://rs.tdwg.org/ontology/voc/TaxonRank#Species", "Species"),
    NOTHOSPECIES(7001, "nothospecies", null, null, 7001, false),
    HOLOMORPH(7100, "holomorph", null, null, 7100, false),
    ANAMORPH(7120, "anamorph", null, null, 7120, false),
    TELEOMORPH(7140, "teleomorph", null, null, 7140, false),
    SUBSPECIES(8000, "subspecies", Rank.SUBSPECIES, null, 8000, false, "http://rs.tdwg.org/ontology/voc/TaxonRank#Subspecies", "subsp", "subsp.", "ssp", "subtaxon", "staxon", "subsp..", "susp"),
    NOTHOSUBPECIES(8001, "nothosubspecies", null, null, 8001, false),
    INFRASPECIFICNAME(8005, "infraspecificname", Rank.INFRASPECIFIC_NAME, null, 8005, false, "Infraspecies"),
    INFRASUBSPECIESNAME(-1, "infrasubspeciesname", Rank.INFRASUBSPECIFIC_NAME, null, 8007, false),
    VARIETY(8010, "variety", Rank.VARIETY, null, 8010, false, "http://rs.tdwg.org/ontology/voc/TaxonRank#Variety", "var.", "var,", "var"),
    NOTHOVARIETY(8011, "nothovariety", null, null, 8011, false),
    SUBVARIETY(8015, "subvariety", Rank.SUBVARIETY, null, 8015, false, "http://rs.tdwg.org/ontology/voc/TaxonRank#Sub-Variety", "subvar."),
    FORM(8020, "form", Rank.FORM, null, 8020, false, "http://rs.tdwg.org/ontology/voc/TaxonRank#Form", "forma"),
    NOTHOFORM(8021, "nothoform", null, null, 8021, false),
    SUBFORM(8025, "subform", Rank.SUBFORM, null, 8025, false, "http://rs.tdwg.org/ontology/voc/TaxonRank#Subform"),
    BOIVAR(8030, "biovar", null, null, 8030, false),
    SEROVAR(8040, "serovar", Rank.SEROVAR, null, 8040, false),
    FORMASPECIALIS(8043, "forma specialis", Rank.FORMA_SPECIALIS, null, 8043, true, "f.sp.", "formaspecialis"),
    CULTIVARGROUP(-1, "cultivargroup", Rank.CULTIVAR_GROUP, null, 8045, true),
    CULTIVAR(8050, "cultivar", Rank.CULTIVAR, null, 8050, false, "http://rs.tdwg.org/ontology/voc/TaxonRank#Cultivar", "cv."),
    PATHOVAR(8080, "pathovar", Rank.PATHOVAR, null, 8080, false),
    INFORMAL(-1, "informal", null, null, 100000, true),
    UNRANKED(0, "unranked", Rank.UNRANKED, null, 200000, true),
    SUPRAGENERICNAME(8200, "supragenericname", Rank.SUPRAGENERIC_NAME, null, 8200, true, "http://rs.tdwg.org/ontology/voc/TaxonRank#SupragenericTaxon"),
    HYBRID(8150, "hybrid", null, null, 8150, true);


    // Allow reverse-lookup (based on http://www.ajaxonomy.com/2007/java/making-the-most-of-java-50-enum-tricks)
    private static final Map<String, RankType> fieldLookup = new HashMap<String, RankType>();
    private static final Map<Integer, RankType> idLookup = new HashMap<Integer, RankType>();
    private static final Map<Rank, RankType> cbRankLookup = new HashMap<Rank, RankType>();

    private static final Map<String, RankType> strRankLookup = new HashMap<String, RankType>();

    static {
        for (RankType rt : EnumSet.allOf(RankType.class)) {
            try {
                fieldLookup.put(rt.getRank(), rt);
                idLookup.put(rt.getId(), rt);
                if (rt.cbRank != null)
                    cbRankLookup.put(rt.cbRank, rt);
                strRankLookup.put(rt.getRank(), rt);
                if (rt.strRanks != null) {
                    for (String rank : rt.strRanks)
                        strRankLookup.put(rank.toLowerCase(), rt);
                }
            } catch (RuntimeException ex) {
                System.err.println("Unable to load " + rt);
                throw ex;
            }
        }
    }

    private Integer id;
    private String field;
    private Rank cbRank;
    private Float boost;
    private int sortOrder;
    private boolean loose;
    private String[] strRanks;

    RankType(Integer id, String field, Rank rank, Float boost, int sortOrder, boolean loose, String... strRanks) {
        this.id = id;
        this.sortOrder = sortOrder;
        this.field = field;
        this.cbRank = rank;
        this.strRanks = strRanks;
        this.boost = boost;
        this.loose = loose;
    }

    public static Set<RankType> getAllRanksBelow(Integer rank) {
        Set<RankType> ranks = new TreeSet<RankType>();
        for (RankType rt : EnumSet.allOf(RankType.class)) {
            if (rt.getId() >= rank)
                ranks.add(rt);
        }
        return ranks;
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
     * Get the equivalent GBIF rank
     *
     * @return The GBIF rank
     */
    public Rank getCbRank() {
        return this.cbRank;
    }

    /**
     * Get the sort order.
     * <p>
     * Wobbly ranks tend to get placed last
     * </p>
     *
     * @return The sort order for this rank.
     */
    public int getSortOrder() {
        return sortOrder;
    }

    /**
     * @return The additional labels for the rank
     */
    public String[] getStrRanks() {
        return strRanks;
    }

    /**
     * Gets the Rank Type for the supplied raw string...
     *
     * @param rank
     * @return
     */
    public static RankType getForStrRank(String rank) {
        return strRankLookup.get(rank.toLowerCase());
    }

    public Float getBoost() {
        return boost;
    }

    /**
     * Is this a "loose" rank, likely to be used as a grab-bag of information.
     * 
     * @return True if loose
     */
    public boolean isLoose() {
        return loose;
    }

    /**
     * Compare two ranks.
     * <p>
     * Not all ranks are comparable.
     * Informal and unplaced ranks can't be compared with ordinary ranks.
     * The result is then false
     * </p>
     *
     * @param other The other rank
     *
     * @return True if the ranks are comparable and this rank is higher than the other rank
     */
    public boolean isHigherThan(RankType other) {
        Integer l = other != null ? other.getId() : null;
        if (this.id == null || this.id <= 0 || l == null || l <= 0) // Incomparable
            return false;
        return id < l;
    }

    /**
     * @param field
     * @return The rankType for the field
     */
    public static RankType getForName(String field) {
        return fieldLookup.get(field);
    }

    public static RankType getForCBRank(Rank cbRank) {
        return cbRankLookup.get(cbRank);
    }

    /**
     * @param id
     * @return The RankType for the specified Portal id
     */
    public static RankType getForId(Integer id) {
        return idLookup.get(id);
    }

}



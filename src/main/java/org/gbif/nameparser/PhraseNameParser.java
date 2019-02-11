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

package org.gbif.nameparser;

import au.org.ala.names.model.ALAParsedName;
import au.org.ala.names.model.RankType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.gbif.api.exception.UnparsableException;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.Rank;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Parser that can be used to parse a "Phrase" name.  It is assumed
 * that any name being parsed has not been matched to a regular scientific name.
 * Along with a number of other special cases:
 * <ul>
 *     <li>Phrase names have the form <em>Genus rank "Name" (Voucher)</em>
 *      Everything to the right of the rank marker to match the phrase name form.
 *     </li>
 *     <li>Cases where someone has screwed up the case of an infrageneric name</li>
 *     <li>Names of the form <em>Genus</em> species <em>12</em> are treated as placeholder names</li>
 * </ul>
 * <p>
 * It extends the GBIF NameParser {@see org.gbif.nameparser.GBIFNameParser}, when the name is not wellformed this parser will then
 * attempt parse it into a phrase name. See https://code.google.com/p/ala-portal/wiki/ALANames#Glossary
 * for more information about phrase names.
 * Moved to package org.gbif.nameparser to allow access to recently protected fields
 * </p>
 *
 * @author Natasha Carter
 */
public class PhraseNameParser extends GBIFNameParser {

    public static final HashMap<String, Rank> VALID_PHRASE_RANKS;

    static {
        HashMap<String, Rank> ranks = new HashMap<String, Rank>();
        ranks.put("subsp", Rank.SUBSPECIES);
        ranks.put("ssp", Rank.SUBSPECIES);
        ranks.put("var", Rank.VARIETY);
        ranks.put("sp", Rank.SPECIES);
        ranks.put("cv", Rank.CULTIVAR); // added for the situation where phrase names are mistaken for cultivars
        VALID_PHRASE_RANKS = ranks;
    }

    //protected static final String LOCATION_OR_DESCR = "["+NAME_LETTERS+"0-9'](?:["+all_letters_numbers+" -]+|\\.)";
    public static final String ALL_LETTERS_NUMBERS = NormalisedNameParser.NAME_LETTERS + NormalisedNameParser.name_letters + "0-9";
    protected static final String LOCATION_OR_DESCR = "(?:[" + ALL_LETTERS_NUMBERS + " -'\"_\\.]+|\\.)";
    protected static final String VOUCHER = "(\\([" + ALL_LETTERS_NUMBERS + "- \\./&,']+\\))";
    protected static final String SOURCE_AUTHORITY = "([" + ALL_LETTERS_NUMBERS + "\\[\\]'\" -,\\.]+|\\.)";
    protected static final String PHRASE_RANKS = "(?:" + StringUtils.join(VALID_PHRASE_RANKS.keySet(), "|") + ")\\.? ";
    private static final String RANK_MARKER_ALL = "(notho)? *(" + StringUtils.join(RankUtils.RANK_MARKER_MAP.keySet(), "|")
            + ")\\.?";
    public static final Pattern RANK_MARKER = Pattern.compile("^" + RANK_MARKER_ALL + "$");
    protected static final Pattern SPECIES_PATTERN = Pattern.compile("sp\\.?");

    protected static final Pattern POTENTIAL_SPECIES_PATTERN = Pattern.compile("^" + "([\\x00-\\x7F\\s]*)(" + SPECIES_PATTERN.pattern() + " )" + "([" + NormalisedNameParser.name_letters + "]{3,})(?: *)" + "([\\x00-\\x7F\\s]*)");

    protected static final Pattern PHRASE_PATTERN = Pattern.compile("^" +
            //GROUP 1 is normal scientific name - will either represent a Monomial or binomial
            "([\\x00-\\x7F\\s]*)(?: *)"
            // Group 2 is the Rank marker.  For a phrase it needs to be sp. subsp. or var.
            + "(" + PHRASE_RANKS + ")(?: *)"
            // Group 3 indicates the mandatory location/desc for the phrase name. But it may be possible to have homonyms if the VOUCHER is not supplied
            + "(" + LOCATION_OR_DESCR + ")"
            //Group 4 is the VOUCHER for the phrase it indicates the collector and a voucher id
            + VOUCHER + "?"
            //Group 5 is the party propsoing addition of the taxon
            + SOURCE_AUTHORITY + "?$"
    );

    protected static final Pattern WRONG_CASE_INFRAGENERIC = Pattern.compile("(?:" + "\\( ?([" + NormalisedNameParser.name_letters + "-]+) ?\\)"
            + "|" + "(" + StringUtils.join(RankUtils.RANK_MARKER_MAP_INFRAGENERIC.keySet(), "|") + ")\\.? ?([" + NormalisedNameParser.NAME_LETTERS
            + "][" + NormalisedNameParser.name_letters + "-]+)" + ")");

    protected static final String RANK_MARKER_INFRAGENERIC = "(?:" + StringUtils.join(RankUtils.RANK_MARKER_MAP_INFRAGENERIC.keySet(), "|") + ")\\.?";
    protected static final String NUMBER_PLACEHOLDER = "\\d+\\.?";
    protected static final Pattern NUMBERED_PLACEHOLDER = Pattern.compile("(" + NormalisedNameParser.MONOMIAL + ")\\s+((" + RANK_MARKER_INFRAGENERIC + "|[Gg]roup|[Ss]ub[Gg]roup)[\\s_\\-]*" + NUMBER_PLACEHOLDER + ")(\\s+" + NormalisedNameParser.AUTHOR_TEAM + "(\\s*,\\s*" + NormalisedNameParser.YEAR + ")?)?");

    protected static final Pattern IGNORE_MARKERS = Pattern.compile("s[\\.| ]+str[\\. ]+");


    @Override
    public ParsedName parse(String scientificName, Rank rank) throws UnparsableException {
        ParsedName pn = super.parse(scientificName, rank);
        // Check for a numbered placeholder
        Matcher m = NUMBERED_PLACEHOLDER.matcher(scientificName);
        if (m.matches()) {
            String nameRank = m.group(3);
            String genus = m.group(1);
            String epithet = m.group(2);
            String author = m.group(4) == null ? null : m.group(4).trim();
            if (StringUtils.isNotBlank(genus) && StringUtils.isNotBlank(epithet) && StringUtils.isNotBlank(nameRank)) {
                if (StringUtils.isNotBlank(author))
                    pn.setAuthorship(author);
                pn.setAuthorsParsed(true);
                pn.setGenusOrAbove(genus);
                pn.setRank(rank != null ? rank : RankUtils.inferRank(nameRank));
                pn.setSpecificEpithet(epithet.replaceAll("[ _-]+", "-"));
                pn.setType(NameType.PLACEHOLDER);
                return pn;
            }
        }
        if (pn.getType() != NameType.SCIENTIFIC && isPhraseRank(pn.getRank()) && (!pn.isAuthorsParsed() || pn.getSpecificEpithet() == null || SPECIES_PATTERN.matcher(pn.getSpecificEpithet()).matches())) {
            //if the rank marker is sp. and the word after the rank marker is lower case check to see if removing the marker will result is a wellformed name
            if (SPECIES_PATTERN.matcher(scientificName).find()) {
                Matcher m1 = POTENTIAL_SPECIES_PATTERN.matcher(scientificName);
                //System.out.println(POTENTIAL_SPECIES_PATTERN.pattern());
                if (m1.find()) {
                    //now reparse without the rankMarker
                    String newName = m1.group(1) + m1.group(3) + StringUtils.defaultString(m1.group(4), "");
                    pn = super.parse(newName, rank);
                    if (pn.getType() == NameType.SCIENTIFIC)
                        return pn;
                }
            }
            //check to see if the name represents a phrase name
            m = PHRASE_PATTERN.matcher(scientificName);
            if (m.find()) {
                ALAParsedName alapn = new ALAParsedName(pn);
                alapn.setInfraGeneric(null);
                alapn.setSpecificEpithet(null);
                alapn.setInfraSpecificEpithet(null);
                alapn.setAuthorship(null);
                alapn.setAuthorsParsed(false);
                alapn.setLocationPhraseDescription(StringUtils.trimToNull(m.group(3)));
                alapn.setPhraseVoucher(StringUtils.trimToNull(m.group(4)));
                alapn.setPhraseNominatingParty(StringUtils.trimToNull(m.group(5)));
                return alapn;
            }

        } else {
            //check for the situation where the subgenus was supplied without Title case.
            m = WRONG_CASE_INFRAGENERIC.matcher(scientificName);
            if (m.find()) {
                scientificName = WordUtils.capitalize(scientificName, '(');
                pn = super.parse(scientificName, rank);
            }
        }
        return pn;
    }

    private boolean isPhraseRank(Rank rank) {

        if (rank == null)
            return false;
        return VALID_PHRASE_RANKS.containsValue(rank);
    }
}

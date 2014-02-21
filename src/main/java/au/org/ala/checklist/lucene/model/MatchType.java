package au.org.ala.checklist.lucene.model;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum MatchType{
        DIRECT("directMatch", "Deprecated"),
        EXACT("exactMatch","The supplied name matched the name exactly.  Very small chance of an incorrect match."),
        CANONICAL("canonicalMatch","The supplied name was parsed into canonical form before a match was obtained. There is a chance that the match is incorrect due to parse errors."),
        PHRASE("phraseMatch", "A match was determined by parsing the name into a phrase name.  Very small chance of an incorrect match."),
        SOUNDEX("fuzzyMatch", "A match was determined by using a sound expression of the supplied name.  There is a greater that average chance that the match is incorrect."),
        ALTERNATE("alternateMatch", "Deprecated"),
        SEARCHABLE("searchableMatch", "Deprecated"),
        VERNACULAR("vernacularMatch", "A match was determined by the vernacular name. Matches of this type may be unreliable due to the regional/duplicate nature of common names."),
        RECURSIVE("higherMatch", "The match is based on the higher level classification"),
        TAXON_ID("taxonIdMatch","The match was based on the supplied taxon concept ID rather than the scientific name.");
        private String title;
        private String description;

        private static final Map<String, MatchType> titleLookup = new HashMap<String, MatchType>();

        static {
        for (MatchType mt : EnumSet.allOf(MatchType.class)) {
                titleLookup.put(mt.title, mt);
            }
        }

        MatchType(String title, String description){

            this.title = title;
            this.description = description;
        }
        @Override
        public String toString(){
            return title;
        }
        public MatchType getMatchType(String match){
            return titleLookup.get(match);
        }
}
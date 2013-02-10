
package au.org.ala.data.util;

import au.org.ala.data.model.ALAParsedName;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.gbif.ecat.model.ParsedName;
import org.gbif.ecat.parser.NameParser;
import org.gbif.ecat.parser.UnparsableException;
import org.gbif.ecat.voc.NameType;
import org.gbif.ecat.voc.Rank;

/**
 *
 * A Parser that can be used to parse a "Phrase" name.  It is assumed
 * that any name being parsed has not been matched to a regular scientific name.
 *
 * It expects everything to the right of the rank marker.
 *
 * @author Natasha Carter
 */
public class PhraseNameParser extends NameParser{

    public static final HashMap<String,Rank> VALID_PHRASE_RANKS;
       static
       {
    	   HashMap<String,Rank> ranks = new HashMap<String,Rank>();
    	   ranks.put("subsp",Rank.SUBSPECIES);
    	   ranks.put("ssp",Rank.SUBSPECIES);
           ranks.put("var",Rank.VARIETY);
           ranks.put("sp", Rank.SPECIES);
           ranks.put("cv", Rank.SPECIES); // added for the situation where phrase names are mistaken for cultivars
           VALID_PHRASE_RANKS = ranks;
       }

    //protected static final String LOCATION_OR_DESCR = "["+NAME_LETTERS+"0-9'](?:["+all_letters_numbers+" -]+|\\.)";
   protected static final String LOCATION_OR_DESCR = "(?:["+all_letters_numbers+" -'\"_\\.]+|\\.)";
    protected static final String VOUCHER ="(\\(["+all_letters_numbers+"- \\./&,']+\\))";
    protected static final String SOURCE_AUTHORITY="(["+all_letters_numbers+"\\[\\]'\" -,\\.]+|\\.)";
    protected static final String PHRASE = "";
    protected static final String  PHRASE_RANKS = "(?:"+StringUtils.join(VALID_PHRASE_RANKS.keySet(), "|")+")\\.? ";
    private static final String RANK_MARKER_ALL = "(notho)? *(" + StringUtils.join(Rank.RANK_MARKER_MAP.keySet(), "|")
      + ")\\.?";
    public static final Pattern RANK_MARKER = Pattern.compile("^" + RANK_MARKER_ALL + "$");
    protected static final Pattern SPECIES_PATTERN = Pattern.compile("sp\\.?");

    protected static final Pattern POTENTIAL_SPECIES_PATTERN = Pattern.compile("^"+"([\\x00-\\x7F\\s]*)("+SPECIES_PATTERN.pattern()+" )" + "(["+name_letters+"]{3,})(?: *)" + "([\\x00-\\x7F\\s]*)");

    protected static final Pattern PHRASE_PATTERN = Pattern.compile("^" +
            //GROUP 1 is normal scientific name - will either represent a Monomial or binomial
            "([\\x00-\\x7F\\s]*)(?: *)"
            // Group 2 is the Rank marker.  For a phrase it needs to be sp. subsp. or var.
            + "("+PHRASE_RANKS +")(?: *)"
            // Group 3 indicates the mandatory location/desc for the phrase name. But it may be possible to have homonyms if the VOUCHER is not supplied
            + "(" +LOCATION_OR_DESCR +")"
            //Group 4 is the VOUCHER for the phrase it indicates the collector and a voucher id
            + VOUCHER +"?" 
            //Group 5 is the party propsoing addition of the taxon
            + SOURCE_AUTHORITY +"?$"
            );

    protected static final Pattern WRONG_CASE_INFRAGENERIC = Pattern.compile("(?:" + "\\( ?([" + name_letters + "-]+) ?\\)"
      + "|" + "(" + StringUtils.join(Rank.RANK_MARKER_MAP_INFRAGENERIC.keySet(), "|") + ")\\.? ?([" + NAME_LETTERS
      + "][" + name_letters + "-]+)" + ")");

    protected static final Pattern IGNORE_MARKERS = Pattern.compile("s[\\.| ]+str[\\. ]+");

    public void testParse(String scientificName){
        //extract the complete name into components
        // (valid scientific name part)(rank marker)(phrase name part)
        Matcher matcher = PHRASE_PATTERN.matcher(scientificName);
        if(matcher.matches()){
            System.out.println("MATCHES :)");
           // matcher.find();
            System.out.println(matcher.group(1));
            System.out.println(matcher.group(2));
            System.out.println(matcher.group(3));
            System.out.println(matcher.group(4));
            System.out.println(matcher.group(5));
        }
        else{
            System.out.println("DOES NOT MATCH");
        }

    }
    @Override
    public <T> ParsedName<T> parse(String scientificName) throws UnparsableException {        
        ParsedName pn= super.parse(scientificName);
        //System.out.println(pn.authorsParsed + " " + pn.type + pn.rank);
        if(pn.getType() != NameType.wellformed && isPhraseRank(pn.rank) && (!pn.authorsParsed|| pn.specificEpithet == null || SPECIES_PATTERN.matcher(pn.specificEpithet).matches())){
            //if the rank marker is sp. and the word after the rank marker is lower case check to see if removing the marker will result is a wellformed name
            if(SPECIES_PATTERN.matcher(pn.rank).matches()){
                Matcher m1 = POTENTIAL_SPECIES_PATTERN.matcher(scientificName);
                //System.out.println(POTENTIAL_SPECIES_PATTERN.pattern());
                if(m1.find()){
                    //now reparse without the rankMarker
                    String newName = m1.group(1) + m1.group(3) + StringUtils.defaultString(m1.group(4), "");
                    pn = super.parse(newName);
                    if(pn.getType() == NameType.wellformed)
                        return pn;
                }
            }
            //check to see if the name represents a phrase name
            Matcher m = PHRASE_PATTERN.matcher(scientificName);
            if(m.find()){
                ALAParsedName alapn = new ALAParsedName(pn);
                alapn.setLocationPhraseDescription(StringUtils.trimToNull(m.group(3)));
                alapn.setPhraseVoucher(StringUtils.trimToNull(m.group(4)));
                alapn.setPhraseNominatingParty(StringUtils.trimToNull(m.group(5)));
                return alapn;
            }

        }
        else{
            //check for the situation where the subgenus was supplied without Title case.
            Matcher m = WRONG_CASE_INFRAGENERIC.matcher(scientificName);
            if(m.find()){
                scientificName = WordUtils.capitalize(scientificName, '(');
                pn = super.parse(scientificName);
            }
        }

        return pn;
    }

    private boolean isPhraseRank(String rank){
        //System.out.println(rank.replaceAll("\\.", ""));
        if(rank == null)
            return false;
        return VALID_PHRASE_RANKS.containsKey(rank.replaceAll("\\.", ""));
    }

    public static void main(String[] args){
//        String test = "One (two) three";
//        Matcher m = WRONG_CASE_INFRAGENERIC.matcher(test);
//        if(m.find()){
//            String name = m.group();
//            System.out.println(WordUtils.capitalize(test, '('));
//            System.out.println(WordUtils.capitalize(name, '('));
//        }
         String name1 ="Goodenia sp. Bachsten Creek (M.D. Barrett 685) WA Herbarium";
         PhraseNameParser parser = new PhraseNameParser();
         System.out.println(PHRASE_PATTERN.pattern());
         parser.testParse(name1);
         parser.testParse("Goodenia sp. Bachsten Creek (M.D. Barrett 685)");
         try{
         System.out.println(parser.parse(name1));
         parser.parse("Dendronephthya michaelseni var laevis");
         System.out.println("(M.D. Barrett 685)".replaceAll("[\\.| ]", ""));
          org.apache.lucene.analysis.Analyzer analyzer = new au.org.ala.checklist.lucene.analyzer.LowerCaseKeywordAnalyzer();
          String virus = "Cucumovirus cucumber mosaic virus";
          org.apache.lucene.analysis.TokenStream ts = analyzer.tokenStream("name", new java.io.StringReader("Cucumovirus cucumber mosaic virus"));
          //System.out.println(new String(ts.next().termBuffer()));
          System.out.println(au.org.ala.checklist.lucene.CBIndexSearch.virusStopPattern.matcher(virus).replaceAll(" "));
          ParsedName pn = parser.parse("Acacia sp. Manmanning (BR Maslin 7711) [aff. multispicata]");
          System.out.println(pn);
        }
         catch(Exception e){
             e.printStackTrace();
         }
    }

}

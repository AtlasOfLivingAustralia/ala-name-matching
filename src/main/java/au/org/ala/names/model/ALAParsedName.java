
package au.org.ala.names.model;

import java.util.regex.Pattern;
import org.gbif.ecat.model.ParsedName;
import org.gbif.ecat.utils.RankUtil;
import org.gbif.ecat.voc.NothoRank;
import org.gbif.ecat.voc.Rank;

/**
 *
 * Stores the extra information for an ALA Parsed Phrase name.
 *
 * @author Natasha Carter
 */
public class ALAParsedName<T> extends ParsedName<T> {

    public String locationPhraseDescription=null;
    public String cleanPhrase=null;
    public String phraseVoucher=null;
    public String cleanVoucher=null;// a clean voucher is on that is missing all punctuation and initials for people
    public String phraseNominatingParty=null;
    public static final Pattern multipleSpaces = Pattern.compile("\\s{2,}");
    public static final Pattern voucherBlacklist = Pattern.compile(" and | AND | And | s.n.| sn ");
    public static final Pattern voucherRemovePattern = Pattern.compile("[^\\w]");
    public static final Pattern potentialVoucherId = Pattern.compile("([^A-Z][A-Z]{1,3} [0-9])");
    public static final Pattern initialOnePattern = Pattern.compile("(?:[A-Z][\\.]){1,3}");//supports initials like A.B.C.
    public static final Pattern initialTwoPattern = Pattern.compile("(?:[^A-Z][A-Z]{1,3} )");//supports initials like AB
    public static final Pattern phraseBlacklist = Pattern.compile("&| AND | and |Stn|Stn\\.|Station|Mt |Mt\\.|Mount");
    public static final Pattern phrasePunctuationRemoval = Pattern.compile("'|\"");
    public ALAParsedName(){

    }
    public static void main(String[] args){
        String test = "Grevillea sp. Cape Arid (R.Spjut & R.Smith RS 12562)";
        java.util.regex.Matcher m =potentialVoucherId.matcher(test);
        m.find();

        System.out.println(m.replaceFirst(" " +m.group().replaceAll(" ", "")));

        System.out.println(multipleSpaces.matcher("The      test").replaceAll(" "));


    }
    public ALAParsedName(ParsedName<T> pn){
        this.authorsParsed = pn.authorsParsed;
        this.setAuthorship(pn.getAuthorship());
        this.setBracketAuthorship(pn.getBracketAuthorship());
        this.setBracketYear(pn.getBracketYear());
        this.setCode(pn.getCode());
        this.setCultivar(pn.getCultivar());
        this.setGenusOrAbove(pn.getGenusOrAbove());
        this.setId(pn.getId());
        this.setInfraGeneric(pn.getInfraGeneric());
        this.setInfraSpecificEpithet(pn.getInfraSpecificEpithet());
        this.setNomStatus(pn.getNomStatus());
        this.setNotho(pn.getNotho());
        this.setRank(pn.getRank());
        this.setRankMarker(pn.getRankMarker());
        this.setRemarks(pn.getRemarks());
        this.setSensu(pn.getSensu());
        this.setSpecificEpithet(pn.getSpecificEpithet());
        this.setType(pn.getType());
        this.setYear(pn.getYear());
    }

    public String getLocationPhraseDesciption() {
        return locationPhraseDescription;
    }

    public void setLocationPhraseDescription(String locationPhraseDescription) {
        this.locationPhraseDescription = locationPhraseDescription;
        if(rank == "sp"){
            this.specificEpithet = locationPhraseDescription;
        }
        else{
            this.infraSpecificEpithet = locationPhraseDescription;
        }
        if(locationPhraseDescription != null){
            cleanPhrase = phraseBlacklist.matcher(" " + locationPhraseDescription).replaceAll(" ").trim();
            cleanPhrase = phrasePunctuationRemoval.matcher(cleanPhrase).replaceAll("");
            cleanPhrase = multipleSpaces.matcher(cleanPhrase).replaceAll(" ");
        }

    }

    public String getPhraseNominatingParty() {
        return phraseNominatingParty;
    }

    public void setPhraseNominatingParty(String phraseNominatingParty) {
        this.phraseNominatingParty = phraseNominatingParty;
    }

    public String getPhraseVoucher() {
        return phraseVoucher;
    }

    public void setPhraseVoucher(String phraseVoucher) {
        this.phraseVoucher = phraseVoucher;
        //set the clean version of the phrase voucher
        if(phraseVoucher != null){
            this.cleanVoucher = phraseVoucher;
            java.util.regex.Matcher m =potentialVoucherId.matcher(this.cleanVoucher);
            if(m.find())
                cleanVoucher = m.replaceFirst(" " +m.group().replaceAll(" ", ""));

            this.cleanVoucher = voucherBlacklist.matcher(cleanVoucher).replaceAll(" ");
            this.cleanVoucher = initialOnePattern.matcher(cleanVoucher).replaceAll(" ");
            this.cleanVoucher = initialTwoPattern.matcher(cleanVoucher).replaceAll(" ");
            this.cleanVoucher = voucherRemovePattern.matcher(cleanVoucher).replaceAll("");
        }
    }

//    public String buildName(boolean hybridMarker, boolean rankMarker, boolean authorship, boolean subgenus, boolean abbreviateGenus, boolean decomposition,
//      boolean showIndet, boolean nomNote, boolean remarks, boolean showSensu) {
//    StringBuffer sb = new StringBuffer();
//    Rank rnk = getRank();
//    if (genusOrAbove != null) {
//      if (hybridMarker && NothoRank.Generic == notho) {
//        sb.append(HYBRID_MARKER);
//      }
//      if (abbreviateGenus) {
//        sb.append(genusOrAbove.substring(0, 1)).append('.');
//      } else {
//        sb.append(genusOrAbove);
//      }
//    }
//    if (specificEpithet == null) {
//
//      if (Rank.SPECIES == rnk) {
//        // no species epitheton given, but rank=species. Indetermined species!
//        if (showIndet) {
//          sb.append(" spec.");
//        }
//      } else if (RankUtil.isLowerRank(rnk, Rank.SPECIES)) {
//        // no species epitheton given, but rank below species. Indetermined!
//        if (showIndet) {
//          sb.append(' ');
//          sb.append(rnk.marker);
//        }
//      } else if (infraGeneric != null) {
//        // this is the terminal name part - always show it!
//        // We dont use parenthesis to indicate a subgenus, but use explicit rank markers instead
//        if (rankMarker) {
//          if (rank != null) {
//            sb.append(' ').append(rank);
//          } else {
//            // assume its subgenus. Zoological subgenera in brackets dont have a rank marker in the parsed name
//            sb.append(' ').append(Rank.SUBGENUS.marker);
//          }
//        }
//        sb.append(' ').append(infraGeneric);
//      }
//      // genus/infrageneric authorship
//      if (authorship) {
//        appendAuthorship(sb);
//      }
//
//    } else {
//      if (subgenus && infraGeneric != null && (rank == null || getRank() == Rank.GENUS)) {
//        // only show subgenus if requested
//        sb.append(" (");
//        sb.append(infraGeneric);
//        sb.append(')');
//      }
//
//      // species part
//      sb.append(' ');
//      if (hybridMarker && NothoRank.Specific == notho) {
//        sb.append(HYBRID_MARKER);
//      }
//      String epi = specificEpithet.replaceAll("[ _-]", "-");
//      sb.append(epi);
//
//      if (infraSpecificEpithet == null) {
//        // Indetermined?
//        if (showIndet && RankUtil.isLowerRank(rnk, Rank.SPECIES)) {
//          // no infraspecific epitheton given, but rank below species. Indetermined!
//          sb.append(' ');
//          sb.append(rnk.marker);
//        }
//
//        // species authorship
//        if (authorship) {
//          appendAuthorship(sb);
//        }
//
//      } else {
//        // infraspecific part
//        // autonym authorship ?
//        if (authorship && isAutonym()) {
//          appendAuthorship(sb);
//        }
//        sb.append(' ');
//        if (hybridMarker && NothoRank.Infraspecific == notho) {
//          if (rankMarker) {
//            sb.append("notho");
//          } else {
//            sb.append(HYBRID_MARKER);
//          }
//        }
//        if (rankMarker) {
//          String rm = getInfraspecificRankMarker();
//          if (rm != null) {
//            sb.append(rm);
//            sb.append(' ');
//          }
//        }
//        epi = infraSpecificEpithet.replaceAll("[ _-]", "-");
//        sb.append(epi);
//        // non autonym authorship ?
//        if (authorship && !isAutonym()) {
//          appendAuthorship(sb);
//        }
//      }
//    }
//
//    // add cultivar name
//    if (cultivar != null) {
//      sb.append(" '");
//      sb.append(cultivar);
//      sb.append("'");
//    }
//
//    // add sensu/sec reference
//    if (showSensu && sensu != null) {
//      sb.append(" ");
//      sb.append(sensu);
//    }
//
//    // add nom status
//    if (nomNote && nomStatus != null) {
//      sb.append(", ");
//      sb.append(nomStatus);
//    }
//
//    // add remarks
//    if (remarks && this.remarks != null) {
//      sb.append(" [");
//      sb.append(this.remarks);
//      sb.append("]");
//    }
//
//    return sb.toString().trim();
//  }
//
//
// private void appendAuthorship(StringBuffer sb) {
//    if (bracketAuthorship != null) {
//      sb.append(" (");
//      sb.append(bracketAuthorship);
//      if (bracketYear != null) {
//        sb.append(", ");
//        sb.append(bracketYear);
//      }
//      sb.append(")");
//    } else if (bracketYear != null) {
//      sb.append(" (");
//      sb.append(bracketYear);
//      sb.append(")");
//    }
//    if (authorship != null) {
//      sb.append(" " + authorship);
//    }
//    if (year != null) {
//      sb.append(", ");
//      sb.append(year);
//    }
//
//    }
}

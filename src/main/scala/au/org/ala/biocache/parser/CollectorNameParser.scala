package au.org.ala.biocache.parser

import org.apache.commons.lang.StringUtils
import org.slf4j.LoggerFactory

object CollectorNameParser {

  val logger = LoggerFactory.getLogger("CollectorNameParser")

  val NAME_LETTERS = "A-ZÃÃ‹Ã–ÃœÃ„Ã‰ÃˆÄŒÃÃ€Ã†Å’\\p{Lu}"
  val name_letters = "a-zÃ¯Ã«Ã¶Ã¼Ã¤Ã¥Ã©Ã¨ÄÃ¡Ã Ã¦Å“\\p{Ll}"
  val na = "[nN]/[aA]|\\([\\x00-\\x7F\\s]*?\\)"
  val titles = """Dr|DR|dr|\(Professor\)|Mr|MR|mr|Mrs|mrs|MRS|Ms|ms|MS|Lieutenant"""
  val etAl = "[eE][tT][. ] ?[aA][Ll][. ]?"
  val initialsRegEx = """((?:[A-Z][-. ]? ?){0,4})"""
  val ORGANISATION_WORDS = """collection|Entomology|University|Oceanographic|Indonesia|Division|American|Photographic|SERVICE|Section|Arachnology|Northern|Institute|Ichthyology|AUSTRALIA|Malacology|Institution|Department|Survey|DFO|Society|FNS-\(SA\)|Association|Government|COMMISSION|Department|Conservation|Expedition|NPWS-\(SA\)|Study Group|DIVISION|Melbourne|ATLAS|summer parties|Macquarie Island|NSW|Australian|Museum|Herpetology|ORNITHOLOGICAL|ASSOCIATION|SURVEY|Fisheries|Queensland|Griffith Npws|NCS-\(SA\)|UNIVERSITY|SCIENTIFIC|Ornithologists|Bird Observation|CMAR|Kangaroo Management Program"""
  //val SURNAME_PREFIXES ="(?:[vV](?:an)(?:[ -](?:den|der) )? ?|von[ -](?:den |der |dem )?|(?:del|Des|De|de|di|Di|da|N)[`' _]|le |d'|D'|de la |Mac|Mc|Le|St\\.? ?|Ou|O')"
  val surname_prefixes = Array("ben", "da", "Da", "Dal", "de", "De", "del", "Del", "den", "der", "Di", "du", "e", "la", "La", "Le", "Mc", "San", "St", "Ste", "van", "Van", "Vander", "vel", "von", "Von")
  val SURNAME_PREFIX_REGEX = """((?:(?:""" + surname_prefixes.mkString(("|")) + """)(?:[. ]|$)){0,2})"""
  val INITIALS_Surname = ("(?:(?:" + titles + ")(?:[. ]|$))?" + initialsRegEx + "[. ]([\\p{Lu}\\p{Ll}'-]*) ?(?:(?:" + titles + ")(?:[. ]|$)?)?(?:" + etAl + ")?").r
  val SURNAMEFirstnamePattern = (""""?([\p{Lu}'-]*) ((?:[A-Z][-. ] ?){0,4}) ?([\p{Lu}\p{Ll}']*)(?: """ + na + """)?"?""").r
  val SurnamePuncFirstnamePattern = ("\"?" + SURNAME_PREFIX_REGEX + """([\p{Lu}\p{Ll}'-]*) ?[,] ?(?:(?:""" + titles + """)(?:[. ]|$))? ?((?:[A-Z][-. ] ?){0,4}) ?""" + SURNAME_PREFIX_REGEX + """([\p{Lu}\p{Ll}']*)? ?([\p{Lu}\p{Ll}']{3,})? ?((?:[A-Z][. ]? ?){0,4})""" + SURNAME_PREFIX_REGEX + """(?: """ + na + ")?\"?").r
  val SINGLE_NAME_PATTERN = ("(?:(?:" + titles + ")(?:[. ]|$))?([\\p{Lu}\\p{Ll}']*)").r
  val ORGANISATION_PATTERN = ("((?:.*?)?(?:" + ORGANISATION_WORDS + ")(?:.*)?)").r
  val AND = "AND|and|And|&"
  val COLLECTOR_DELIM = ";|\"\"| - ".r;
  val COMMA_LIST = ",|&".r
  val suffixes = "jr|Jr|JR"
  val AND_NAME_LISTPattern = ("((?:[A-Z][. ] ?){0,3})([" + NAME_LETTERS + "][\\p{Ll}-']*)? ?([" + NAME_LETTERS + "][\\p{Ll}\\p{Lu}'-]*)? ?" + "(?:" + AND + ") ?((?:[A-Z][. ] ?){0,3})([" + NAME_LETTERS + "][\\p{Ll}'-]*)? ?([" + NAME_LETTERS + "][\\p{Ll}\\p{Lu}'-]*)?").r
  val FirstnameSurnamePattern = ("([" + NAME_LETTERS + "][\\p{Ll}']*) ((?:[A-Z][. ] ?){0,4}) ?([\\p{Lu}\\p{Ll}'-]*)? ?(?:" + na + ")?").r
  //"(["+NAME_LETTERS +"][" + name_letters + "?]{1,}" + " ) (["+NAME_LETTERS +"][" + name_letters + "?]{1,}" + " )"
  val unknown = List("\"?ANON  N/A\"?", "\"NOT ENTERED[ ]*-[ ]*SEE ORIGINAL DATA[ ]*-[ ]*\"", "\\[unknown\\]", "Anon.", "No data", "Unknown", "Anonymous", "\\?")
  val unknownPattern = ("(" + unknown.mkString("|") + ")").r

  if (logger.isDebugEnabled) {
    logger.debug(FirstnameSurnamePattern.toString)
    logger.debug(SurnamePuncFirstnamePattern.toString())
    logger.debug(unknownPattern.toString)
  }

  def parseForList(stringValue: String): Option[List[String]] = {

    stringValue match {
      case AND_NAME_LISTPattern(initials1, firstName, secondName, initials2, thirdName, forthName) => {
        if (StringUtils.isEmpty(secondName)) {
          if (StringUtils.isEmpty(forthName) && StringUtils.isEmpty(initials1)) {
            //we have 2 surnames
            Some(List(generateName(null, firstName, initials1), generateName(null, thirdName, initials2)))
          } else {
            //we have 2 people who share the same surname
            if (StringUtils.isNotEmpty(initials1) && StringUtils.isNotEmpty(initials2))
              Some(List(generateName(null, thirdName, initials1), generateName(null, thirdName, initials2)))
            else
              Some(List(generateName(firstName, forthName, initials1), generateName(thirdName, forthName, initials2)))
          }
        } else {
          if (StringUtils.isEmpty(forthName)) {
            //first person has 2 names second has a surname
            Some(List(generateName(firstName, secondName, initials1), generateName(null, thirdName, initials2)))
          }
          else
            Some(List(generateName(firstName, secondName, initials1), generateName(thirdName, forthName, initials2)))
        }
      }
      case unknownPattern(value) => Some(List("UNKNOWN OR ANONYMOUS"))
      case _ => {

        //else{
        //check to see if it contains a "collector" delimitter
        var list = COLLECTOR_DELIM.split(stringValue).toList
        if (list.size > 1) {
          list = list.map(value => {

            val name = parse(value.trim)
            //println(value.trim + " : " + name)
            if (name.isDefined)
              name.get
            else
              null
          })
        } else {
          val res = parse(stringValue)
          if (res.isDefined){
            return Some(List(res.get))
          } else {
            //check to see if it contains a comma delimited list - this needs to be done outside the other items due to mixed meaning of comma
            list = COMMA_LIST.split(stringValue).toList
            if (list.size > 1) {
              list = list.map(value => {
                val name = parse(value.trim)
                if (name.isDefined)
                  name.get
                else
                  null
              })
            }
          }
        }

        if (list.size > 0)
          Some(list)
        else
          None //Some(List(stringValue))
        //}
      }
    }

  }

  def parse(stringValue: String): Option[String] = {
    stringValue match {
      case unknownPattern(value) => logger.debug(stringValue + " UNKNOWN PATTERN"); Some("UNKNOWN OR ANONYMOUS")
      case ORGANISATION_PATTERN(org) => logger.debug(stringValue + " ORGANISTION_PATTERN"); Some(org)
      case INITIALS_Surname(initials, surname) => logger.debug(stringValue + " INTIALS SURNAME PATTERN"); Some(generateName(null, surname, initials))
      case SURNAMEFirstnamePattern(surname, initials, firstname) => logger.debug(stringValue + " SURNAME FIRSTNAME PATTERN"); Some(generateName(firstname, surname, initials))
      case SurnamePuncFirstnamePattern(prefix, surname, initials, prefix2, firstname, middlename, initials2, prefix3) => logger.debug(stringValue + " SURNAME PUNCT PATTERN"); Some(generateName(firstname, surname, if (StringUtils.isEmpty(initials)) initials2 else initials, middlename, if (StringUtils.isNotEmpty(prefix3)) prefix3 else if (StringUtils.isNotEmpty(prefix2)) prefix2 else prefix))
      case FirstnameSurnamePattern(firstname, initials, surname) => logger.debug(stringValue + " FIRSTNAME SURNAME PATTERN"); Some(generateName(firstname, surname, initials))
      case SINGLE_NAME_PATTERN(surname) => logger.debug(stringValue + " SINGLENAME PATTERN"); Some(generateName(null, surname, null))
      case _ => None
    }
  }

  def generateName(firstName: String, surname: String, initials: String, middlename: String = null, surnamePrefix: String = null): String = {
    var name = "";
    if (surnamePrefix != null)
      name += surnamePrefix.trim + " "
    if (surname != null)
      name += org.apache.commons.lang3.text.WordUtils.capitalize(surname.toLowerCase(), '-', '\'')
    if (StringUtils.isNotBlank(initials)) {
      name += ", "
      val newinit = initials.trim.replaceAll("[^\\p{Lu}\\p{Ll}-]", "")
      //println(newinit)
      newinit.toCharArray().foreach(c =>
        name += c + "."
      )
      name = name.replaceAll("\\.-\\.", "-")

    }
    if (StringUtils.isNotBlank(firstName)) {

      if (StringUtils.isBlank(initials)) {
        name += ", " + firstName.charAt(0).toUpper + "."
        if (StringUtils.isNotBlank(middlename))
          name += middlename.charAt(0).toUpper + "."
      }
      name += " " + org.apache.commons.lang3.StringUtils.capitalize(firstName.toLowerCase())
    }
    name.trim
  }

}

package au.org.ala.biocache.processor

import au.org.ala.biocache._
import scala.collection.mutable.ArrayBuffer
import org.apache.commons.lang.StringUtils
import scala.Some
import au.org.ala.biocache.model.{QualityAssertion, FullRecord}
import au.org.ala.biocache.vocab.{EstablishmentMeans, Interactions, AssertionCodes}
import au.org.ala.biocache.parser.CollectorNameParser
import au.org.ala.biocache.load.MediaStore

/**
 * A processor of miscellaneous information.
 */
class MiscellaneousProcessor extends Processor {

  val LIST_DELIM = ";".r
  val interactionPattern = """([A-Za-z]*):([\x00-\x7F\s]*)""".r

  def process(guid: String, raw: FullRecord, processed: FullRecord, lastProcessed: Option[FullRecord]=None): Array[QualityAssertion] = {
    val assertions = new ArrayBuffer[QualityAssertion]
    processImages(guid, raw, processed, assertions)
    processInteractions(guid, raw, processed)
    processEstablishmentMeans(raw, processed, assertions)
    processIdentification(raw,processed,assertions)
    processCollectors(raw, processed, assertions)
    processMiscOccurrence(raw, processed, assertions)
    assertions.toArray
  }

  def processMiscOccurrence(raw:FullRecord, processed: FullRecord, assertions: ArrayBuffer[QualityAssertion]){
    if(StringUtils.isBlank(raw.occurrence.catalogNumber)){
      assertions += QualityAssertion(AssertionCodes.MISSING_CATALOGUENUMBER,"No catalogue number provided")
    } else {
      assertions += QualityAssertion(AssertionCodes.MISSING_CATALOGUENUMBER, 1)
    }
    //check to see if the source data has been provided in a generalised form
    if(StringUtils.isNotBlank(raw.occurrence.dataGeneralizations)){
      assertions += QualityAssertion(AssertionCodes.DATA_ARE_GENERALISED)
    } else {
      //data not generalised by the provider
      assertions += QualityAssertion(AssertionCodes.DATA_ARE_GENERALISED, 1)
    }
  }

  /**
   * parse the collector string to place in a consistent format
   */
  def processCollectors(raw: FullRecord, processed: FullRecord, assertions: ArrayBuffer[QualityAssertion]) = {
    if (StringUtils.isNotBlank(raw.occurrence.recordedBy)) {
      val parsedCollectors = CollectorNameParser.parseForList(raw.occurrence.recordedBy)
      if (parsedCollectors.isDefined) {
        processed.occurrence.recordedBy = parsedCollectors.get.mkString("|")
        assertions += QualityAssertion(AssertionCodes.RECORDED_BY_UNPARSABLE, 1)
      } else {
        //println("Unable to parse: " + raw.occurrence.recordedBy)
        assertions += QualityAssertion(AssertionCodes.RECORDED_BY_UNPARSABLE, "Can not parse recordedBy")
      }
    }
  }

  def processEstablishmentMeans(raw: FullRecord, processed: FullRecord, assertions:ArrayBuffer[QualityAssertion]) : Unit = {
    //2012-0202: At this time AVH is the only data resource to support this. In the future it may be necessary for the value to be a list...
    //handle the "cultivated" type
    //2012-07-13: AVH has moved this to establishmentMeans and has also include nativeness
    if (StringUtils.isNotBlank(raw.occurrence.establishmentMeans)) {
      val ameans = LIST_DELIM.split(raw.occurrence.establishmentMeans)
      val newmeans = ameans.map(means => {
        val term = EstablishmentMeans.matchTerm(means)
        if (term.isDefined) term.get.getCanonical else ""
      }).filter(_.length > 0)

      if (!newmeans.isEmpty){
        processed.occurrence.establishmentMeans = newmeans.mkString("; ")
      }

      //check to see if the establishment mean corresponds to culitvated or escaped
      //FIXME extract to a vocabulary
      val cultEscaped = newmeans.find(em => em == "cultivated" || em == "assumed to be cultivated" || em == "formerly cultivated (extinct)" || em == "possibly cultivated" || em == "presumably cultivated")
      if(cultEscaped.isDefined){
        assertions += QualityAssertion(AssertionCodes.OCCURRENCE_IS_CULTIVATED_OR_ESCAPEE)
      } else {
        //represents a natural occurrence. not cultivated ot escaped
        assertions += QualityAssertion(AssertionCodes.OCCURRENCE_IS_CULTIVATED_OR_ESCAPEE, 1)
      }
    }
  }

  def processIdentification(raw: FullRecord, processed: FullRecord, assertions: ArrayBuffer[QualityAssertion]) = {
    //check missing identification qualifier
    if (raw.identification.identificationQualifier == null)
      assertions += QualityAssertion(AssertionCodes.MISSING_IDENTIFICATIONQUALIFIER, "Missing identificationQualifier")
    else
      assertions += QualityAssertion(AssertionCodes.MISSING_IDENTIFICATIONQUALIFIER, 1)
    //check missing identifiedBy
    if (raw.identification.identifiedBy == null)
      assertions += QualityAssertion(AssertionCodes.MISSING_IDENTIFIEDBY, "Missing identifiedBy")
    else
      assertions += QualityAssertion(AssertionCodes.MISSING_IDENTIFIEDBY, 1)
    //check missing identification references
    if (raw.identification.identificationReferences == null)
      assertions += QualityAssertion(AssertionCodes.MISSING_IDENTIFICATIONREFERENCES, "Missing identificationReferences")
    else
      assertions += QualityAssertion(AssertionCodes.MISSING_IDENTIFICATIONREFERENCES,1)
    //check missing date identified
    if (raw.identification.dateIdentified == null)
      assertions += QualityAssertion(AssertionCodes.MISSING_DATEIDENTIFIED, "Missing dateIdentified")
    else
      assertions += QualityAssertion(AssertionCodes.MISSING_DATEIDENTIFIED,1)
  }

  def processInteractions(guid: String, raw: FullRecord, processed: FullRecord) = {
    //interactions are supplied as part of the assciatedTaxa string
    //TODO more sophisticated parsing of the string. ATM we are only supporting the structure for dr642
    //TODO support multiple interactions
    if (raw.occurrence.associatedTaxa != null && !raw.occurrence.associatedTaxa.isEmpty) {
      val interaction = parseInteraction(raw.occurrence.associatedTaxa)
      if (!interaction.isEmpty) {
        val term = Interactions.matchTerm(interaction.get)
        if (!term.isEmpty) {
          processed.occurrence.interactions = Array(term.get.getCanonical)
        }
      }
    }
  }

  def parseInteraction(raw: String): Option[String] = raw match {
    case interactionPattern(interaction, taxa) => Some(interaction)
    case _ => None
  }

  /**
   * validates that the associated media is a valid image url
   */
  def processImages(guid: String, raw: FullRecord, processed: FullRecord, assertions: ArrayBuffer[QualityAssertion]) = {
    val urls = raw.occurrence.associatedMedia
    // val matchedGroups = groups.collect{case sg: SpeciesGroup if sg.values.contains(cl.getter(sg.rank)) => sg.name}
    if (urls != null) {
      val aurls = urls.split(";").map(url => url.trim)
      processed.occurrence.images = aurls.filter(url => MediaStore.isValidImageURL(url) && MediaStore.doesFileExist(url))
      processed.occurrence.sounds = aurls.filter(url => MediaStore.isValidSoundURL(url) && MediaStore.doesFileExist(url))
      processed.occurrence.videos = aurls.filter(url => MediaStore.isValidVideoURL(url) && MediaStore.doesFileExist(url))

      if (aurls.length != (processed.occurrence.images.length + processed.occurrence.sounds.length + processed.occurrence.videos.length))
        assertions += QualityAssertion(AssertionCodes.INVALID_IMAGE_URL, "URL refers to an invalid file.")
      else
        assertions += QualityAssertion(AssertionCodes.INVALID_IMAGE_URL,1)
    }
  }

  def getName = "image"
}

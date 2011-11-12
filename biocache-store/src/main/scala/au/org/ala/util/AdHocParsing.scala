package au.org.ala.util

import au.org.ala.biocache._
import scala.collection.mutable.ListBuffer
import scala.reflect.BeanProperty
import au.org.ala.checklist.lucene.HomonymException
import au.com.bytecode.opencsv.{CSVWriter, CSVReader}
import java.io._

case class ProcessedValue(@BeanProperty name: String, @BeanProperty raw: String, @BeanProperty processed: String)

case class ParsedRecord(@BeanProperty values: Array[ProcessedValue], @BeanProperty assertions: Array[QualityAssertion])

/**
 * Runnable for testing column parsing functionality.
 */
object ColumnParserTest {

  def main(args:Array[String]){

    var noExit = true
    while(noExit){
      val input = readLine
      val columns = input.split(",")
      val matchedColumns = AdHocParser.processColumnHeaders(columns)
      (matchedColumns zip columns).foreach(x =>{
        println(x._1 +" : " + x._2)
      })
    }
  }
}

object ProcessToStreamTest {

  def main(args:Array[String]){
    val headers = Array("country", "recordNumber","stateProvince", "scientificName", "decimalLatitude","decimalLongitude")
    val data = """Australia,1,TAS,Macropus rufus,12.0,12.3"""
    val fout = new FileOutputStream("/tmp/testoutput.csv")
    AdHocParser.processToStream(headers, data, fout)
    fout.close
  }
}

/**
 * Parser for CSV style data which attempts to guess the data types in use.
 * It will then parse and process the data and output the findings.
 */
object AdHocParser {

  import au.org.ala.util.StringHelper._
  import FileHelper._

  def main(args: Array[String]) {
    var filePath = ""
    val parser = new OptionParser("Parse a CSV file") {
      arg("<path-to-CSV-file>", "The UID of the data resource to load", { v: String => filePath = v })
    }
    if (parser.parse(args)) processCSV(filePath)
  }

  /**
   * TODO re-order as per the original headers
   */
  def processToStream(headers:Array[String], csvData:String, outputStream:OutputStream){

    val output = new CSVWriter(new OutputStreamWriter(outputStream), ',', '"')
    val reader = new CSVReader(new StringReader(csvData))
    var row = reader.readNext

    //take that list of raw headers
    val listBuffer = new ListBuffer[ParsedRecord]
    while(row != null){
      listBuffer += processLine(headers, row)
      row = reader.readNext
    }

    //list to export
    val l = listBuffer.toList

    //from this, derive a list of unique column headers.....
    val commonFields = {
      (for (parsedRecord <- l)
        yield parsedRecord.values.filter(p => p.processed != "" && p.raw != "").map(p => p.name)
      ).flatten.distinct
    }

    val processedFields = {
      (for (parsedRecord <- l)
        yield parsedRecord.values.filter(p => p.processed != "" && p.raw == "").map(p => p.name)
      ).flatten.distinct
    }

    //val rawFields =
    val rawFields = {
      (for (parsedRecord <- l)
        yield parsedRecord.values.filter(p => p.raw != "" && p.processed == "").map(p => p.name)
      ).flatten.distinct
    }

    //println("**Processed fields: " + processedFields.mkString(","))
    //println("**Raw fields: " + rawFields.mkString(","))
    //need a list of common to both....
    val commonHdrs = commonFields.map(x => List(x, x + " (processed)")).flatten
    //println("**Common fields: " + commonFields.mkString(","))

    //write out headers for CSV
    //println((commonHdrs :::  rawOnly  ::: processedOnly).toArray.mkString(","))
    output.writeNext((commonHdrs :::  rawFields  ::: processedFields).toArray)

    //for each row, construct the output
    for (el <- l){
      //do common fields first - with raw/processed
      val valueMap = el.values.map(p => (p.name -> p)).toMap

      val commonOutput:List[String] = commonFields.map(cf => valueMap.get(cf) match {
        case Some(processedValue) => List(processedValue.raw,processedValue.processed)
        case _ => List("","")
      }).flatten

      val rawOutput:List[String] = rawFields.map(cf => valueMap.get(cf) match {
        case Some(processedValue) => processedValue.raw
        case _ => ""
      })

      val processedOutput:List[String] = processedFields.map(cf => valueMap.get(cf) match {
        case Some(processedValue) => processedValue.processed
        case _ => ""
      })

      //println((commonOutput ::: rawOutput ::: processedOutput).toArray.asInstanceOf[Array[String]].mkString(","))
      output.writeNext((commonOutput ::: rawOutput ::: processedOutput).toArray.asInstanceOf[Array[String]])
      output.flush
    }
    reader.close
  }

  def areColumnHeaders(list:Array[String]) : Boolean = {
    val matchedCount = DwC.retrieveCanonicalsOrNothing(list.toList).count(x => x != "")
    //if not try and match them
    matchedCount > (list.size/3)
  }

  def processColumnHeaders(list: Array[String]): Array[String] = {
    //are these darwin core terms?
      val matchedCount = DwC.retrieveCanonicalsOrNothing(list.toList).count(x => x != "")
    //if not try and match them
    if (matchedCount > 2) {
      val t = DwC.retrieveCanonicals(list.toList)
      println("Matched terms: " + t)
      t.toArray
    } else {
      val t = guessColumnHeaders(list)
      println("Guessed terms: " + t.zip(list).filter(x => x._2 !="").map(x=> x._1+":"+x._2).mkString(","))
      t
    }
  }

  def mapColumnHeaders(list: Array[String]): Array[String] = DwC.retrieveCanonicalsOrNothing(list.toList).toArray

  def processLine(hdrs: Array[String], values: Array[String]): ParsedRecord = {
    val tuples = (hdrs zip values).toMap
    val raw = FullRecordMapper.createFullRecord("", tuples, Versions.RAW)
    //println(raw.classification.scientificName)
    val p = new RecordProcessor
    val (processed, assertions) = p.processRecord(raw)
    //what values are processed???

    val rawAndProcessed = raw.objectArray zip processed.objectArray
    val listBuff = new ListBuffer[ProcessedValue]
    for ((rawPoso, procPoso) <- rawAndProcessed) {

      if (!rawPoso.isInstanceOf[ContextualLayers] && !rawPoso.isInstanceOf[EnvironmentalLayers]) {
        rawPoso.propertyNames.foreach(name => {
          val rawValue = rawPoso.getProperty(name)
          val procValue = procPoso.getProperty(name)
          if (!rawValue.isEmpty || !procValue.isEmpty) {
            val term = ProcessedValue(name, rawValue.getOrElse(""), procValue.getOrElse(""))
            listBuff += term
          }
        })
      }
    }
    ParsedRecord(listBuff.toList.toArray, assertions.values.flatten.toArray)
  }

  def processCSV(filepath: String) {
    (new File(filepath)).readAsCSV(',', '"', processColumnHeaders, (hdrs, values) => {
      val result = processLine(hdrs, values)
      CommandLineTool.printTable(result.values.map(r => {
        Map("name" -> r.name, "raw" -> r.raw, "processed" -> r.processed)
      }).toList)
    })
  }

  def processReader(reader: java.io.Reader) {
    val csvReader = new CSVReader(reader, ',', '"')
    val rawColumnHdrs = csvReader.readNext
    val columnHdrs = processColumnHeaders(rawColumnHdrs)
    var currentLine = csvReader.readNext
    while (currentLine != null) {
      val result = processLine(columnHdrs, currentLine)
      currentLine = csvReader.readNext
    }
  }

  def guessColumnHeaders(values: Array[String]): Array[String] = {
    //assume we have darwin core terms
    val matchedDwc = DwC.retrieveCanonicalsOrNothing(values.toList)
    val nofMatched = matchedDwc.filter(x => x.size > 0).size

    if (nofMatched > 4 || (nofMatched.toFloat /values.size.toFloat)  < 0.25) {

      val parsedValues = {
        val parsedValues = parse(values)

        if (values.size >1) {
            val firstCols = parseHead(values(0), values(1))
            if(!firstCols.isEmpty){
              val (col1, col2) = firstCols.get
              if (col1 != "" & col2 != "") {
                parsedValues updated (0, col1) updated (0, col2)
              } else if (col1 != "") {
                parsedValues updated (0, col1)
              } else {
                parsedValues
              }
            } else {
               parsedValues
            }
        } else {
            parsedValues
        }
      }

      //TODO replace this with a simple function that iterates through and checks previous
      val termsWithZip = parsedValues.zipWithIndex
      //are there any duplicates?
      val duplicateTerms = termsWithZip.filter(y => y._1 !="").groupBy(x => x._1).filter(y => y._2.size > 1)
      //if decimal latitude is duplicated, look for which instance has an instance of longitude
      if(!duplicateTerms.get("decimalLatitude").isEmpty){
        // are the duplicate terms sequential
        val duplicateIndexes = duplicateTerms.get("decimalLatitude").get.map(x => x._2)
        val sequentialPairs = (for(i <- 0 to duplicateIndexes.size-2; if(duplicateIndexes(i)==duplicateIndexes(i+1)-1) )
	       yield (duplicateIndexes(i),duplicateIndexes(i+1))
	      )

        if(!sequentialPairs.isEmpty){
          //replace with (decimalLat, decimalLong)
          val tuple = sequentialPairs.first
          parsedValues updated (tuple._1, "decimalLatitude") updated (tuple._2, "decimalLongitude")
        } else {
          parsedValues
        }
      } else {
        parsedValues
      }
    } else {
      matchedDwc.toArray
    }
  }

  def parseHead(column1: String, column2: String): Option[(String, String)] = column1 match {
    case it if (column1.isLatitude && column2.isLongitude) => Some("decimalLatitude", "decimalLongitude")
    case it if (column1.isLatitude && column2.isLongitude) => Some("decimalLongitude", "decimalLatitude")
    case it if it.isInt => Some("recordNumber", "")
    case it if it.startsWith("urn") => Some("occurrenceID", "")
    case it if it.startsWith("http://") => Some("occurrenceID", "")
    case _ => None
  }

  /**
   * Just returns the best guess for each field.
   */
  def parse(values: Array[String]): Array[String] = values.map(value => parse(value))

  /**
   * Just return the best guess for field value.
   */
  def parse(value:String): String = {
    if (value == null) return ""
    value.trim match {
      case GeodeticDatumExtractor(value) => "geodeticDatum"
      case AssociatedMediaExtractor(value) => "associatedMedia"
      case BasisOfRecordExtractor(value) => "basisOfRecord"
      case TypeStatusExtractor(value) => "typeStatus"
      case DateExtractor(value) => "eventDate"
      case DecimalLatitudeExtractor(value) => "decimalLatitude"
      case DecimalLongitudeExtractor(value) => "decimalLongitude"
      case VerbatimLatitudeExtractor(value) => "verbatimLatitude"
      case VerbatimLongitudeExtractor(value) => "verbatimLongitude"
      case CountryExtractor(value) => "country"
      case StateProvinceExtractor(value) => "stateProvince"
      case OccurrenceIDExtractor(value) => "occurrenceID"
      case CatalogExtractor(value) => "catalogNumber"
      case LifeStageExtractor(value) => "lifeStage"
      case SexExtractor(value) => "sex"
      case CoordinateUncertaintyExtractor(value) => "coordinateUncertaintyInMeters"
      case ScientificNameExtractor(value) => value
      case CommonNameExtractor(value) => "vernacularName"
      case OccurrenceStatusExtractor(value) => "occurrenceStatus"
      case _ => ""
    }
  }
}

object AssociatedMediaExtractor {
  val imageParser = """^(https?://(?:[a-zA-Z0-9\-]+\.)+[a-zA-Z]{2,6}(?:/[^/#]+)+\.(?:jpg|gif|png|jpeg))$""".r
  def unapply(str: String): Option[String] = if(!imageParser.unapplySeq(str.trim).isEmpty) Some("image") else None
}

object TypeStatusExtractor {
  def unapply(str: String): Option[Term] = TypeStatus.matchTerm(str)
}

object CatalogExtractor {
  val regex = "[A-Za-z]{1,}[-]{0,}[0-9]{1,}".r

  def unapply(str: String): Option[String] = {
    if (str != "") {
      regex.findFirstMatchIn(str) match {
        case Some(firstMatch) => Some(firstMatch.toString)
        case None => None
      }
    } else {
      None
    }
  }
}

object OccurrenceIDExtractor {
  val regex = "urn".r

  def unapply(str: String): Option[String] = {
    if (str != "") {
      regex.findFirstMatchIn(str) match {
        case Some(firstMatch) => Some(firstMatch.toString)
        case None => None
      }
    } else {
      None
    }
  }
}

object ScientificNameExtractor {

  def unapply(str: String): Option[String] = {
    if (str != "") {
      try {
        val nsr = Config.nameIndex.searchForRecord(str, null, null)
        if (nsr != null && nsr.getLsid != null && nsr.getRank != null) {
          Some(nsr.getRank.getRank)
        } else {
          None
        }
      } catch {
        case e:HomonymException => Some("scientificName")
        case _ => None
      }
    } else {
      None
    }
  }
}

object CommonNameExtractor {
  def unapply(str: String): Option[String] = {
    if (str!=null && str.trim != "") {
      try {
        val nsr = Config.nameIndex.searchForCommonName(str)
        if (nsr != null && nsr.getLsid != null) {
          Some(nsr.getLsid)
        } else {
          None
        }
      } catch {
        case e:HomonymException => Some("commonName")
        case _ => None
      }
    } else {
      None
    }
  }
}

object DateExtractor {
  def unapply(str: String): Option[EventDate] = {
    val parsed = DateParser.parseDate(str)
    parsed match {
      case it if !it.isEmpty && DateParser.isValid(it.get) => parsed
      case _ => None
    }
  }
}

object DecimalLatitudeExtractor {
  import au.org.ala.util.StringHelper._
  def unapply(str: String): Option[Float] = {
    try {
      val latitude = str.toFloat
      if (str.isDecimalNumber && latitude <= 90.0 && latitude >= -90.0) {
        Some(latitude)
      } else {
        None
      }
    } catch {
      case e: Exception => None
    }
  }
}

object DecimalLongitudeExtractor {
  import au.org.ala.util.StringHelper._
  def unapply(str: String): Option[Float] = {
    try {
      val longitude = str.toFloat
      if (str.isDecimalNumber && longitude <= 180.0 && longitude >= -180.0) {
        Some(longitude)
      } else {
        None
      }
    } catch {
      case e: Exception => None
    }
  }
}

object VerbatimLatitudeExtractor {
  def unapply(str: String): Option[Float] = VerbatimLatLongParser.parse(str)
}

object VerbatimLongitudeExtractor {
  def unapply(str: String): Option[Float] = VerbatimLatLongParser.parse(str)
}

object BasisOfRecordExtractor {
  def unapply(str: String): Option[Term] = BasisOfRecord.matchTerm(str)
}

object GeodeticDatumExtractor {
  def unapply(str: String): Option[Term] = GeodeticDatum.matchTerm(str)
}

object SexExtractor {
  def unapply(str: String): Option[Term] = Sex.matchTerm(str)
}

object LifeStageExtractor {
  def unapply(str: String): Option[Term] = LifeStage.matchTerm(str)
}

object CountryExtractor {
  def unapply(str: String): Option[Term] = Countries.matchTerm(str)
}

object StateProvinceExtractor {
  def unapply(str: String): Option[Term] = StateProvinces.matchTerm(str)
}

object CoordinateUncertaintyExtractor {
  def unapply(str: String): Option[Float] = DistanceRangeParser.parse(str)
}

object OccurrenceStatusExtractor {
  def unapply(str: String): Option[Term] = OccurrenceStatus.matchTerm(str)
}
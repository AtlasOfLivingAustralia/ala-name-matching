package au.org.ala.biocache.qa
import java.text.MessageFormat
import scala.io.Source
import scala.util.parsing.json.JSON
import scala.collection.mutable.ArrayBuffer
import au.org.ala.biocache.Config
import java.io.FileWriter
import java.io.File
import au.org.ala.biocache.outliers.Timings
import org.slf4j.LoggerFactory
import au.org.ala.biocache.util.{Json, OptionParser, BiocacheConversions}
import au.org.ala.biocache.model.ValidationRule
import au.org.ala.biocache.load.FullRecordMapper
import au.org.ala.biocache.index.IndexRecords

/**
 * Executable that applies the validation rules provided by users.
 */
object ValidationRuleRunner {

  def main(args: Array[String]) {

    var apiKey:Option[String] = None
    var id:Option[String] = None
    var unapply = false
    var reindex = false
    var reapply = false
    var test = false
    var retrofit =false
    var tempFilePath = "/tmp/queryAssertionReindex.txt"
    val parser = new OptionParser("Application of Validation Rules") {
      opt("a", "apiKey","The apiKey whose assertions should be applied", {v:String => apiKey = Some(v)})
      opt("i","record id", "The uuid or the rowKey for the validation rule to apply", {v:String => id = Some(v)})
      opt("retro","Retrofit the id's",{retrofit=true})
      opt("unapply", "Unapply the validation rules", {unapply=true})
      opt("reindex", "Reindex all the records for the validation rules", {reindex=true})
      opt("reapply", "Forces a reapplication of the validation rules",{reapply=true})
      opt("test", "Performs a test of the validation rules", {test = true})
      opt("tempFilePath", "Location of a temporary file for validation rule outputs", {v:String => tempFilePath = v})
    }
    if(parser.parse(args)){      
      val runner = new ValidationRuleRunner
      if(apiKey.isDefined){
        runner.apply(apiKey.get, unapply, reindex, tempFilePath, reapply,test)
      } else if(id.isDefined){
        runner.applySingle(id.get, unapply, reindex, tempFilePath, reapply,test)
      } else if(retrofit){
        runner.retrofit(test)
      }
      Config.persistenceManager.shutdown
      Config.indexDAO.shutdown
    } else {
      parser.showUsage
    }
    //new QueryAssertion().pageOverQuery("?q=lsid:urn:lsid:biodiversity.org.au:afd.taxon:1277dd43-882d-49d8-a009-2def58b0446e&wkt=MULTIPOLYGON(((144.75:-38,144.75:-37.75,145:-37.75,145.25:-37.75,145.25:-38,145:-38,144.75:-38)),((145.25:-38,145.5:-38,145.5:-38.25,145.25:-38.25,145.25:-38)),((145.25:-37.5,145.25:-37.25,145.5:-37.25,145.5:-37.5,145.25:-37.5)),((143.75:-37.75,143.75:-37.5,144:-37.5,144:-37.75,143.75:-37.75)))", Array(), {list => println(list);true;})
  }
}

class ValidationRuleRunner {

  import BiocacheConversions._

  val logger = LoggerFactory.getLogger("ValidationRuleRunner")

  val BIOCACHE_QUERY_URL = Config.biocacheServiceUrl + "/occurrences/search{0}&facet=off{1}&pageSize={2}&startIndex={3}&fl=row_key,id"

  def retrofit(test:Boolean){
    Config.validationRuleDAO.pageOver(vr =>{
      if(vr.isDefined){
        if (vr.get.getId != null && vr.get.getId.contains("|")){
          val oldId = vr.get.getId
          val newId = oldId.substring(oldId.indexOf("|")+1, oldId.length)
          if(!test){
            Config.persistenceManager.put(vr.get.getRowKey,"queryassert","id",newId)
          } else{
            logger.info("Changing id from " + oldId + " to " + newId)
          }
        }
      }
      true
    })
  }

  def applySingle(rowKey:String, unapply:Boolean=false, reindexOnly:Boolean=false, tempFilePath:String="/tmp/queryAssertionReindex.txt", forceReapply:Boolean=false, test:Boolean=false){
    //get the query for the supplied key
    var buffer = new ArrayBuffer[String]
    val qa = Config.validationRuleDAO.get(rowKey)
    val filename = "/tmp/query_"+rowKey+"_reindex.txt"
    val reindexWriter = new FileWriter(filename)
    if(qa.isDefined){
      applyCommon(qa.get, buffer, unapply, reindexOnly, forceReapply,test)
    }
    val rowKeySet = buffer.toSet[String]
    logger.debug("Row set : " + rowKeySet)
    rowKeySet.foreach(v => reindexWriter.write(v + "\n"))
    reindexWriter.flush
    reindexWriter.close
    if(!test) {
      //need to index both ways until we reprocess all validation rule.
      IndexRecords.indexListThreaded(new File(filename), 4)
      IndexRecords.indexListOfUUIDs(new File(filename))
    }
  }
  
  def apply(apiKey:String, unapply:Boolean=false, reindexOnly:Boolean=false, tempFilePath:String="/tmp/queryAssertionReindex.txt", forceReapply:Boolean=false, test:Boolean=false){
    //val queryPattern = """\?q=([\x00-\x7F\s]*)&wkt=([\x00-\x7F\s]*)""".r
    val start = apiKey + "|"
    val end = start + "~"
    var buffer = new ArrayBuffer[String]

    val reindexWriter = new FileWriter(tempFilePath)
    logger.info("Starting...")
    Config.validationRuleDAO.pageOver(aq => {

      if(aq.isDefined){
        applyCommon(aq.get, buffer, unapply, reindexOnly, forceReapply,test)
      }
      true
    },start,end)

    //write the buffer out to file
    val rowKeySet = buffer.toSet[String]
    rowKeySet.foreach(v=>reindexWriter.write(v + "\n"))
    logger.debug("Row set : " + rowKeySet)
    reindexWriter.flush
    reindexWriter.close
    if(!test){
      //need to index both ways until we reprocess all validation rule.
      IndexRecords.indexListThreaded(new File(tempFilePath), 4)
      IndexRecords.indexListOfUUIDs(new File(tempFilePath))
    }
  }

  /**
   * Performs the common application of the validation rule.  Common code between "apply" and "applySingle"
   * @param validation
   * @param buffer
   * @param unapply
   * @param reindexOnly
   * @param forceReapply
   */
  def applyCommon(validation:ValidationRule, buffer:ArrayBuffer[String], unapply:Boolean, reindexOnly:Boolean, forceReapply:Boolean, test:Boolean){

    if(reindexOnly){
      //need to reindex the records associated with this validation.
      buffer ++= validation.records
      if(test){
        logger.info("Reindexing " + buffer.toList)
      }
    } else if(unapply){
      //need to remove the old valiadations
      modifyList(validation.records.toList, validation, buffer, false,test)
      if(!test){
        Config.persistenceManager.deleteColumns(validation.id, "queryassert","records","lastApplied")
      }  else {
        logger.info("Unapplying " + buffer.toList)
      }
    } else {
      //need to apply or reapply the validation rules.
      applyAssertion(validation, buffer, forceReapply,test)
    }
  }
  
  def applyAssertion(assertion:ValidationRule, buffer:ArrayBuffer[String],forceReapply:Boolean, test:Boolean){
    val timing = new Timings()
    val applicationDate:String = new java.util.Date()
    val newList = new ArrayBuffer[String]
    if(assertion.uuid == null){
      //need to create one
      assertion.uuid = Config.validationRuleDAO.createUuid
      if(!test) {
        Config.persistenceManager.put(assertion.id, "queryassert", "uuid", assertion.uuid)
      } else {
        logger.info("New uuid for validation rule " + assertion.uuid)
      }
    }

    if(assertion.disabled){
      //potentially need to remove the associated records
      modifyList(assertion.records.toList, assertion, buffer, false, test)
      if(!test){
        Config.persistenceManager.deleteColumns(assertion.id, "queryassert","records")
      } else{
        logger.info("Disabling " + assertion)
      }

    } else if((assertion.lastApplied != null && assertion.lastApplied.before(assertion.modifiedDate)) || forceReapply){
      //need to reprocess all records and only add the records that haven't already been applied
      //remove records that have been applied but no longer exist
      pageOverQuery(assertion.rawQuery, Array[String](), { list =>
        newList ++= list
        true
      })
      val recSet = assertion.records.toSet[String]
      val newSet = newList.toSet[String]
      val removals = recSet diff newSet
      val additions = newSet diff recSet
      //remove records no longer part of the query assertion
      modifyList(removals.toList, assertion, buffer, false, test)
      //add records that a newly part of the query assertion
      modifyList(additions.toList, assertion,buffer, true, test)

      val props = Map("lastApplied" -> applicationDate,
        "records" -> Json.toJSON(newList.toArray[String].asInstanceOf[Array[AnyRef]])
      )
      if(!test){
        Config.persistenceManager.put(assertion.id, "queryassert",props)
      } else {
        logger.info("Updating last applied or force reapplied with " + props)
      }

    } else {
      val fqs = {
         if(assertion.lastApplied == null){
           Array[String]()
         } else {
           val dateString:String = assertion.lastApplied
           Array("(last_processed_date:["+dateString+" TO *] OR last_load_date:["+dateString+" TO *])")
         }
      }
      pageOverQuery(assertion.rawQuery, fqs, { list =>
        newList ++= list
        true
      })

      //println(newList)
      modifyList(newList.toList, assertion, buffer, true, test)
      def set = (assertion.records.toList ++ newList).toSet[String]
      if (!test){
        Config.persistenceManager.put(assertion.id, "queryassert", Map("lastApplied"->applicationDate,"records"->Json.toJSON(set.toArray[String].asInstanceOf[Array[AnyRef]])))
      } else{
        logger.info("Applying validation " +  Map("lastApplied"->applicationDate,"records"->Json.toJSON(set.toArray[String].asInstanceOf[Array[AnyRef]])))
      }
    }
    timing.checkpoint("Finished applying " + assertion.id)
  }
  
  /**
   * Either removes or add the query assertion to the supplied list.
   */
  def modifyList(list:List[String], aq:ValidationRule, buffer:ArrayBuffer[String], isAdd:Boolean, test:Boolean){
    list.foreach(uuid => {
      //get the rowKey for the supplied uuid
      val rowKey = Config.occurrenceDAO.getRowKeyFromUuid(uuid).getOrElse(uuid)
      //check to see if it is already part of the assertion
      if((isAdd && !aq.records.contains(rowKey))||(!isAdd && aq.records.contains(rowKey))){
        //get the queryAssertions for the record
        val map  = Json.toJavaStringMap(Config.persistenceManager.get(rowKey, "occ", FullRecordMapper.queryAssertionColumn).getOrElse("{}"))
        if(isAdd){
          map.put(aq.uuid, aq.assertionType)
        } else {
          map.remove(aq.uuid)
        }
        //println(rowKey + " " + map)
        if (!test){
          Config.persistenceManager.put(rowKey, "occ", FullRecordMapper.queryAssertionColumn,Json.toJSON(map))//.asScala[String,String].asInstanceOf[Map[String, Any]]))
        } else {
          logger.info("Modifying the list to " + map)
        }
        buffer += uuid
      }
    })
  }
  
  def pageOverQuery(query:String, fqs:Array[String], proc:List[String]=>Boolean){
    var pageSize=500
    var startIndex=0
    var total = -1
    var filter = fqs.mkString("&fq=","&fq=","").replaceAll(" " ,"%20") 
    while(total == -1 || total > startIndex){
      val url = MessageFormat.format(BIOCACHE_QUERY_URL, query.replaceAll(" " ,"%20"),filter, pageSize.toString(), startIndex.toString())
      logger.info(url)
      val jsonString = Source.fromURL(url).getLines.mkString
      val json = JSON.parseFull(jsonString).get.asInstanceOf[Map[String, String]]
      //println(json)
      val results = json.get("occurrences").get.asInstanceOf[List[Map[String, String]]]
      val list = results.map(map => map.getOrElse("uuid",""))
      total = json.get("totalRecords").get.asInstanceOf[Double].toInt
      if(!proc(list)){
        startIndex = total
      }
      startIndex += pageSize
    }
  }
}
package au.org.ala.bioache.qa
import au.org.ala.biocache.Config
import java.text.MessageFormat
import scala.io.Source
import scala.util.parsing.json.JSON
import au.org.ala.biocache.BiocacheConversions
import scala.collection.mutable.ArrayBuffer
import au.org.ala.biocache.AssertionQuery
import au.org.ala.biocache.FullRecordMapper
import au.org.ala.biocache.Json
import java.io.FileWriter
import au.org.ala.util.{IndexRecords, OptionParser, FileHelper}
import java.io.File
import scala.collection.JavaConversions
import au.org.ala.biocache.outliers.Timings

object QueryAssertion{
  def main(args: Array[String]) {
    //new QueryAssertion().applySingle("jcuap03|5069")
    var apiKey:Option[String] = None
    var id:Option[String] = None
    var unapply =false
    val parser = new OptionParser("Application of Query Assertions") {
      opt("a", "apiKey","The apiKey whose assertions shoud be applied", {v:String => apiKey = Some(v)})
      opt("i","record id", "The uuid or the rowKey for the query assertion to apply", {v:String => id = Some(v)})
      opt("unapply", "Unapply the query assertions",{unapply=true})
    }
    if(parser.parse(args)){      
      val qaApplier = new QueryAssertion()
      if(apiKey.isDefined)
        qaApplier.apply(apiKey.get, unapply)
      else if(id.isDefined)
        qaApplier.applySingle(id.get, unapply)
      Config.persistenceManager.shutdown
      Config.indexDAO.shutdown
    }
    //new QueryAssertion().pageOverQuery("?q=lsid:urn:lsid:biodiversity.org.au:afd.taxon:1277dd43-882d-49d8-a009-2def58b0446e&wkt=MULTIPOLYGON(((144.75:-38,144.75:-37.75,145:-37.75,145.25:-37.75,145.25:-38,145:-38,144.75:-38)),((145.25:-38,145.5:-38,145.5:-38.25,145.25:-38.25,145.25:-38)),((145.25:-37.5,145.25:-37.25,145.5:-37.25,145.5:-37.5,145.25:-37.5)),((143.75:-37.75,143.75:-37.5,144:-37.5,144:-37.75,143.75:-37.75)))", Array(), {list => println(list);true;})
  }
}

class QueryAssertion {
  import BiocacheConversions._
  import JavaConversions._
  val BIOCACHE_QUERY_URL = Config.biocacheServiceURL +"/occurrences/search{0}&facet=off{1}&pageSize={2}&startIndex={3}&fl=row_key"
  
  def applySingle(rowKey:String, unapply:Boolean=false){
    //get the query for the supplied key
    var buffer = new ArrayBuffer[String]
    def qa=Config.assertionQueryDAO.getAssertionQuery(rowKey)
    val filename = "/tmp/query_"+rowKey+"_reindex.txt"
    val reindexWriter = new FileWriter(filename)
    if(qa.isDefined){
      if(unapply){
        modifyList(qa.get.records.toList, qa.get, buffer, false)
        Config.persistenceManager.deleteColumns(qa.get.id, "queryassert","records","lastApplied")
      }
      else
        applyAssertion(qa.get,buffer)
    }
    val rowKeySet = buffer.toSet[String]
    rowKeySet.foreach(v=>reindexWriter.write(v + "\n"))
    reindexWriter.flush
    reindexWriter.close
    IndexRecords.indexListThreaded(new File(filename), 4)
  }
  
  def apply(apiKey:String, unapply:Boolean=false){
    //val queryPattern = """\?q=([\x00-\x7F\s]*)&wkt=([\x00-\x7F\s]*)""".r
    val start = apiKey+"|"
    val end = start+"~"
    var buffer = new ArrayBuffer[String]
    
    
    val reindexWriter = new FileWriter("/tmp/queryAssertionReindex.txt")
    println("Starting...")
    Config.assertionQueryDAO.pageOver(aq =>{
     
     
      if(aq.isDefined){
        val assertion = aq.get
//        val queryPattern(query, wkt)=assertion.getRawQuery()
        if(unapply){
          modifyList(assertion.records.toList, assertion, buffer, false)
          Config.persistenceManager.deleteColumns(assertion.id, "queryassert","records","lastApplied")
        }
        else
          applyAssertion(assertion, buffer)

      }
      true
      },start,end)
      //write the buffer out to file 
      val rowKeySet = buffer.toSet[String]
      rowKeySet.foreach(v=>reindexWriter.write(v + "\n"))
      reindexWriter.flush
      reindexWriter.close
      IndexRecords.indexListThreaded(new File("/tmp/queryAssertionReindex.txt"), 4)
  }
  
  def applyAssertion(assertion:AssertionQuery, buffer:ArrayBuffer[String]){
    val timing = new Timings()
    val applicationDate:String = new java.util.Date()
    val newList = new ArrayBuffer[String]
    if(assertion.uuid == null){
          //need to create one
          assertion.uuid = Config.assertionQueryDAO.createUuid
          Config.persistenceManager.put(assertion.id, "queryassert","uuid",assertion.uuid)
        }
        if(assertion.disabled){
          //potentially need to remove the associated records
          modifyList(assertion.records.toList, assertion, buffer, false)
          Config.persistenceManager.deleteColumns(assertion.id, "queryassert","records")
        }
        else if(assertion.lastApplied != null && assertion.lastApplied.before(assertion.modifiedDate)){
          //need to reprocess all records and only add the records that haven't already been applied
          //remove records that have been applied but no longer exist
          
          pageOverQuery(assertion.rawQuery, Array[String](),{list =>
            newList ++= list
            true;
            })
            def recSet = assertion.records.toSet[String]
            def newSet = newList.toSet[String]
            def removals = recSet &~ newSet
            def additions = newSet &~ recSet
            //remove records no longer part of the query assertion
            modifyList(removals.toList, assertion,buffer,false)
            //add records that a newly part of the query assertion
            modifyList(additions.toList, assertion,buffer, true)
            Config.persistenceManager.put(assertion.id, "queryassert",Map("lastApplied"->applicationDate,"records"->Json.toJSON(newList.toArray[String].asInstanceOf[Array[AnyRef]])))
        }
        else{
          val fqs ={            
             if(assertion.lastApplied == null){
                Array[String]()
             }
             else{
                val dateString:String = assertion.lastApplied
                Array("(last_processed_date:["+dateString+" TO *] OR last_load_date:["+dateString+" TO *])")
             }
          }
          pageOverQuery(assertion.rawQuery, fqs, {list=>
            newList ++= list
            true;})
            //println(newList)
            modifyList(newList.toList, assertion, buffer, true)
            def set = (assertion.records.toList ++ newList).toSet[String]            
            Config.persistenceManager.put(assertion.id, "queryassert",Map("lastApplied"->applicationDate,"records"->Json.toJSON(set.toArray[String].asInstanceOf[Array[AnyRef]])))
        }
        timing.checkpoint("Finished applying " + assertion.id)
      
  }
  
  /**
   * Either removes or add the query assertion to the supplied list.
   */
  def modifyList(list:List[String], aq:AssertionQuery, buffer:ArrayBuffer[String], isAdd:Boolean){
    list.foreach(rowKey =>{
      //check to see if it is already part of the assertion
      if((isAdd && !aq.records.contains(rowKey))||(!isAdd &&aq.records.contains(rowKey))){
        //get the queryAssertions for the record
        val map  = Json.toJavaStringMap(Config.persistenceManager.get(rowKey, "occ",FullRecordMapper.queryAssertionColumn).getOrElse("{}"))
        if(isAdd)
          map.put(aq.uuid, aq.assertionType)
        else
          map.remove(aq.uuid)
        //println(rowKey + " " + map)
        Config.persistenceManager.put(rowKey, "occ",FullRecordMapper.queryAssertionColumn,Json.toJSON(map))//.asScala[String,String].asInstanceOf[Map[String, Any]]))
        buffer + rowKey
      }
    })
  }
  
  def pageOverQuery(query:String, fqs:Array[String], proc:List[String]=>Boolean){
    var pageSize=500
    var startIndex=0
    var total = -1
    var filter = fqs.mkString("&fq=","&fq=","").replaceAll(" " ,"%20") 
    //println(filter)
    while(total == -1 || total > startIndex){
      val url = MessageFormat.format(BIOCACHE_QUERY_URL, query,filter, pageSize.toString(), startIndex.toString())
      //println(url)
      val jsonString = Source.fromURL(url).getLines.mkString
      val json = JSON.parseFull(jsonString).get.asInstanceOf[Map[String, String]]
      //println(json)
      val results = json.get("occurrences").get.asInstanceOf[List[Map[String, String]]]
      val list = results.map(map=>map.getOrElse("rowKey",""))
      total = json.get("totalRecords").get.asInstanceOf[Double].toInt
      if(!proc(list))
        startIndex = total
      startIndex +=pageSize
    }
  }
}
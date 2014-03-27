package au.org.ala.biocache.load

import scala.collection.JavaConversions
import scala.collection.mutable.ArrayBuffer
import au.org.ala.biocache.model.Versions

/**
 * Created by mar759 on 17/02/2014.
 */
class MapDataLoader extends DataLoader {

  import JavaConversions._

  def load(dataResourceUid:String, values:List[java.util.Map[String,String]], uniqueTerms:List[String]):List[String]={
    val rowKeys = new ArrayBuffer[String]
    values.foreach(jmap =>{
        val map = jmap.toMap[String,String]
        val uniqueTermsValues = uniqueTerms.map(t => map.getOrElse(t,""))
        val fr = FullRecordMapper.createFullRecord("", map, Versions.RAW)
        load(dataResourceUid, fr, uniqueTermsValues, true, true)
        rowKeys += fr.rowKey
    })
    rowKeys.toList
  }
}

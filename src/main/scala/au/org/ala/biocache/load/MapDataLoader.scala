package au.org.ala.biocache.load

import scala.collection.JavaConversions
import scala.collection.mutable.ArrayBuffer
import au.org.ala.biocache.model.Versions
import au.org.ala.biocache.vocab.DwC

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
      //map the keys to DWC values
      val keys = map.keySet.toList
      val biocacheModelValues = DwC.retrieveCanonicals(keys)
      val keysToDwcMap = (keys zip biocacheModelValues).toMap
      val dwcMap = map.map{case (k,v) => (keysToDwcMap.getOrElse(k,k), v)}
      val fr = FullRecordMapper.createFullRecord("", dwcMap, Versions.RAW)
      load(dataResourceUid, fr, uniqueTermsValues, true, true)
      rowKeys += fr.rowKey
    })
    rowKeys.toList
  }
}

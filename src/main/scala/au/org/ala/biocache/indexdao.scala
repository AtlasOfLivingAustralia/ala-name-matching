/*
 * The configuration required for the SOLR index
 */

package au.org.ala.biocache


import org.apache.commons.lang.time.DateUtils
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer
import org.apache.solr.core.CoreContainer

object SolrIndexDao {
  val solrHome = "/data/solr/bio-proto"
  //set the solr home
  System.setProperty("solr.solr.home", solrHome)

  val cc = new CoreContainer.Initializer().initialize
  val solrServer = new EmbeddedSolrServer(cc, "")
   

}
/**
 * All Index implementations need to extend this trait.
 */
trait IndexDao {
  import au.org.ala.util.ReflectBean._
  /**
   * Index a list of Occurrence Index Model objects
   */
  def index(items: java.util.List[OccurrenceIndex]):Boolean
  /**
   * Index a single Occurrence Index Model
   */
  def index(item:OccurrenceIndex):Boolean
  def emptyIndex()
  /**
   * Perform
   */
  def finaliseIndex()
  /**
   * Generate an index model from the supplied FullRecords
   */
  def getOccIndexModel(records: Array[FullRecord]):Option[OccurrenceIndex]={
    //cycle through all the items in each record and attempt to add them to the index
    val occ = new OccurrenceIndex
    for(i <- 0 to 1){
        val record = records(i)
        for(anObject <- record.objectArray){
            val defn = DAO.getDefn(anObject)
            for(field <- defn){
                //first time through we are processing the raw values
                var fieldName = if(i ==0) "raw_" + field else field
                //we only want to attempt to add the items that should appear in the occurrence
                if(DAO.occurrenceIndexDefn.contains(fieldName)){
                    val fieldValue = anObject.getClass.getMethods.find(_.getName == field).get.invoke(anObject).asInstanceOf[Any]
                    if(fieldValue!=null){
                        occ.setter(fieldName, fieldValue);
                    }
                }
            }
        }
    }
    //perform all the point construction
    if(occ.getDecimalLatitude != null && occ.getDecimalLongitude != null){
      var pat = "#"
      var fieldPre ="point"
      val lat = occ.getDecimalLatitude
      val long = occ.getDecimalLongitude
      for{ i <- 0 to 4}{
        val df =new java.text.DecimalFormat(pat)
        occ.setter(fieldPre + "1" ,df.format(lat) + "," + df.format(long))
        if(i == 0) pat = pat +"."
        pat = pat +"#"
        fieldPre = fieldPre + "0"
      }
      occ.setLatLong(occ.getDecimalLatitude.toString +"," + occ.getDecimalLongitude)
    }
    //set the id for the occurrence record to the uuid
    occ.uuid = records(0).occurrence.uuid
    //set the systemAssertions 
    occ.assertions = records(1).assertions
    Some(occ)
  }

}

object SolrOccurrenceDAO extends IndexDao{
  /**
   * returns whether or not the insert was successful
   */
  import org.apache.commons.lang.StringUtils.defaultString
  def index(items: java.util.List[OccurrenceIndex]):Boolean ={
    try{
    SolrIndexDao.solrServer.addBeans(items)
    SolrIndexDao.solrServer.commit

    }
    catch{
      case e:Exception => false
    }
    true
  }
  def index(item:OccurrenceIndex):Boolean ={
    try{
      SolrIndexDao.solrServer.addBean(item)
    }
    catch{
      case e:Exception => false
    }
    true
  }
  def emptyIndex(){
    SolrIndexDao.solrServer.deleteByQuery("*:*")
  }
  def finaliseIndex(){
    SolrIndexDao.solrServer.commit
    SolrIndexDao.solrServer.optimize
  }
   
   override def getOccIndexModel(records: Array[FullRecord]):Option[OccurrenceIndex]={
    //set the SOLR specific value
    val occ = super.getOccIndexModel(records)
     if(!occ.isEmpty){
     //set the names lsid
      val v = List(defaultString(occ.get.scientificName), defaultString(occ.get.taxonConceptID), defaultString(occ.get.vernacularName), defaultString(occ.get.kingdom), defaultString(occ.get.family))
      occ.get.setNamesLsid(v.mkString("|"))
      occ.get.setMultimedia(if(occ.get.getRaw_associatedMedia() != null) "Multimedia" else "None")
     }
     
     occ
   }

}




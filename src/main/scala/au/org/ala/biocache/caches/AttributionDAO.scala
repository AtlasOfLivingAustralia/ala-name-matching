package au.org.ala.biocache.caches

import scala.collection.JavaConversions
import au.org.ala.biocache._
import org.slf4j.LoggerFactory
import scala.io.Source
import scala.util.parsing.json.JSON
import scala.Some
import java.net.URLEncoder
import au.org.ala.biocache.model.Attribution
import au.org.ala.biocache.load.FullRecordMapper
import au.org.ala.biocache.persistence.PersistenceManager
import au.org.ala.biocache.util.Json
import au.org.ala.biocache.vocab.DwC

/**
 * A DAO for attribution data. The source of this data should be the registry.
 */
object AttributionDAO {

  import JavaConversions._
  private val columnFamily = "attr"
  //can't use a scala hashmap because missing keys return None not null...
  private val lru = new org.apache.commons.collections.map.LRUMap(10000)//new HashMap[String, Option[Attribution]]
  private val persistenceManager = Config.getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]
  //A mapping of the ws json properties to attribution properties
  private val wsPropertyMap = Map("name"->"collectionName", "uid"->"collectionUid", "taxonomyCoverageHints"->"taxonomicHints", "institutionUid"->"institutionUid", "institution"->"institutionName")
  private val lock : AnyRef = new Object()
  val logger = LoggerFactory.getLogger("AttributionDAO")

  /**
   * Persist the attribution information.
   */
  def add(institutionCode:String, collectionCode:String, attribution:Attribution){
    val guid = institutionCode.toUpperCase +"|"+collectionCode.toUpperCase
    val map = FullRecordMapper.mapObjectToProperties(attribution)
    persistenceManager.put(guid,columnFamily,map)
  }

  /**
   * Obtain the data resource attribution information from the cache
   *
   * TODO: Probably should cache these in persistence manager so that they are available if the WS is down
   *
   */
  def getDataResourceByUid(uid:String) : Option[Attribution] ={

    val cachedObject = lru.get(uid)
      if(cachedObject!=null){
        cachedObject.asInstanceOf[Option[Attribution]]
      } else {
        val att = getDataResourceFromWS(uid)
        //cache the data resource info
        lru.put(uid, att)
        att
      }
  }

   def getDataProviderAsMap(value:String):Map[String,String]={
     val json = Source.fromURL(Config.registryUrl+"/dataProvider/" + value + ".json").getLines.mkString
     JSON.parseFull(json).get.asInstanceOf[Map[String, String]]
   }

   def getDataResourceAsMap(value:String):Map[String,String]={
     val json = Source.fromURL(Config.registryUrl+"/dataResource/" + value + ".json").getLines.mkString
     JSON.parseFull(json).get.asInstanceOf[Map[String, String]]
   }

   def getDataResourceFromWS(value:String):Option[Attribution]={
    try {
      if(value == null) return None

      val attribution = new Attribution
      logger.info("Calling web service for " + value)

      val wscontent = WebServiceLoader.getWSStringContent(Config.registryUrl+"/dataResource/"+value+".json")

      val wsmap = Json.toMap(wscontent)

      val name = wsmap.getOrElse("name","").asInstanceOf[String]

      val provenance = wsmap.getOrElse("provenance","").asInstanceOf[String]

      val hints = wsmap.getOrElse("taxonomyCoverageHints",null)
      val ahints = {
        if(hints != null){
          hints.asInstanceOf[java.util.ArrayList[Object]].toArray.map((o:Object) => {
            o.toString().replace("=",":").replace("{","").replace("}","")
          })
        } else {
          null
        }
      }

      //the hub membership
      val hub = wsmap.getOrElse("hubMembership", null)
      val ahub = {
        if(hub != null){
          hub.asInstanceOf[java.util.ArrayList[Object]].toArray.map((o:Object) => {
            (o.asInstanceOf[java.util.LinkedHashMap[Object,Object]]).get("uid").toString
          })
        } else {
          null
        }
      }

      //data Provider
      val dp = wsmap.getOrElse("provider", null).asInstanceOf[java.util.Map[String,String]]
      val dpname = if(dp != null) dp.get("name") else null
      val dpuid = if(dp != null) dp.get("uid") else null
      val hasColl = wsmap.getOrElse("hasMappedCollections", false).asInstanceOf[Boolean]

      //the default DWC terms
      val defaultDwc = wsmap.getOrElse("defaultDarwinCoreValues", null)
      attribution.dataResourceName = name
      attribution.dataProviderUid = dpuid
      attribution.dataProviderName = dpname
      attribution.dataHubUid = ahub
      attribution.taxonomicHints = ahints
      attribution.hasMappedCollections = hasColl
      attribution.provenance = provenance
      if(defaultDwc != null){
        //retrieve the dwc values for the supplied values
          val map = defaultDwc.asInstanceOf[java.util.LinkedHashMap[String,String]]
          val newMap = new java.util.LinkedHashMap[String,String]()
          map.keys.foreach { key:String => {
            val value=map.get(key)
            //get the vocab value
            val v = DwC.matchTerm(key)
            if(v.isDefined && !v.get.canonical.equals(key)){

              //map.remove(key);
              newMap.put(v.get.canonical, value);
            } else {
              newMap.put(key, value)
            }
          }
          attribution.defaultDwcValues = newMap.toMap
        }
      }
      Some(attribution)
    } catch {
      case e:Exception => { logger.error(e.getMessage,e); None }
    }
  }

  /**
   * Retrieve attribution via institution/collection codes.
   * We need to ensure that the cache is large enough to hold all possible values
   * <ol>
   * <li> Check if it is in the local cache</li>
   * <li> Request update from collectory service (cache it locally and in cassandra)</li>
   * <li> If service can not be contacted get the value from cassandra (cache it) </li>
   * </ol>
   */
  def getByCodes(institutionCode:String, collectionCode:String) : Option[Attribution] = {

    if(institutionCode!=null && collectionCode!=null){
      val uuid = institutionCode.toUpperCase+"|"+collectionCode.toUpperCase
      val cachedObject = lru.get(uuid)

      if(cachedObject != null){
        cachedObject.asInstanceOf[Option[Attribution]]
      } else {

        //lookup the collectory against the WS
        logger.info("Looking up collectory web service for " + uuid)
        val wscontent = WebServiceLoader.getWSStringContent(Config.registryUrl+"/lookup/inst/"+URLEncoder.encode(institutionCode)+"/coll/"+URLEncoder.encode(collectionCode)+".json")
        val wsmap = Json.toMap(wscontent)

        if(!wsmap.isEmpty && !wsmap.contains("error")){

          //attempt to map the attribution properties from the JSON objects
          val attribution = new Attribution
          //handle the non standard properties
          val hints = wsmap.getOrElse("taxonomyCoverageHints",null)

          if(hints != null){
            val ahint = hints.asInstanceOf[java.util.ArrayList[Object]].toArray.map((o:Object)=> o.toString().replace("=",":").replace("{","").replace("}",""));
            attribution.taxonomicHints = ahint
          }

          //update the properties
          FullRecordMapper.mapmapPropertiesToObject(attribution, wsmap - "taxonomyCoverageHints", wsPropertyMap)
          val result = Some(attribution)
          //add it to the caches
          lock.synchronized { lru.put(uuid,result) }
          add(institutionCode, collectionCode, attribution)
          result

        } else {

          // grab the value from the cache if it exists
          val map = persistenceManager.get(uuid,"attr")
          val result = {
              if(!map.isEmpty){
                val attribution = new Attribution
                FullRecordMapper.mapPropertiesToObject(attribution,map.get)
                Some(attribution)
              } else {
                None
              }
          }
          lock.synchronized { lru.put(uuid,result) }

          //lru.put(uuid,result)
          result
        }
      }
    } else {
      None
    }
  }
}

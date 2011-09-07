package au.org.ala.biocache

import collection.mutable.HashMap
import org.junit.Ignore
import java.util.UUID
import scala.collection.mutable.ListBuffer

class MockPersistenceManager extends PersistenceManager {

  private val mockStore = new HashMap[String, HashMap[String,HashMap[String,String]]]
  

  def clear = mockStore.clear

  def get(uuid: String, entityName: String, propertyName: String) = {
    val entityMap = mockStore.getOrElseUpdate(entityName,  HashMap[String,HashMap[String,String]]() )
    entityMap.get(uuid) match {
      case Some(map) => map.get(propertyName)
      case None => None
    }
  }

  def get(uuid: String, entityName: String) = {
    val entityMap = mockStore.getOrElseUpdate(entityName,  HashMap[String,HashMap[String,String]]() )
    entityMap.get(uuid) match {
      case Some(x) => Some(x.toMap)
      case None => None
    }
  }

  def getByIndex(uuid: String, entityName: String, idxColumn: String) =
    throw new RuntimeException("not implemented yet")

  def getByIndex(uuid: String, entityName: String, idxColumn: String, propertyName: String) =
    throw new RuntimeException("not implemented yet")

  def getList[A](uuid: String, entityName: String, propertyName: String, theClass: Class[_]) : List[A] = {
      mockStore.get(entityName).get.get(uuid) match {
          case Some(x) => {
              val list = x.get(propertyName)
              if(list.isEmpty)
                  List()
              else
                  Json.toListWithGeneric(list.get, theClass)
              }
          case None => List()
      }
    //throw new RuntimeException("not implemented yet")
  }

  def put(uuid: String, entityName: String, propertyName: String, propertyValue: String) = {
    val entityMap = mockStore.getOrElseUpdate(entityName, HashMap(uuid -> HashMap[String,String]()))
    val recordMap = entityMap.get(uuid).get
    recordMap.put(propertyName, propertyValue)
    uuid
  }

  def put(uuid: String, entityName: String, keyValuePairs: Map[String, String]) = {
    val entityMap = mockStore.getOrElseUpdate(entityName,HashMap(uuid -> HashMap[String,String]()))
    val recordMap = entityMap.getOrElse(uuid, HashMap[String,String]())
    entityMap.put(uuid,(recordMap ++ keyValuePairs).asInstanceOf[HashMap[String,String]])    
    uuid
  }

  def putBatch(entityName: String, batch: Map[String, Map[String, String]]) =
   throw new RuntimeException("not implemented yet")

  def putList[A](uuid: String, entityName: String, propertyName: String, newList: List[A], theClass: Class[_], overwrite: Boolean) ={
      val recordId = { if(uuid != null) uuid else UUID.randomUUID.toString }
      val entityMap = mockStore.getOrElseUpdate(entityName, HashMap(uuid -> HashMap[String,String]()))
      val recordMap = entityMap.getOrElse(uuid, HashMap[String,String]())
      if(overwrite){
          val json:String = Json.toJSONWithGeneric(newList)
          recordMap.put(propertyName,json);
      }else{
          val currentList = getList(uuid, entityName, propertyName, theClass);
          var buffer = new ListBuffer[A]

                for (theObject <- currentList) {
                    if (!newList.contains(theObject)) {
                        //add to buffer
                        buffer + theObject
                    }
                }

                //PRESERVE UNIQUENESS
                buffer ++= newList

                // check equals
                //val newJson = Json.toJSON(buffer.toList)
                val newJson:String = Json.toJSONWithGeneric(buffer.toList)
                recordMap.put(propertyName, newJson)
      }
      recordId
      
  }

  def pageOverAll(entityName: String, proc: (String, Map[String, String]) => Boolean, startUuid: String, endUuid: String, pageSize: Int) =
    throw new RuntimeException("not implemented yet")

  def pageOverSelect(entityName: String, proc: (String, Map[String, String]) => Boolean, startUuid: String, endUuid:String, pageSize: Int, columnName: String*) =
    throw new RuntimeException("not implemented yet")

  def selectRows(uuids: Array[String], entityName: String, propertyNames: Array[String], proc: (Map[String, String]) => Unit) =
    throw new RuntimeException("not implemented yet")

  def deleteColumns(uuid: String, entityName: String, columnName: String*) =
    throw new RuntimeException("not implemented yet")

  def delete(uuid: String, entityName: String) = {
      val entityMap = mockStore.getOrElseUpdate(entityName,HashMap(uuid -> HashMap[String,String]()))
      entityMap.remove(uuid)
  }

  def shutdown = mockStore.clear
}

@Ignore
//The mock config module to be used for the tests
class TestConfigModule extends com.google.inject.AbstractModule{
    
    override def configure() {
val properties = {
      val properties = new java.util.Properties()
      properties.load(this.getClass.getResourceAsStream("/biocache.properties"))
      properties
    }
        com.google.inject.name.Names.bindProperties(this.binder, properties)

        //bind concrete implementations
        bind(classOf[OccurrenceDAO]).to(classOf[OccurrenceDAOImpl]).in(com.google.inject.Scopes.SINGLETON)
        bind(classOf[IndexDAO]).to(classOf[SolrIndexDAO]).in(com.google.inject.Scopes.SINGLETON)
        try {
            val nameIndex = new au.org.ala.checklist.lucene.CBIndexSearch(properties.getProperty("nameIndexLocation"))
            bind(classOf[au.org.ala.checklist.lucene.CBIndexSearch]).toInstance(nameIndex)
           
            
        } catch {
            case e: Exception =>e.printStackTrace()
        }
        bind(classOf[PersistenceManager]).to(classOf[MockPersistenceManager]).in(com.google.inject.Scopes.SINGLETON)
        println("Using Test Config")
    }
    
}

@Ignore
object TestMocks {

  def main(args:Array[String]){
    val m = new MockPersistenceManager
    m.put("test-uuid", "occ", "dave", "daveValue")
    m.put("12.12|12.43", "loc", "ibra", "Australian Alps")
    println(m.get("test-uuid", "occ", "dave"))
    println(m.get("12.12|12.43", "loc", "dave"))
    println(m.get("12.12|12.43sss", "loc", "dave"))
  }
}
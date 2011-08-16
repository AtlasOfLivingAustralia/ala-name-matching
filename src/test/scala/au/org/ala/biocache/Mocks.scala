package au.org.ala.biocache

import collection.mutable.HashMap

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
    mockStore.get(entityName).get.get(uuid) match {
      case Some(x) => Some(x.toMap)
      case None => None
    }
  }

  def getByIndex(uuid: String, entityName: String, idxColumn: String) =
    throw new RuntimeException("not implemented yet")

  def getByIndex(uuid: String, entityName: String, idxColumn: String, propertyName: String) =
    throw new RuntimeException("not implemented yet")

  def getList[A](uuid: String, entityName: String, propertyName: String, theClass: Class[_]) =
    throw new RuntimeException("not implemented yet")

  def put(uuid: String, entityName: String, propertyName: String, propertyValue: String) = {
    val entityMap = mockStore.getOrElseUpdate(entityName, HashMap(uuid -> HashMap[String,String]()))
    val recordMap = entityMap.get(uuid).get
    recordMap.put(propertyName, propertyValue)
    uuid
  }

  def put(uuid: String, entityName: String, keyValuePairs: Map[String, String]) = {
    mockStore.get(entityName).get.put(uuid, (HashMap() ++ keyValuePairs).asInstanceOf[HashMap[String,String]])
    uuid
  }

  def putBatch(entityName: String, batch: Map[String, Map[String, String]]) =
   throw new RuntimeException("not implemented yet")

  def putList[A](uuid: String, entityName: String, propertyName: String, objectList: List[A], theClass: Class[_], overwrite: Boolean) =
    throw new RuntimeException("not implemented yet")

  def pageOverAll(entityName: String, proc: (String, Map[String, String]) => Boolean, startUuid: String, endUuid: String, pageSize: Int) =
    throw new RuntimeException("not implemented yet")

  def pageOverSelect(entityName: String, proc: (String, Map[String, String]) => Boolean, startUuid: String, pageSize: Int, columnName: String*) =
    throw new RuntimeException("not implemented yet")

  def selectRows(uuids: Array[String], entityName: String, propertyNames: Array[String], proc: (Map[String, String]) => Unit) =
    throw new RuntimeException("not implemented yet")

  def deleteColumns(uuid: String, entityName: String, columnName: String*) =
    throw new RuntimeException("not implemented yet")

  def delete(uuid: String, entityName: String) =
    throw new RuntimeException("not implemented yet")

  def shutdown = mockStore.clear
}


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
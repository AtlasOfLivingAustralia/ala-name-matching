package au.org.ala.biocache.persistence

import org.scale7.cassandra.pelops._
import org.apache.cassandra.thrift._
import java.util.UUID
import au.org.ala.biocache.util.Json
import java.util
import scala.collection.{JavaConversions, mutable}
import scala.Some
import scala.collection.mutable.ListBuffer
import com.google.inject.name.Named
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import org.scale7.cassandra.pelops.pool.CommonsBackedPool
import scala.Some

/**
 * Cassandra based implementation of a persistence manager.
 * This should maintain most of the cassandra logic
 *
 * Major change: The thrift API now works with ByteBuffer instead of byte[]
 */
class CassandraPersistenceManager @Inject() (
       @Named("cassandra.hosts") val host:String = "localhost",
       @Named("cassandra.port") val port:Int = 9160,
       @Named("cassandra.pool") val poolName:String = "biocache-store-pool",
       @Named("cassandra.keyspace") val keyspace:String = "occ",
       @Named("cassandra.max.connections")val maxConnections:Int= -1,
       @Named("cassandra.max.retries") val maxRetries:Int= 3,
       @Named("thrift.operation.timeout") val operationTimeout:Int= 4000) extends PersistenceManager {

  import JavaConversions._

  val logger = LoggerFactory.getLogger("CassandraPersistenceManager")

  val maxColumnLimit = 10000
  val cluster = new Cluster(host,port,operationTimeout, false)
  val policy = new CommonsBackedPool.Policy()
  policy.setMaxTotal(maxConnections)
  //According to Pelops : As a general rule the pools maxWaitForConnection should be three times larger than the thrift timeout value.
  policy.setMaxWaitForConnection(3 * operationTimeout)
  //operations policy, first arg indicates how many time a failed operation will be retried, the second indicates that null value insert should be treated as a delete
  val operandPolicy = new OperandPolicy(maxRetries,false)

  initialise     //setup DB connections

  def initialise {
    logger.debug("Initialising cassandra connection pool with pool name: " + poolName)
    logger.debug("Initialising cassandra connection pool with hosts: " + host)
    logger.debug("Initialising cassandra connection pool with port: " + port)
    logger.debug("Initialising cassandra connection pool with max connections: " + maxConnections)
    logger.debug("Initialising cassandra connection pool with max retries: " + maxRetries)
    logger.debug("Initialising cassandra connection pool with operation timeout: " + operationTimeout)

    //Cluster wide settings including the thrift operation timeout
    Pelops.addPool(poolName, cluster, keyspace, policy, operandPolicy)
  }

  /**
   * Retrieve an array of objects, parsing the JSON stored.
   */
  def get(uuid:String, entityName:String) = {
    val selector = Pelops.createSelector(poolName)
    val slicePredicate = Selector.newColumnsPredicateAll(true, maxColumnLimit)
    try {
      val columnList = selector.getColumnsFromRow(entityName,uuid, slicePredicate, ConsistencyLevel.ONE)
      if(columnList.isEmpty){
        None
      } else {
        Some(columnList2Map(columnList))
      }
    } catch {
      case e:Exception => logger.trace(e.getMessage, e); None   //this is expected behaviour where no value exists
    }
  }

  /**
   * Retrieves a range of columns for the supplied uuid from the specified entity
   */
  def get(uuid:String, entityName:String, startProperty:String, endProperty:String):Option[java.util.List[Column]]={
    val selector = Pelops.createSelector(poolName)
    val slicePredicate = Selector.newColumnsPredicate(startProperty,endProperty,false,maxColumnLimit)
    try {
      Some(selector.getColumnsFromRow(entityName, uuid, slicePredicate, ConsistencyLevel.ONE))
    } catch {
      case e:Exception => None
    }
  }

  /**
   * Retrieves a list of columns and the last time in ms that they were modified.
   *
   * This will support removing columns that were not updated during a reload
   */
  def getColumnsWithTimestamps(uuid:String, entityName:String): Option[Map[String, Long]]={
    val selector = Pelops.createSelector(poolName)
    val slicePredicate = Selector.newColumnsPredicateAll(true, maxColumnLimit)
    try {
      val columnList = selector.getColumnsFromRow(entityName,uuid, slicePredicate, ConsistencyLevel.ONE)
      if(columnList.isEmpty){
        None
      } else {
        Some(columnList2TimeMap(columnList))
      }
    } catch {
      case e:Exception => logger.debug(e.getMessage, e); None
    }
  }

  /**
   * Retrieve an array of objects, parsing the JSON stored.
   *
   * We are storing rows keyed against unique id's that we don't wish to expose
   * to users. We wish to expose a static UUID to the uses. This UUID will be
   * indexed against thus is queryable.
   *
   * The performance of the index is slightly worse that lookup by key. This should
   * be alright because the index should only be hit for reads via webapp.
   *
   */
  def getByIndex(uuid:String, entityName:String, idxColumn:String) : Option[Map[String,String]] =
    getFirstValuesFromIndex(entityName, idxColumn, uuid, Selector.newColumnsPredicateAll(true, maxColumnLimit))

  /**
   * Retrieves a specific property value using the index as the retrieval method.
   */
  def getByIndex(uuid:String, entityName:String, idxColumn:String, propertyName:String) = {
    val map = getFirstValuesFromIndex(entityName, idxColumn, uuid, Selector.newColumnsPredicate(propertyName))
    if(map.isEmpty)
      None
    else
      Some(map.get.getOrElse(propertyName, ""))
  }

  /**
   * Retrieves the first record from the index that matches the value.
   *
   * In the use case for the biocache index there will only erve be one record for each value.
   */
  def getFirstValuesFromIndex(entityName:String, idxColumn:String, value:String, slicePredicate:SlicePredicate) : Option[Map[String,String]] ={
    val selector = Pelops.createSelector(poolName)
    //set up the index clause information
    val indexClause = Selector.newIndexClause(value, 1, Selector.newIndexExpression(idxColumn, IndexOperator.EQ, Bytes.fromUTF8(value) ))
    try {
      val columnMap = selector.getIndexedColumns(entityName, indexClause,slicePredicate, ConsistencyLevel.ONE)
      if(columnMap != null && !columnMap.isEmpty){
        val columnList = columnMap.entrySet.iterator.next.getValue.asInstanceOf[java.util.List[Column]]
        val map = columnList2Map(columnList)
        Some(map)
      } else {
        None
      }
    } catch {
      case e:Exception => logger.warn(e.getMessage, e); None
    }
  }

  /**
   * Retrieve the column value, handling NotFoundExceptions from cassandra thrift.
   *
   * NC:2013-05-10: we want to throw the exception when any other exception is received.  This is so that errors are
   * not swallowed in the guise of being a "NotFoundException". Thus allowing us to terminate a load midcycle.
   *
   */
  def get(uuid:String, entityName:String, propertyName:String) = {
    try {
      val selector = Pelops.createSelector(poolName)
      val column = selector.getColumnFromRow(entityName, uuid, propertyName, ConsistencyLevel.ONE)
      Some(new String(column.getValue, "UTF-8"))
    } catch {
      case e:org.scale7.cassandra.pelops.exceptions.NotFoundException => None   //this is epected behaviour with cassandra
      case e:Exception => logger.error(e.getMessage, e); throw e
    }
  }

  /**
   * Only retrieves the supplied fields for the record.
   */
  def getSelected(uuid:String, entityName:String, propertyNames:Seq[String]):Option[Map[String,String]] ={
    val selector = Pelops.createSelector(poolName)
    val slicePredicate = Selector.newColumnsPredicate(propertyNames:_*)
    try {
      val columnList = selector.getColumnsFromRow(entityName,uuid, slicePredicate, ConsistencyLevel.ONE)
      if(columnList.isEmpty){
        None
      } else {
        Some(columnList2Map(columnList))
      }
    } catch {
      case e:Exception => logger.trace(e.getMessage, e); None   //this is expected behaviour where no value exists
    }
  }

  /**
   * Store the supplied batch of maps of properties as separate columns in cassandra.
   */
  def putBatch(entityName: String, batch: Map[String, Map[String, String]]) = {
    val mutator = Pelops.createMutator(poolName)
    batch.foreach(uuidMap => {
      val uuid = uuidMap._1
      val keyValuePairs = uuidMap._2
      keyValuePairs.foreach( keyValue => {
        mutator.writeColumn(entityName, uuid, mutator.newColumn(keyValue._1, keyValue._2))
      })
    })
    mutator.execute(ConsistencyLevel.ONE)
  }

  /**
   * Store the supplied map of properties as separate columns in cassandra.
   */
  def put(uuid:String, entityName:String, keyValuePairs:Map[String, String]) = {

    val recordId = { if(uuid != null) uuid else UUID.randomUUID.toString }

    val mutator = Pelops.createMutator(poolName)
    keyValuePairs.foreach( keyValue => {
      //NC: only add the column if the value is not null
      if(keyValue._2!=null){
        mutator.writeColumn(entityName, recordId, mutator.newColumn(keyValue._1, keyValue._2))
      }
    })
    //add the recordId to the columns if it has been generated.  This makes uuid value reads faster that ByteBuffer key conversions
    if(uuid == null){
      mutator.writeColumn(entityName, recordId, mutator.newColumn("uuid", recordId))
    }
    mutator.execute(ConsistencyLevel.ONE)
    recordId
  }

  /**
   * Store the supplied property value in the column
   */
  def put(uuid:String, entityName:String, propertyName:String, propertyValue:String) = {
    val recordId = { if(uuid != null) uuid else UUID.randomUUID.toString }
    val mutator = Pelops.createMutator(poolName)
    mutator.writeColumn(entityName, recordId, mutator.newColumn(propertyName, propertyValue))
    //add the recordId to the columns if it has been generated.  This makes uuid value reads faster that ByteBuffer key conversions
    if(uuid == null){
      mutator.writeColumn(entityName, recordId, mutator.newColumn("uuid", recordId))
    }
    mutator.execute(ConsistencyLevel.ONE)
    recordId
  }

  /**
   * Retrieve the column value, and parse from JSON to Array
   */
  def getList[A](uuid:String, entityName:String, propertyName:String, theClass:java.lang.Class[_]): List[A] = {
    val column = getColumn(uuid, entityName, propertyName)
    if (column.isEmpty) {
      List()
    } else {
      val json = new String(column.get.getValue)
      Json.toListWithGeneric(json, theClass)
    }
  }

  /**
   * Store arrays in a single column as JSON.
   */
  //def putList(uuid:String, entityName:String, propertyName:String, newList:List[AnyRef], overwrite:Boolean) = {
  def putList[A](uuid: String, entityName: String, propertyName: String, newList: Seq[A], theClass:java.lang.Class[_], overwrite: Boolean) = {

    val recordId = { if(uuid != null) uuid else UUID.randomUUID.toString }
    //initialise the serialiser
    val mutator = Pelops.createMutator(poolName)

    if (overwrite) {
      //val json = Json.toJSON(newList)
      val json:String = Json.toJSONWithGeneric(newList)
      mutator.writeColumn(entityName, recordId, mutator.newColumn(propertyName, json))
    } else {

      //retrieve existing values
      val column = getColumn(uuid, entityName, propertyName)
      //if empty, write, if populated resolve
      if (column.isEmpty) {
        //write new values
        val json:String = Json.toJSONWithGeneric(newList)
        mutator.writeColumn(entityName, recordId, mutator.newColumn(propertyName, json))
      } else {
        //retrieve the existing objects
        val currentJson = new String(column.get.getValue)
        val currentList = Json.toListWithGeneric(currentJson, theClass)
        var buffer = new ListBuffer[A]

        for (theObject <- currentList) {
          if (!newList.contains(theObject)) {
            //add to buffer
            buffer += theObject
          }
        }

        //PRESERVE UNIQUENESS
        buffer ++= newList

        // check equals
        //val newJson = Json.toJSON(buffer.toList)
        val newJson:String = Json.toJSONWithGeneric(buffer.toList)
        mutator.writeColumn(entityName, recordId, mutator.newColumn(propertyName, newJson))
      }
    }
    mutator.execute(ConsistencyLevel.ONE)
    recordId
  }

  /**
   * Generic page over method. Individual pageOver methods should provide a slicePredicate that
   * is used to determine the columns that are returned...
   *
   * We need to ignore empty rows if the SlicePredicate id for ALL columns.  This is configuration in Cassandra 0.8.8:
   * https://issues.apache.org/jira/browse/CASSANDRA-2855
   *
   *
   * @param startUuid, The uuid of the occurrence at which to start the paging
   */
  def pageOver(entityName:String,proc:((String, Map[String,String])=>Boolean), pageSize:Int,
               slicePredicate:SlicePredicate, checkEmpty:Boolean=false,startUuid:String="",endUuid:String="") = {

    var startKey = new Bytes(startUuid.getBytes)
    var endKey = new Bytes(endUuid.getBytes)
    var keyRange = Selector.newKeyRange(startKey, endKey, pageSize+1)
    var hasMore = true
    var counter = 0
    //Please note we are not paging by UTF8 because it is much slower
    var continue = true
    var columnMap = getColumnsFromRowsWithRetries(entityName, keyRange, slicePredicate, ConsistencyLevel.ONE, 10)

    while (!columnMap.isEmpty && continue) {
      val columnsObj = List(columnMap.keySet.toArray : _*)
      //convert to scala List
      val keys = columnsObj.asInstanceOf[List[Bytes]]
      startKey = keys.last
      keys.foreach(buuid =>{
        val columnList = columnMap.get(buuid)
        if(!checkEmpty || !columnList.isEmpty){
          //procedure a map of key value pairs
          val map = columnList2Map(columnList)
          //more efficient to use a stored version of the rowKey then attempt to convert the buuid
          val uuid = map.getOrElse("rowKey", buuid.toUTF8)
          //pass the record ID and the key value pair map to the proc
          continue = proc(uuid, map)
        }
      })
      counter += keys.size
      keyRange = Selector.newKeyRange(startKey, endKey, pageSize+1)
      columnMap = getColumnsFromRowsWithRetries(entityName, keyRange, slicePredicate, ConsistencyLevel.ONE, 10)
      columnMap.remove(startKey)
    }

    if(counter > 0){
      logger.debug("Finished paging. Records paged over : " + counter)
    }
  }

  /**
   * Retrieve a list of records as maps of columns.
   */
  private def getColumnsFromRowsWithRetries(entityName:String, keyRange:KeyRange, slicePredicate:SlicePredicate,
                                            cl:ConsistencyLevel, permittedRetries:Int) : java.util.Map[Bytes, java.util.List[Column]] = {
    var success = false
    var noOfRetries = 0
    var columnMap:java.util.Map[Bytes, java.util.List[Column]] = null
    while(!success){
      try {
        val selector = Pelops.createSelector(poolName)
        columnMap = selector.getColumnsFromRows(entityName, keyRange, slicePredicate, cl)
        success = true
      } catch {
        case ir:org.scale7.cassandra.pelops.exceptions.InvalidRequestException => {
          logger.debug("Invalid key range supplied to cassandra: " + ir.getMessage, ir)
          success = true
          columnMap = new util.HashMap[Bytes, java.util.List[Column]]
        }
        case e:Exception => {
          logger.debug("Problem retrieving data. Number of retries left:" + (permittedRetries - noOfRetries) +
            ", Error: " + e.getMessage)
          Thread.sleep(20000)
          //Don't remove the pool because all requests while the reinit happens will result in NPE.
          //Pelops.removePool(poolName)
          initialise //re-initialise
          if (noOfRetries == permittedRetries){
            logger.error("Problem retrieving data. Number of DB connection retries exceeded. Error: " + e.getMessage, e)
            throw new RuntimeException(e)
          }
          noOfRetries += 1
        }
      }
    }
    columnMap
  }

  /**
   * Pages over all the records with the selected columns.
   * @param columnName The names of the columns that need to be provided for processing by the proc
   */
  def pageOverSelect(entityName:String, proc:((String, Map[String,String]) => Boolean), startUuid:String, endUuid:String, pageSize:Int, columnName:String*){
    val slicePredicate = Selector.newColumnsPredicate(columnName:_*)
    pageOver(entityName, proc, pageSize, slicePredicate, startUuid=startUuid, endUuid=endUuid)
  }

  /**
   * Pages over the records returns the columns that fit within the startColumn and endColumn range
   */
  def pageOverColumnRange(entityName:String, proc:((String, Map[String,String])=>Boolean), startUuid:String="", endUuid:String="", pageSize:Int=1000, startColumn:String="", endColumn:String=""){
    val slicePredicate = Selector.newColumnsPredicate(startColumn, endColumn, false, maxColumnLimit)
    //can't use this because we want to page of the column range too just in case the number of columns > than maxColumnLimit
    //pageOver(entityName, proc, pageSize, slicePredicate,true,startUuid=startUuid, endUuid=endUuid)
    var startKey = new Bytes(startUuid.getBytes)
    var endKey = new Bytes(endUuid.getBytes)
    var keyRange = Selector.newKeyRange(startKey, endKey, pageSize+1)
    var hasMore = true
    var counter = 0
    //Please note we are not paging by UTF8 because it is much slower
    var continue = true
    var columnMap = getColumnsFromRowsWithRetries(entityName, keyRange, slicePredicate, ConsistencyLevel.ONE, 10)

    while (!columnMap.isEmpty && continue) {
      val columnsObj = List(columnMap.keySet.toArray : _*)
      //convert to scala List
      val keys = columnsObj.asInstanceOf[List[Bytes]]
      startKey = keys.last
      keys.foreach(buuid => {
        val columnList = columnMap.get(buuid)

        //now get the remaining columns for the record
        var moreCols = true
        val uuid = buuid.toUTF8
        var startCol = new String(columnList.get(columnList.size-1).getName(),"UTF-8")
        while(moreCols){
          var nextCols = get(uuid, entityName,startCol,endColumn)
          if(nextCols.isDefined){
            nextCols.get.remove(0) //remove the repeated item from the last set of columns
            if(!nextCols.get.isEmpty()){
              columnList.addAll(nextCols.get)
              startCol = new String(columnList.get(columnList.size-1).getName(),"UTF-8")
            } else {
              moreCols = false
            }
          }
        }

        if(!columnList.isEmpty){
          //procedure a map of key value pairs
          val map = columnList2Map(columnList)
          //pass the record ID and the key value pair map to the proc
          continue = proc(uuid, map)
        }
      })
      counter += keys.size
      keyRange = Selector.newKeyRange(startKey, endKey, pageSize+1)
      columnMap = getColumnsFromRowsWithRetries(entityName, keyRange, slicePredicate, ConsistencyLevel.ONE, 10)
      columnMap.remove(startKey)
    }

    if(counter > 0){
      logger.debug("Finished paging. Records paged over : " + counter)
    }
  }

  /**
   * Iterate over all occurrences, passing the objects to a function.
   * Function returns a boolean indicating if the paging should continue.
   *
   * @param proc
   * @param startUuid, The uuid of the occurrence at which to start the paging
   */
  def pageOverAll(entityName:String, proc:((String, Map[String,String])=>Boolean),startUuid:String="", endUuid:String="", pageSize:Int = 1000) {
    val slicePredicate = Selector.newColumnsPredicateAll(true, maxColumnLimit)
    pageOver(entityName, proc, pageSize, slicePredicate,true,startUuid=startUuid, endUuid=endUuid)
  }

  /**
   * Select fields from rows and pass to the supplied function.
   */
  def selectRows(rowkeys:Seq[String], entityName:String, fields:Seq[String], proc:((Map[String,String])=>Unit)) {
    val selector:Selector = Pelops.createSelector(poolName)
    val slicePredicate = Selector.newColumnsPredicate(fields:_*)

    //retrieve the columns
    val columnMap = selector.getColumnsFromRowsUtf8Keys(entityName, rowkeys.toList, slicePredicate, ConsistencyLevel.ONE)

    //write them out to the output stream
    val keys = List(columnMap.keySet.toArray : _*)

    //identify el* cl* fields
    keys.foreach(key =>{
      val columnsList = columnMap.get(key)
      val fieldValues = columnsList.map(column => (new String(column.getName, "UTF-8"),new String(column.getValue, "UTF-8"))).toArray
      val map = scala.collection.mutable.Map.empty[String,String]
      fieldValues.foreach(fieldValue =>  map(fieldValue._1) = fieldValue._2)
      proc(map.toMap) //pass the map to the function for processing
    })
  }

  /**
   * Convert a set of cassandra columns into a key-value pair map.
   */
  protected def columnList2Map(columnList:java.util.List[Column]) : Map[String,String] = {
    val map = new mutable.HashMap[String, String]
    val iter = columnList.iterator()
    while(iter.hasNext){
      val c = iter.next()
      map.put(new String(c.getName, "UTF-8"), new String(c.getValue, "UTF-8"))
    }
    map.toMap
  }

  /**
   * Converts a set of cassandra columns to a column name to last modified map.
   *
   *  This will support the removal of columns that were not updated during a new load
   *
   *  NB Pelops provides timestamps in microseconds thus values will be /1000 and returned in milliseconds
   *
   */
  protected def columnList2TimeMap(columnList:java.util.List[Column]) :Map[String,Long]={
    val map = new mutable.HashMap[String, Long]
    val iter = columnList.iterator()
    while(iter.hasNext){
      val c = iter.next()
      map.put(new String(c.getName, "UTF-8"), c.getTimestamp()/1000)
    }
    map.toMap
  }

  /**
   * Convienience method for accessing values.
   */
  protected def getColumn(uuid: String, columnFamily: String, columnName: String): Option[Column] = {
    try {
      val selector = Pelops.createSelector(poolName)
      Some(selector.getColumnFromRow(columnFamily, uuid, columnName, ConsistencyLevel.ONE))
    } catch {
      case e: Exception => {
        logger.debug(e.getMessage + " for " + uuid + " - " + columnFamily + " - " + columnName)
        None //expected behaviour when row doesnt exist
      }
    }
  }

  def shutdown = try {
    Pelops.shutdown
  } catch {
    case e:Exception => logger.warn(e.getMessage(), e)
  }

  /**
   * Delete the value for the supplied column
   */
  def deleteColumns(uuid:String, entityName:String, columnName:String*)={
    if(uuid != null && entityName != null && columnName != null){
      val mutator = Pelops.createMutator(poolName)
      mutator.deleteColumns(entityName, uuid, columnName:_*)
      mutator.execute(ConsistencyLevel.ONE)
    }
  }

  /**
   * Removes the record for the supplied uuid from entityName.
   */
  def delete(uuid:String, entityName:String)={
    if(uuid != null && entityName != null){
      val deletor = Pelops.createRowDeletor(poolName)
      deletor.deleteRow(entityName, uuid, ConsistencyLevel.ONE)
    }
  }
}

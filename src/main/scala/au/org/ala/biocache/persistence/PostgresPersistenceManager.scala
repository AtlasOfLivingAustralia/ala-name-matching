package au.org.ala.biocache.persistence

import collection.JavaConversions
import scala.collection.mutable.{ArrayBuffer, HashMap, ListBuffer}
import org.slf4j.LoggerFactory
import com.google.inject.name.Named
import com.google.inject.Inject
import java.util.UUID
import scala.slick.session.Database
import Database.threadLocalSession
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import org.postgresql.util.PSQLException
import org.postgresql.util.HStoreConverter

/**
 * An unfinished and experimental persistence manager that backs on to Postgres and makes use of a HStore data type.
 * Test DB schema is in postgres/db.sql
 */
class PostgresPersistenceManager() extends PersistenceManager {

  import JavaConversions._

  val logger = LoggerFactory.getLogger("PostgresPersistenceManager")

   /**
    * Put a single property.
    */
   def put(uuid: String, entityName: String, propertyName: String, propertyValue: String): String = {
     Database.forURL("jdbc:postgresql://localhost/occ", driver = "org.postgresql.Driver", user = "postgres", password = "postgres") withSession {
       // Create the tables, including primary and foreign keys
       Q.updateNA(s"""INSERT INTO $entityName (uuid, doc) VALUES ('$uuid', '"$propertyName" => "$propertyValue"');""").execute
     }
     uuid
   }

   /**
    * Put a single property.
    */
   def put(uuid: String, entityName: String, properties:Map[String,String]): String = {

     try {
       Database.forURL("jdbc:postgresql://localhost/occ", driver = "org.postgresql.Driver", user = "postgres", password = "postgres") withSession {
         // Create the tables, including primary and foreign keys
         var buff = new ArrayBuffer[String]()
         properties.foreach({ case (key:String, value:String) => {
           val valEscaped = value.replaceAll("\"", "\\\\\"").replaceAll("'", "''")
           buff +=  s""""$key" => "$valEscaped""""
         }})
         val keyValuePairs = buff.mkString(",")
         Q.updateNA(s"""INSERT INTO $entityName (uuid, doc) VALUES ('$uuid', '$keyValuePairs');""").execute
       }
     } catch {
       case e:PSQLException => {
         //already exists...
         Database.forURL("jdbc:postgresql://localhost/occ", driver = "org.postgresql.Driver", user = "postgres", password = "postgres") withSession {
           // Create the tables, including primary and foreign keys
           var buff = new ArrayBuffer[String]()
           properties.foreach({ case (key:String, value:String) => {
             val valEscaped = value.replaceAll("\"", "\\\\\"").replaceAll("'", "''")
             buff +=  s""""$key" => "$valEscaped""""
           }})
           val keyValuePairs = buff.mkString(",")
           Q.updateNA(s"""UPDATE $entityName SET doc = '$keyValuePairs' WHERE uuid = '$uuid' ;""").execute
         }
       }
     }
     uuid
   }

   /**
    * Get a single property.
    */
   def get(uuid: String, entityName: String, propertyName: String): Option[String] = {
     var value:Option[String] = None
     Database.forURL("jdbc:postgresql://localhost/occ", driver = "org.postgresql.Driver", user = "postgres", password = "postgres") withSession {
       val result = Q.queryNA[String](s"SELECT doc->'$propertyName' from $entityName where uuid = '$uuid';")
       value = result.firstOption
     }
     value
   }

   /**
    * Gets the supplied properties for this record
    */
   def getSelected(uuid: String, entityName: String, propertyNames: Seq[String]): Option[Map[String, String]] = {
     throw new RuntimeException("Not implemented")
   }

   /**
    * Get a key value pair map for this record.
    */
   def get(uuid: String, entityName: String): Option[Map[String, String]] = {

     var returnValue:Option[Map[String,String]] = None

   Database.forURL("jdbc:postgresql://localhost/occ", driver = "org.postgresql.Driver", user = "postgres", password = "postgres") withSession {
     val results = Q.queryNA[(String,String)](s"SELECT uuid, doc from $entityName where uuid = '$uuid';")
     results.firstOption match {
       case Some((uuid,doc)) => {
       val map:Map[String,String] = HStoreConverter.fromString(doc).asInstanceOf[java.util.HashMap[String,String]].toMap
       returnValue = Some(map)
     }
     case None => None
       }
     }
     returnValue
   }

   /**
    * Get a key value pair map for this column time stamps of this record.
    */
   def getColumnsWithTimestamps(uuid: String, entityName: String): Option[Map[String, Long]] = {
     throw new RuntimeException("Not implemented")
   }

   /**
    * Gets KVP map for a record based on a value in an index
    */
   def getByIndex(uuid: String, entityName: String, idxColumn: String): Option[Map[String, String]] = {
     throw new RuntimeException("Not implemented")
   }

   /**
    * Gets a single property based on an indexed value.  Returns the value of the "first" matched record.
    */
   def getByIndex(uuid: String, entityName: String, idxColumn: String, propertyName: String): Option[String] = {
     throw new RuntimeException("Not implemented")
   }

   /**
    * Retrieve an array of objects from a single column.
    */
   def getList[A](uuid: String, entityName: String, propertyName: String, theClass: Class[_]): List[A] = {
     //throw new RuntimeException("Not implemented")
     List[A]()
   }

   /**
    * Add a batch of properties.
    */
   def putBatch(entityName: String, batch: Map[String, Map[String, String]]): Unit = {
     logger.warn("loading batch....WARNING not implemented......")
   }

   /**
    * @param overwrite if true, current stored value will be replaced without a read.
    */
   def putList[A](uuid: String, entityName: String, propertyName: String, objectList:Seq[A], theClass: Class[_], overwrite: Boolean): String = {
     //throw new RuntimeException("Not implemented")
     logger.warn("putList....WARNING not implemented......")
     uuid
   }

   /**
    * Page over all entities, passing the retrieved UUID and property map to the supplied function.
    * Function should return false to exit paging.
    */
   def pageOverAll(entityName: String, proc: (String, Map[String, String]) => Boolean, startUuid:String, endUuid:String, pageSize:Int): Unit = {

     var hasMore = true
     var offset = 0

     while(hasMore){
       var count = 0
     Database.forURL("jdbc:postgresql://localhost/occ", driver = "org.postgresql.Driver", user = "postgres", password = "postgres") withSession {
       val results = Q.queryNA[(String,String)](s"SELECT uuid, doc from $entityName where uuid >= '$startUuid' limit $pageSize offset $offset;")
       results.foreach(result => {
         val uuid = result._1
         val map:Map[String,String] = HStoreConverter.fromString(result._2).asInstanceOf[java.util.HashMap[String,String]].toMap
         proc(uuid, map)
         println(result)
         count += 1
       })
       }
       hasMore = count > 0
       offset += pageSize
     }
   }

   /**
    * Page over the records, retrieving the supplied columns only.
    */
   def pageOverSelect(entityName: String, proc: (String, Map[String, String]) => Boolean, startUuid: String, endUuid: String, pageSize: Int, columnName: String*): Unit = {
     pageOverAll(entityName, proc, startUuid, endUuid, pageSize)
   }

   /**
    * Page over the records, retrieving the supplied columns range.
    */
   def pageOverColumnRange(entityName: String, proc: (String, Map[String, String]) => Boolean, startUuid: String, endUuid: String, pageSize: Int, startColumn: String, endColumn: String): Unit = throw new RuntimeException("Not implemented")

   /**
    * Select the properties for the supplied record UUIDs
    */
   def selectRows(uuids: Seq[String], entityName: String, propertyNames: Seq[String], proc: (Map[String, String]) => Unit): Unit = throw new RuntimeException("Not implemented")

   /**
    * The column to delete.
    */
   def deleteColumns(uuid: String, entityName: String, columnName: String*): Unit = throw new RuntimeException("Not implemented")

   /**
    * Delete row
    */
   def delete(uuid: String, entityName: String): Unit = throw new RuntimeException("Not implemented")

   /**
    * Close db connections etc
    */
   def shutdown: Unit = {}
 }

/*
 * Some Reports that were necessary to provide used DwC fields and evaulate
 * possible bugs...
 *
 */

package au.org.ala.report

import au.org.ala.biocache._
import au.org.ala.util.FileHelper
import java.io.File

object LatLongReport{
  val persistenceManager = Config.getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]

   def main(args: Array[String]): Unit = {
     val start = System.currentTimeMillis
        var startTime = System.currentTimeMillis
        var finishTime = System.currentTimeMillis
        var counter=0
        val llSet :scala.collection.mutable.HashSet[String] = new scala.collection.mutable.HashSet[String]
        
            persistenceManager.pageOverAll("occ", (guid, map)=> {
              //get the lat long
              counter+=1
              if(map.contains("decimalLongitude") && map.contains("decimalLatitude")){
                llSet.add(map.get("decimalLatitude").get + "," + map.get("decimalLongitude").get)
//                if(map.get("decimalLatitude").get.contains("000000"))
//                  println(guid + " has long latitude")
              }
              if (counter % 1000 == 0) {
              finishTime = System.currentTimeMillis
              println(counter + " >> Last key : " + guid + " records per sec: " + 1000f / (((finishTime - startTime).toFloat) / 1000f))
              startTime = System.currentTimeMillis

              //exit(0);
            }
              true
            })
          //write the content to stream
          printToFile(new File("/data/biocache/distinctpoints.csv"))(p => {
              llSet.foreach(p.println)
            })



   }
   def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    p.println("Latitude,Longitude")
    try { op(p) } finally { p.close() }
   }
}

object Report {

    val occurrenceDAO = Config.getInstance(classOf[OccurrenceDAO]).asInstanceOf[OccurrenceDAO]
    val persistenceManager = Config.getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]

   def main(args: Array[String]): Unit = {
      val start = System.currentTimeMillis
        var startTime = System.currentTimeMillis
        var finishTime = System.currentTimeMillis
        var counter=0
        val nameSet :scala.collection.mutable.HashSet[String] = new scala.collection.mutable.HashSet[String]
            persistenceManager.pageOverAll("occ", (guid, map)=> {
            counter += 1

            //get the field names
            val instUid = map.get("institutionUid.p").getOrElse(null)
            if(instUid != null && instUid != "in4"){
              //add the raw field names to the
              val columns = map.keySet
              for(fieldName<-columns){
                if(!fieldName.contains("."))
                  nameSet.add(fieldName)
              }

            }

            if (counter % 1000 == 0) {
              finishTime = System.currentTimeMillis
              println(counter + " >> Last key : " + guid + " records per sec: " + 1000f / (((finishTime - startTime).toFloat) / 1000f))
              startTime = System.currentTimeMillis
              println(nameSet)
              //exit(0);
            }

            true
        })
      println(">>>>>>>>>>>>>>>>>>")
      println(nameSet)
   }
}
   /**
    * Reports statistics on Australian Museums Spider and Mammal collections
    */

object AMMammalSpiderReport{

  val occurrenceDAO = Config.getInstance(classOf[OccurrenceDAO]).asInstanceOf[OccurrenceDAO]
  val persistenceManager = Config.getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]

  def main(args: Array[String]): Unit = {
      val start = System.currentTimeMillis
      var startTime = System.currentTimeMillis
      var finishTime = System.currentTimeMillis
      var mamDelCount =0
      var spiDelCount =0
      var mammalCount=0
      var spiderCount =0
      var counter =0
      persistenceManager.pageOverSelect("occ", (guid, map)=> {
          counter += 1

          //check to see if the criteria in the MAP
          val instUid = map.get("institutionUid.p").getOrElse(null)
          val collectionCode = map.get("collectionCode").getOrElse(null)
          val deleted = map.get("deleted").getOrElse(null)

          if(instUid != null && instUid.equals("in4") && collectionCode != null && (collectionCode=="Mammalogy" || collectionCode=="Arachnology")){
            if(collectionCode == "Mammalogy"){
              mammalCount+=1
              if(deleted == "true")
                mamDelCount+=1
            }
            else if(collectionCode == "Arachnology"){
              spiderCount+=1
              if(deleted == "true")
                spiDelCount+=1
            }
          }

          if (counter % 1000 == 0) {
            finishTime = System.currentTimeMillis
            println(counter + " >> Last key : " + guid + ",("+map+")  spider count: " + spiderCount + "("+spiDelCount+") mammal count "+mammalCount + "("+mamDelCount+") records per sec: " + 1000f / (((finishTime - startTime).toFloat) / 1000f))
            startTime = System.currentTimeMillis

          }

          true
        },"", 1000, "institutionUid.p","collectionCode", "deleted")

      finishTime = System.currentTimeMillis
      println("Total indexing time " + ((finishTime-start).toFloat)/1000f + " seconds")
      println(counter + " >>   spider count: " + spiderCount + "("+spiDelCount+") mammal count "+mammalCount + "("+mamDelCount+") records per sec: " + 1000f / (((finishTime - startTime).toFloat) / 1000f))
  }
}




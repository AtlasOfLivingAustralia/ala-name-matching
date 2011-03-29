/*
 * Some Reports that were necessary to provide used DwC fields and evaulate
 * possible bugs...
 *
 */

package au.org.ala.report

import au.org.ala.biocache.DAO

object Report {
   def main(args: Array[String]): Unit = {
      val start = System.currentTimeMillis
        var startTime = System.currentTimeMillis
        var finishTime = System.currentTimeMillis
        var counter=0
        val nameSet :scala.collection.mutable.HashSet[String] = new scala.collection.mutable.HashSet[String]
            DAO.persistentManager.pageOverAll("occ", (guid, map)=> {
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
  def main(args: Array[String]): Unit = {
  val start = System.currentTimeMillis
  var startTime = System.currentTimeMillis
  var finishTime = System.currentTimeMillis
  var mamDelCount =0
  var spiDelCount =0
  var mammalCount=0
  var spiderCount =0
  var counter =0
  DAO.persistentManager.pageOverSelect("occ", (guid, map)=> {
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
    }, 1000, "institutionUid.p","collectionCode", "deleted")

  finishTime = System.currentTimeMillis
  println("Total indexing time " + ((finishTime-start).toFloat)/1000f + " seconds")
  println(counter + " >>   spider count: " + spiderCount + "("+spiDelCount+") mammal count "+mammalCount + "("+mamDelCount+") records per sec: " + 1000f / (((finishTime - startTime).toFloat) / 1000f))
  }
}



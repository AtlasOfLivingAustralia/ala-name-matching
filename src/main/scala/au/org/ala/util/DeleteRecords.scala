/*
 * Mark's records for deletion.
 *
 * TODO Make more generic...
 */

package au.org.ala.util

import au.org.ala.biocache._

object DeleteRecords {

  val occurrenceDAO = Config.getInstance(classOf[OccurrenceDAO]).asInstanceOf[OccurrenceDAO]
  val persistenceManager = Config.getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]

  def main(args: Array[String]): Unit = {
    println("Starting to delete based on the criteria: institutionUid=in4 and catalogNUm doesn't contain ecatalogue")

    //TO DO generic criteria to delete records...
    //maybe specify list of collection uids, institution, etc
    // a file that contains a lits of record source uids
    var counter = 0
    var delCount =0
    val start = System.currentTimeMillis
    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis

    persistenceManager.pageOverSelect("occ", (guid, map)=> {
        counter += 1

        //check to see if the criteria in the MAP
        val instUid = map.get("institutionUid.p").getOrElse(null)
        val catalogNum = map.get("catalogNumber").getOrElse(null)
        if(instUid != null && instUid.equals("in4") && catalogNum != null && !catalogNum.contains("ecatalogue")){
          //println("Need to delete : " + guid)
          delCount+=1
          occurrenceDAO.setUuidDeleted(guid, true)
        }

        if (counter % 1000 == 0) {
          finishTime = System.currentTimeMillis
          println(counter + " >> Last key : " + guid + ",("+map+")  delete count: " + delCount +"records per sec: " + 1000f / (((finishTime - startTime).toFloat) / 1000f))
          startTime = System.currentTimeMillis
        }

        true
    },"", 1000, "institutionUid.p", "catalogNumber")

    finishTime = System.currentTimeMillis
    println("Total indexing time " + ((finishTime-start).toFloat)/1000f + " seconds")
  }
}
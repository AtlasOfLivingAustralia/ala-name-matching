package au.org.ala.util

import au.org.ala.biocache._

object ProcessSinglePhase {

  def main(args:Array[String]){

    val t = new au.org.ala.biocache.TypeStatusProcessor
    var counter = 0
    au.org.ala.biocache.Config.occurrenceDAO.pageOverRawProcessed(recordOption => {

      counter += 1
      if(counter % 1000 ==0) println(counter)

      recordOption match {
        case Some(r) => {
          val (raw, processed) = r
          if(raw.identification.typeStatus != null){
            println(raw.rowKey)
            t.process(raw.rowKey, raw, processed)
            au.org.ala.biocache.Config.occurrenceDAO.updateOccurrence(raw.rowKey, processed.identification, Versions.PROCESSED)
          }
          true
        }
        case None => true
      }
    })
    println("finished")
    au.org.ala.biocache.Config.persistenceManager.shutdown
  }
}
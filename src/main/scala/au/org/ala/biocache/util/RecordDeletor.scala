package au.org.ala.biocache.util

import au.org.ala.biocache.Config

/**
 * Created by mar759 on 17/02/2014.
 */
trait RecordDeletor {
    val pm = Config.persistenceManager
    val indexer = Config.indexDAO
    val occurrenceDAO = Config.occurrenceDAO
    def deleteFromPersistent
    def deleteFromIndex
    def close {
      pm.shutdown
      indexer.shutdown
    }
}

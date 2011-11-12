package au.org.ala.util

import au.org.ala.biocache._

/**
 * Utility to delete one or more columns.
 */
object DeleteColumn {

  def main(args: Array[String]): Unit = {
    if(args.length<2){
        println("Usage: entityName column1 column2 column3 column4")
        exit(1)
    }
    
    val entityName = args(0)
    val columnsToDelete = args.tail
    printf("Deleting from entity %s, columns: %s. Hit return to proceed...", entityName, entityName.mkString(", "))
    val line = readLine
    println("proceeding.")
    
    Config.persistenceManager.pageOverSelect(entityName, (guid, map)=> {
        map.keys.foreach( key => {
            if(!map.get(key).isEmpty){
            	Config.persistenceManager.deleteColumns(guid, entityName, key)
            }
        })
        true
    }, "","", 1000, args.tail:_*)
    
    Config.persistenceManager.shutdown
    println("Complete.")
  }
}

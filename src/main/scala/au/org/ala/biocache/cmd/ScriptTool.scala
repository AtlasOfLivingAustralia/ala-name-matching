package au.org.ala.biocache.cmd

import au.org.ala.biocache.Config
import au.org.ala.biocache
import au.org.ala.biocache.index.IndexRecords
import java.io.{BufferedReader, InputStreamReader}

/**
 * Tool for running a script against the biocache.
 */
object ScriptTool {

  def main(args: Array[String]) {
    val isReader = new InputStreamReader(System.in)
    val bufReader = new BufferedReader(isReader)
    try {
      var inputStr = bufReader.readLine()
      while (inputStr != null) {
        println("Executing command '" + inputStr + "'")
        if (inputStr.trim.length > 0) CMD.executeCommand(inputStr,true)
        inputStr = bufReader.readLine()
      }
      Config.persistenceManager.shutdown
      IndexRecords.indexer.shutdown
      println("Script complete.\n")
      System.exit(0) //need a successful exit
    } catch {
      case e: Exception => {
        biocache.Config.persistenceManager.shutdown
        IndexRecords.indexer.shutdown
        System.exit(1)
      }
    }
  }
}

package au.org.ala.biocache.cmd

import au.org.ala.biocache.Config
import au.org.ala.biocache.index.IndexRecords

/**
 * Command line tool that allows administrators to run commands on
 * the biocache. Any additional tools should be incorporated into this executable.
 */
object CommandLineTool {

  def main(args: Array[String]) {

    if (args.isEmpty) {
      println("----------------------------")
      println("| Biocache management tool |")
      println("----------------------------")
      print("\nPlease supply a command or hit ENTER to view command list. \nbiocache> ")

      var input = readLine
      while (input != "exit" && input != "q" && input != "quit") {
        CMD.executeCommand(input)
        print("\nbiocache> ")
        input = readLine
      }
    } else {
      CMD.executeCommand(args.mkString(" "))
    }
    //close down the data store and index so the program can exit normally
    Config.persistenceManager.shutdown
    IndexRecords.indexer.shutdown
    println("Bye.\n")
  }
}

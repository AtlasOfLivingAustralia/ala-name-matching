package au.org.ala.util

object CommandLineTool {

    def main(args: Array[String]) {

        println("Welcome to the biocache management tool.")
        print("Please supply a command or hit ENTER to view command list:")
        var input = readLine

        val l = new Loader
        while (input != "exit" && input != "q" && input != "quit") {

            try {
                input.toLowerCase.trim match {
                    case it if (it startsWith "describe ") || (it startsWith "d ") => l.describeResource(it.split(" ").map(x => x.trim).toList.tail)
                    case it if (it startsWith "list") || (it == "l") => l.printResourceList
                    case it if (it startsWith "load") || (it startsWith "ld") => l.load(it.split(" ").map(x => x.trim).toList.last)
                    case it if (it startsWith "process") || (it startsWith "process") => {
                        val drs = it.split(" ").map(x => x.trim).toList.tail
                        for(dr <- drs){
                        	ProcessWithActors.processRecords(4, None, Some(dr))
                        }
                    }
                    case it if (it startsWith "index") || (it startsWith "index") => {
                        val drs = it.split(" ").map(x => x.trim).toList.tail
                        for(dr <- drs){
                        	IndexRecords.index(None, Some(dr), false, false)
                        }
                    }
                    case it if (it startsWith "createdwc")=> {
                        val args = it.split(" ").map(x => x.trim).toArray.tail
                        DwCACreator.main(args)
                    }
                    case it if (it startsWith "optimise") => {
                       	IndexRecords.indexer.optimise
                    }
                    case it if (it startsWith "healthcheck") => l.healthcheck
                    case it if (it startsWith "export") => {
                        val args = it.split(" ").map(x => x.trim).toArray.tail
                        ExportUtil.main(args)
                    }
                    case it if (it startsWith "import") => {
                        val args = it.split(" ").map(x => x.trim).toArray.tail
                        ImportUtil.main(args)
                    }
                    case _ => printHelp
                }
            } catch {
                case e: Exception => e.printStackTrace
            }
            print("\nPlease supply a command or hit ENTER to view command list:")
            input = readLine
        }
        //close down the data store and index so the program can exit normally
        au.org.ala.biocache.Config.persistenceManager.shutdown
        IndexRecords.indexer.shutdown
        println("Goodbye\n")
    }

    def printHelp = {
        println("1)  list - print list of resources")
        println("2)  describe <dr-uid> <dr-uid1> <dr-uid2>... - print list of resources")
        println("3)  load <dr-uid> - load resource")
        println("4)  process <dr-uid> - process resource")
        println("5)  index <dr-uid> - index resource")
        println("6)  createdwc <dr-uid> <export directory>")
        println("7)  healthcheck")
        println("8)  export")
        println("9)  import")
        println("10)  optimise")
        println("11)  exit")
    }

    def printTable(table: List[Map[String, String]]) {

        val keys = table(0).keys.toList
        val valueLengths = keys.map(k => { (k, table.map(x => x(k).length).max) }).toMap[String, Int]
        val columns = table(0).keys.map(k => {
            if (k.length < valueLengths(k)) {
                k + (List.fill[String](valueLengths(k) - k.length)(" ").mkString)
            } else {
                k
            }
        }).mkString(" | ", " | ", " |")

        val sep = " " + List.fill[String](columns.length - 1)("-").mkString
        println(sep)
        println(columns)
        println(" |" + List.fill[String](columns.length - 3)("-").mkString + "|")

        table.foreach(dr => {
            println(dr.map(kv => {
                if (kv._2.length < valueLengths(kv._1)) {
                    kv._2 + (List.fill[String](valueLengths(kv._1) - kv._2.length)(" ").mkString)
                } else {
                    kv._2
                }
            }).mkString(" | ", " | ", " |"))
        })

        println(" " + List.fill[String](columns.length - 1)("-").mkString)
    }
}
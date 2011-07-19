package au.org.ala.util

object CommandLineTool {

  def main(args: Array[String]): Unit = {  
      
      //need a mechanism to delegate to other tools
      
      var tool:Option[String] = None
      
      val parser = new OptionParser("biocache") {
         opt("t", "Select the tool to use", {v: String => tool = Some(v)})
      }
      
      if(parser.parse(args)){
    	  println("""=================================""")
    	  println("""=== biocache commandline tool """)
    	  println("""=================================""")
    	  println("""1) harvest resource """)
    	  println("""2) process resource """)
    	  println("""3) index resource / all / recently modified""")
      }
  }

  def printTable(table:List[Map[String,String]]){
      
      val keys = table(0).keys.toList
      val valueLengths = keys.map(k => { (k, table.map(x => x(k).length).max) }).toMap[String, Int]
      
      
      val columns = table(0).keys.map(k => {
              if(k.length < valueLengths(k)){
                  k + (List.fill[String](valueLengths(k) - k.length)(" ").mkString)
              } else {
                  k
          	  }
      }).mkString(" | ", " | ", " |")
      
      val sep = " " + List.fill[String](columns.length-1)("-").mkString
      println(sep)
      println(columns)
      println(" |" + List.fill[String](columns.length-3)("-").mkString+"|")
      
      
      table.foreach(dr => {
          println(dr.map(kv => {
              if(kv._2.length < valueLengths(kv._1)){
                  kv._2 + (List.fill[String](valueLengths(kv._1) - kv._2.length)(" ").mkString)
              } else {
                  kv._2
          	  }
          }).mkString(" | ", " | ", " |"))
      })
      
      println(" " + List.fill[String](columns.length-1)("-").mkString)
  }
}
package au.org.ala.biocache.export

import org.apache.lucene.index.IndexReader
import org.apache.lucene.store.FSDirectory
import java.io.{FileWriter, File}
import au.org.ala.biocache.util.OptionParser

object ExportForOutliers {

  def main(args:Array[String]){

    var indexDirectory = ""    
    var exportDirectory = ""

    val parser = new OptionParser("Test for outliers") {
      arg("indexDirectory", "The Lucene/SOLR index to export from e.g. /data/solr/biocache/data/index", {v:String => indexDirectory = v})
      arg("exportDirectory", "The directory to export to", {v:String => exportDirectory = v})
    }
    if(parser.parse(args)){    
    	runExtract(indexDirectory,exportDirectory)
    }
  }
  
  def runExtract(indexDir:String, exportDir:String, separator:Char = '\t'){
    println("Running extract....")
    
    val indexReader = IndexReader.open(FSDirectory.open(new File(indexDir)))
    
    val spWriter = new FileWriter(new File(exportDir + File.separator + "species-unsorted.txt"))
    val sbpWriter = new FileWriter(new File(exportDir + File.separator + "subspecies-unsorted.txt"))
    
    var counter = 0
    val maxDocId = indexReader.maxDoc()
    println("Number of documents...." + maxDocId)    

    while(counter < maxDocId){
      val doc = indexReader.document(counter)
      if(!doc.getValues("species_guid").isEmpty
        && !doc.getValues("latitude").isEmpty
        && !doc.getValues("el882").isEmpty
        && !doc.getValues("rank").isEmpty){

        val rank = doc.getValues("rank").head
        if (rank == "species" || rank == "subspecies"){

          spWriter.write(doc.getValues("species_guid").head
            +separator+ doc.getValues("id").head
            +separator+ doc.getValues("latitude").head
            +separator+ doc.getValues("longitude").head
            +separator+ doc.getValues("el882").head
            +separator+ doc.getValues("el889").head
            +separator+ doc.getValues("el887").head
            +separator+ doc.getValues("el865").head
            +separator+ doc.getValues("el894").head
            +"\n"
          )
        }
        
        if (rank == "subspecies" && !doc.getValues("subspecies_guid").isEmpty){
          sbpWriter.write(doc.getValues("subspecies_guid").head
            +separator+ doc.getValues("id").head
            +separator+ doc.getValues("latitude").head
            +separator+ doc.getValues("longitude").head
            +separator+ doc.getValues("el882").head
            +separator+ doc.getValues("el889").head
            +separator+ doc.getValues("el887").head
            +separator+ doc.getValues("el865").head
            +separator+ doc.getValues("el894").head
            +"\n"
          )
        }
      }
      sbpWriter.flush
      spWriter.flush
      counter += 1
    }
    indexReader.close
    sbpWriter.flush
    spWriter.flush
    sbpWriter.close
    spWriter.close
    
    println("Extract complete. Files located in " + exportDir)
  }
}

object ExportSpecies {

  def main(args:Array[String]){

    val indexReader = IndexReader.open(FSDirectory.open(new File(args.head)))
    var counter = 0
    val maxDocId = indexReader.maxDoc()
    val separator = '\t'

    while(counter < maxDocId){
      val doc = indexReader.document(counter)
      if(!doc.getValues("species_guid").isEmpty
        && !doc.getValues("latitude").isEmpty
        && !doc.getValues("el882").isEmpty
        && !doc.getValues("rank").isEmpty){

        val rank = doc.getValues("rank").head
        if (rank == "species" || rank == "subspecies"){
        
          println(doc.getValues("species_guid").head
            +separator+ doc.getValues("id").head
            +separator+ doc.getValues("latitude").head
            +separator+ doc.getValues("longitude").head
            +separator+ doc.getValues("el882").head
            +separator+ doc.getValues("el889").head
            +separator+ doc.getValues("el887").head
            +separator+ doc.getValues("el865").head
            +separator+ doc.getValues("el894").head
          )
        }
      }
      counter += 1
    }
    indexReader.close
  }
}

object ExportSubspecies {

  def main(args:Array[String]){

    val indexReader = IndexReader.open(FSDirectory.open(new File(args.head)))
    var counter = 0
    val maxDocId = indexReader.maxDoc()
    val separator = '\t'

    while(counter < maxDocId){
      val doc = indexReader.document(counter)
      if(!doc.getValues("subspecies_guid").isEmpty
        && !doc.getValues("latitude").isEmpty
        && !doc.getValues("el882").isEmpty
        && !doc.getValues("rank").isEmpty){

        val rank = doc.getValues("rank").head
        if (rank == "subspecies"){

          println(doc.getValues("subspecies_guid").head
            +separator+ doc.getValues("id").head
            +separator+ doc.getValues("latitude").head
            +separator+ doc.getValues("longitude").head
            +separator+ doc.getValues("el882").head
            +separator+ doc.getValues("el889").head
            +separator+ doc.getValues("el887").head
            +separator+ doc.getValues("el865").head
            +separator+ doc.getValues("el894").head
          )
        }
      }
      counter += 1
    }
    indexReader.close
  }
}

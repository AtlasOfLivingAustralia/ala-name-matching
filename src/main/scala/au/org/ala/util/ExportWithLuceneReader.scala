package au.org.ala.util

import org.apache.lucene.index.IndexReader
import org.apache.lucene.store.FSDirectory
import java.io.{FileWriter, File}

object ExportForOutliers {

  def main(args:Array[String]){

    val indexReader = IndexReader.open(FSDirectory.open(new File(args(0))))
    
    val spWriter = new FileWriter(new File(args(1)+File.separator+"species-unsorted.txt"))
    val sbpWriter = new FileWriter(new File(args(1)+File.separator+"subspecies-unsorted.txt"))
    
    var counter = 0
    val maxDocId = indexReader.maxDoc()

    while(counter < maxDocId){
      val doc = indexReader.document(counter)
      if(!doc.getValues("species_guid").isEmpty
        && !doc.getValues("latitude").isEmpty
        && !doc.getValues("el882").isEmpty
        && !doc.getValues("rank").isEmpty){

        val rank = doc.getValues("rank").head
        if (rank == "species" || rank == "subspecies"){

          spWriter.write(doc.getValues("species_guid").head
            +"\t"+ doc.getValues("id").head
            +"\t"+ doc.getValues("latitude").head
            +"\t"+ doc.getValues("longitude").head
            +"\t"+ doc.getValues("el882").head
            +"\t"+ doc.getValues("el889").head
            +"\t"+ doc.getValues("el887").head
            +"\t"+ doc.getValues("el865").head
            +"\t"+ doc.getValues("el894").head
            +"\n"
          )
        }
        
        if (rank == "subspecies" && !doc.getValues("subspecies_guid").isEmpty){
          sbpWriter.write(doc.getValues("subspecies_guid").head
            +"\t"+ doc.getValues("id").head
            +"\t"+ doc.getValues("latitude").head
            +"\t"+ doc.getValues("longitude").head
            +"\t"+ doc.getValues("el882").head
            +"\t"+ doc.getValues("el889").head
            +"\t"+ doc.getValues("el887").head
            +"\t"+ doc.getValues("el865").head
            +"\t"+ doc.getValues("el894").head
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
  }
}


object ExportSpecies {

  def main(args:Array[String]){

    val indexReader = IndexReader.open(FSDirectory.open(new File(args.head)))
    var counter = 0
    val maxDocId = indexReader.maxDoc()

    while(counter < maxDocId){
      val doc = indexReader.document(counter)
      if(!doc.getValues("species_guid").isEmpty
        && !doc.getValues("latitude").isEmpty
        && !doc.getValues("el882").isEmpty
        && !doc.getValues("rank").isEmpty){

        val rank = doc.getValues("rank").head
        if (rank == "species" || rank == "subspecies"){
        
          println(doc.getValues("species_guid").head
            +"\t"+ doc.getValues("id").head
            +"\t"+ doc.getValues("latitude").head
            +"\t"+ doc.getValues("longitude").head
            +"\t"+ doc.getValues("el882").head
            +"\t"+ doc.getValues("el889").head
            +"\t"+ doc.getValues("el887").head
            +"\t"+ doc.getValues("el865").head
            +"\t"+ doc.getValues("el894").head
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

    while(counter < maxDocId){
      val doc = indexReader.document(counter)
      if(!doc.getValues("subspecies_guid").isEmpty
        && !doc.getValues("latitude").isEmpty
        && !doc.getValues("el882").isEmpty
        && !doc.getValues("rank").isEmpty){

        val rank = doc.getValues("rank").head
        if (rank == "subspecies"){

          println(doc.getValues("subspecies_guid").head
            +"\t"+ doc.getValues("id").head
            +"\t"+ doc.getValues("latitude").head
            +"\t"+ doc.getValues("longitude").head
            +"\t"+ doc.getValues("el882").head
            +"\t"+ doc.getValues("el889").head
            +"\t"+ doc.getValues("el887").head
            +"\t"+ doc.getValues("el865").head
            +"\t"+ doc.getValues("el894").head
          )
        }
      }
      counter += 1
    }
    indexReader.close
  }
}

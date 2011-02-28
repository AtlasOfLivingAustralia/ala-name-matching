package au.org.ala.biocache

import org.scalatest.FunSuite
import au.org.ala.util.ProcessRecords
import au.org.ala.data.model.LinnaeanRankClassification

class TaxonomicHintsTest extends FunSuite {


    test("parse hints"){
       val hintMap = ProcessRecords.parseHints(List("phylum:A", "phylum:B", "class:N"))
       expect(2){hintMap.get("phylum").get.size}
       expect(1){hintMap.get("class").get.size}
       expect(true){hintMap.get("phylum").get.contains("B".toLowerCase)}
    }

    test("happy days - no conflicts"){
       val hintMap = ProcessRecords.parseHints(List("phylum:A", "kingdom:K", "phylum:B", "kingdom:L", "class:N"))
       val cln = new LinnaeanRankClassification("L", "bus")
       expect(true){ProcessRecords.isMatchValid(cln,hintMap)._2}
    }

    test("Phylum mismatch"){
       // (String kingdom, String phylum, String klass, String order, String family, String genus, String scientificName)

       val hintMap = ProcessRecords.parseHints(List("phylum:A", "kingdom:K", "phylum:B", "kingdom:L", "class:N"))
       val cln = new LinnaeanRankClassification("L", "C", null, null, "bus",null,null)
       expect(false){ProcessRecords.isMatchValid(cln,hintMap)._2}
    }

    test("CLass mismatch"){
       val hintMap = ProcessRecords.parseHints(List("class:B", "phylum:annelida", "phylum:arthropoda"))
       val cln = new LinnaeanRankClassification("L", null, "A", null, "bus",null,null)
       expect(false){ProcessRecords.isMatchValid(cln,hintMap)._2}
    }

    test("Arthropoda"){
//        Taxonhints: List(phylum:annelida, phylum:arthropoda)
  //      Classification: au.org.ala.data.model.LinnaeanRankClassification@61557a77[kingdom=Animalia,phylum=Arthropoda,klass=Insecta,order=Coleoptera,family=Chrysomelidae,genus=Elaphodes,species=Elaphodes signifer,specificEpithet=<null>,subspecies=<null>,infraspecificEpithet=<null>,scientificName=Elaphodes signifer]
       val hintMap = ProcessRecords.parseHints(List("phylum:annelida", "phylum:arthropoda"))
       val cln = new LinnaeanRankClassification("Animalia","Arthropoda","Insecta","Coleoptera","Chrysomelidae","Elaphodes","Elaphodes signifer")
       expect(true){ProcessRecords.isMatchValid(cln,hintMap)._2}
    }

}
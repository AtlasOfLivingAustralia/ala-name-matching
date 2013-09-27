package au.org.ala.biocache

import org.scalatest.FunSuite
import au.org.ala.data.model.LinnaeanRankClassification
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.junit.Ignore

@RunWith(classOf[JUnitRunner])
class TaxonomicHintsTest extends ConfigFunSuite {

    test("parse hints"){
       val hintMap = (new ClassificationProcessor).parseHints(List("phylum:A", "phylum:B", "class:N"))
       expectResult(2){hintMap.get("phylum").get.size}
       expectResult(1){hintMap.get("class").get.size}
       expectResult(true){hintMap.get("phylum").get.contains("B".toLowerCase)}
    }

    test("happy days - no conflicts"){
       val hintMap =  (new ClassificationProcessor).parseHints(List("phylum:A", "kingdom:K", "phylum:B", "kingdom:L", "class:N"))
       val cln = new LinnaeanRankClassification("L", "bus")
       expectResult(true){(new ClassificationProcessor).isMatchValid(cln,hintMap)._1}
    }

    test("Phylum mismatch"){
       val hintMap =  (new ClassificationProcessor).parseHints(List("phylum:A", "kingdom:K", "phylum:B", "kingdom:L", "class:N"))
       val cln = new LinnaeanRankClassification("L", "C", null, null, "bus",null,null)
       expectResult(false){(new ClassificationProcessor).isMatchValid(cln,hintMap)._1}
    }

    test("Class mismatch"){
       val hintMap =  (new ClassificationProcessor).parseHints(List("class:B", "phylum:annelida", "phylum:arthropoda"))
       val cln = new LinnaeanRankClassification("L", null, "A", null, "bus",null,null)
       expectResult(false){(new ClassificationProcessor).isMatchValid(cln,hintMap)._1}
    }

    test("Arthropoda"){
       val hintMap =  (new ClassificationProcessor).parseHints(List("phylum:annelida", "phylum:arthropoda"))
       val cln = new LinnaeanRankClassification("Animalia","Arthropoda","Insecta","Coleoptera","Chrysomelidae","Elaphodes","Elaphodes signifer")
       expectResult(true){(new ClassificationProcessor).isMatchValid(cln,hintMap)._1}
    }
}
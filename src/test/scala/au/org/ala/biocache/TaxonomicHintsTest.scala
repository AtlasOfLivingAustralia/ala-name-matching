package au.org.ala.biocache

import org.scalatest.FunSuite
import au.org.ala.data.model.LinnaeanRankClassification
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class TaxonomicHintsTest extends FunSuite {

    test("parse hints"){
       val hintMap = ClassificationProcessor.parseHints(List("phylum:A", "phylum:B", "class:N"))
       expect(2){hintMap.get("phylum").get.size}
       expect(1){hintMap.get("class").get.size}
       expect(true){hintMap.get("phylum").get.contains("B".toLowerCase)}
    }

    test("happy days - no conflicts"){
       val hintMap = ClassificationProcessor.parseHints(List("phylum:A", "kingdom:K", "phylum:B", "kingdom:L", "class:N"))
       val cln = new LinnaeanRankClassification("L", "bus")
       expect(true){ClassificationProcessor.isMatchValid(cln,hintMap)._1}
    }

    test("Phylum mismatch"){
       val hintMap = ClassificationProcessor.parseHints(List("phylum:A", "kingdom:K", "phylum:B", "kingdom:L", "class:N"))
       val cln = new LinnaeanRankClassification("L", "C", null, null, "bus",null,null)
       expect(false){ClassificationProcessor.isMatchValid(cln,hintMap)._1}
    }

    test("Class mismatch"){
       val hintMap = ClassificationProcessor.parseHints(List("class:B", "phylum:annelida", "phylum:arthropoda"))
       val cln = new LinnaeanRankClassification("L", null, "A", null, "bus",null,null)
       expect(false){ClassificationProcessor.isMatchValid(cln,hintMap)._1}
    }

    test("Arthropoda"){
       val hintMap = ClassificationProcessor.parseHints(List("phylum:annelida", "phylum:arthropoda"))
       val cln = new LinnaeanRankClassification("Animalia","Arthropoda","Insecta","Coleoptera","Chrysomelidae","Elaphodes","Elaphodes signifer")
       expect(true){ClassificationProcessor.isMatchValid(cln,hintMap)._1}
    }
}
package au.org.ala.biocache

import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class TaxonomicNameTest extends ConfigFunSuite {

    
    test("recursive issue"){
      val raw = new FullRecord
      val processed = new FullRecord
      raw.classification.scientificName="Pseudosuberia genthi"
      raw.classification.phylum="Cnidaria"
      raw.classification.kingdom="Animalia"
      raw.classification.genus ="Pseudosuberia"
      raw.classification.family="Briareidae"
      val qas = (new ClassificationProcessor).process("test", raw, processed)
      println(processed.classification.scientificName)
      
    }
  
//    test("name not recognised"){
//        val raw = new FullRecord
//        var processed = new FullRecord
//        raw.classification.scientificName = "dummy name"
//        val qas = (new ClassificationProcessor).process("test", raw, processed)
//        expect(10004){qas(0).code}
//    }
//    
//    test("Parse type"){
//      val raw = new FullRecord
//      val processed = new FullRecord
//      raw.classification.scientificName ="Zabidius novemaculeatus"
//      (new ClassificationProcessor).process("test",raw,processed)
//      expect("wellformed"){processed.classification.nameParseType}
//    }
//    
//    test("name not in national checklists"){
//        val raw = new FullRecord
//        var processed = new FullRecord
//        raw.classification.scientificName = "Amanita farinacea"
//        var qas = (new ClassificationProcessor).process("test", raw, processed)
//        expect(true){qas.isEmpty}
//        
//        raw.classification.scientificName = "Acridotheres tristis"
//        qas = (new ClassificationProcessor).process("test", raw, processed)
//        expect(10005){qas(0).code}
//    }
//    
//    test("homonym issue"){
//        val raw = new FullRecord
//        var processed = new FullRecord
//        raw.classification.genus = "Macropus";
//        raw.classification.scientificName = "Macropus ?";
//        val qas = (new ClassificationProcessor).process("test", raw, processed);
//        println(processed.classification.taxonConceptID)
//        expect(true){processed.classification.getTaxonomicIssue().contains("homonym")}
//        expect(true){processed.classification.getTaxonomicIssue().contains("questionSpecies")}
////        expect(10006){qas(0).code}
//    }
//    
//    test("missing accepted name"){
//      val raw = new FullRecord
//      var processed = new FullRecord
//      raw.classification.scientificName="Gnaphalium collinum"
//      (new ClassificationProcessor).process("test", raw, processed)
//      expect(null){processed.classification.scientificName}
//      //raw.classification.genus = "Gnaphalium"
//      raw.classification.family ="Asteraceae"
//      (new ClassificationProcessor).process("test", raw, processed)
//      expect("Asteraceae"){processed.classification.scientificName}
//    }
}
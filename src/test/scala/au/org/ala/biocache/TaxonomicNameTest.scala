package au.org.ala.biocache

import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class TaxonomicNameTest extends ConfigFunSuite {

    test("name not recognised"){
        val raw = new FullRecord
        var processed = new FullRecord
        raw.classification.scientificName = "dummy name"
        val qas = (new ClassificationProcessor).process("test", raw, processed)
        expect(10004){qas(0).code}
    }
    
    test("name not in national checklists"){
        val raw = new FullRecord
        var processed = new FullRecord
        raw.classification.scientificName = "Amanita farinacea"
        var qas = (new ClassificationProcessor).process("test", raw, processed)
        expect(true){qas.isEmpty}
        
        raw.classification.scientificName = "Camponotus reticulatus fullawayi"
        qas = (new ClassificationProcessor).process("test", raw, processed)
        expect(10005){qas(0).code}
    }
    
    test("homonym issue"){
        val raw = new FullRecord
        var processed = new FullRecord
        raw.classification.genus = "Heteropoda";
        raw.classification.scientificName = "Heteropoda";
        val qas = (new ClassificationProcessor).process("test", raw, processed);
        println(processed.classification.taxonConceptID)
        expect(10006){qas(0).code}
    }
}
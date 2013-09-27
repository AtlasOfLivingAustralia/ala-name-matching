package au.org.ala.biocache

import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class MiscTest extends ConfigFunSuite {

    test("missing basis of record"){
        val raw = new FullRecord
        var processed = new FullRecord
        val qas = (new BasisOfRecordProcessor).process("test", raw, processed)
        expectResult(0){qas.find(_.code == 20001).get.qaStatus}
    }
    
    test("badly formed basis of record"){
        val raw = new FullRecord
        var processed = new FullRecord
        raw.occurrence.basisOfRecord = "dummy"
        val qas = (new BasisOfRecordProcessor).process("test", raw, processed)
        expectResult(0){qas.find(_.code == 20002).get.qaStatus}
    }
    
    test("unrecognised type status"){
        val raw = new FullRecord
        var processed = new FullRecord
        raw.identification.typeStatus = "dummy"
        val qas = (new TypeStatusProcessor).process("test", raw, processed)
        expectResult(0){qas.find(_.code == 20004).get.qaStatus}
    }
    
    test("unrecognised collection code"){
        val raw = new FullRecord
        var processed = new FullRecord
        raw.attribution.dataResourceUid = "dr368"
        raw.occurrence.collectionCode = "dummy"
        raw.occurrence.institutionCode = "dummy"
        var qas = (new AttributionProcessor).process("test", raw, processed)
        expectResult(0){qas.find(_.code == 20005).get.qaStatus}
    }
    
    test("invalid image url"){
        val raw = new FullRecord
        var processed = new FullRecord
        raw.occurrence.associatedMedia = "invalidimageurl.ppp"
        var qas = (new MiscellaneousProcessor).process("test", raw, processed)
        expectResult(0){qas.find(_.code == 20007).get.qaStatus}
    }

    test("is valid image url"){
      val url ="/data/biocache-media/dr344/12874/2ec04a49-7e44-4a0b-8351-5d37155e3ef7/o29275a.jpg"
      expectResult(true){(MediaStore.isValidImageURL(url))}
      //"""^(https?://(?:[a-zA-Z0-9\-]+(\.)+([a-zA-Z]{2,6})?(?:/[^/#]+)+\.?(?:jpg|gif|png|jpeg))$"""
      var url2 ="http://74.50.62.163/images/display/biodiversity/vertebrates/id_images/web-birds/o29275a.jpg"
      expectResult(true){(MediaStore.isValidImageURL(url2))}
      expectResult(true){MediaStore.isValidImageURL("https://test.ala.org.au/images/image1/img.gif")}
      expectResult(false){MediaStore.isValidImageURL("https://test.ala.org.au/images/image1/img.bmp")}
      expectResult(false){MediaStore.isValidImageURL("http://tests.ala.org.au/images/image1/img")}
    }
    
    test("invalid collection date"){
        val raw = new FullRecord
        var processed = new FullRecord
        raw.event.year = "2010"
        raw.event.month = "11"
        raw.event.day = "22"
        var qas = (new EventProcessor).process("test", raw, processed)
        expectResult(1){qas.find(_.code == 30007).get.qaStatus}
        
        //future dated - make sure that it far intot the future so that the test will always pass
        raw.event.year = "2212"
        raw.event.month = "11"
        raw.event.day = "22"
        qas = (new EventProcessor).process("test", raw, processed)
        expectResult(false){qas.isEmpty}
        expectResult(0){qas.find(_.code == 30007).get.qaStatus}
        
        raw.event.year = "2011"
        raw.event.month = "13"
        raw.event.day = "22"
        qas = (new EventProcessor).process("test", raw, processed)
        expectResult(0){qas.find(_.code == 30007).get.qaStatus}
        
        raw.event.year = "2011"
        raw.event.month = "11"
        raw.event.day = "33"
        qas = (new EventProcessor).process("test", raw, processed)
        expectResult(0){qas.find(_.code == 30007).get.qaStatus}
        
        raw.event.year = "2011"
        raw.event.month = "11"
        raw.event.day = "31"
        qas = (new EventProcessor).process("test", raw, processed)
        expectResult(0){qas.find(_.code == 30007).get.qaStatus}
        
        //First Fleet date test
        raw.event.year="1788"
        raw.event.month="1"
        raw.event.day="26"
        qas = (new EventProcessor).process("test", raw, processed)
        expectResult(0){qas.find(_.code == 30007).get.qaStatus}
    }
    
    test("Default DwC Values"){
        val raw = new FullRecord
        val processed = new FullRecord
        raw.attribution.dataResourceUid = "dr354"
        val qas = (new BasisOfRecordProcessor).process("test", raw,processed)
        expectResult(0){qas.find(_.code == 20001).get.qaStatus}
        (new DefaultValuesProcessor).process("test", raw, processed)
        expectResult("HumanObservation"){processed.occurrence.basisOfRecord}
        val qas2 = (new BasisOfRecordProcessor).process("test", raw,processed)
        //when the default value is being used no test is being performed so the BoR QA should NOT be in the results
        expectResult(None){qas2.find(_.code == 20001)}
    }
    
    test("interactions"){
        val raw = new FullRecord
        var processed = new FullRecord
        raw.occurrence.associatedTaxa = "infects:Test Species"
        (new MiscellaneousProcessor).process("test", raw, processed)
        expectResult("Infects"){processed.occurrence.interactions(0)}
    }
    test("modified"){
        val raw = new FullRecord
        val processed = new FullRecord
        raw.occurrence.modified = "2004-08-17"
        (new EventProcessor).process("test", raw, processed)
        expectResult("2004-08-17"){processed.occurrence.modified}
    }
    
    test("establishment means"){
      val raw = new FullRecord
      var processed = new FullRecord
      raw.occurrence.establishmentMeans ="not cultivated; not native"
      val mp = new MiscellaneousProcessor 
      mp.process("test",raw, processed)
      expectResult("not cultivated; not native"){processed.occurrence.establishmentMeans}
      raw.occurrence.establishmentMeans="not cultivated; missing from vocab"
      mp.process("test", raw, processed)
      expectResult("not cultivated"){processed.occurrence.establishmentMeans}
      processed = new FullRecord
      raw.occurrence.establishmentMeans=null
      mp.process("test,",raw,processed)
      expectResult(null){processed.occurrence.establishmentMeans}
    }
    
    test("species group" ){
      println(SpeciesGroups.getStringList)
      println(SpeciesGroups.getSpeciesGroups("1768844","1781197"))
    }

    test ("taxon rank missing"){
      val raw = new FullRecord
      val processed = new FullRecord
      raw.classification.scientificName="Macropus rufus"
      var qas = (new ClassificationProcessor).process("test",raw,processed)
      expectResult(0){
        qas.find(_.getName == "missingTaxonRank").get.qaStatus
      }
      raw.classification.taxonRank="species"
      qas = (new ClassificationProcessor).process("test",raw,processed)
      expectResult(1){
        qas.find(_.getName == "missingTaxonRank").get.qaStatus
      }
    }

  test ("missing catalogue number"){
    val raw = new FullRecord
    val processed = new FullRecord
    var qas = (new MiscellaneousProcessor).process("test", raw, processed)
    expectResult(0){
      qas.find(_.getName == "missingCatalogueNumber").get.qaStatus
    }
    raw.occurrence.catalogNumber="XYZABC"
    qas = (new MiscellaneousProcessor).process("test", raw, processed)
    expectResult(1){
      qas.find(_.getName == "missingCatalogueNumber").get.qaStatus
    }
  }

  test ("name not supplied") {
    val raw= new FullRecord
    val processed = new FullRecord
    raw.classification.genus="Macropus"
    raw.classification.specificEpithet="rufus"
    var qas = (new ClassificationProcessor).process("test", raw, processed)
    expectResult(0) {
      qas.find(_.getName == "nameNotSupplied").get.qaStatus
    }
    raw.classification.vernacularName="Red Kangaroo"
    qas = (new ClassificationProcessor).process("test", raw, processed)
    expectResult(1) {
      qas.find(_.getName == "nameNotSupplied").get.qaStatus
    }
    raw.classification.vernacularName = null
    raw.classification.scientificName = "Macropus rufus"
    qas = (new ClassificationProcessor).process("test", raw, processed)
    expectResult(1) {
      qas.find(_.getName == "nameNotSupplied").get.qaStatus
    }
  }

  test ("Invalid scientificName") {
    val raw= new FullRecord
    val processed = new FullRecord
    raw.classification.scientificName = "UNKNOWN"
    var qas = (new ClassificationProcessor).process("test", raw, processed)
    expectResult(0) {
      //invalid scientific name
      qas.find(_.getName == "invalidScientificName").get.qaStatus
    }

    raw.classification.scientificName = "Macropus rufus"
    qas = (new ClassificationProcessor).process("test", raw, processed)
    expectResult(1) {
      //valid scientific name
      qas.find(_.getName == "invalidScientificName").get.qaStatus
    }
  }

  test ("Unknown Kingdom") {
    val raw = new FullRecord
    val processed = new FullRecord
    raw.classification.scientificName = "Macropus rufus"
    raw.classification.kingdom = "Animals"

    var qas = (new ClassificationProcessor).process("test", raw, processed)
    expectResult(0) {
      //Animals is an unknown kingdom
      qas.find(_.getName == "unknownKingdom").get.qaStatus
    }
    raw.classification.kingdom = "ANIMALIA"
    qas = (new ClassificationProcessor).process("test", raw, processed)
    expectResult(1) {
      //Kingdom is known
      qas.find(_.getName == "unknownKingdom").get.qaStatus
    }
  }

  test ("Supplied data generalised"){
    val raw = new FullRecord
    val processed = new FullRecord
    raw.occurrence.dataGeneralizations = "coordinates to 1dp"

    var qas = (new MiscellaneousProcessor).process("test", raw, processed)
    expectResult(0){
      qas.find(_.getName == "dataAreGeneralised").get.qaStatus
    }

    raw.occurrence.dataGeneralizations=null
    qas = (new MiscellaneousProcessor).process("test", raw, processed)
    expectResult(1){
      //data has not been generalised by the provider
      qas.find(_.getName == "dataAreGeneralised").get.qaStatus
    }
  }

  test ("occurrence cultivatd or escaped"){
    val raw = new FullRecord
    val processed = new FullRecord
    raw.occurrence.establishmentMeans="cultivated"
    var qas = (new MiscellaneousProcessor).process("test", raw, processed)
    expectResult(0){
      qas.find(_.getName == "occCultivatedEscapee").get.qaStatus
    }

    raw.occurrence.establishmentMeans="not cultivated"
    qas = (new MiscellaneousProcessor).process("test", raw, processed)
    expectResult(1){
      //represents natural occurrence
      qas.find(_.getName == "occCultivatedEscapee").get.qaStatus
    }

    raw.occurrence.establishmentMeans=null
    qas = (new MiscellaneousProcessor).process("test", raw, processed)
    expectResult(None){
      //not checked
      qas.find(_.getName == "occCultivatedEscapee")
    }
  }
    /*test("Layers Test" ){
        expectResult("ibra_merged"){Layers.idToNameMap("ibra")}
        expectResult("el790"){Layers.nameToIdMap("worldclim_bio_3")}
        expectResult("el790"){Store.getLayerId("WorldClim_bio_3")}
        expectResult("worldclim_bio_3"){Store.getLayerName("el790")}
    
    }*/
    
}
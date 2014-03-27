package au.org.ala.biocache

import org.junit.Ignore
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import com.fasterxml.jackson.annotation.JsonInclude.Include
import au.org.ala.biocache.model._
import au.org.ala.biocache.load.FullRecordMapper
import au.org.ala.biocache.vocab.AssertionCodes
import au.org.ala.biocache.util.Json

@Ignore
class DAOLayerTest extends ConfigFunSuite {
  val occurrenceDAO = Config.occurrenceDAO
  val persistenceManager = Config.persistenceManager
  val rowKey = "test-rowKey"
  val uuid = "35b3ff3-test-uuid"

  test("Kosher test") {
    persistenceManager.put("qatestkosher","occ", Map("attr.qa"-> "[]", "bor.qa" -> "[]", "default.qa" ->"[]", "duplicates.qa"->"[20014]", "event.qa"->"[]", "image.qa" ->"[10009,10010,10011,10012]", "loc.qa"->"[45,29,21,31,32,33,34,42]", "offline.q" ->"[20014]", "type.qa"->"[]"))
    val qualityAssertions = """[{"name":"missingBasisOfRecord","code":20001,"uuid":"91fc7048-0fe3-424d-946f-b250cbfb0da2","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"badlyFormedBasisOfRecord","code":20002,"uuid":"7aa48b5b-18f1-4331-9a9f-793282461528","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"invalidCollectionDate","code":30007,"uuid":"c6af646e-78aa-4b0a-9966-940f5259b9e6","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"firstOfMonth","code":30003,"uuid":"2f8be2ea-aab2-4372-affb-88bb71060511","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"missingIdentificationQualifier","comment":"Missing identificationQualifier","code":10009,"uuid":"f283b659-1d80-4239-a237-338e90a401c6","qaStatus":0,"created":"2013-05-31T18:31:20Z"},{"name":"missingIdentifiedBy","comment":"Missing identifiedBy","code":10010,"uuid":"92b2767b-eb23-4b0d-b527-400eb57b8d2b","qaStatus":0,"created":"2013-05-31T18:31:20Z"},{"name":"missingIdentificationReferences","comment":"Missing identificationReferences","code":10011,"uuid":"15f1e1fe-a5ef-476e-a7ae-dd0e8d845b79","qaStatus":0,"created":"2013-05-31T18:31:20Z"},{"name":"missingDateIdentified","comment":"Missing dateIdentified","code":10012,"uuid":"653ee10c-ff82-47fd-befb-9407689f819f","qaStatus":0,"created":"2013-05-31T18:31:20Z"},{"name":"missingCatalogueNumber","code":20015,"uuid":"0fbf3e6e-6e1c-4ebe-8bc1-013c9ddb5a50","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"dataAreGeneralised","code":20009,"uuid":"ad5c0c31-14fd-44c8-9ebb-a302c471a3a3","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"inferredDuplicateRecord","comment":"Record has been inferred as closely related to  e20f2dc0-9404-4782-a5b3-7050ebef9d1a","code":20014,"uuid":"3a3daf60-618c-4a7c-b18b-9242e953c332","qaStatus":0,"created":"2013-05-31T18:31:20Z"},{"name":"missingTaxonRank","comment":"Missing taxonRank","code":10008,"uuid":"1839d64a-97af-4e21-af33-f8c37fcaee57","qaStatus":0,"created":"2013-05-31T18:31:20Z"},{"name":"nameNotSupplied","code":10015,"uuid":"89f33662-d9ce-4e11-b0f8-e4402821c563","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"nameNotRecognised","code":10004,"uuid":"774c0abd-8098-4ce6-873b-7b39e9b8532c","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"invalidScientificName","code":10001,"uuid":"e8fc31a5-07d8-4776-802d-c9e619878d98","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"nameNotInNationalChecklists","code":10005,"uuid":"14132c67-b833-447b-8a66-9463c49debc6","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"decimalCoordinatesNotSupplied","code":44,"uuid":"409baab2-ba41-4234-94ac-2590e43fe262","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"geodeticDatumAssumedWgs84","code":51,"uuid":"3e1c9213-79a6-4c8a-a00e-bd04f000ae0f","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"unrecognizedGeodeticDatum","code":52,"uuid":"a70304c4-7c4d-4d7e-b9f9-8853a6b2b932","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"decimalLatLongConverionFailed","code":46,"uuid":"14aa1b77-6432-4a82-8dec-e6ba96c616b7","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"decimalLatLongConverted","comment":"Decimal latitude and longitude were converted to WGS84 (EPSG:4326)","code":45,"uuid":"562974d2-e063-498e-ae92-67580d89b9ac","qaStatus":0,"created":"2013-05-31T18:31:20Z"},{"name":"invertedCoordinates","code":3,"uuid":"c9d1c52a-912d-4e04-9e8d-2819cd3f2866","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"coordinatesOutOfRange","code":5,"uuid":"af5cb31c-e80c-4f22-a818-06613918bbd2","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"zeroCoordinates","code":4,"uuid":"060443b9-cbfb-4e96-8fcc-aebe339551f6","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"uncertaintyRangeMismatch","code":24,"uuid":"97ef8648-31e3-4d36-a616-9477f9893d2d","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"missingCoordinatePrecision","comment":"Missing coordinatePrecision","code":29,"uuid":"341f9c10-672d-467d-add2-cebc40c7d2dd","qaStatus":0,"created":"2013-05-31T18:31:20Z"},{"name":"uncertaintyNotSpecified","code":27,"uuid":"d3aaa6f1-d46c-471a-a078-ab0633571e21","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"habitatMismatch","code":19,"uuid":"1c9cdb4b-85b8-4303-8d77-b67e5202aa5c","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"locationNotSupplied","code":43,"uuid":"6b259577-a5c3-4560-bb2b-95ce4cd3a0d1","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"countryInferredByCoordinates","code":21,"uuid":"f9a4048d-dfd0-4bf4-b853-fbc4fe89b605","qaStatus":0,"created":"2013-05-31T18:31:20Z"},{"name":"coordinatesCentreOfStateProvince","code":22,"uuid":"2c6aa9c8-864c-4132-bbaf-59b0ddea3d16","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"coordinatesCentreOfCountry","code":28,"uuid":"db0a0a6e-f81e-4557-a8b2-6eec0f88ebe3","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"missingGeodeticDatum","code":30,"uuid":"ac1a68b3-4fe6-4def-92f5-6a722d8693e8","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"missingGeorefencedBy","comment":"Missing georeferencedBy","code":31,"uuid":"959f8af0-84e1-4ef0-b0c1-435ae56227ba","qaStatus":0,"created":"2013-05-31T18:31:20Z"},{"name":"missingGeoreferenceProtocol","comment":"Missing georeferenceProtocol","code":32,"uuid":"df7da30b-ef1f-4b85-a95e-4289a9de99dd","qaStatus":0,"created":"2013-05-31T18:31:20Z"},{"name":"missingGeoreferenceSources","comment":"Missing georeferenceSources","code":33,"uuid":"7066673d-02ef-4cb6-a189-f082ae04ea76","qaStatus":0,"created":"2013-05-31T18:31:20Z"},{"name":"missingGeoreferenceVerificationStatus","comment":"Missing georeferenceVerificationStatus","code":34,"uuid":"48fa79a2-a96e-42fc-8128-7c390f525af5","qaStatus":0,"created":"2013-05-31T18:31:20Z"},{"name":"missingGeoreferenceDate","code":42,"uuid":"5c5bbada-1578-49c1-9132-9c7fafb863e6","qaStatus":0,"created":"2013-05-31T18:31:20Z"},{"name":"inferredDuplicateRecord","comment":"Record has been inferred as closely related to  e20f2dc0-9404-4782-a5b3-7050ebef9d1a","code":20014,"uuid":"355b8e86-113b-4545-850c-bb1cc4b3ec65","problemAsserted":true,"qaStatus":0,"created":"2013-04-13T03:36:41Z"}]"""
    val qaList = Json.toListWithGeneric(qualityAssertions, classOf[QualityAssertion])
    Config.occurrenceDAO.updateAssertionStatus("qatestkosher", QualityAssertion(AssertionCodes.UNRECOGNIZED_GEODETIC_DATUM), qaList, List())
    val values = persistenceManager.get("qatestkosher", "occ")
    expectResult(true){
      values.isDefined
    }
    expectResult("true"){
      values.get.getOrElse(FullRecordMapper.geospatialDecisionColumn, "false")
    }
//    println(Config.indexDAO.sortOutQas("qatest", qaList))
//    println(Config.indexDAO.sortOutQas("qatest", List(QualityAssertion(AssertionCodes.COORDINATES_OUT_OF_RANGE))))
//    println(Config.indexDAO.sortOutQas("qatest",Json.toListWithGeneric("""[{"name":"missingBasisOfRecord","comment":"Missing basis of record","code":20001,"uuid":"87eb2d13-2284-4892-9af3-34676a088265","qaStatus":0,"created":"2013-06-01T09:21:06Z"},{"name":"invalidCollectionDate","code":30007,"uuid":"7e8796f4-da13-4fe4-8103-230f2634e2a4","qaStatus":1,"created":"2013-06-01T09:21:06Z"},{"name":"firstOfMonth","code":30003,"uuid":"03a0b391-833d-4dbf-8962-fe6ea6155e1e","qaStatus":1,"created":"2013-06-01T09:21:06Z"},{"name":"missingIdentificationQualifier","comment":"Missing identificationQualifier","code":10009,"uuid":"3932736d-a8ba-4e2e-bf40-31ce3cb483ef","qaStatus":0,"created":"2013-06-01T09:21:06Z"},{"name":"missingIdentifiedBy","comment":"Missing identifiedBy","code":10010,"uuid":"fea2065c-3fec-4f9d-942a-ad3bb3a13afc","qaStatus":0,"created":"2013-06-01T09:21:06Z"},{"name":"missingIdentificationReferences","comment":"Missing identificationReferences","code":10011,"uuid":"c352dbac-d943-4f66-89ae-86dc5b7f5031","qaStatus":0,"created":"2013-06-01T09:21:06Z"},{"name":"missingDateIdentified","comment":"Missing dateIdentified","code":10012,"uuid":"7272357a-fbe1-455e-b536-6e95eab60f5d","qaStatus":0,"created":"2013-06-01T09:21:06Z"},{"name":"missingCatalogueNumber","comment":"No catalogue number provided","code":20015,"uuid":"b418c14f-75e3-4357-9f6d-6a5fa8e9bbf3","qaStatus":0,"created":"2013-06-01T09:21:06Z"},{"name":"dataAreGeneralised","code":20009,"uuid":"8f2e7709-5012-4581-877c-f8c3762c9f38","qaStatus":1,"created":"2013-06-01T09:21:06Z"},{"name":"missingTaxonRank","comment":"Missing taxonRank","code":10008,"uuid":"83de7924-45f2-4a31-a025-a2cb2e0e5639","qaStatus":0,"created":"2013-06-01T09:21:06Z"},{"name":"nameNotSupplied","code":10015,"uuid":"ce0699ea-cf60-4701-8071-5496f3b5001e","qaStatus":1,"created":"2013-06-01T09:21:06Z"},{"name":"nameNotRecognised","code":10004,"uuid":"845affac-eb97-4b9b-838c-07d362049690","qaStatus":1,"created":"2013-06-01T09:21:06Z"},{"name":"invalidScientificName","code":10001,"uuid":"65881de2-b979-4102-8d07-f806e680fd94","qaStatus":1,"created":"2013-06-01T09:21:06Z"},{"name":"nameNotInNationalChecklists","code":10005,"uuid":"b871cf48-5f35-4013-815b-affac8d4fe3e","qaStatus":1,"created":"2013-06-01T09:21:06Z"},{"name":"decimalCoordinatesNotSupplied","code":44,"uuid":"f3d98565-0d22-428c-9b81-232ace998889","qaStatus":1,"created":"2013-06-01T09:21:06Z"},{"name":"geodeticDatumAssumedWgs84","code":51,"uuid":"025f51fb-b0d8-42ba-b646-358b2088d454","qaStatus":1,"created":"2013-06-01T09:21:06Z"},{"name":"unrecognizedGeodeticDatum","comment":"Geodetic datum \"AGD84/66\" not recognized.","code":52,"uuid":"9628cf19-cd2e-4f3b-83a4-fc3a18aab280","qaStatus":0,"created":"2013-06-01T09:21:06Z"},{"name":"invertedCoordinates","code":3,"uuid":"833f4b25-2974-4292-b556-42d88b008125","qaStatus":1,"created":"2013-06-01T09:21:06Z"},{"name":"coordinatesOutOfRange","code":5,"uuid":"d5dde0d4-6fdd-4aa3-8330-15b4c9cc7ea9","qaStatus":1,"created":"2013-06-01T09:21:06Z"},{"name":"zeroCoordinates","code":4,"uuid":"17a94a27-64ec-43d9-956f-8ba74149281b","qaStatus":1,"created":"2013-06-01T09:21:06Z"},{"name":"uncertaintyRangeMismatch","code":24,"uuid":"61669c6a-c744-4d0f-9e49-815af245de9b","qaStatus":1,"created":"2013-06-01T09:21:06Z"},{"name":"missingCoordinatePrecision","comment":"Missing coordinatePrecision","code":29,"uuid":"9dba0368-79f8-43d2-a085-cd74964b2583","qaStatus":0,"created":"2013-06-01T09:21:06Z"},{"name":"uncertaintyNotSpecified","code":27,"uuid":"a6d327f9-34ce-4fa0-a8d7-dac3b97b14d8","qaStatus":1,"created":"2013-06-01T09:21:06Z"},{"name":"stateCoordinateMismatch","code":18,"uuid":"9bf541f4-4464-48a9-8cb1-ee8f59376581","qaStatus":1,"created":"2013-06-01T09:21:06Z"},{"name":"habitatMismatch","code":19,"uuid":"d99cdfb5-6549-42d1-9258-4ed0c18b739a","qaStatus":1,"created":"2013-06-01T09:21:06Z"},{"name":"locationNotSupplied","code":43,"uuid":"eee56ca8-0fc3-42cd-a784-2da96dc91f1c","qaStatus":1,"created":"2013-06-01T09:21:06Z"},{"name":"countryInferredByCoordinates","code":21,"uuid":"9efdac62-f606-4b1c-8dff-f1f59bfceb3a","qaStatus":0,"created":"2013-06-01T09:21:06Z"},{"name":"coordinatesCentreOfStateProvince","code":22,"uuid":"2da5d341-d1c8-4aee-be84-ad03bb82509d","qaStatus":1,"created":"2013-06-01T09:21:06Z"},{"name":"coordinatesCentreOfCountry","code":28,"uuid":"ad26b6a7-6f61-4709-9aab-7665e176ebd3","qaStatus":1,"created":"2013-06-01T09:21:06Z"},{"name":"missingGeodeticDatum","code":30,"uuid":"0508d0be-bdc8-4d70-8fde-8c16708d3b59","qaStatus":1,"created":"2013-06-01T09:21:06Z"},{"name":"missingGeorefencedBy","comment":"Missing georeferencedBy","code":31,"uuid":"1347dc8f-6c5f-4b62-a02d-8359ee9d9344","qaStatus":0,"created":"2013-06-01T09:21:06Z"},{"name":"missingGeoreferenceProtocol","code":32,"uuid":"665c9ef6-0c2e-4a13-8c34-196ec722d219","qaStatus":1,"created":"2013-06-01T09:21:06Z"},{"name":"missingGeoreferenceSources","comment":"Missing georeferenceSources","code":33,"uuid":"616b7164-1f8a-41e4-9097-76c73aa17668","qaStatus":0,"created":"2013-06-01T09:21:06Z"},{"name":"missingGeoreferenceVerificationStatus","comment":"Missing georeferenceVerificationStatus","code":34,"uuid":"d7f59d8f-c5ef-47cf-a0ac-bfc313bd9d61","qaStatus":0,"created":"2013-06-01T09:21:06Z"},{"name":"missingGeoreferenceDate","code":42,"uuid":"12d6a230-7f24-4e91-803a-54da48ff0b8d","qaStatus":0,"created":"2013-06-01T09:21:06Z"}]""",classOf[QualityAssertion])))
//    println(Config.indexDAO.sortOutQas("qatest",Json.toListWithGeneric("""[{"name":"missingBasisOfRecord","code":20001,"uuid":"91fc7048-0fe3-424d-946f-b250cbfb0da2","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"badlyFormedBasisOfRecord","code":20002,"uuid":"7aa48b5b-18f1-4331-9a9f-793282461528","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"invalidCollectionDate","code":30007,"uuid":"c6af646e-78aa-4b0a-9966-940f5259b9e6","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"firstOfMonth","code":30003,"uuid":"2f8be2ea-aab2-4372-affb-88bb71060511","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"missingIdentificationQualifier","comment":"Missing identificationQualifier","code":10009,"uuid":"f283b659-1d80-4239-a237-338e90a401c6","qaStatus":0,"created":"2013-05-31T18:31:20Z"},{"name":"missingIdentifiedBy","comment":"Missing identifiedBy","code":10010,"uuid":"92b2767b-eb23-4b0d-b527-400eb57b8d2b","qaStatus":0,"created":"2013-05-31T18:31:20Z"},{"name":"missingIdentificationReferences","comment":"Missing identificationReferences","code":10011,"uuid":"15f1e1fe-a5ef-476e-a7ae-dd0e8d845b79","qaStatus":0,"created":"2013-05-31T18:31:20Z"},{"name":"missingDateIdentified","comment":"Missing dateIdentified","code":10012,"uuid":"653ee10c-ff82-47fd-befb-9407689f819f","qaStatus":0,"created":"2013-05-31T18:31:20Z"},{"name":"missingCatalogueNumber","code":20015,"uuid":"0fbf3e6e-6e1c-4ebe-8bc1-013c9ddb5a50","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"dataAreGeneralised","code":20009,"uuid":"ad5c0c31-14fd-44c8-9ebb-a302c471a3a3","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"inferredDuplicateRecord","comment":"Record has been inferred as closely related to  e20f2dc0-9404-4782-a5b3-7050ebef9d1a","code":20014,"uuid":"3a3daf60-618c-4a7c-b18b-9242e953c332","qaStatus":0,"created":"2013-05-31T18:31:20Z"},{"name":"missingTaxonRank","comment":"Missing taxonRank","code":10008,"uuid":"1839d64a-97af-4e21-af33-f8c37fcaee57","qaStatus":0,"created":"2013-05-31T18:31:20Z"},{"name":"nameNotSupplied","code":10015,"uuid":"89f33662-d9ce-4e11-b0f8-e4402821c563","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"nameNotRecognised","code":10004,"uuid":"774c0abd-8098-4ce6-873b-7b39e9b8532c","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"invalidScientificName","code":10001,"uuid":"e8fc31a5-07d8-4776-802d-c9e619878d98","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"nameNotInNationalChecklists","code":10005,"uuid":"14132c67-b833-447b-8a66-9463c49debc6","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"decimalCoordinatesNotSupplied","code":44,"uuid":"409baab2-ba41-4234-94ac-2590e43fe262","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"geodeticDatumAssumedWgs84","code":51,"uuid":"3e1c9213-79a6-4c8a-a00e-bd04f000ae0f","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"unrecognizedGeodeticDatum","code":52,"uuid":"a70304c4-7c4d-4d7e-b9f9-8853a6b2b932","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"decimalLatLongConverionFailed","code":46,"uuid":"14aa1b77-6432-4a82-8dec-e6ba96c616b7","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"decimalLatLongConverted","comment":"Decimal latitude and longitude were converted to WGS84 (EPSG:4326)","code":45,"uuid":"562974d2-e063-498e-ae92-67580d89b9ac","qaStatus":0,"created":"2013-05-31T18:31:20Z"},{"name":"invertedCoordinates","code":3,"uuid":"c9d1c52a-912d-4e04-9e8d-2819cd3f2866","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"coordinatesOutOfRange","code":5,"uuid":"af5cb31c-e80c-4f22-a818-06613918bbd2","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"zeroCoordinates","code":4,"uuid":"060443b9-cbfb-4e96-8fcc-aebe339551f6","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"uncertaintyRangeMismatch","code":24,"uuid":"97ef8648-31e3-4d36-a616-9477f9893d2d","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"missingCoordinatePrecision","comment":"Missing coordinatePrecision","code":29,"uuid":"341f9c10-672d-467d-add2-cebc40c7d2dd","qaStatus":0,"created":"2013-05-31T18:31:20Z"},{"name":"uncertaintyNotSpecified","code":27,"uuid":"d3aaa6f1-d46c-471a-a078-ab0633571e21","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"habitatMismatch","code":19,"uuid":"1c9cdb4b-85b8-4303-8d77-b67e5202aa5c","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"locationNotSupplied","code":43,"uuid":"6b259577-a5c3-4560-bb2b-95ce4cd3a0d1","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"countryInferredByCoordinates","code":21,"uuid":"f9a4048d-dfd0-4bf4-b853-fbc4fe89b605","qaStatus":0,"created":"2013-05-31T18:31:20Z"},{"name":"coordinatesCentreOfStateProvince","code":22,"uuid":"2c6aa9c8-864c-4132-bbaf-59b0ddea3d16","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"coordinatesCentreOfCountry","code":28,"uuid":"db0a0a6e-f81e-4557-a8b2-6eec0f88ebe3","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"missingGeodeticDatum","code":30,"uuid":"ac1a68b3-4fe6-4def-92f5-6a722d8693e8","qaStatus":1,"created":"2013-05-31T18:31:20Z"},{"name":"missingGeorefencedBy","comment":"Missing georeferencedBy","code":31,"uuid":"959f8af0-84e1-4ef0-b0c1-435ae56227ba","qaStatus":0,"created":"2013-05-31T18:31:20Z"},{"name":"missingGeoreferenceProtocol","comment":"Missing georeferenceProtocol","code":32,"uuid":"df7da30b-ef1f-4b85-a95e-4289a9de99dd","qaStatus":0,"created":"2013-05-31T18:31:20Z"},{"name":"missingGeoreferenceSources","comment":"Missing georeferenceSources","code":33,"uuid":"7066673d-02ef-4cb6-a189-f082ae04ea76","qaStatus":0,"created":"2013-05-31T18:31:20Z"},{"name":"missingGeoreferenceVerificationStatus","comment":"Missing georeferenceVerificationStatus","code":34,"uuid":"48fa79a2-a96e-42fc-8128-7c390f525af5","qaStatus":0,"created":"2013-05-31T18:31:20Z"},{"name":"missingGeoreferenceDate","code":42,"uuid":"5c5bbada-1578-49c1-9132-9c7fafb863e6","qaStatus":0,"created":"2013-05-31T18:31:20Z"},{"name":"inferredDuplicateRecord","comment":"Record has been inferred as closely related to  e20f2dc0-9404-4782-a5b3-7050ebef9d1a","code":20014,"uuid":"355b8e86-113b-4545-850c-bb1cc4b3ec65","problemAsserted":true,"qaStatus":0,"created":"2013-04-13T03:36:41Z"}]""",classOf[QualityAssertion])))
    //def convertAssertionsToMap(rowKey:String, systemAssertions: Map[String,Array[QualityAssertion]], verified:Boolean): Map[String, String] = {
    //def updateAssertionStatus(rowKey: String, assertion:QualityAssertion, systemAssertions: List[QualityAssertion], userAssertions: List[QualityAssertion]) {

  }

  test("Write and lookup occ record") {
    val record = new FullRecord(rowKey, uuid)
    record.classification.scientificName = "Test species"
    occurrenceDAO.updateOccurrence(rowKey, record, Versions.RAW)
    val newrecord = occurrenceDAO.getByRowKey(rowKey);
    //val newrecord = occurrenceDAO.getByUuid(uuid)

    expectResult(rowKey) {
      newrecord.get.getRowKey
    }
    expectResult(uuid) {
      newrecord.get.uuid
    }
  }

  test("Write Double value to processed record then read it out") {
    val record = new FullRecord(rowKey, uuid)
    val processedRecord = record.createNewProcessedRecord
    processedRecord.location.setProperty("distanceOutsideExpertRange", "1.23456")
    val retrievedDistance = processedRecord.location.getProperty("distanceOutsideExpertRange")
    expectResult("1.23456") {
      retrievedDistance.get
    }
  }

  test("Write map value to processed record then read it out") {
    val record = new FullRecord(rowKey, uuid)
    val processedRecord = record.createNewProcessedRecord
    processedRecord.occurrence.setProperty("originalSensitiveValues", "{\"a\":\"1\",\"b\":\"2\"}")
    val retrievedMap = processedRecord.occurrence.getProperty("originalSensitiveValues")
    expectResult("{\"a\":\"1\",\"b\":\"2\"}") {
      retrievedMap.get
    }
  }

  test("User Assertions addition and deletion") {
    val qa = QualityAssertion(AssertionCodes.GEOSPATIAL_ISSUE, true)
    qa.comment = "My comment"
    qa.userId = "Natasha.Carter@csiro.au"
    qa.userDisplayName = "Natasha Carter"
    occurrenceDAO.addUserAssertion(rowKey, qa)
    expectResult(1) {
      val userAssertions = occurrenceDAO.getUserAssertions(rowKey)
      userAssertions.size
    }
    val qaRowKey = rowKey + "|" + qa.getUserId + "|" + qa.getCode
    val qatest = persistenceManager.get(qaRowKey, "qa")
    println(qatest)

    expectResult(true) {
      !qatest.isEmpty
    }
    val qa2 = QualityAssertion(AssertionCodes.TAXONOMIC_ISSUE, false)
    qa2.comment = "My comment"
    qa2.userId = "Natasha.Carter@csiro.au"
    qa2.userDisplayName = "Natasha Carter"
    occurrenceDAO.addUserAssertion(rowKey, qa2)
    println(persistenceManager.get(qaRowKey, "qa"))
    expectResult(2) {
      val userAssertions = occurrenceDAO.getUserAssertions(rowKey)
      userAssertions.size
    }
    occurrenceDAO.deleteUserAssertion(rowKey, qa2.uuid)
    expectResult(1) {
      val userAssertions = occurrenceDAO.getUserAssertions(rowKey)
      userAssertions.size
    }
  }

  test("JSON parsing for Duplicates"){
    val json ="""{"rowKey":"dr376|CANB|CANB708196","uuid":"bd2d23c3-5fd8-43d1-9b54-e52776bdc78c","taxonConceptLsid":"urn:lsid:biodiversity.org.au:apni.taxon:373696","year":"1981","month":"11","day":"10","point1":"-28,153","point0_1":"-27.5,152.7","point0_01":"-27.52,152.75","point0_001":"-27.523,152.748","point0_0001":"-27.5233,152.7483","latLong":"-27.5233,152.7483","rawScientificName":"Erythrina numerosa A.R.Bean","collector":"[Bird, L.]","status":"R","druid":"dr376","duplicates":[{"rowKey":"dr376|MEL|MEL2332425A","uuid":"4587c30c-92f9-4d7d-bdb0-92f00122d673","taxonConceptLsid":"urn:lsid:biodiversity.org.au:apni.taxon:373696","year":"1981","month":"11","day":"10","point1":"-28,153","point0_1":"-27.5,152.7","point0_01":"-27.52,152.75","point0_001":"-27.523,152.748","point0_0001":"-27.5233,152.7483","latLong":"-27.5233,152.7483","rawScientificName":"Erythrina numerosa A.R.Bean","collector":"[Bird, L.]","status":"D1","druid":"dr376","dupTypes":[{"id":6},{"id":4}]}]}"""
    val mapper = new ObjectMapper
    mapper.registerModule(DefaultScalaModule)
    mapper.setSerializationInclusion(Include.NON_NULL)
    val t = new OutlierResult("Natasha",0)
    println(mapper.writeValueAsString(t))
    val d = new DuplicateRecordDetails("drtest|dfghui|34", "sdf2-34-3", "urn:lsid:biodiversity.org.au:apni.taxon:373696", "1981","11","10","-27,152","-27.5,152.7","-27.52,152.74","-27.523,152.748","-27.5233,152.7483","-27.5233,152.7483","Erythrina numerosa A.R.Bean","[Bird, L.]","","")
    println(mapper.writeValueAsString(d))
    /*
    @BeanProperty var rowKey:String, @BeanProperty var uuid:String, @BeanProperty var taxonConceptLsid:String,
                    @BeanProperty var year:String, @BeanProperty var month:String, @BeanProperty var day:String,
                    @BeanProperty var point1:String, @BeanProperty var point0_1:String,
                    @BeanProperty var point0_01:String, @BeanProperty var point0_001:String,
                    @BeanProperty var point0_0001:String,@BeanProperty var latLong:String,
                    @BeanProperty var rawScientificName:String, @BeanProperty var collector:String,
                    @BeanProperty var oldStatus:String, @BeanProperty var oldDuplicateOf:String
     */
    //mapper.registerModule(new DefaultScalaModule())
    //mapper.readValue[DuplicateRecordDetails](json,classOf[DuplicateRecordDetails])
    mapper.readValue[OutlierResult]("""{"testUuid":"dr376|CANB|CANB708196","outlierForLayersCount":0}""", classOf[OutlierResult])

  }

//  class Test(@BeanProperty name:String, @BeanProperty value:String){
//    def this() = this(null,null)
//  }


}

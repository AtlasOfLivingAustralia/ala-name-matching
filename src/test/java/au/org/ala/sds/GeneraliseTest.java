/***************************************************************************
 * Copyright (C) 2010 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 ***************************************************************************/
package au.org.ala.sds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import au.org.ala.names.search.ALANameSearcher;
import au.org.ala.sds.model.SensitiveTaxon;
import au.org.ala.sds.util.Configuration;
import au.org.ala.sds.validation.FactCollection;
import au.org.ala.sds.validation.ServiceFactory;
import au.org.ala.sds.validation.ValidationOutcome;
import au.org.ala.sds.validation.ValidationService;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class GeneraliseTest {

//    static DataSource dataSource;
    static ALANameSearcher nameSearcher;
    static SensitiveSpeciesFinder finder;

    @BeforeClass
    public static void runOnce() throws Exception {
//        dataSource = new BasicDataSource();
//        ((BasicDataSource) dataSource).setDriverClassName("com.mysql.jdbc.Driver");
//        ((BasicDataSource) dataSource).setUrl("jdbc:mysql://localhost/portal");
//        ((BasicDataSource) dataSource).setUsername("root");
//        ((BasicDataSource) dataSource).setPassword("password");

        nameSearcher = new ALANameSearcher(Configuration.getInstance().getNameMatchingIndex());
        //finder = SensitiveSpeciesFinderFactory.getSensitiveSpeciesFinder(cbIndexSearch);
        String uri = nameSearcher.getClass().getClassLoader().getResource("sensitive-species.xml").toURI().toString();
        finder = SensitiveSpeciesFinderFactory.getSensitiveSpeciesFinder(uri, nameSearcher, true);
    }

    /**
     * Birds Australia occurrence in ACT - position generalised
     */
    @SuppressWarnings("unchecked")
    @Test
    public void birdsAustraliaInAct() {
        SensitiveTaxon ss = finder.findSensitiveSpecies("Crex crex");
        assertNotNull(ss);
        String latitude = "-35.276771";   // Epicorp
        String longitude = "149.112539";

        Map<String, String> facts = new HashMap<String, String>();
        facts.put("dataResourceUid", "dr359");
        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);
        facts.put("minimumElevationInMeters", "Do nothing");
        facts.put("eventID", "1234");
        facts.put("stateProvince", "Australian Capital Territory");
        facts.put("scientificName", "Crex crex");
        facts.put("taxonConceptID", "urn:lsid:biodiversity.org.au:afd.taxon:2ef4ac9c-7dfb-4447-8431-e337355ac1ca");
        facts.put("locationRemarks", "remarks");
        facts.put("day", "10");
        facts.put("month", "10");
        facts.put("year", "2010");
        facts.put("verbatimCoordinates", "These need to be withheld");

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertTrue(outcome.isValid());
        assertTrue(outcome.isSensitive());
        Map<String, Object> result = outcome.getResult();
        assertNotNull(result);

        assertEquals("Latitude", "-35.3", result.get("decimalLatitude"));
        assertEquals("Longitude", "149.1", result.get("decimalLongitude"));
        assertEquals("InMetres", "10000", result.get("generalisationInMetres"));
        assertEquals("eventID", "", result.get("eventID"));
        assertEquals("locationRemarks", "", result.get("locationRemarks"));
        assertEquals("day", "", result.get("day"));
        assertEquals("informationWithheld", "The eventID and day information has been withheld in accordance with Birds Australia data policy", result.get("informationWithheld"));
        assertEquals("dataGeneralizations", "Location in ACT generalised to 0.1 degrees. \nSensitive in AUS [Endangered, Birds Australia]", result.get("dataGeneralizations"));

        Map<String, String> originalSenstiveValues = (Map<String, String>) outcome.getResult().get("originalSensitiveValues");
        assertNotNull(originalSenstiveValues);
        
        assertFalse(outcome.getResult().get("verbatimCoordinates").toString().length()>0);

        assertEquals("Original latitude", "-35.276771", originalSenstiveValues.get("decimalLatitude"));
        assertEquals("Original longitude", "149.112539", originalSenstiveValues.get("decimalLongitude"));
        assertEquals("Original eventID", "1234", originalSenstiveValues.get("eventID"));
        assertEquals("Original locationRemarks", "remarks", originalSenstiveValues.get("locationRemarks"));
        assertEquals("Original day", "10", originalSenstiveValues.get("day"));
    }

    /**
     * Not Birds Australia occurrence in ACT - position not generalised
     */
    @Test
    public void notBirdsAustraliaInAct() {
        SensitiveTaxon ss = finder.findSensitiveSpecies("Crex crex");
        assertNotNull(ss);
        String latitude = "-35.276771";   // Epicorp
        String longitude = "149.112539";

        Map<String, String> facts = new HashMap<String, String>();
        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);
        facts.put("stateProvince", "Australian Capital Territory");
        facts.put("scientificName", "Crex crex");

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertTrue(outcome.isValid());
        assertFalse(outcome.isSensitive());
    }

    /**
     * Not Birds Australia occurrence in NSW - position generalised
     */
    @Test
    public void notBirdsAustraliaInNsw() {
        SensitiveTaxon ss = finder.findSensitiveSpecies("Callocephalon fimbriatum");
        assertNotNull(ss);
        String latitude = "-34.023071";   // NSW
        String longitude = "150.056763";

        Map<String, String> facts = new HashMap<String, String>();
        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertTrue(outcome.isValid());
        assertTrue(outcome.isSensitive());
    }

    /**
     * NSW species in ACT - not generalised
     */
    @Test
    public void nswSpeciesInAct() {
        SensitiveTaxon ss = finder.findSensitiveSpecies("Wollemia nobilis");
        assertNotNull(ss);
        String latitude = "-35.276771";   // Epicorp
        String longitude = "149.112539";

        Map<String, String> facts = new HashMap<String, String>();
        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertTrue(outcome.isValid());
        assertNotNull(outcome.getResult());

        assertFalse(outcome.isSensitive());
    }

    /**
     * NSW species in NZ - not generalised
     */
    @Test
    public void nswSpeciesInNZ() {
        SensitiveTaxon ss = finder.findSensitiveSpecies("Wollemia nobilis");
        assertNotNull(ss);
        String latitude = "-41.538137";    // NZ
        String longitude = "173.968817";

        Map<String, String> facts = new HashMap<String, String>();
        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertTrue(outcome.isValid());
        assertNotNull(outcome.getResult());

        assertFalse(outcome.isSensitive());
    }

    /**
     * NSW Cat 1 species in NSW - position not published
     */
    @Test
    public void nswCat1SpeciesInNSW() {
        SensitiveTaxon ss = finder.findSensitiveSpecies("Wollemia nobilis");
        assertNotNull(ss);
        String latitude = "-33.630629";    // NSW
        String longitude = "150.441284";

        Map<String, String> facts = new HashMap<String, String>();
        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertTrue(outcome.isValid());
        assertNotNull(outcome.getResult());

        assertEquals("Latitude", "", outcome.getResult().get("decimalLatitude"));
        assertEquals("Longitude", "", outcome.getResult().get("decimalLongitude"));
        assertEquals("InMetres", "", outcome.getResult().get("generalisationInMetres"));
        assertEquals("Location withheld. \nSensitive in NSW [Endangered, NSW OEH]", outcome.getResult().get("dataGeneralizations"));
        assertTrue(outcome.isSensitive());
    }

    /**
     * TAS species in TAS - generalised
     */
    @Test
    public void tasSpeciesInTas() {
        SensitiveTaxon ss = finder.findSensitiveSpecies("Galaxias fontanus");
        assertNotNull(ss);
        String latitude = "-40.111689";    // TAS
        String longitude = "148.095703";

        Map<String, String> facts = new HashMap<String, String>();
        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertTrue(outcome.isValid());
        assertNotNull(outcome.getResult());

        assertEquals("Latitude", "", outcome.getResult().get("decimalLatitude"));
        assertEquals("Longitude", "", outcome.getResult().get("decimalLongitude"));
        assertEquals("InMetres", "", outcome.getResult().get("generalisationInMetres"));
        assertEquals("Location withheld. \nSensitive in TAS [Endangered, Tas DPIPWE]", outcome.getResult().get("dataGeneralizations"));
        assertTrue(outcome.isSensitive());
    }

    /**
     * Find sensitive species (Lophochroa leadbeateri | Major Mitchell's Cockatoo)
     */
    @Test
    public void vicSpeciesInVic() {
        SensitiveTaxon ss = finder.findSensitiveSpecies("Lophochroa leadbeateri");
        assertNotNull(ss);
        String latitude = "-36.330615";    // Beechworth, Vic
        String longitude = "146.719666";

        Map<String, String> facts = new HashMap<String, String>();
        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertTrue(outcome.isValid());
        assertNotNull(outcome.getResult());
        assertTrue(outcome.isSensitive()); // Sensitive in Victoria
        assertEquals("Latitude", "-36.3", outcome.getResult().get("decimalLatitude"));
        assertEquals("Longitude", "146.7", outcome.getResult().get("decimalLongitude"));
        assertEquals("InMetres", "10000", outcome.getResult().get("generalisationInMetres"));

    }

    /**
     * Find sensitive species (Lophochroa leadbeateri | Major Mitchell's Cockatoo) by LSID
     */
    @Test
    public void findSpeciesByLsid() {
        SensitiveTaxon ss = finder.findSensitiveSpeciesByLsid("urn:lsid:biodiversity.org.au:afd.taxon:1365807d-927b-4219-97bf-7e619afa5f72");
        assertNotNull(ss);
        String latitude = "-33.630629";    // NSW
        String longitude = "150.441284";

        Map<String, String> facts = new HashMap<String, String>();
        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);
        facts.put(FactCollection.STATE_PROVINCE_KEY, "New South Wales");

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertTrue(outcome.isValid());
        assertNotNull(outcome.getResult());

        assertTrue(outcome.isSensitive()); // Sensitive for Birds Australia, NSW, Victoria and WA
    }

    /**
     * Find sensitive species by accepted name (that differs from provided name)
     */
    //@Test
    //TODO Find a new example that will work for this test case and re-enable
    public void findSpeciesByAcceptedName() {
        SensitiveTaxon ss = finder.findSensitiveSpeciesByAcceptedName("Diplodium elegans"); //NC 2013-04-10 : This species is no longer has synonyms
        assertNotNull(ss);
        String latitude = "-33.630629";    // NSW
        String longitude = "150.441284";

        Map<String, String> facts = new HashMap<String, String>();
        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertTrue(outcome.isValid());
        assertNotNull(outcome.getResult());

        assertEquals("Latitude", "-33.6", outcome.getResult().get("decimalLatitude"));
        assertEquals("Longitude", "150.4", outcome.getResult().get("decimalLongitude"));
        assertEquals("InMetres", "10000", outcome.getResult().get("generalisationInMetres"));
    }

    /**
     * Try generalising a location that is already generalised
     */
    @Test
    public void generaliseLocationAlreadyGeneralised() {
        SensitiveTaxon ss = finder.findSensitiveSpecies("Lophochroa leadbeateri");
        assertNotNull(ss);
        String latitude = "-37.9";    // Vic
        String longitude = "145.4";

        Map<String, String> facts = new HashMap<String, String>();
        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertTrue(outcome.isValid());
        assertNotNull(outcome.getResult());

        assertEquals("Latitude", "-37.9", outcome.getResult().get("decimalLatitude"));
        assertEquals("Longitude", "145.4", outcome.getResult().get("decimalLongitude"));
        assertEquals("InMetres", "", outcome.getResult().get("generalisationInMetres"));
        assertEquals("Location in VIC is already generalised to 0.1 degrees. \nSensitive in VIC [Endangered, Vic DSE]", outcome.getResult().get("dataGeneralizations"));
        assertTrue(outcome.isSensitive());
    }


    @Test
    public void generaliseLocationFromState() {
        SensitiveTaxon ss = finder.findSensitiveSpecies("Lophochroa leadbeateri");
        assertNotNull(ss);
        String latitude = "-37.9";    // Vic
        String longitude = "145.4";

        Map<String, String> facts = new HashMap<String, String>();
        facts.put(FactCollection.STATE_PROVINCE_KEY, "VIC");
        facts.put(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
        facts.put(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertTrue(outcome.isValid());
        assertNotNull(outcome.getResult());

        assertEquals("Latitude", "-37.9", outcome.getResult().get("decimalLatitude"));
        assertEquals("Longitude", "145.4", outcome.getResult().get("decimalLongitude"));
        assertEquals("InMetres", "", outcome.getResult().get("generalisationInMetres"));
        assertTrue(outcome.isSensitive());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void generaliseNoLocationFromState() {
        SensitiveTaxon ss = finder.findSensitiveSpecies("Diuris disposita");
        assertNotNull(ss);

        Map<String, String> facts = new HashMap<String, String>();
        facts.put("dataResourceUid", "dr376");
        facts.put("minimumElevationInMeters", "Do nothing");
        facts.put("stateProvince", "New South Wales");
        facts.put("scientificName", "Diuris disposita");
        facts.put("taxonConceptID", "urn:lsid:biodiversity.org.au:apni.taxon:167966");
        facts.put("locality", "2.7 km by road towards Armidale from Toorooka road on Kempsey-Armidale road, NE side of road.");
        facts.put("day", "06");
        facts.put("month", "10");
        facts.put("year", "1992");

        ValidationService service = ServiceFactory.createValidationService(ss);
        ValidationOutcome outcome = service.validate(facts);

        assertTrue(outcome.isValid());
        assertTrue(outcome.isSensitive());
        Map<String, Object> result = outcome.getResult();
        assertNotNull(result);

        assertEquals("locality", "", result.get("locality"));
        assertEquals("dataGeneralizations", "Latitude/Longitude is missing. \nSensitive in NSW [Endangered, NSW OEH]", result.get("dataGeneralizations"));

        Map<String, String> originalSenstiveValues = (Map<String, String>) outcome.getResult().get("originalSensitiveValues");
        assertNotNull(originalSenstiveValues);
        assertEquals("Original locality", "2.7 km by road towards Armidale from Toorooka road on Kempsey-Armidale road, NE side of road.", originalSenstiveValues.get("locality"));
    }
}

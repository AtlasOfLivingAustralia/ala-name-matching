package au.org.ala.biocache

import org.scalatest.FunSuite
import com.google.inject.Guice
/**
 * All tests should extend this to access the correct DI
 */
class ConfigFunSuite extends FunSuite {
    Config.inj = Guice.createInjector(new TestConfigModule)
    val pm = Config.getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]
    //WS will automatically grab the location details that are necessary
    //Add conservation status
    println("The PM before setup: " + pm)
    val taxonProfile = new TaxonProfile
    taxonProfile.setGuid("urn:lsid:biodiversity.org.au:afd.taxon:3809b1ca-8b60-4fcb-acf5-ca4f1dc0e263")
    taxonProfile.setScientificName("Victaphanta compacta")
    taxonProfile.setConservation(Array(new ConservationSpecies("Victoria", "aus_states/Victoria", "Endangered", "Endangered")
      , new ConservationSpecies("Victoria", "aus_states/Victoria", null, "Listed under FFG Act")))
    TaxonProfileDAO.add(taxonProfile)

    val tp = new TaxonProfile
    tp.setGuid("urn:lsid:biodiversity.org.au:afd.taxon:aa745ff0-c776-4d0e-851d-369ba0e6f537")
    tp.setScientificName("Macropus rufus")
    tp.setHabitats(Array("Non-marine"))
    TaxonProfileDAO.add(tp);

    pm.put("-35.21667|144.81060", "loc", "stateProvince", "New South Wales")
    pm.put("-35.21667|144.81060", "loc", "cl927", "New South Wales")
    pm.put("-35.21667|144.8106", "loc", "cl927", "New South Wales")
    pm.put("-35.2|144.8", "loc", "stateProvince", "New South Wales")
    pm.put("-35.2|144.8", "loc", "cl927", "New South Wales")
    pm.put("-40.857|145.52","loc","cl21","onwater")
    pm.put("-23.73750|133.85720","loc","cl20","onland")
    
    pm.put("-31.2532183|146.921099","loc","cl927","New South Wales")
    pm.put("-31.253218|146.9211","loc","cl927","New South Wales")//NC 20130515: There is an issue where by our location cache converst toFLoat and loses accurancy.  This is the point above after going through the system
    
    
    println("THE pm after setup: " +pm)
}
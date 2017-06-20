package au.org.ala.names.index;

import au.org.ala.names.model.RankType;
import au.org.ala.names.model.TaxonomicType;
import org.gbif.api.vocabulary.NomenclaturalCode;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * File description.
 * <p>
 * More description.
 * </p>
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class ScientificNameTest {
    private static final String PROVIDER_ID = "P-1";
    private static final String TAXON_ID = "ID-1";
    private static final String NAME_1 = "Petrogale rothschildi";
    private static final String AUTHOR_1 = "Thomas, 1904";
    private static final String YEAR_1 = "1904";

    private ALANameAnalyser analyser;
    private NameProvider provider;

    @Before
    public void setup() {
        this.analyser = new ALANameAnalyser();
        this.provider = new NameProvider(PROVIDER_ID, 150);
    }

    @Test
    public void testAddInstance1() throws Exception {
        TaxonConceptInstance instance = new TaxonConceptInstance(
                TAXON_ID,
                NomenclaturalCode.ZOOLOGICAL,
                provider,
                NAME_1,
                AUTHOR_1,
                YEAR_1,
                TaxonomicType.ACCEPTED,
                RankType.SPECIES,
                null,
                null,
                null,
                null
        );
        NameKey instanceKey = this.analyser.analyse(NomenclaturalCode.ZOOLOGICAL, NAME_1, AUTHOR_1);
        NameKey nameKey = instanceKey.toNameKey();
        ScientificName name = new ScientificName(nameKey);
        name.addInstance(instanceKey, instance);
        TaxonConcept concept = instance.getTaxonConcept();
        assertNotNull(concept);
        assertSame(name, concept.getScientificName());
    }
}

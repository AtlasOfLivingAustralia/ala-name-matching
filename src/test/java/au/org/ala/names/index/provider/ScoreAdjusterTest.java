package au.org.ala.names.index.provider;

import au.org.ala.names.index.NameProvider;
import au.org.ala.names.index.TaxonConceptInstance;
import au.org.ala.names.model.RankType;
import au.org.ala.names.model.TaxonomicType;
import au.org.ala.names.util.TestUtils;
import org.gbif.api.vocabulary.NomenclaturalCode;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for {@link ScoreAdjuster}
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class ScoreAdjusterTest extends TestUtils {
    private ScoreAdjuster adjuster;
    private NameProvider provider;

    @Before
    public void setup() {
        this.provider = new NameProvider();
        this.adjuster = new ScoreAdjuster();
        MatchTaxonCondition condition1 = new MatchTaxonCondition();
        condition1.setTaxonomicStatus(TaxonomicType.INCERTAE_SEDIS);
        this.adjuster.addForbidden(condition1);
        MatchTaxonCondition condition2 = new MatchTaxonCondition();
        condition2.setTaxonomicStatus(TaxonomicType.MISAPPLIED);
        this.adjuster.addAdjustment(new ScoreAdjustment(condition2, -10));

        MatchTaxonCondition condition3 = new MatchTaxonCondition();
        condition3.setTaxonRank(RankType.DOMAIN);
        this.adjuster.addAdjustment(new ScoreAdjustment(condition3, 15));
    }

    @Test
    public void testForbidden1() {
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalCode.ZOOLOGICAL, "Osphranter rufus", this.provider, TaxonomicType.ACCEPTED );
        assertFalse(adjuster.forbid(instance));
    }


    @Test
    public void testForbidden2() {
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalCode.ZOOLOGICAL, "Osphranter rufus", this.provider, TaxonomicType.INCERTAE_SEDIS );
        assertTrue(adjuster.forbid(instance));
    }

    @Test
    public void testScore1() {
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalCode.ZOOLOGICAL, "Osphranter rufus", this.provider, TaxonomicType.ACCEPTED );
        assertEquals(0, adjuster.score(0, instance));
    }

    @Test
    public void testScore2() {
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalCode.ZOOLOGICAL, "Osphranter rufus", this.provider, TaxonomicType.ACCEPTED );
        assertEquals(50, adjuster.score(50, instance));
    }

    @Test
    public void testScore3() {
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalCode.ZOOLOGICAL, "Osphranter rufus", this.provider, TaxonomicType.MISAPPLIED );
        assertEquals(-10, adjuster.score(0, instance));
    }

    @Test
    public void testScore4() {
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalCode.ZOOLOGICAL, "Osphranter rufus", this.provider, TaxonomicType.MISAPPLIED );
        assertEquals(90, adjuster.score(100, instance));
    }

    @Test
    public void testScore5() {
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, this.provider,"Acacia dealbata", "Link.", null, TaxonomicType.ACCEPTED, RankType.DOMAIN, null, null, null, null);
        assertEquals(115, adjuster.score(100, instance));
    }

    @Test
    public void testScore6() {
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, this.provider,"Acacia dealbata", "Link.", null, TaxonomicType.MISAPPLIED, RankType.DOMAIN, null, null, null, null);
        assertEquals(105, adjuster.score(100, instance));
    }
}

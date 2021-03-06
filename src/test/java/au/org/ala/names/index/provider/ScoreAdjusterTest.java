package au.org.ala.names.index.provider;

import au.org.ala.names.index.ALANameAnalyser;
import au.org.ala.names.index.NameKey;
import au.org.ala.names.index.NameProvider;
import au.org.ala.names.index.TaxonConceptInstance;
import au.org.ala.names.model.RankType;
import au.org.ala.names.model.TaxonomicType;
import au.org.ala.names.util.TestUtils;
import org.gbif.api.vocabulary.NomenclaturalCode;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test cases for {@link ScoreAdjuster}
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class ScoreAdjusterTest extends TestUtils {
    private ScoreAdjuster adjuster;
    private NameProvider provider;
    private ALANameAnalyser analyser;

    @Before
    public void setup() {
        this.provider = new NameProvider();
        this.adjuster = new ScoreAdjuster();
        this.analyser = new ALANameAnalyser();
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
        NameKey key = this.analyser.analyse(instance);
        assertNull(adjuster.forbid(instance, key));
    }


    @Test
    public void testForbidden2() {
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalCode.ZOOLOGICAL, "Osphranter rufus", this.provider, TaxonomicType.INCERTAE_SEDIS );
        NameKey key = this.analyser.analyse(instance);
        assertEquals("taxonomicStatus:INCERTAE_SEDIS", adjuster.forbid(instance, key));
    }

    @Test
    public void testScore1() {
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalCode.ZOOLOGICAL, "Osphranter rufus", this.provider, TaxonomicType.ACCEPTED );
        NameKey key = this.analyser.analyse(instance);
        assertEquals(0, adjuster.score(0, instance, key));
    }

    @Test
    public void testScore2() {
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalCode.ZOOLOGICAL, "Osphranter rufus", this.provider, TaxonomicType.ACCEPTED );
        NameKey key = this.analyser.analyse(instance);
        assertEquals(50, adjuster.score(50, instance, key));
    }

    @Test
    public void testScore3() {
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalCode.ZOOLOGICAL, "Osphranter rufus", this.provider, TaxonomicType.MISAPPLIED );
        NameKey key = this.analyser.analyse(instance);
        assertEquals(-10, adjuster.score(0, instance, key));
    }

    @Test
    public void testScore4() {
        TaxonConceptInstance instance = this.createInstance("ID-1", NomenclaturalCode.ZOOLOGICAL, "Osphranter rufus", this.provider, TaxonomicType.MISAPPLIED );
        NameKey key = this.analyser.analyse(instance);
        assertEquals(90, adjuster.score(100, instance, key));
    }

    @Test
    public void testScore5() {
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider,"Acacia dealbata", "Link.", null, null, TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.DOMAIN,  RankType.DOMAIN.getRank(), null, null,null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertEquals(115, adjuster.score(100, instance, key));
    }

    @Test
    public void testScore6() {
        TaxonConceptInstance instance = new TaxonConceptInstance("ID-1", NomenclaturalCode.BOTANICAL, NomenclaturalCode.BOTANICAL.getAcronym(), this.provider,"Acacia dealbata", "Link.", null, null, TaxonomicType.MISAPPLIED, TaxonomicType.MISAPPLIED.getTerm(), RankType.DOMAIN,  RankType.DOMAIN.getRank(), null, null,null, null, null, null, null, null, null, null);
        NameKey key = this.analyser.analyse(instance);
        assertEquals(105, adjuster.score(100, instance, key));
    }
}

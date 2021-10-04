package au.org.ala.names.index;

import static au.org.ala.names.model.RankType.*;

import static org.gbif.checklistbank.model.Equality.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class RankComparatorTest {
    private RankComparator comparator;
    
    @Before
    public void setUp() throws Exception {
        this.comparator = new RankComparator();
    }

    @Test
    public void testCompare1() {
        assertEquals(EQUAL, this.comparator.compare(KINGDOM, KINGDOM));
        assertEquals(DIFFERENT, this.comparator.compare(KINGDOM, PHYLUM));
        assertEquals(DIFFERENT, this.comparator.compare(KINGDOM, CLASS));
        assertEquals(DIFFERENT, this.comparator.compare(KINGDOM, ORDER));
        assertEquals(DIFFERENT, this.comparator.compare(KINGDOM, FAMILY));
        assertEquals(DIFFERENT, this.comparator.compare(KINGDOM, GENUS));
        assertEquals(DIFFERENT, this.comparator.compare(KINGDOM, SPECIES));
        assertEquals(DIFFERENT, this.comparator.compare(KINGDOM, SUBSPECIES));
    }


    @Test
    public void testCompare2() {
        assertEquals(DIFFERENT, this.comparator.compare(PHYLUM, KINGDOM));
        assertEquals(EQUAL, this.comparator.compare(PHYLUM, PHYLUM));
        assertEquals(DIFFERENT, this.comparator.compare(PHYLUM, CLASS));
        assertEquals(DIFFERENT, this.comparator.compare(PHYLUM, ORDER));
        assertEquals(DIFFERENT, this.comparator.compare(PHYLUM, FAMILY));
        assertEquals(DIFFERENT, this.comparator.compare(PHYLUM, GENUS));
        assertEquals(DIFFERENT, this.comparator.compare(PHYLUM, SPECIES));
        assertEquals(DIFFERENT, this.comparator.compare(PHYLUM, SUBSPECIES));
    }

    @Test
    public void testCompare3() {
        assertEquals(DIFFERENT, this.comparator.compare(CLASS, KINGDOM));
        assertEquals(DIFFERENT, this.comparator.compare(CLASS, PHYLUM));
        assertEquals(EQUAL, this.comparator.compare(CLASS, CLASS));
        assertEquals(DIFFERENT, this.comparator.compare(CLASS, ORDER));
        assertEquals(DIFFERENT, this.comparator.compare(CLASS, FAMILY));
        assertEquals(DIFFERENT, this.comparator.compare(CLASS, GENUS));
        assertEquals(DIFFERENT, this.comparator.compare(CLASS, SPECIES));
        assertEquals(DIFFERENT, this.comparator.compare(CLASS, SUBSPECIES));
    }
    
    @Test
    public void testCompare4() {
        assertEquals(DIFFERENT, this.comparator.compare(ORDER, KINGDOM));
        assertEquals(DIFFERENT, this.comparator.compare(ORDER, PHYLUM));
        assertEquals(DIFFERENT, this.comparator.compare(ORDER, CLASS));
        assertEquals(EQUAL, this.comparator.compare(ORDER, ORDER));
        assertEquals(DIFFERENT, this.comparator.compare(ORDER, FAMILY));
        assertEquals(DIFFERENT, this.comparator.compare(ORDER, GENUS));
        assertEquals(DIFFERENT, this.comparator.compare(ORDER, SPECIES));
        assertEquals(DIFFERENT, this.comparator.compare(ORDER, SUBSPECIES));
    }
    
    @Test
    public void testCompare5() {
        assertEquals(DIFFERENT, this.comparator.compare(FAMILY, KINGDOM));
        assertEquals(DIFFERENT, this.comparator.compare(FAMILY, PHYLUM));
        assertEquals(DIFFERENT, this.comparator.compare(FAMILY, CLASS));
        assertEquals(DIFFERENT, this.comparator.compare(FAMILY, ORDER));
        assertEquals(EQUAL, this.comparator.compare(FAMILY, FAMILY));
        assertEquals(DIFFERENT, this.comparator.compare(FAMILY, GENUS));
        assertEquals(DIFFERENT, this.comparator.compare(FAMILY, SPECIES));
        assertEquals(DIFFERENT, this.comparator.compare(FAMILY, SUBSPECIES));
    }
    
    @Test
    public void testCompare6() {
        assertEquals(DIFFERENT, this.comparator.compare(GENUS, KINGDOM));
        assertEquals(DIFFERENT, this.comparator.compare(GENUS, PHYLUM));
        assertEquals(DIFFERENT, this.comparator.compare(GENUS, CLASS));
        assertEquals(DIFFERENT, this.comparator.compare(GENUS, ORDER));
        assertEquals(DIFFERENT, this.comparator.compare(GENUS, FAMILY));
        assertEquals(EQUAL, this.comparator.compare(GENUS, GENUS));
        assertEquals(DIFFERENT, this.comparator.compare(GENUS, SPECIES));
        assertEquals(DIFFERENT, this.comparator.compare(GENUS, SUBSPECIES));
    }
    
    @Test
    public void testCompare7() {
        assertEquals(DIFFERENT, this.comparator.compare(SPECIES, KINGDOM));
        assertEquals(DIFFERENT, this.comparator.compare(SPECIES, PHYLUM));
        assertEquals(DIFFERENT, this.comparator.compare(SPECIES, CLASS));
        assertEquals(DIFFERENT, this.comparator.compare(SPECIES, ORDER));
        assertEquals(DIFFERENT, this.comparator.compare(SPECIES, FAMILY));
        assertEquals(DIFFERENT, this.comparator.compare(SPECIES, GENUS));
        assertEquals(EQUAL, this.comparator.compare(SPECIES, SPECIES));
        assertEquals(DIFFERENT, this.comparator.compare(SPECIES, SUBSPECIES));
    }
    
    @Test
    public void testCompare8() {
        assertEquals(DIFFERENT, this.comparator.compare(SUBSPECIES, KINGDOM));
        assertEquals(DIFFERENT, this.comparator.compare(SUBSPECIES, PHYLUM));
        assertEquals(DIFFERENT, this.comparator.compare(SUBSPECIES, CLASS));
        assertEquals(DIFFERENT, this.comparator.compare(SUBSPECIES, ORDER));
        assertEquals(DIFFERENT, this.comparator.compare(SUBSPECIES, FAMILY));
        assertEquals(DIFFERENT, this.comparator.compare(SUBSPECIES, GENUS));
        assertEquals(DIFFERENT, this.comparator.compare(SUBSPECIES, SPECIES));
        assertEquals(EQUAL, this.comparator.compare(SUBSPECIES, SUBSPECIES));
    }

    @Test
    public void testCompare9() {
        assertEquals(DIFFERENT, this.comparator.compare(KINGDOM, SUBPHYLUM));
        assertEquals(DIFFERENT, this.comparator.compare(CLASS, INFRAGENUS));
        assertEquals(DIFFERENT, this.comparator.compare(SUBORDER, CLASS));
        assertEquals(DIFFERENT, this.comparator.compare(INFRAORDER, INFRAFAMILY));
        assertEquals(DIFFERENT, this.comparator.compare(SUBGENUS, SUBSECTION_ZOOLOGY));
        assertEquals(DIFFERENT, this.comparator.compare(INFRAFAMILY, SECTION_ZOOLOGY));
        assertEquals(DIFFERENT, this.comparator.compare(INFRAFAMILY, SECTION_BOTANY));
        assertEquals(DIFFERENT, this.comparator.compare(PARVORDER, SPECIES));
    }


    @Test
    public void testCompare10() {
        assertEquals(EQUAL, this.comparator.compare(INFRAKINGDOM, SUBPHYLUM));
        assertEquals(EQUAL, this.comparator.compare(SUPERCLASS, INFRACLASS));
        assertEquals(EQUAL, this.comparator.compare(SUBORDER, SUPERFAMILY));
        assertEquals(EQUAL, this.comparator.compare(ORDER, SUPERFAMILY));
        assertEquals(EQUAL, this.comparator.compare(INFRACLASS, ORDER));
        assertEquals(EQUAL, this.comparator.compare(SECTION_BOTANY, SERIES_BOTANY));
        assertEquals(EQUAL, this.comparator.compare(INFRAFAMILY, GENUS));
        assertEquals(EQUAL, this.comparator.compare(PARVORDER, SUBFAMILY));
    }

    @Test
    public void testCompare11() {
        assertEquals(EQUAL, this.comparator.compare(SUBSPECIES, CULTIVAR));
        assertEquals(EQUAL, this.comparator.compare(VARIETY, FORM));
        assertEquals(EQUAL, this.comparator.compare(SUBVARIETY, SUBSPECIES));
        assertEquals(EQUAL, this.comparator.compare(NOTHOSPECIES, SUBFORM));
    }

    @Test
    public void testCompare12() {
        assertEquals(EQUAL, this.comparator.compare(SUBSPECIES, INFORMAL));
        assertEquals(DIFFERENT, this.comparator.compare(INCERTAE_SEDIS, FORM));
        assertEquals(EQUAL, this.comparator.compare(UNRANKED, SUBSPECIES));
        assertEquals(DIFFERENT, this.comparator.compare(NOTHOSPECIES, SPECIES_INQUIRENDA));
    }


    @Test
    public void testCompare13() {
        assertEquals(EQUAL, this.comparator.compare(INCERTAE_SEDIS, INFORMAL));
        assertEquals(EQUAL, this.comparator.compare(UNRANKED, SPECIES_INQUIRENDA));
        assertEquals(EQUAL, this.comparator.compare(UNRANKED, INCERTAE_SEDIS));
        assertEquals(EQUAL, this.comparator.compare(UNRANKED, SPECIES_INQUIRENDA));
    }

}
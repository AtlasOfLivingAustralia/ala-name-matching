package au.org.ala.names.index;

import au.org.ala.names.model.RankType;
import org.gbif.checklistbank.model.Equality;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static au.org.ala.names.model.RankType.*;

/**
 * Compare two ranks and establish whether they are close enough to each other or different.
 */
public class RankComparator {
    private static final RankType[][] RANK_RANGES = new RankType[][] {
            new RankType[] { DOMAIN, DOMAIN, KINGDOM },
            new RankType[] { KINGDOM, DOMAIN, INFRAKINGDOM },
            new RankType[] { SUBKINGDOM, KINGDOM, SUPERPHYLUM },
            new RankType[] { INFRAKINGDOM, KINGDOM, SUPERPHYLUM },
            new RankType[] { SUPERPHYLUM, INFRAKINGDOM, INFRAPHYLUM },
            new RankType[] { PHYLUM, INFRAKINGDOM, SUPERCLASS },
            new RankType[] { SUBPHYLUM, INFRAKINGDOM, CLASS },
            new RankType[] { INFRAPHYLUM, PHYLUM, CLASS },
            new RankType[] { SUPERCLASS, INFRAPHYLUM, INFRACLASS },
            new RankType[] { CLASS, SUBPHYLUM, SUPERORDER },
            new RankType[] { SUBCLASS, INFRAPHYLUM, ORDER },
            new RankType[] { INFRACLASS, CLASS, ORDER },
            new RankType[] { SUBINFRACLASS, SUBCLASS, ORDER },
            new RankType[] { SUPERDIVISION_ZOOLOGY, SUBCLASS, ORDER },
            new RankType[] { DIVISION_ZOOLOGY, SUBCLASS, ORDER },
            new RankType[] { SUBDIVISION_ZOOLOGY, SUBCLASS, ORDER },
            new RankType[] { SUPERCOHORT, SUBCLASS, ORDER },
            new RankType[] { COHORT, SUBCLASS, ORDER },
            new RankType[] { SUBCOHORT, SUBCLASS, ORDER },
            new RankType[] { SUPERORDER, INFRACLASS, INFRAORDER },
            new RankType[] { ORDER, SUBCLASS, SUPERFAMILY },
            new RankType[] { SUBORDER, INFRACLASS, FAMILY },
            new RankType[] { INFRAORDER, ORDER, FAMILY },
            new RankType[] { PARVORDER, SUBORDER, FAMILY },
            new RankType[] { SUPERSERIES_ZOOLOGY, SUBORDER, FAMILY  },
            new RankType[] { SERIES_ZOOLOGY, SUBORDER, FAMILY  },
            new RankType[] { SUBSERIES_ZOOLOGY, SUBORDER, FAMILY  },
            new RankType[] { SUPERSECTION_ZOOLOGY, SUBORDER, FAMILY  },
            new RankType[] { SECTION_ZOOLOGY, SUBORDER, FAMILY  },
            new RankType[] { SUBSECTION_ZOOLOGY, SUBORDER, FAMILY  },
            new RankType[] { FAMILY, SUBORDER, INFRAFAMILY },
            new RankType[] { SUBFAMILY, INFRAORDER, GENUS },
            new RankType[] { INFRAFAMILY, FAMILY, GENUS },
            new RankType[] { SUPERTRIBE, SUBFAMILY, GENUS },
            new RankType[] { TRIBE, SUBFAMILY, GENUS },
            new RankType[] { SUBTRIBE, SUBFAMILY, GENUS },
            new RankType[] { SUPERGENUS, INFRAFAMILY, INFRAGENUS },
            new RankType[] { GENUS_GROUP, INFRAFAMILY, INFRAGENUS },
            new RankType[] { GENUS, INFRAFAMILY, SUPERSPECIES },
            new RankType[] { SUBGENUS, INFRAFAMILY, SUPERSPECIES },
            new RankType[] { INFRAGENUS, INFRAFAMILY, SUPERSPECIES },
            new RankType[] { SUPERSECTION_BOTANY, GENUS, SPECIES_SUBGROUP },
            new RankType[] { SECTION_BOTANY, GENUS, SPECIES_SUBGROUP },
            new RankType[] { SUBSECTION_BOTANY, GENUS, SPECIES_SUBGROUP },
            new RankType[] { SUPERSERIES_BOTANY, GENUS, SPECIES_SUBGROUP },
            new RankType[] { SERIES_BOTANY, SUBGENUS, SPECIES_SUBGROUP },
            new RankType[] { SUBSERIES_BOTANY, SUBGENUS, SPECIES_SUBGROUP },
            new RankType[] { INFRAGENERICNAME, SUBGENUS, SPECIES_SUBGROUP },
            new RankType[] { SPECIES_GROUP, SUBGENUS, SPECIES_SUBGROUP },
            new RankType[] { SUPERSPECIES, SUBGENUS, SPECIES_SUBGROUP },
            new RankType[] { SPECIES_SUBGROUP, SUBGENUS, SPECIES_SUBGROUP },
            new RankType[] { SPECIES, SPECIES, TELEOMORPH },
            new RankType[] { NOTHOSPECIES, SPECIES, SUBSPECIES },
            new RankType[] { HOLOMORPH, SPECIES, SUBSPECIES },
            new RankType[] { ANAMORPH, SPECIES, SUBSPECIES },
            new RankType[] { TELEOMORPH, SPECIES, SUBSPECIES },
            new RankType[] { SUBSPECIES, NOTHOSPECIES, SUPRAGENERICNAME },
            new RankType[] { NOTHOSUBSPECIES, NOTHOSPECIES, SUPRAGENERICNAME },
            new RankType[] { INFRASPECIFICNAME, NOTHOSPECIES, SUPRAGENERICNAME },
            new RankType[] { INFRASUBSPECIESNAME, NOTHOSPECIES, SUPRAGENERICNAME },
            new RankType[] { VARIETY, NOTHOSPECIES, SUPRAGENERICNAME },
            new RankType[] { NOTHOVARIETY, NOTHOSPECIES, SUPRAGENERICNAME },
            new RankType[] { SUBVARIETY, NOTHOSPECIES, SUPRAGENERICNAME },
            new RankType[] { FORM, NOTHOSPECIES, SUPRAGENERICNAME },
            new RankType[] { NOTHOFORM, NOTHOSPECIES, SUPRAGENERICNAME },
            new RankType[] { SUBFORM, NOTHOSPECIES, SUPRAGENERICNAME },
            new RankType[] { BIOVAR, NOTHOSPECIES, SUPRAGENERICNAME },
            new RankType[] { SEROVAR, NOTHOSPECIES, SUPRAGENERICNAME },
            new RankType[] { FORMASPECIALIS, NOTHOSPECIES, SUPRAGENERICNAME },
            new RankType[] { CULTIVARGROUP, NOTHOSPECIES, SUPRAGENERICNAME },
            new RankType[] { CULTIVAR, NOTHOSPECIES, SUPRAGENERICNAME },
            new RankType[] { PATHOVAR, NOTHOSPECIES, SUPRAGENERICNAME },
            new RankType[] { HYBRID, NOTHOSPECIES, SUPRAGENERICNAME },
            new RankType[] { SUPRAGENERICNAME, NOTHOSPECIES, SUPRAGENERICNAME }
    };

    private static final Map<RankType, RankType> UPPER_BOUND = Arrays.stream(RANK_RANGES).collect(Collectors.toMap(
            r -> r[0],
            r -> r[1]
    ));

    private static final Map<RankType, RankType> LOWER_BOUND = Arrays.stream(RANK_RANGES).collect(Collectors.toMap(
            r -> r[0],
            r -> r[2]
    ));

    /**
     * Compare two ranges and see if they are equivalent-ish.
     * <p>
     * Rank comparators allow a degree of slop between ranks, so that a
     * subclass and order or supergenus and family are considered close enough.
     * Incomparable ranks {@link RankType#INFORMAL} and the like are equal to each other
     * and not equal to any other rank.
     * </p>
     * @param rank1 The first rank
     * @param rank2 The second rank
     *
     * @return An equality statement
     */
    public Equality compare(RankType rank1, RankType rank2) {
        if (rank1 == rank2)
            return Equality.EQUAL;
        if (rank1.getId() <= 0 && rank2.getId() <= 0)
            return Equality.EQUAL;
        if (rank1 == UNRANKED || rank2 == UNRANKED || rank1 == INFORMAL || rank2 == INFORMAL)
            return Equality.EQUAL;
        if (rank1.getId() <= 0 && rank2.getId() <= 0)
            return Equality.UNKNOWN;
        RankType r1u = UPPER_BOUND.get(rank1);
        RankType r1l = LOWER_BOUND.get(rank1);
        if ((r1u != null && rank2.compareTo(r1u) >= 0) && (r1l != null && rank2.compareTo(r1l) <= 0))
            return Equality.EQUAL;
        RankType r2u = UPPER_BOUND.get(rank2);
        RankType r2l = LOWER_BOUND.get(rank2);
        if ((r2u != null && rank1.compareTo(r2u) >= 0) && (r2l != null && rank1.compareTo(r2l) <= 0))
            return Equality.EQUAL;
        return Equality.DIFFERENT;
    }

}

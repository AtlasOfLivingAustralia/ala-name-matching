package au.org.ala.names.index;

import au.org.ala.names.model.RankType;
import org.gbif.dwca.io.DwcaWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A scientific name.
 * <p>
 * Scientific names are unique across nomenclatural codes.
 * They can have numerous taxon concepts attached to them.
 * </p>
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright (c) 2017 CSIRO
 */
public class ScientificName extends Name implements Comparable<ScientificName> {
    /** The concepts that correspond to this name */
    private Map<NameKey, TaxonConcept> concepts;
    /** The principalprincipal taxon concept for this name */
    private TaxonConcept principal;

    public ScientificName(NameKey key) {
        super(key);
        this.concepts = new HashMap<NameKey, TaxonConcept>();
        this.principal = null;
    }

    /**
     * Add an instance to the list of concepts.
     *
     * @param instanceKey The name key for this instance
     * @param instance The instance
     *
     * @return The matched taxon concept
     */
    public TaxonConcept addInstance(NameKey instanceKey, TaxonConceptInstance instance) {
        TaxonConcept concept = this.concepts.get(instanceKey);
        if (concept == null) {
            concept = new TaxonConcept(this, instanceKey);
            this.concepts.put(instanceKey, concept);
        }
        concept.addInstance(instance);
        return concept;
    }

    /**
     * Find an instance of this name key with a specific provider.
     *
     * @param provider The provider
     * @return
     */
    public TaxonConceptInstance findInstance(NameProvider provider) {
        for (TaxonConcept tc: this.concepts.values()) {
            TaxonConceptInstance instance = tc.findInstance(provider);
            if (instance != null)
                return instance;
        }
        return null;
    }

    /**
     * Resolve the principal taxon concept.
     * <ul>
     *     <li>If there are no accepted taxon convepts, then there is no principal concept</li>
     *     <li>If the only accepted taxon concept is authorless, then that is the principal concept</li>
     *     <li>If there is a single, authored, accepted taxon concept, then that is the principal concept</li>
     *     <li>If there is more than one authored, accepted taxon concept then the one with the highest score is chosen and an issue logged</li>
     *     <li>If there are multiple accepted taxon concepts, then those concepts are resolved to the principal concept.</li>
     * </ul>
     * @param taxonomy
     */
    public void resolvePrincipal(Taxonomy taxonomy) {
        List<TaxonConcept> accepted = this.concepts.values().stream().filter(tc -> tc.isFormal() && tc.hasAccepted()).collect(Collectors.toList());
        List<TaxonConcept> authored = accepted.stream().filter(tc -> tc.isAuthored()).collect(Collectors.toList());
        this.principal = null;
        if (accepted.isEmpty()) {
            return;
        }
        if (accepted.size() == 1) {
            this.principal = accepted.get(0);
        } else if (authored.size() == 1) {
            this.principal = authored.get(0);
        } else if (authored.size() > 1) {
            taxonomy.reportError("Scientific name collision between " + authored.size() + " concepts: {} and {}", authored.get(0), authored.get(1));
            final int score = authored.stream().map(TaxonConcept::getRepresentative).mapToInt(TaxonConceptInstance::getScore).max().orElse(-1);
            List<TaxonConcept> candidates = authored.stream().map(TaxonConcept::getRepresentative).filter(tci -> tci.getScore() == score).map(TaxonConceptInstance::getTaxonConcept).collect(Collectors.toList());
            if (candidates.size() > 1)
                taxonomy.reportError("More than one possible best match for collision: {}, {}, choosing first", candidates.get(0), candidates.get(1));
            this.principal = candidates.get(0);
        }
        if (this.principal != null) {
            boolean owned = this.principal.isOwned();
            for (TaxonConcept tc : accepted)
                if (tc != this.principal && (owned || !tc.isAuthored()))
                    tc.addInferredSynonym(this.principal, taxonomy);
        }
    }

    /**
     * Scienmtific name comparison for sorting.
     * <p>
     * Names are ordered first by the rank of the principal taxon concept
     * and then ny name.
     * </p>
     *
     * @param o The other scientific name
     * @return
     */
    @Override
    public int compareTo(ScientificName o) {
        RankType r1 = this.principal == null ? null : this.principal.getRank();
        RankType r2 = o.principal == null ? null : o.principal.getRank();

        if (r1 == null && r2 != null)
            return 1;
        if (r1 != null && r2 == null)
            return -1;
        if (r1 != null && r2 != null && r1 != r2) {
            return r1.getSortOrder() - r2.getSortOrder();
        }
        return this.getKey().compareTo(o.getKey());
    }

    /**
     * Write out the name to a DwCA.
     * <p>
     * If we have a principal concept, write that first, then write out all the concepts.
     * </p>
     *
     * @param taxonomy The total taxonomy
     * @param writer The write to write to
     *
     * @throws IOException if unable to write to the archive
     */
    public void write(Taxonomy taxonomy, DwcaWriter writer) throws IOException {
        if (this.principal != null)
            this.principal.write(taxonomy, writer);
        for (TaxonConcept concept: this.concepts.values())
            if (concept != this.principal)
                concept.write(taxonomy, writer);
    }


    /**
     * A human readbale label for the concept
     *
     * @return The label
     */
    @Override
    public String getLabel() {
        return "SN[" + this.getKey().getScientificName() + "]";
    }

}

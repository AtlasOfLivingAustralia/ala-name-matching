package au.org.ala.names.index;

import au.org.ala.names.model.RankType;
import au.org.ala.names.util.DwcaWriter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A scientific name.
 * <p>
 * Scientific names are expected to be unique in a single nomenclatural code.
 * They can have numerous taxon concepts attached to them.
 * </p>
 * <p>
 * Different nomenclatural codes can have the same scientific name.
 * These are known as <em>homonyms</em>.
 * Generally, homonyms are kept separate during processing by code and during name matching
 * by using higher-order taxonomy, such as kingdom, to distingush between them.
 * </p>
 * <p>
 * In very rare cases, a code has ended up with intra-code homonyms where the same name appears
 * almost simultaneously and nobody has put in the effort to sort things out.
 * These cases are not handled correctly at the moment and tend to be squelched together.
 * </p>
 *
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright (c) 2017 CSIRO
 */
public class ScientificName extends Name<ScientificName, UnrankedScientificName, TaxonConcept> implements Comparable<ScientificName> {
    /**
     * Construct for a container and a key
     *
     * @param container
     * @param key
     */
    public ScientificName(UnrankedScientificName container, NameKey key) {
        super(container, key);
    }

    /**
     * Create a taxon concept for this instance key
     *
     * @param stageKey The instance key
     *
     * @return The new taxon concept
     */
    @Override
    TaxonConcept createConcept(NameKey stageKey) {
        return new TaxonConcept(this, stageKey);
    }

    /**
     * Create a key for this stage.
     *
     * @param instanceKey
     *
     * @return A key that corresponds to the keys used by the concept map
     */
    NameKey buildStageKey(NameKey instanceKey) {
        return instanceKey;
    }


    /**
     * Find an instance of this name key.
     * <p>
     * First try for an instance with a specific provider.
     * If that doesn't work then go for the name itself and hope that the principal resolution catches up with it.
     * </p>
     *
     * @param provider The provider
     * @return
     */
    public TaxonomicElement findElement(Taxonomy taxonomy, NameProvider provider) {
        TaxonConceptInstance instance;
        for (TaxonConcept tc: this.getConcepts()) {
            if ((instance = tc.findInstance(provider, true)) != null)
                return instance;
        }
        for (TaxonConcept tc: this.getConcepts()) {
            if ((instance = tc.findInstance(provider, false)) != null)
                return instance;
        }
        return this;
    }

    /**
     * Find something that might be good as a candidiate principal
     * <ul>
     *     <li>If there is a single taxon concept, then that is the principal.</li>
     *     <li>If the only accepted taxon concept is authorless, then that is the principal concept</li>
     *     <li>If there is a single, authored, accepted taxon concept, then that is the principal concept</li>
     *     <li>If there is more than one authored, accepted taxon concept then the one with the highest score is chosen and an issue logged</li>
     *     <li>If there are multiple accepted taxon concepts, then those concepts are resolved to the principal concept.</li>
     * </ul>
     * Note that accepted includes accpeted values with scores greater than zero.
     * This allows dodgy inferred accepted taxa to be ignored.
     *
     * @param taxonomy Where to The source taxonomy
     * @return
     */
    @Override
    protected TaxonConcept findPrincipal(Taxonomy taxonomy) {
        List<TaxonConcept> concepts = this.getConcepts();
        if (concepts.isEmpty())
            return null;
        if (concepts.size() == 1)
            return concepts.get(0);
        concepts.sort(REVERSE_PROVIDER_SCORE_COMPARATOR);
        final int cutoff = taxonomy.getAcceptedCutoff();
        List<TaxonConcept> accepted = concepts.stream().filter(tc -> tc.isFormal() && tc.hasAccepted() && tc.getPrincipalScore() > cutoff).collect(Collectors.toList());
        if (accepted.size() == 0)
            return concepts.get(0);
        if (accepted.size() == 1)
            return accepted.get(0);
        List<TaxonConcept> authored = accepted.stream().filter(tc -> tc.isAuthored()).collect(Collectors.toList());
        if (authored.size() == 0)
            return accepted.get(0);
        if (authored.size() == 1)
            return authored.get(0);
        taxonomy.report(IssueType.COLLISION, "scientificName.collision", this, authored.get(0), authored.get(1));
        final int score = authored.stream().mapToInt(TaxonConcept::getPrincipalScore).max().orElse(TaxonomicElement.MIN_SCORE);
        List<TaxonConcept> candidates = authored.stream().filter(tc -> tc.getPrincipalScore() == score).collect(Collectors.toList());
        if (candidates.size() > 1)
            taxonomy.report(IssueType.PROBLEM, "scientificName.collision.match", this, candidates.get(0), candidates.get(1));
        return candidates.get(0);
     }

    /**
     * Reallocate authorless or unowned taxon concepts to the principal and add inferred synonyms for authored taxon concepts.
     *
     * @param taxonomy The taxonomy
     * @param principal The principal concept
     */
    @Override
    protected void reallocateDanglingConcepts(Taxonomy taxonomy, TaxonConcept principal) {
        if (principal != null) {
            List<TaxonConcept> accepted = this.getConcepts().stream().filter(tc -> tc.isFormal() && tc.hasAccepted()).collect(Collectors.toList());
            List<TaxonConcept> reallocated = new ArrayList<>();
            boolean owned = principal.isOwned();
            for (TaxonConcept tc : accepted)
                if (tc != principal && !tc.isOwned() && (owned || !tc.isAuthored())) {
                    if (tc.isAuthored())
                        tc.addInferredSynonym(principal, taxonomy);
                    else {
                        principal.reallocate(tc, taxonomy);
                        reallocated.add(tc);
                    }
                }
            this.removeConcepts(reallocated);
            taxonomy.count("count.resolve.scientificName.principal");
        }
    }

    /**
     * Reallocate another scientific name to this name.
     * <p>
     * These are added to the principal scientific name
     * </p>
     *
     * @param element The element to reallocate
     * @param taxonomy The resolving taxonomy
     */
    @Override
    public void reallocate(ScientificName element, Taxonomy taxonomy) {
        TaxonConcept principal = this.getPrincipal();
        taxonomy.report(IssueType.NOTE, "scientificName.reallocated", element, this);
        taxonomy.count("count.reallocate.scientificName");
        if (principal == null)
            throw new IndexBuilderException("Unable to reallocate " + element + " to " + this + " without principal");
        for (TaxonConcept tc: element.getConcepts()) {
            principal.reallocate(tc, taxonomy);
        }
        element.clear(principal);
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
        TaxonConcept p1 = this.getPrincipal();
        TaxonConcept p2 = o.getPrincipal();
        RankType r1 = p1 == null ? null : p1.getRank();
        RankType r2 = p2 == null ? null : p2.getRank();

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
        boolean written = false;
        TaxonConcept principal = this.getPrincipal();

        if (principal != null && this.getConcepts().contains(principal)) {
            principal.write(taxonomy, writer);
            written = true;
        }
        for (TaxonConcept concept: this.getConcepts())
            if (concept != principal) {
                concept.write(taxonomy, writer);
                written = true;
            }
        if (written)
            taxonomy.count("count.write.scientificName");
    }

    /**
     * A human readbale label for the concept
     *
     * @return The label
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(64);
        builder.append("SN[");
        builder.append(this.getKey().getCode());
        builder.append(", ");
        builder.append(this.getKey().getScientificName());
        builder.append(", ");
        builder.append(this.getKey().getRank());
        if (this.getPrincipal() != null) {
            builder.append(", = ");
            builder.append(this.getPrincipal().getKey());
        }
        builder.append("]");
        return builder.toString();
    }
}

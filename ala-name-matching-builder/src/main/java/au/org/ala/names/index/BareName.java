/*
 * Copyright (c) 2021 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 */

package au.org.ala.names.index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A bare scientific name without code, authorship or rank information.
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class BareName extends Name<BareName, BareName, UnrankedScientificName> {
    /**
     * Construct a bare name.
     *
     * @param key The bare name key
     */
    public BareName(NameKey key) {
        super(key);
    }

    /**
     * Create an unranked scientific name
     *
     * @param stageKey The stage key
     *
     * @return The new unranked scientific name
     */
    @Override
    UnrankedScientificName createConcept(NameKey stageKey) {
        return new UnrankedScientificName(this, stageKey);
    }


    /**
     * Create a key for this stage.
     *
     * @param instanceKey
     *
     * @return A key that corresponds to the keys used by the concept map
     */
    NameKey buildStageKey(NameKey instanceKey) {
        return instanceKey.toUnrankedNameKey();
    }


    /**
     * If we have a principal, then any uncoded names are reallocated to the principal
     *
     * @param taxonomy The resolving taxonomy
     * @param principal The principal scientific name
     */
    @Override
    protected void reallocateDanglingConcepts(Taxonomy taxonomy, UnrankedScientificName principal) {
        if (principal == null)
            return;
        List<UnrankedScientificName> reallocated = new ArrayList<>();
        for (UnrankedScientificName name: this.getConcepts()) {
            if (name != principal && name.getKey().isUncoded()) {
                principal.reallocate(name, taxonomy, "uncodedScientificName.reallocated.provenance");
                reallocated.add(name);
            }
        }
        this.removeConcepts(reallocated);
        taxonomy.count("count.resolve.uncodedScientificName.principal");
    }

    /**
     * Find a principal unranked name.
     * <ul>
     *     <li>If there is only one uncoded name, then that is it.</li>
     *     <li>If there is only one coded scientific name, then that is it.</li>
     *     <li>If there is more than one coded scientific name, then choose the one with a principal with the highest score. (Or the first if there are multiple ones)</li>
     * </ul>
     * @param taxonomy The resolving taxonomy.
     *
     * @return
     */
    @Override
    protected UnrankedScientificName findPrincipal(Taxonomy taxonomy) {
        List<UnrankedScientificName> names = this.getConcepts();

        if (names.isEmpty())
            return null;
        if (names.size() == 1)
            return names.get(0);
        names.sort(REVERSE_PROVIDER_SCORE_COMPARATOR);
        final int cutoff = taxonomy.getAcceptedCutoff();
        List<UnrankedScientificName> coded = names.stream().filter(sn -> !sn.getKey().isUncoded() && sn.getPrincipal() != null && sn.getPrincipalScore() > cutoff).collect(Collectors.toList());
        if (coded.size() == 0)
            return names.get(0);
        if (coded.size() == 1) {
            return coded.get(0);
        }
        taxonomy.report(IssueType.COLLISION, "uncodedScientificName.collision", this, coded);
        final int score = coded.stream().mapToInt(UnrankedScientificName::getPrincipalScore).max().orElse(TaxonomicElement.MIN_SCORE);
        List<UnrankedScientificName> candidates = coded.stream().filter(sn -> sn.getPrincipalScore() == score).collect(Collectors.toList());
        if (candidates.size() > 1)
            taxonomy.report(IssueType.PROBLEM, "uncodedScientificName.collision.warn", this, candidates);
        return candidates.get(0);
    }


    /**
     * Reallocate the elements of another taxon concept to this taxon concept.
     *
     * @param element The element to reallocate
     * @param taxonomy The resolving taxonomy
     * @param reason The reason code
     */
    @Override
    public void reallocate(BareName element, Taxonomy taxonomy, String reason) {
        UnrankedScientificName principal = this.getPrincipal();
        taxonomy.report(IssueType.NOTE, "uncodedScientificName.reallocated", element, Arrays.asList(this));
        taxonomy.count("count.reallocate.uncodedScientificName");
        if (principal == null)
            throw new IndexBuilderException("Unable to reallocate " + element + " to " + this + " without principal");
        for (UnrankedScientificName name: element.getConcepts()) {
            principal.reallocate(name, taxonomy, reason);
        }
        element.clear(principal);
    }

    /**
     * A human readbale label for the concept
     *
     * @return The label
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(64);
        UnrankedScientificName principal = this.getPrincipal();
        TaxonConceptInstance representative = this.getRepresentative();
        builder.append("BN[");
        builder.append(this.getKey().getScientificName());
        if (principal != null) {
            builder.append(" = ");
            builder.append(principal.getKey());
        }
        if (representative != null) {
            builder.append(" = ");
            builder.append(representative.getLocator());
        }
        builder.append("]");
        return builder.toString();
    }

}

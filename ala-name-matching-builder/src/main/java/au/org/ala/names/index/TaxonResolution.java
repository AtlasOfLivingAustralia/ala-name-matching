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

import au.org.ala.names.model.RankType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The resolution of a taxon concept.
 * <p>
 * This is essentially a map of instances onto the other instances that
 * they resolve to.
 * However, there are a number of complicating cases and
 * </p>
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class TaxonResolution {
    /** Compare rank scores. The maximum rank is the highest rank, preferencing accepted taxa */
    private static Comparator<TaxonConceptInstance> RANK_ORDER =  new Comparator<TaxonConceptInstance>() {
        @Override
        public int compare(TaxonConceptInstance o1, TaxonConceptInstance o2) {
            if (o1.isPrimary() && !o2.isPrimary())
                return 1;
            if (!o1.isPrimary() && o2.isPrimary())
                return -1;
            if (o1.isAccepted() && !o2.isAccepted())
                return 1;
            if (!o1.isAccepted() && o2.isAccepted())
                return -1;
            return o1.getRank().getSortOrder() - o2.getRank().getSortOrder();
        }
    };

    /** The taxon resolution */
    private Map<TaxonConceptInstance, TaxonConceptInstance> resolution;
    /** The list of principal taxon concept instances, ordered by score */
    private List<TaxonConceptInstance> principal;
    /** All used taxon concepts with the principal list at the start and then additional resolutions added */
    private List<TaxonConceptInstance> used;
    /** The list of non-resplvable instances */
    private Set<TaxonConceptInstance> unresolved;
    /** The map of distributions */
    private Map<TaxonConceptInstance, List<Distribution>> distributions;

    /**
     * Construct with an initial list of principals.
     *
     * @param principal The principal list
     */
    public TaxonResolution(List<TaxonConceptInstance> principal) {
        this.principal = principal;
        this.resolution = new HashMap<>();
        this.used = new ArrayList<>(principal);
        this.unresolved= new HashSet<>();
        this.distributions = new HashMap<>();
    }

    /**
     * Empty constructor
     */
    public TaxonResolution() {
        this(new ArrayList<>());
    }

    /**
     * Get the resolution map.
     *
     * @return The map of resolutions.
     */
    public Map<TaxonConceptInstance, TaxonConceptInstance> getResolution() {
        return resolution;
    }

    /**
     * Get the list of principal instances.
     *
     * @return The list of principals
     */
    public List<TaxonConceptInstance> getPrincipal() {
        return principal;
    }

    /**
     * Get the list of used instances including non-principals.
     *
     * @return The list of used instances
     */
    public List<TaxonConceptInstance> getUsed() {
        return used;
    }

    /**
     * Get the set of unresolvable taxa.
     *
     * @return The unresolved taxa
     */
    public Set<TaxonConceptInstance> getUnresolved() {
        return unresolved;
    }

    /**
     * Get the distribution for an instance
     *
     * @param instance The instance
     * @return The distribution
     */
    public List<Distribution> getDistribution(TaxonConceptInstance instance) {
        return this.distributions.get(instance);
    }

    /**
     * Add a distribution map to the distributions
     *
     * @param instance The taxon instance
     * @param distribution The distribution (null for universal)
     */
    public void addDistribution(TaxonConceptInstance instance, List<Distribution> distribution) {
        this.distributions.put(instance, distribution);
    }

    /**
     * Add an internal resolution.
     * <p>
     * If the resolved instance is not part of the currently used list, then it is added to the used list.
     * </p>
     *
     * @param instance The instance
     * @param resolved The resolved instance
     * @param taxonomy The parent taxonomy
     *
     */
    public void addInternal(TaxonConceptInstance instance, TaxonConceptInstance resolved, Taxonomy taxonomy) {
        if (!this.used.contains(resolved)) {
            taxonomy.report(IssueType.NOTE,"taxonResolution.added", resolved, null);
            this.used.add(resolved);
        }
        this.resolution.put(instance, resolved);
    }


    /**
     * Add an external resolution.
     *
     * @param instance The instance
     * @param resolved The resolved instance
     * @param taxonomy The parent taxonomy
     *
     */
    public void addExternal(TaxonConceptInstance instance, TaxonConceptInstance resolved, Taxonomy taxonomy) {
        this.resolution.put(instance, resolved);
    }

    /**
     * Get the rank associated with this resolution.
     * <p>
     * Preferably, this is the highest accepted real rank.
     * </p>
     *
     * @return The highest rank associated with a principal.
     */
    public RankType getRank() {
        return this.principal.stream().max(RANK_ORDER).map(TaxonConceptInstance::getRank).orElse(null);
    }

    /**
     * Does this resolution have an accepted taxon?
     *
     * @return True if there is an accepted principal taxon
     */
    public boolean hasAccepted() {
        return this.principal.stream().anyMatch(TaxonConceptInstance::isAccepted);
    }

    /**
     * Get the resolution of an instance.
     *
     * @param instance The instance
     *
     * @return The matching resolution or null for no resolution
     */
    public TaxonConceptInstance getResolved(TaxonConceptInstance instance) {
        return this.resolution.get(instance);
    }

    /**
     * Get the children of a taxon concept instance
     *
     * @param instance The instance
     *
     * @return All the instances that resolve to the supplied instance
     */
    public List<TaxonConceptInstance> getChildren(TaxonConceptInstance instance) {
        return this.resolution.keySet().stream().filter(tci -> this.resolution.get(tci) == instance).collect(Collectors.toList());
    }

    /**
     * Validate the resolution.
     * <p>
     * Basically, everything in the instance list, along with everything in the used list needs to have a resolution.
     * </p>
     *
     * @param instances The list of instances
     * @param reporter The issue reporter
     *
     * @return True if the resolution is valid, false if there is a problem
     */
    public boolean validate(Collection<TaxonConceptInstance> instances, Reporter reporter) {
        boolean valid = true;
        for (TaxonConceptInstance tci: this.getUsed())
            valid = this.validate(tci, reporter) && valid; // Report all errors
        for (TaxonConceptInstance tci: this.getPrincipal())
            valid = this.validate(tci, reporter) && valid;
        for (TaxonConceptInstance tci: instances)
            valid = this.validate(tci, reporter) && valid;
        return valid;
    }

    /**
     * Validate a single taxon concept instance
     *
     * @param instance The instance
     * @param reporter The reporter
     *
     * @return True if the instance is resolved, false otherwise
     */
    protected boolean validate(TaxonConceptInstance instance, Reporter reporter) {
        boolean valid = this.getResolved(instance) != null;
        if (!valid)
            reporter.report(IssueType.ERROR, "taxonResolver.validation.noResolution", instance, null);
        return valid;
    }
}

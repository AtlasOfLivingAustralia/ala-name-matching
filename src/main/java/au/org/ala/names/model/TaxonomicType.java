package au.org.ala.names.model;

/**
 * Taxonomic types.
 * <p>
 * This gives the sd
 * </p>
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public enum TaxonomicType {
    /** A taxon explicitly placed in the taxonomic tree */
    ACCEPTED("accepted", TaxonomicTypeGroup.ACCEPTED, true, true, false, false),
    /** A taxon placed in the taxonomiuc tree by an algorithm */
    INFERRED_ACCEPTED("inferredAccepted", TaxonomicTypeGroup.ACCEPTED, true, true, false, false),
    /** A generic synonym */
    SYNONYM("synonym", TaxonomicTypeGroup.SYNONYM, true, false, true, false),
    /** A homotypic synonym (botany). A nomenclatural synonym, meaning that the same taxon has gone under a different name. This can occur when two people describe the same species. @see #OBJECTIVE_SYNONYM */
    HOMOTYPIC_SYNONYM("homotypicSynonym", TaxonomicTypeGroup.SYNONYM, true, false, true, false),
    /** An objective synonym (zoology). A nomenclatural synonym, meaning that the same taxon has gone under a different name. This can occur when two people describe the same species. @see #HOMOTYPIC_SYNONYM */
    OBJECTIVE_SYNONYM("objectiveSynonym", TaxonomicTypeGroup.SYNONYM, true, false, true, false),
    /** A heterotypic synonym (botany). A taxonomic synonym, meaning that a species that was originally considered to be separate has been lumped into another species. @see #SUBJECTIVE_SYNONYM  */
    HETEROTYPIC_SYNONYM("heterotypicSynonym", TaxonomicTypeGroup.SYNONYM, true, false, true, false),
    /** A subjective synonym (zoology). A taxonomic synonym, meaning that a species that was originally considered to be separate has been lumped into another species. A subjective synonym, since whether they are synonymns or not is a matter of opinion. @see #HETEROTYPIC_SYNONYM */
    SUBJECTIVE_SYNONYM("subjectiveSynonym", TaxonomicTypeGroup.SYNONYM, true, false, true, false),
    /** A synonym where part of an original taxon has been divided. This means that the original name may still be in use or may have been mapped onto several other taxa. */
    PRO_PARTE_SYNONYM("proParteSynonym", TaxonomicTypeGroup.SYNONYM, true, false, true, false),
    /** A name incorrectly applied in a publication to a different species. However, the name itself is perfectly valid and has its own taxon. */
    MISAPPLIED("misapplied", TaxonomicTypeGroup.MISAPPLIED,  false, false, true, false),
    /** A synonym inferred by an algorithm */
    INFERRED_SYNONYM("inferredSynonym", TaxonomicTypeGroup.SYNONYM, true, false, true, false),
    /** A name that shouldn't be used, since it refers to something not found in the region of the occurrence record. */
    EXCLUDED("excluded", TaxonomicTypeGroup.EXCLUDED, false, false, false, false),
    /** A taxon of uncertain placement */
    INCERTAE_SEDIS("incertaeSedis", TaxonomicTypeGroup.INCERTAE_SEDIS, false, true, false, true),
    /** A taxon of doubtful identity, requiring further investigation, but placed in the taxonomy */
    SPECIES_INQUIRENDA("speciesInquirenda", TaxonomicTypeGroup.INCERTAE_SEDIS, false, true, false, true),
    /** An unplaced taxon */
    UNPLACED("unplaced", TaxonomicTypeGroup.UNPLACED, false, false, false, true, "unknown"),
    /** An inferred unplaced taxon */
    INFERRED_UNPLACED("inferredUnplaced", TaxonomicTypeGroup.UNPLACED, false, false, false, true),
    /** An invalid taxon (usually needed to allow parents to be found */
    INVALID("invalid", TaxonomicTypeGroup.INVALID, false, false, false, false),
    /** An inferred invalid taxon (usually needed to allow parents to be found */
    INFERRED_INVALID("inferredInvalid", TaxonomicTypeGroup.INVALID, false, false, false, false),
    /** A dubious taxon */
    DOUBTFUL("doubtful", TaxonomicTypeGroup.DOUBTFUL, false, false, false, false),
    /** A name that occurs in other literature. */
    MISCELLANEOUS_LITERATURE("miscellaneousLiterature", TaxonomicTypeGroup.MISCELLANEOUS, false, false, false, false);

    /** The standardised term */
    private String term;
    /** The general group this type belongs to */
    private TaxonomicTypeGroup group;
    /** Is this a primary taxon type */
    private boolean primary;
    /** Is this an accepted taxon */
    private boolean accepted;
    /** Is this a synonym */
    private boolean synonym;
    /** Is this a placeholder of some sort */
    private boolean placeholder;
    /** Additional labels */
    private String[] labels;

    TaxonomicType(String term, TaxonomicTypeGroup group, boolean primary, boolean accepted, boolean synonym, boolean placeholder, String... labels) {
        this.term = term;
        this.group = group;
        this.primary = primary;
        this.accepted = accepted;
        this.synonym = synonym;
        this.placeholder = placeholder;
        this.labels = labels;
    }

    /**
     * Get the standard term for the type.
     *
     * @return The standard term
     */
    public String getTerm() {
        return term;
    }

    /**
     * Get the general group a taxonomic type is part of.
     *
     * @return The group
     */
    public TaxonomicTypeGroup getGroup() {
        return group;
    }

    /**
     * Is this a primary taxonomic type?
     * <p>
     * Primary taxa can be fitted into the resolved tree.
     * Non-primary taxa are ancillary taxa, such as misapplied names.
     * </p>
     *
     * @return True if this taxon is primary
     */
    public boolean isPrimary() {
        return primary;
    }

    /**
     * Does this term lead to an accepted taxon in the taxonomic tree
     *
     * @return True if accepted
     */
    public boolean isAccepted() {
        return accepted;
    }

    /**
     * Does this taxonomic type represent a synonym
     *
     * @return True if a synonym
     */
    public boolean isSynonym() {
        return synonym;
    }

    /**
     * Does this taxonomic type represent a placeholder
     *
     * @return True if a placeholder
     */
    public boolean isPlaceholder() {
        return placeholder;
    }

    /**
     * Get alternative labels
     *
     * @return Other terms that may describe the taxonomic type
     */
    public String[] getLabels() {
        return labels;
    }
}

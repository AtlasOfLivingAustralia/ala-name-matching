package au.org.ala.names.index.provider;

/**
 * How to handle differences of opinion about taxon concepts in terms of authorship, etc.
 * <p>
 * When resolving secondary taxon concepts, if there is an additional or filler non-primary concept,
 * then it can be reallocated to the primary concept.
 * </p>
 */
public enum ConceptResolutionPriority {
    /** This provider is an authoratative source of taxon concepts. */
    AUTHORATATIVE,
    /** This provider is an additional source of taxon concepts */
    ADDITIONAL,
    /** This provider is a provider of filler concepts */
    FILLER;
}

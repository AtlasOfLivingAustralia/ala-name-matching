package au.org.ala.checklist.lucene.model;

/**
 *
 * The error type that occurred at a lower level in the match
 * Only used for recursive matches.
 *
 * @author Natasha Carter
 */
public enum ErrorType {

    SPECIES_PLURAL("speciesPlural", "An spp. marker was identified."),
    INDETERMINATE_SPECIES("indeterminateSpecies", "An indeterminate marker was detected and and exact match could not be found."),
    QUESTION_SPECIES("questionSpecies", "An unsure identification of the species was made using a ? indcator."),
    AFFINITY_SPECIES("affinitySpecies","An aff. marker was detected in the original scientific name."),
    CONFER_SPECIES("conferSpecies","A cf. marker was detected in the original scientific name"),
    HOMONYM("homonym", "A homonym was detected."),
    GENERIC("genericError", "A generic SearchResultException was detected."),
    PARENT_CHILD_SYNONYM("parentChildSynonym", "The parent names has been detected as a synonym of the child"),
    EXCLUDED("excludedSpecies", "The species is excluded from the species list.  Ususally because it is not found in Australia"),
    ASSOCIATED_EXCLUDED("associatedNameExcluded", "There are 2 species names one is excluded and the other is not"),
    NONE("noIssue","No issue was detected");
    private String title;
    private String description;

    ErrorType(String title, String description) {

        this.title = title;
        this.description = description;
    }
    @Override
    public String toString(){
            return title;
    }
}

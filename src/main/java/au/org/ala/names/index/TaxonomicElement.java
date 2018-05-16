package au.org.ala.names.index;

import au.org.ala.names.model.RankType;
import org.gbif.api.vocabulary.NomenclaturalCode;

import java.util.Comparator;

/**
 * Some sort of taxonomic element.
 *
 * @param <T> The type of element
 * @param <C> The type of the containing element
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
abstract public class TaxonomicElement<T extends TaxonomicElement, C extends TaxonomicElement> {
    /** The maximum possible score a taxonomic element can have. Limited to avoid problems with arithmetic */
    public static final int MAX_SCORE = 1000000;
    /** The minimum possible score a taxonomic element can have. Limited to avoid problems with arithmetic */
    public static final int MIN_SCORE = -1000000;

    /** The provider score comparator, which also handles over/underflow */
    public static final Comparator<TaxonomicElement> PROVIDER_SCORE_COMPARATOR = new Comparator<TaxonomicElement>() {
        @Override
        public int compare(TaxonomicElement e1, TaxonomicElement e2) {
            if (e1 == null && e2 == null)
                return 0;
            if (e1 == null && e2 != null)
                return TaxonomicElement.MIN_SCORE;
            if (e1 != null && e2 == null)
                return TaxonomicElement.MAX_SCORE;
            int o1 = e1.getProviderScore();
            int o2 = e2.getProviderScore();
            try {
                return Math.subtractExact(o1, o2);
            } catch (Exception ex) {
                if (o1 > o2)
                    return TaxonomicElement.MAX_SCORE;
                if (o2 > o1)
                    return TaxonomicElement.MIN_SCORE;
                return 0; // Shouldn't need to return this
            }
        }
    };

    /** Places the highest scored provider first */
    public static final Comparator<TaxonomicElement> REVERSE_PROVIDER_SCORE_COMPARATOR = PROVIDER_SCORE_COMPARATOR.reversed();

    /** The principal score comparator, which also handles over/underflow */
    public static final Comparator<TaxonomicElement> PRINCIPAL_SCORE_COMPARATOR = new Comparator<TaxonomicElement>() {
        @Override
        public int compare(TaxonomicElement e1, TaxonomicElement e2) {
            if (e1 == null && e2 == null)
                return 0;
            if (e1 == null && e2 != null)
                return TaxonomicElement.MIN_SCORE;
            if (e1 != null && e2 == null)
                return TaxonomicElement.MAX_SCORE;
            int o1 = e1.getPrincipalScore();
            int o2 = e2.getPrincipalScore();
            try {
                return Math.subtractExact(o1, o2);
            } catch (Exception ex) {
                if (o1 > o2)
                    return TaxonomicElement.MAX_SCORE;
                if (o2 > o1)
                    return TaxonomicElement.MIN_SCORE;
                return 0; // Shouldn't need to return this
            }
        }
    };

    /** Places the highest scored principal first */
    public static final Comparator<TaxonomicElement> REVERSE_PRINCIPAL_SCORE_COMPARATOR = PRINCIPAL_SCORE_COMPARATOR.reversed();


    /** The containing element */
    private C container;

    /**
     * Create a taxonomic element without a container.
     */
    public TaxonomicElement() {
        this.container = null;
    }

    /**
     * Create a taxonomic element with a container
     *
     * @param container The container
     */
    public TaxonomicElement(C container) {
        this.container = container;
    }

    /**
     * Get the containing element of this element
     *
     * @return The container
     */
    public C getContainer() {
        return container;
    }

    /**
     * Set the container of this element
     *
     * @param container The new container
     */
    public void setContainer(C container) {
        this.container = container;
    }

    /**
     * Validate this element.
     * <p>
     * Validation errors are reported to the taxonomy
     * </p>
     *
     * @param taxonomy The taxonomy to validate against and report to
     *
     * @return False if the element is not valid, true otherwise
     *
     */
    abstract public boolean validate(Taxonomy taxonomy);

    /**
     * A taxon identifier for the element for logging purposes
     *
     * @return A human readable identifier for loggging
     */
    abstract public String getTaxonID();

    /**
     * An identifier for the element for logging purposes
     *
     * @return A human readable identifier for loggging
     */
    abstract public String getId();

    /**
     * The scientific name for the element for logging purposes
     *
     * @return A human readable name for loggging
     */
    abstract public String getScientificName();

    /**
     * The scientific name authorship for the element for logging purposes
     *
     * @return A human readable authorship for loggging
     */
    abstract public String getScientificNameAuthorship();

    /**
     * Get the rank of the taxon that this represents.
     * <p>
     * In some cases, this will be {@link RankType#UNRANKED}
     * </p>
     * @return The rank
     */
    abstract public RankType getRank();

    /**
     * Get the instance that represents this element.
     *
     * @return The representative instance
     */
    abstract public TaxonConceptInstance getRepresentative();

    /**
     * Get the score of the principal element.
     *
     * @return The principal score
     */
    abstract public int getPrincipalScore();

    /**
     * Get the score of the provider.
     *
     * @return The provider score
     */
    abstract public int getProviderScore();

    /**
     * Add an instance to the element.
     *
     * @param instanceKey The name key for the instance
     * @param instance The instance
     */
    abstract public <E extends TaxonomicElement> E addInstance(NameKey instanceKey, TaxonConceptInstance instance);

    /**
     * Reallocate an element to this element.
     * <p>
     * This is part of resolution, where some other concepts are found wanting and need to
     * be given a new home.
     * </p>
     *
     * @param element The element to reallocate
     * @param taxonomy The resolving taxonomy
     */
    abstract public void reallocate(T element, Taxonomy taxonomy);
}

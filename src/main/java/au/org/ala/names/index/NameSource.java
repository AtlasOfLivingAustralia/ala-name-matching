package au.org.ala.names.index;

import au.org.ala.vocab.ALATerm;
import org.apache.commons.collections.MapUtils;
import org.gbif.api.model.registry.Citation;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.vocabulary.Country;
import org.gbif.dwc.terms.*;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;

/**
 * An abstract source of names.
 * <p>
 * Subclasses can be used to load name information into a taxonomy.
 * </p>
 * <p>
 * Required and additional fields are derived from
 * https://tools.gbif.org/dwca-validator/extensions.do
 * </p>
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
abstract public class NameSource {
    /** Fields expected in the DwCA */
    protected static final List<Term> TAXON_REQUIRED = Arrays.asList(
            DwcTerm.taxonID,
            DwcTerm.parentNameUsageID,
            DwcTerm.acceptedNameUsageID,
            DwcTerm.nomenclaturalCode,
            DwcTerm.scientificName,
            DwcTerm.scientificNameAuthorship,
            DwcTerm.taxonomicStatus,
            DwcTerm.taxonRank,
            DwcTerm.datasetID
    );
    /** Optional fields from the DwCA */
    protected static final List<Term> TAXON_ADDITIONAL = Arrays.asList(
            DwcTerm.acceptedNameUsage,
            DwcTerm.parentNameUsage,
            DwcTerm.taxonConceptID,
            DwcTerm.scientificNameID,
            DwcTerm.nomenclaturalStatus,
            DwcTerm.kingdom,
            DwcTerm.phylum,
            DwcTerm.class_,
            DwcTerm.order,
            DwcTerm.family,
            DwcTerm.genus,
            DwcTerm.specificEpithet,
            DwcTerm.infraspecificEpithet,
            ALATerm.nameComplete,
            ALATerm.nameFormatted,
            DwcTerm.nameAccordingToID,
            DwcTerm.nameAccordingTo,
            DwcTerm.namePublishedInID,
            DwcTerm.namePublishedIn,
            DwcTerm.namePublishedInYear,
            DcTerm.source
    );
    /** Terms not to be included in taxon outputs */
    protected static final List<Term> TAXON_FORBIDDEN = Arrays.asList(
            DwcTerm.kingdom,
            DwcTerm.phylum,
            DwcTerm.class_,
            DwcTerm.order,
            DwcTerm.family,
            DwcTerm.genus,
            DwcTerm.subgenus,
            DwcTerm.specificEpithet,
            DwcTerm.infraspecificEpithet,
            ALATerm.subphylum,
            ALATerm.subclass,
            ALATerm.suborder,
            ALATerm.infraorder,
            ALATerm.kingdomID,
            ALATerm.phylumID,
            ALATerm.classID,
            ALATerm.orderID,
            ALATerm.familyID,
            ALATerm.genusID,
            ALATerm.speciesID

    );
    /** Fields expected for an identifier */
    protected static final List<Term> IDENTIFIER_REQUIRED = Arrays.asList(
            DcTerm.identifier,
            DwcTerm.datasetID
    );
    /** Fields optional for an identifier */
    protected static final List<Term> IDENTIFIER_ADDITIONAL = Arrays.asList(
            DcTerm.title,
            DcTerm.subject,
            DcTerm.format,
            ALATerm.status,
            DcTerm.source
    );
    /** Terms not to be included in identifier outputs */
    protected static final List<Term> IDENTIFIER_FORBIDDEN = Arrays.asList(
    );
    /** Fields expected for a verncular name */
    protected static final List<Term> VERNACULAR_REQUIRED = Arrays.asList(
            DwcTerm.vernacularName,
            DwcTerm.datasetID
    );
    /** Fields optional for an identifier */
    protected static final List<Term> VERNACULAR_ADDITIONAL = Arrays.asList(
            ALATerm.nameID,
            DcTerm.language,
            DcTerm.temporal,
            DwcTerm.locationID,
            DwcTerm.locality,
            DwcTerm.countryCode,
            DwcTerm.sex,
            DwcTerm.lifeStage,
            GbifTerm.isPlural,
            GbifTerm.isPreferredName,
            GbifTerm.organismPart,
            DwcTerm.taxonRemarks,
            ALATerm.status,
            DcTerm.source
    );
    /** Terms not to be included in vernacular outputs */
    protected static final List<Term> VERNACULAR_FORBIDDEN = Arrays.asList(
            DwcTerm.scientificName,
            DwcTerm.scientificNameAuthorship,
            DwcTerm.nomenclaturalCode,
            DwcTerm.taxonRank,
            DwcTerm.kingdom,
            DwcTerm.phylum,
            DwcTerm.class_,
            DwcTerm.order,
            DwcTerm.family,
            DwcTerm.genus,
            DwcTerm.specificEpithet,
            DwcTerm.infraspecificEpithet
    );
    /** Fields expected for a speices distribution */
    protected static final List<Term> DISTRIBUTION_REQUIRED = Arrays.asList(
            DwcTerm.datasetID
    );
    /** Fields optional for a speices distribution */
    protected static final List<Term> DISTRIBUTION_ADDITIONAL = Arrays.asList(
            DwcTerm.countryCode,
            DwcTerm.stateProvince,
            DwcTerm.locationID,
            DwcTerm.locality,
            DwcTerm.lifeStage,
            DwcTerm.occurrenceStatus,
            IucnTerm.threatStatus,
            DwcTerm.establishmentMeans,
            GbifTerm.appendixCITES,
            DwcTerm.eventDate,
            DwcTerm.startDayOfYear,
            DwcTerm.endDayOfYear,
            DwcTerm.occurrenceRemarks,
            DcTerm.source
    );
    /** Terms not to be included in identifier outputs */
    protected static final List<Term> DISTRIBUTION_FORBIDDEN = Arrays.asList(
    );

    /** Fields expected in the DwCA */
    protected static final List<Term> TAXON_VARIANT_REQUIRED = Arrays.asList(
            DwcTerm.taxonID,
            DwcTerm.nomenclaturalCode,
            DwcTerm.scientificName,
            DwcTerm.scientificNameAuthorship,
            DwcTerm.taxonomicStatus,
            DwcTerm.taxonRank,
            DwcTerm.datasetID,
            ALATerm.priority
    );
    /** Optional fields from the DwCA */
    protected static final List<Term> TAXON_VARIANT_ADDITIONAL = Arrays.asList(
            DwcTerm.taxonConceptID,
            DwcTerm.scientificNameID,
            DwcTerm.nomenclaturalStatus,
            ALATerm.nameComplete,
            ALATerm.nameFormatted,
            DwcTerm.nameAccordingToID,
            DwcTerm.nameAccordingTo,
            DwcTerm.namePublishedInID,
            DwcTerm.namePublishedIn,
            DwcTerm.namePublishedInYear,
            DcTerm.source,
            ALATerm.verbatimNomenclaturalCode,
            ALATerm.verbatimTaxonomicStatus,
            ALATerm.verbatimNomenclaturalStatus,
            DwcTerm.verbatimTaxonRank
    );

    protected static final Map<Term, List<Term>> TAXON_VARIANT_IMPLIED_TERMS = MapUtils.putAll(new HashMap<String, List<Term>>(),
            new Object[][] {
                    { DwcTerm.nomenclaturalCode, Arrays.asList(ALATerm.verbatimNomenclaturalCode) },
                    { DwcTerm.taxonomicStatus, Arrays.asList(ALATerm.verbatimTaxonomicStatus) },
                    { DwcTerm.nomenclaturalStatus, Arrays.asList(ALATerm.verbatimNomenclaturalStatus) }
            });

            /** Fields expected for a taxonomic issue */
    protected static final List<Term> TAXONOMIC_ISSUE_REQUIRED = Arrays.asList(
            DcTerm.type,
            DcTerm.subject,
            DcTerm.description,
            DcTerm.date,
            DwcTerm.taxonID,
            DwcTerm.scientificName,
            DwcTerm.scientificNameAuthorship,
            DwcTerm.associatedTaxa,
            DwcTerm.datasetID
    );

    /** Fields optional for a taxonomic issue */
    protected static final List<Term> TAXONOMIC_ISSUE_ADDITIONAL = Arrays.asList(
    );
    /** Fields expected for a taxonomic issue */
    protected static final List<Term> TAXONOMIC_ISSUE_FORBIDDEN = Arrays.asList(
    );


    /** A map of row types and what is needed to ensure that row is useful */
    protected static final Map<Term, List<Term>> REQUIRED_TERMS = MapUtils.putAll(new HashMap<String, Set<Term>>(),
            new Object[][] {
                    { DwcTerm.Taxon, TAXON_REQUIRED },
                    { GbifTerm.Identifier, IDENTIFIER_REQUIRED },
                    { GbifTerm.VernacularName, VERNACULAR_REQUIRED },
                    { GbifTerm.Distribution, DISTRIBUTION_REQUIRED },
                    { ALATerm.TaxonVariant, TAXON_VARIANT_REQUIRED },
                    { ALATerm.TaxonomicIssue, TAXONOMIC_ISSUE_REQUIRED }
            }
    );
    /** A map of row types and helpful additional terms */
    protected static final Map<Term, List<Term>> ADDITIONAL_TERMS = MapUtils.putAll(new HashMap<String, List<Term>>(),
            new Object[][] {
                    { DwcTerm.Taxon, TAXON_ADDITIONAL },
                    { GbifTerm.Identifier, IDENTIFIER_ADDITIONAL },
                    { GbifTerm.VernacularName, VERNACULAR_ADDITIONAL },
                    { GbifTerm.Distribution, DISTRIBUTION_ADDITIONAL },
                    { ALATerm.TaxonVariant, TAXON_VARIANT_ADDITIONAL },
                    { ALATerm.TaxonomicIssue, TAXONOMIC_ISSUE_ADDITIONAL }
            }
    );

    /** A map of row types and terms not to be included in outputsd */
    protected static final Map<Term, List<Term>> FORBIDDEN_TERMS = MapUtils.putAll(new HashMap<String, List<Term>>(),
            new Object[][] {
                    { DwcTerm.Taxon, TAXON_FORBIDDEN },
                    { GbifTerm.Identifier, IDENTIFIER_FORBIDDEN },
                    { GbifTerm.VernacularName, VERNACULAR_FORBIDDEN },
                    { GbifTerm.Distribution, DISTRIBUTION_FORBIDDEN },
                    { ALATerm.TaxonVariant, TAXON_FORBIDDEN },
                    { ALATerm.TaxonomicIssue, TAXONOMIC_ISSUE_FORBIDDEN },
            }
    );

    /** A map of row types and terms implied by the presence of other terms */
    protected static final Map<Term, Map<Term, List<Term>>> IMPLIED_TERMS = MapUtils.putAll(new HashMap<Term, Map<Term, List<Term>>>(),
            new Object[][] {
                    { ALATerm.TaxonVariant, TAXON_VARIANT_IMPLIED_TERMS }
            }
    );

    /** Only include the required/additional terms */
    protected static final Map<Term, Boolean> ONLY_INCLUDE_ALLOWED = MapUtils.putAll(new HashMap<String, Boolean>(),
            new Object[][] {
                    { DwcTerm.Taxon, false },
                    { GbifTerm.Identifier, false },
                    { GbifTerm.VernacularName, false },
                    { GbifTerm.Distribution, false },
                    { ALATerm.TaxonVariant, true },
                    { ALATerm.TaxonomicIssue, true }
            }
    );

    /**
     * Get a human-readable name for the source.
     *
     * @return The name
     */
    abstract public String getName();

    /**
     * Validate the source of information before loading
     *
     * @throws IndexBuilderException if the source is not valid
     */
    abstract public void validate() throws IndexBuilderException;

    /**
     * Load the information from this source into a taxonomy.
     *
     * @param taxonomy The taxonomy
     *
     * @throws IndexBuilderException if unable to load the data
     */
    abstract public void loadIntoTaxonomy(Taxonomy taxonomy) throws IndexBuilderException;

    /**
     * Get a citation for this source
     *
     * @return The citation
     *
     * @throws IndexBuilderException if unable to build the citation
     */
    abstract public Citation getCitation() throws IndexBuilderException;

    /**
     * Get a list of covered countries for this resource
     *
     * @return The country list
     *
     * @throws IndexBuilderException if unable to build the citation
     */
    abstract public Collection<Country> getCountries() throws IndexBuilderException;

    /**
     * Get a list of contacts for this resource
     *
     * @return The contact list
     *
     * @throws IndexBuilderException if unable to build the citation
     */
    abstract public Collection<Contact> getContacts() throws IndexBuilderException;

    /**
     * Create a name source
     * <p>
     * The name source can either be a simple CSV file or a directory containing
     * a Darwin Core Archive.
     * </p>
     *
     * @param f The file path
     *
     * @return A name source
     *
     * @throws IndexBuilderException if unable to create the name source
     */
    public static NameSource create(String f) throws IndexBuilderException {
        try {
            File nf = new File(f);
            NameSource ns;
            if (!nf.exists())
                throw new IndexBuilderException("Name source " + nf + " does not exist");
            if (nf.isDirectory())
                ns = new DwcaNameSource(nf);
            else
                ns = new CSVNameSource(nf.toPath(), "UTF-8", DwcTerm.Taxon);
            ns.validate();
            return ns;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}

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

import au.org.ala.vocab.ALATerm;
import com.opencsv.exceptions.CsvValidationException;
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
            DcTerm.source,
            ALATerm.doi,
            DwcTerm.taxonRemarks,
            DcTerm.provenance,
            ALATerm.taxonomicFlags
    );
    /** Terms not to be included in taxon outputs */
    protected static final List<Term> TAXON_FORBIDDEN = Arrays.asList(
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
            DcTerm.source,
            ALATerm.doi,
            DwcTerm.taxonRemarks,
            DcTerm.provenance
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
            ALATerm.doi,
            DcTerm.source,
            DcTerm.provenance
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
            DwcTerm.datasetID,
            DwcTerm.locationID
    );
    /** Fields optional for a speices distribution */
    protected static final List<Term> DISTRIBUTION_ADDITIONAL = Arrays.asList(
            DwcTerm.locality,
            DwcTerm.stateProvince,
            DwcTerm.country,
            DwcTerm.countryCode,
            DwcTerm.continent,
            DwcTerm.island,
            DwcTerm.islandGroup,
            DwcTerm.waterBody,
            DwcTerm.lifeStage,
            DwcTerm.occurrenceStatus,
            IucnTerm.threatStatus,
            DwcTerm.establishmentMeans,
            GbifTerm.appendixCITES,
            DwcTerm.eventDate,
            DwcTerm.startDayOfYear,
            DwcTerm.endDayOfYear,
            DwcTerm.occurrenceRemarks,
            DcTerm.source,
            ALATerm.doi,
            DcTerm.provenance
    );
    /** Terms not to be included in identifier outputs */
    protected static final List<Term> DISTRIBUTION_FORBIDDEN = Arrays.asList(
    );

    /** Fields expected for a location */
    protected static final List<Term> LOCATION_REQUIRED = Arrays.asList(
            DwcTerm.locationID,
            DwcTerm.locality
     );
    /** Fields optional for a speices distribution */
    protected static final List<Term> LOCATION_ADDITIONAL = Arrays.asList(
            ALATerm.parentLocationID,
            ALATerm.geographyType,
            DwcTerm.stateProvince,
            DwcTerm.country,
            DwcTerm.countryCode,
            DwcTerm.continent,
            DwcTerm.islandGroup,
            DwcTerm.island,
            DwcTerm.waterBody,
            DwcTerm.locationRemarks,
            DwcTerm.datasetID,
            DwcTerm.datasetName,
            DcTerm.source,
            ALATerm.doi,
            DcTerm.provenance
    );
    /** Terms not to be included in location outputs */
    protected static final List<Term> LOCATION_FORBIDDEN = Arrays.asList(
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
            ALATerm.doi,
            DwcTerm.taxonRemarks,
            DcTerm.provenance,
            ALATerm.verbatimNomenclaturalCode,
            ALATerm.verbatimTaxonomicStatus,
            ALATerm.verbatimNomenclaturalStatus,
            DwcTerm.verbatimTaxonRank,
            ALATerm.verbatimTaxonRemarks
    );

    protected static final Map<Term, List<Term>> TAXON_VARIANT_IMPLIED_TERMS = MapUtils.putAll(new HashMap<String, List<Term>>(),
            new Object[][] {
                    { DwcTerm.nomenclaturalCode, Arrays.asList(ALATerm.verbatimNomenclaturalCode) },
                    { DwcTerm.taxonomicStatus, Arrays.asList(ALATerm.verbatimTaxonomicStatus) },
                    { DwcTerm.nomenclaturalStatus, Arrays.asList(ALATerm.verbatimNomenclaturalStatus) },
                    { DwcTerm.taxonRemarks, Arrays.asList(ALATerm.verbatimTaxonRemarks) }
            });
    /** Fields expected for a reference */
    protected static final List<Term> REFERENCE_REQUIRED = Arrays.asList(
            DwcTerm.datasetID
    );
    /** Fields optional for a reference */
    protected static final List<Term> REFERENCE_ADDITIONAL = Arrays.asList(
            DcTerm.identifier,
            ALATerm.doi,
            DcTerm.bibliographicCitation,
            DcTerm.title,
            DcTerm.creator,
            DcTerm.date,
            DcTerm.source,
            DcTerm.description,
            DcTerm.subject,
            DcTerm.language,
            DcTerm.rights,
            DwcTerm.taxonRemarks,
            DcTerm.type
    );
    /** Terms not to be included in reference outputs */
    protected static final List<Term> REFERENCE_FORBIDDEN = Arrays.asList(
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

    /** Fields expected for a taxonomic issue */
    protected static final List<Term> TAXONOMIC_ISSUE_REQUIRED = Arrays.asList(
            DcTerm.type,
            DcTerm.subject,
            DcTerm.description,
            DcTerm.date,
            DwcTerm.taxonID,
            DwcTerm.scientificName,
            DwcTerm.scientificNameAuthorship,
            ALATerm.nameComplete,
            DwcTerm.nomenclaturalCode,
            DwcTerm.taxonRank,
            DwcTerm.taxonomicStatus,
            DwcTerm.associatedTaxa,
            DwcTerm.datasetID,
            ALATerm.value
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
                    { ALATerm.TaxonomicIssue, TAXONOMIC_ISSUE_REQUIRED },
                    { ALATerm.Location, LOCATION_REQUIRED },
                    { GbifTerm.Reference, REFERENCE_REQUIRED }
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
                    { ALATerm.TaxonomicIssue, TAXONOMIC_ISSUE_ADDITIONAL },
                    { ALATerm.Location, LOCATION_ADDITIONAL },
                    { GbifTerm.Reference, REFERENCE_ADDITIONAL }
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
                    { ALATerm.Location, LOCATION_FORBIDDEN },
                    { GbifTerm.Reference, REFERENCE_FORBIDDEN }
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
                    { ALATerm.TaxonomicIssue, true },
                    { ALATerm.Location, false },
                    { GbifTerm.Reference, false }
            }
    );

    /**
     * Get the type of core row returned by this source
     */
    abstract public Term getCoreType();

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
    public static NameSource create(File f) throws IndexBuilderException {
        try {
            NameSource ns;
            if (!f.exists())
                throw new IndexBuilderException("Name source " + f + " does not exist");
            if (f.isDirectory())
                ns = new DwcaNameSource(f);
            else
                ns = new CSVNameSource(f.toPath(), "UTF-8", DwcTerm.Taxon);
            ns.validate();
            return ns;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        } catch (CsvValidationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}

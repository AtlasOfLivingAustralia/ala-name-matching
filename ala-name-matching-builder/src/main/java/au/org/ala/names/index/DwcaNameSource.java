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
import au.org.ala.names.model.TaxonFlag;
import au.org.ala.names.model.TaxonomicType;
import au.org.ala.names.model.VernacularType;
import au.org.ala.vocab.ALATerm;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.gbif.api.model.registry.Citation;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.vocabulary.*;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwca.io.Archive;
import org.gbif.dwca.io.ArchiveFactory;
import org.gbif.dwca.io.ArchiveFile;
import org.gbif.dwca.io.MetadataException;
import org.gbif.dwca.record.Record;
import org.gbif.dwca.record.StarRecord;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A source of names from a Darwin Core Archive
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright (c) 2016 CSIRO
 */
public class DwcaNameSource extends NameSource {
    private Archive archive;

    /**
     * Create a name source for an archive
     *
     * @param archive The source archive
     */
    public DwcaNameSource(Archive archive) {
        this.archive = archive;
    }

    /**
     * Create a name source for an archive in a working directory
     *
     * @param archiveDir The source archive directory
     */
    public DwcaNameSource(File archiveDir) throws IOException {
        this(ArchiveFactory.openArchive(archiveDir));
    }

    /**
     * Get the name of the source
     *
     * @return The archive path
     */
    @Override
    public String getName() {
        try {
            return this.archive.getLocation().getCanonicalPath();
        } catch (IOException e) {
            return "Unreadable archive";
        }
    }

    /**
     * Get the type of core row this source represents
     *
     * @return The core type from the archive
     */
    @Override
    public Term getCoreType() {
        return this.archive.getCore().getRowType();
    }

    /**
     * Get a citation for this source.
     *
     * @return The citation
     */
    @Override
    public Citation getCitation()  {
        try {
            Dataset dataset = this.archive.getMetadata();
            if (dataset != null)
                return dataset.getCitation();
        } catch (MetadataException e) {
        }
        return new Citation(this.getName(), this.archive.toString());
    }

    /**
     * Get a list of countries for this resource
     *
     * @return The country list
     */
    @Override
    public Collection<Country> getCountries()  {
        try {
            Dataset dataset = this.archive.getMetadata();
            if (dataset != null)
                return dataset.getCountryCoverage();
        } catch (MetadataException e) {
        }
        return Collections.emptySet();
    }

    /**
     * Get a list of contacts for this resource
     *
     * @return The contacts list
     */
    @Override
    public Collection<Contact> getContacts()  {
        try {
            Dataset dataset = this.archive.getMetadata();
            if (dataset != null) {
                List<Contact> contacts = new ArrayList<>();
                for (Contact contact: dataset.getContacts()) {
                    if (contact.isPrimary() || contact.getType() == ContactType.ORIGINATOR) {
                        contact = (Contact) BeanUtils.cloneBean(contact);
                        contact.setPrimary(false);
                        contact.setType(ContactType.CONTENT_PROVIDER);
                        contacts.add(contact);
                    }
                }
                return contacts;
            }
        } catch (Exception e) {
        }
        return Collections.emptySet();
    }


    /**
     * Validate the archive.
     * <p>
     * Ensure all the expected terms are present.
     * </p>
     *
     * @throws IndexBuilderException if ther archive is not usable
     */
    public void validate() throws IndexBuilderException {
        this.checkStructure(this.archive.getCore());
        for (ArchiveFile ext: this.archive.getExtensions())
            this.checkStructure(ext);
    }

    /**
     * Ensure that an archive file has the structure we expect of a file of the supplied row type.
     *
     * @param af The archive file
     *
     * @throws IndexBuilderException if there is a problem with the file
     */
    protected void checkStructure(ArchiveFile af) throws IndexBuilderException {
        List<Term> required = REQUIRED_TERMS.get(af.getRowType());

        if (required != null) {
            for (Term term: required)
                if (!af.hasTerm(term))
                    throw new IndexBuilderException("File " + af.getTitle() + " from " + this.archive.getLocation() + " is of type " + af.getRowType() + " and is missing " + term);

        }
    }

    /**
     * Load this DwCA into a taxonomy.
     *
     * @param taxonomy The taxonomy
     *
     * @throws IndexBuilderException if unable to load a record into the taxonomy.
     */
    @Override
    public void loadIntoTaxonomy(Taxonomy taxonomy) throws IndexBuilderException {
        Term coreType = this.getCoreType();
        if (coreType == DwcTerm.Taxon)
            this.loadTaxonDwCA(taxonomy);
        else if (coreType == GbifTerm.VernacularName)
            this.loadNonTaxonDwCA(taxonomy, ALATerm.UnplacedVernacularName);
        else if (coreType == GbifTerm.Reference)
            this.loadNonTaxonDwCA(taxonomy, ALATerm.UnplacedReference);
        else if (coreType == ALATerm.Location)
            this.loadLocationDwCA(taxonomy);
        else {
            taxonomy.report(IssueType.PROBLEM, "taxonomy.load.rowType.discard", coreType.qualifiedName(), this.archive.getLocation().getPath());
        }
    }
    
    /**
     * Load a non-taxon DwCA.
     * <p>
     * This is often a DwCA with "free-floating" vernacular names or references, usually keyed to a scientific name or classification.
     * Unless they have a specific taxonID, they will, eventually, be linked to the most likely taxon.
     * </p>
     *
     * @param taxonomy The taxonomy
     * @param unplacedClass The class to use to indicate an unplaced element
     *
     * @throws IndexBuilderException if unable to load a record into the taxonomy.
     */
    public void loadNonTaxonDwCA(Taxonomy taxonomy, Term unplacedClass) throws IndexBuilderException {
        taxonomy.addOutputTerms(this.archive.getCore().getRowType(), this.archive.getCore().getTerms());
        String taxonID = null;
        Term type = null;
        for (ArchiveFile ext: archive.getExtensions())
            taxonomy.addOutputTerms(ext.getRowType(), ext.getTerms());
        try {
            for (StarRecord record : this.archive) {
                Record core = record.core();
                taxonID = core.value(DwcTerm.taxonID);
                type = core.rowType();
                if (taxonID == null) {
                    taxonID = UUID.randomUUID().toString();
                    type = unplacedClass;
                }
                List<Document> docs = new ArrayList<>();
                docs.add(this.makeDocument(taxonomy, type, core, taxonID));
                if (type != unplacedClass)
                    addPseudoTaxon(taxonomy, core, taxonID, null);
                for (List<Record> ext: record.extensions().values()) {
                    for (Record er: ext) {
                        docs.add(makeDocument(taxonomy, er.rowType(), er, taxonID));
                    }
                }
                taxonomy.addRecords(docs);
            }
        } catch (Exception ex) {
            throw new IndexBuilderException("Unable to load archive " + this.archive.getLocation() + " at taxon " + taxonID, ex);
        }
    }
    
    /**
     * Load ta taxon DwCA into a taxonomy.
     *
     * @param taxonomy The taxonomy
     *
     * @throws IndexBuilderException if unable to load a record into the taxonomy.
     */
    protected void loadTaxonDwCA(Taxonomy taxonomy) throws IndexBuilderException {
        Term coreType = this.getCoreType();
        String defaultDatasetName = null;
        int line = 0;
        try {
            defaultDatasetName = archive.getMetadata().getTitle();
        } catch (MetadataException e) {
            taxonomy.report(IssueType.PROBLEM, "provider.archive.noMetadata", (String) null, null);
        }
        if (coreType != DwcTerm.Taxon)
            throw new IndexBuilderException("Expecting a core row type of " + DwcTerm.Taxon);
        List<Term> classifiers = TaxonConceptInstance.CLASSIFICATION_FIELDS.stream().filter(t -> archive.getCore().hasTerm(t)).collect(Collectors.toList());
        taxonomy.addOutputTerms(coreType, archive.getCore().getTerms());
        taxonomy.addOutputTerms(ALATerm.TaxonVariant, archive.getCore().getTerms());
        String taxonID = null;
        for (ArchiveFile ext: archive.getExtensions())
            taxonomy.addOutputTerms(ext.getRowType(), ext.getTerms());
        try {
            for (StarRecord record : this.archive) {
                Record core = record.core();
                taxonID = core.value(DwcTerm.taxonID);
                String verbatimNomenclaturalCode = core.value(DwcTerm.nomenclaturalCode);
                String datasetName = core.value(DwcTerm.datasetName);
                if (datasetName == null)
                    datasetName = defaultDatasetName;
                NameProvider provider = taxonomy.resolveProvider(core.value(DwcTerm.datasetID), datasetName);
                NomenclaturalClassifier code = taxonomy.resolveCode(verbatimNomenclaturalCode);
                String scientificName = core.value(DwcTerm.scientificName);
                String scientificNameAuthorship = core.value(DwcTerm.scientificNameAuthorship);
                String nameComplete = core.value(ALATerm.nameComplete);
                if (code == null) {
                    code = provider.getDefaultNomenclaturalCode();
                    if (code == null && !provider.isLoose())
                        throw new IllegalStateException("No nomenclatural code for " + taxonID + " and code " + verbatimNomenclaturalCode);
                    if (code != null) {
                        taxonomy.report(IssueType.PROBLEM, "taxonomy.load.nullCode", taxonID, nameComplete, verbatimNomenclaturalCode);
                        taxonomy.count("count.load.problem");
                    }
                }
                String year = core.value(DwcTerm.namePublishedInYear);
                String verbatimTaxonomicStatus = core.value(DwcTerm.taxonomicStatus);
                TaxonomicType taxonomicStatus = taxonomy.resolveTaxonomicType(verbatimTaxonomicStatus);
                String verbatimTaxonRank = core.value(DwcTerm.taxonRank);
                RankType rank = taxonomy.resolveRank(verbatimTaxonRank);
                String verbatimNomenclaturalStatus = core.value(DwcTerm.nomenclaturalStatus);
                Set<NomenclaturalStatus> nomenclaturalStatus = taxonomy.resolveNomenclaturalStatus(verbatimNomenclaturalStatus);
                String parentNameUsage = core.value(DwcTerm.parentNameUsage);
                String parentNameUsageID = core.value(DwcTerm.parentNameUsageID);
                String acceptedNameUsage = core.value(DwcTerm.acceptedNameUsage);
                String acceptedNameUsageID = core.value(DwcTerm.acceptedNameUsageID);
                String verbatimTaxonRemarks = core.value(DwcTerm.taxonRemarks);
                String verbatimProvenance = core.value(DcTerm.provenance);
                String taxonomicFlags = core.value(ALATerm.taxonomicFlags);
                List<String> taxonRemarks = verbatimTaxonRemarks == null || verbatimTaxonRemarks.isEmpty() ? null : Arrays.stream(verbatimTaxonRemarks.split("\\|")).map(s -> s.trim()).collect(Collectors.toList());
                List<String> provenance = verbatimProvenance == null || verbatimProvenance.isEmpty() ? null : Arrays.stream(verbatimProvenance.split("\\|")).map(s -> s.trim()).collect(Collectors.toList());
                Set<TaxonFlag> flags = taxonomy.resolveTaxonomicFlags(taxonomicFlags);
                Map<Term, Optional<String>> classification = classifiers.stream().collect(Collectors.toMap(t -> t, t -> Optional.ofNullable(core.value(t))));
                List<Distribution> distribution = null;
                if (record.hasExtension(GbifTerm.Distribution)) {
                    distribution = record.extension(GbifTerm.Distribution).stream().map(r -> new Distribution(taxonomy, r)).collect(Collectors.toList());
                }
                TaxonConceptInstance instance = new TaxonConceptInstance(
                        taxonID,
                        code,
                        verbatimNomenclaturalCode,
                        provider,
                        scientificName,
                        scientificNameAuthorship,
                        nameComplete,
                        year,
                        taxonomicStatus,
                        verbatimTaxonomicStatus,
                        rank,
                        verbatimTaxonRank,
                        nomenclaturalStatus,
                        verbatimNomenclaturalStatus,
                        parentNameUsage,
                        parentNameUsageID,
                        acceptedNameUsage,
                        acceptedNameUsageID,
                        taxonRemarks,
                        verbatimTaxonRemarks,
                        provenance,
                        classification,
                        flags,
                        distribution);
                instance = taxonomy.addInstance(instance);

                List<Document> docs = new ArrayList<>();
                docs.add(this.makeDocument(taxonomy, core.rowType(), core, instance.getTaxonID()));
                for (List<Record> ext: record.extensions().values()) {
                    for (Record er: ext) {
                        if (this.acceptExtensionRecord(instance, er)) {
                            addPseudoTaxon(taxonomy, er, instance.getTaxonID(), code);
                            docs.add(makeDocument(taxonomy, er.rowType(), er, instance.getTaxonID()));
                        } else {
                            taxonomy.report(IssueType.NOTE, "taxonomy.load.extension.discard", taxonID, scientificName, er.rowType().simpleName());
                            taxonomy.count("count.load.extension." + er.rowType().simpleName() + ".discarded");
                        }
                    }
                }
                taxonomy.addRecords(docs);
                line++;
            }
        } catch (Exception ex) {
            throw new IndexBuilderException("Unable to load archive " + this.archive.getLocation() + " at taxon " + taxonID + " line " + line, ex);
        }
    }

    /**
     * Add a potential pseudo-taxon.
     * <p>
     * If this is a vernacular name with a type which is {@link VernacularType#isPseudoScientific()} then add
     * a pseudo-taxon, a not output synonym to the correct taxon.
     * </p>
     * @param taxonomy The taxonomy
     * @param record The source vernacular record
     * @param taxonID The identifier of the parent taxon
     * @param code The nomenclatural code (of the source taxon, generally)
     *
     * @throws Exception if unable to load the pseudo-taxon
     */
    private void addPseudoTaxon(Taxonomy taxonomy, Record record, String taxonID, NomenclaturalClassifier code) throws Exception {
        if (record.rowType().equals(GbifTerm.VernacularName)) {
            String vernacularName = record.value(DwcTerm.vernacularName);
            String status = record.value(ALATerm.status);
            VernacularType type = VernacularType.forTerm(status, VernacularType.COMMON);
            if (type.isPseudoScientific()) {
                NameProvider provider = taxonomy.resolveProvider(record.value(DwcTerm.datasetID), record.value(DwcTerm.datasetName));
                String nameID = UUID.randomUUID().toString();
                TaxonConceptInstance psuedo = new TaxonConceptInstance(
                        nameID,
                        code,
                        null,
                        provider,
                        vernacularName,
                        null,
                        null,
                        null,
                        TaxonomicType.PSEUDO_TAXON,
                        status,
                        RankType.UNRANKED,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        taxonID,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );
                taxonomy.addInstance(psuedo);
            }
        }
    }

    /**
     * Load a location DwCA.
     * <p>
     * This models the location information available
     * </p>
     *
     * @param taxonomy The taxonomy
     *
     * @throws IndexBuilderException if unable to load a record into the taxonomy.
     */
    public void loadLocationDwCA(Taxonomy taxonomy) throws IndexBuilderException {
        Term coreType = this.getCoreType();
        taxonomy.addOutputTerms(coreType, archive.getCore().getTerms());
        String taxonID = null;
        try {
            for (StarRecord record : this.archive) {
                Record core = record.core();
                String locationID = core.value(DwcTerm.locationID);
                String parentLocationID = core.value(ALATerm.parentLocationID);
                String locality = core.value(DwcTerm.locality);
                String geographyType = core.value(ALATerm.geographyType);
                Location location = new Location(locationID, parentLocationID, locality, parentLocationID);
                taxonomy.addLocation(location);
            }
        } catch (Exception ex) {
            throw new IndexBuilderException("Unable to load archive " + this.archive.getLocation() + " at taxon " + taxonID, ex);
        }
    }


    /**
     * Decide whether to include an extension record.
     * <p>
     * If the record is a distribution record then it is included in the taxon
     * concept instance and is discarded.
     * </p>
     *
     * @param instance The taxon concept instance
     * @param er The extension record
     *
     * @return True to include the record, false if the record is to be discarded.
     */
    protected boolean acceptExtensionRecord(TaxonConceptInstance instance, Record er) {
        if (er.rowType() == GbifTerm.Distribution) {
            return false;
        }
        return true;
    }

    /**
     * Convert a record into a lucene document
     *
     * @param taxonomy The target taxonomy
     * @param type The type of record
     * @param record The record
     * @param taxonID The actual, stored taxonID (used as a link)
     *
     * @return The record as a document
     */
    private Document makeDocument(Taxonomy taxonomy, Term type, Record record, String taxonID) {
        Document doc = new Document();
        doc.add(new StringField("type", type.qualifiedName(), Field.Store.YES));
        doc.add(new StringField("id", UUID.randomUUID().toString(), Field.Store.YES));
        doc.add(new StringField(Taxonomy.fieldName(DwcTerm.taxonID), taxonID, Field.Store.YES));
        for (Term term: record.terms()) {
            if (term == DwcTerm.taxonID)
                continue;
            String value = record.value(term);
            if (term != null && value != null && !value.isEmpty())
                doc.add(new StringField(Taxonomy.fieldName(term), value, Field.Store.YES));
        }
        return doc;
    }
}

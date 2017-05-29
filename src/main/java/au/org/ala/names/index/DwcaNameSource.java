package au.org.ala.names.index;

import au.org.ala.names.model.RankType;
import au.org.ala.names.model.SynonymType;
import org.apache.commons.collections.MapUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.gbif.api.vocabulary.NomenclaturalCode;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.dwc.record.Record;
import org.gbif.dwc.record.StarRecord;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.text.Archive;
import org.gbif.dwc.text.ArchiveFactory;
import org.gbif.dwc.text.ArchiveFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

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
        Set<Term> required = REQUIRED_TERMS.get(af.getRowType());

        if (required != null) {
            for (Term term: required)
                if (!af.hasTerm(term))
                    throw new IndexBuilderException("File " + af.getTitle() + " is of type " + af.getRowType() + " and is missing " + term);

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
        try {
            for (StarRecord record : this.archive) {
                Record core = record.core();
                String taxonID = core.value(DwcTerm.taxonID);
                NomenclaturalCode code = taxonomy.resolveCode(core.value(DwcTerm.nomenclaturalCode));
                NameProvider provider = taxonomy.resolveProvider(core.value(DwcTerm.datasetID), core.value(DwcTerm.datasetName));
                String scientificName = core.value(DwcTerm.scientificName);
                String scientificNameAuthorship = core.value(DwcTerm.scientificNameAuthorship);
                String year = core.value(DwcTerm.namePublishedInYear);
                TaxonomicStatus taxonomicStatus = taxonomy.resolveTaxonomicStatus(core.value(DwcTerm.taxonomicStatus));
                SynonymType synonymType = taxonomy.resolveSynonymType(core.value(DwcTerm.taxonomicStatus));
                RankType rank = taxonomy.resolveRank(core.value(DwcTerm.taxonRank));
                Set<NomenclaturalStatus> nomenclaturalStatus = taxonomy.resolveNomenclaturalStatus(core.value(DwcTerm.nomenclaturalStatus));
                String parentNameUsageID = core.value(DwcTerm.parentNameUsageID);
                String acceptedNameUsageID = core.value(DwcTerm.acceptedNameUsageID);
                TaxonConceptInstance instance = new TaxonConceptInstance(taxonID, code, provider, scientificName, scientificNameAuthorship, year, taxonomicStatus, synonymType, rank, nomenclaturalStatus, parentNameUsageID, acceptedNameUsageID);
                taxonomy.addInstance(instance);
            }
        } catch (IndexBuilderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IndexBuilderException("Unable to load archive " + this.archive.getLocation(), ex);
        }
    }
}

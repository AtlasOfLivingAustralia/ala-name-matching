package au.org.ala.names.util;

import au.org.ala.names.index.NameProvider;
import au.org.ala.names.index.TaxonConceptInstance;
import au.org.ala.names.model.RankType;
import au.org.ala.names.model.TaxonomicType;
import org.apache.commons.io.IOUtils;
import org.gbif.api.vocabulary.NomenclaturalCode;
import org.gbif.api.vocabulary.Rank;

import java.io.*;

/**
 * Handy test utilities.
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class TestUtils {
    /**
     * Load a class resource into a string.
     * <p>
     * Useful for comparing generated data against a file.
     * </p>
     * @param path The path relative to the class
     *
     * @return The resource as a string
     *
     * @throws IOException If unable to read the resource
     */
    public String loadResource(String path) throws IOException {
        StringWriter writer = new StringWriter(1024);
        try (Reader reader = this.resourceReader(path)) {
            IOUtils.copy(reader, writer);
        }
        return writer.toString();
    }

    public Reader resourceReader(String path) throws IOException {
        InputStream is = this.getClass().getResourceAsStream(path);
        return new InputStreamReader(is, "UTF-8");
    }

    public TaxonConceptInstance createInstance(String id, NomenclaturalCode code, String name, NameProvider provider) {
        return new TaxonConceptInstance(id, code, code.getAcronym(), provider, name, null, null, TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null,null, null, null, null, null);
    }

    public TaxonConceptInstance createInstance(String id, NomenclaturalCode code, String name, NameProvider provider, TaxonomicType taxonomicStatus) {
        return new TaxonConceptInstance(id, code, code.getAcronym(), provider, name, null, null, taxonomicStatus, taxonomicStatus.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null);
    }

}

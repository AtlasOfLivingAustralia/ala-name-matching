package au.org.ala.names.util;

import au.org.ala.names.index.NameProvider;
import au.org.ala.names.index.TaxonConceptInstance;
import au.org.ala.names.model.RankType;
import au.org.ala.names.model.TaxonomicType;
import org.apache.commons.io.IOUtils;
import org.gbif.api.vocabulary.NomenclaturalCode;
import org.gbif.api.vocabulary.Rank;

import java.io.*;
import java.util.regex.Pattern;

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
        return new TaxonConceptInstance(id, code, code.getAcronym(), provider, name, null, null, null, TaxonomicType.ACCEPTED, TaxonomicType.ACCEPTED.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null,null, null, null, null, null, null, null, null);
    }

    public TaxonConceptInstance createInstance(String id, NomenclaturalCode code, String name, NameProvider provider, TaxonomicType taxonomicStatus) {
        return new TaxonConceptInstance(id, code, code.getAcronym(), provider, name, null, null, null, taxonomicStatus, taxonomicStatus.getTerm(), RankType.SPECIES, RankType.SPECIES.getRank(), null, null, null, null, null, null, null, null, null, null);
    }

    public int rowCount(File file) throws IOException {
        if (!file.exists())
            return -1;
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        return (int) reader.lines().count();
    }

    public boolean fileContains(File file, String string) throws IOException {
        String line;

        if (!file.exists())
            return false;
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        while ((line = reader.readLine()) !=  null) {
            if (line.contains(string))
                return true;
        }
        return false;
    }

    public boolean fileContains(File file, Pattern pattern) throws IOException {
        String line;

        if (!file.exists())
            return false;
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        while ((line = reader.readLine()) !=  null) {
            if (pattern.matcher(line).matches())
                return true;
        }
        return false;
    }

    public void dumpFile(File file) throws IOException {
        String line;

        if (!file.exists())
            return;
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        while ((line = reader.readLine()) !=  null) {
            System.out.println(line);
        }
    }

}

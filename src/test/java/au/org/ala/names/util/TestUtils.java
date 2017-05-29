package au.org.ala.names.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

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
        Reader reader = this.resourceReader(path);
        StringBuffer sb = new StringBuffer(1024);
        char[] buffer = new char[1024];
        int n;

        while ((n = reader.read(buffer)) >= 0) {
            if (n == 0)
                Thread.yield();
            else {
                sb.append(buffer, 0, n);
            }
        }
        reader.close();
        return sb.toString();
    }

    public Reader resourceReader(String path) throws IOException {
        InputStream is = this.getClass().getResourceAsStream(path);
        return new InputStreamReader(is, "UTF-8");
    }

}

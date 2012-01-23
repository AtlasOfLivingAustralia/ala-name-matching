
package au.org.ala.data.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author car61w
 */
public class FileUtils {


  public static Set<String> streamToSet(InputStream source, Set<String> resultSet, boolean toLowerCase) throws IOException {
    LineIterator lines = getLineIterator(source,"UTF8");
    while (lines.hasNext()) {
      String line = lines.nextLine().trim();
      if(toLowerCase)
          line = line.toLowerCase();
      // ignore comments
      if (!ignore(line)) {
        resultSet.add(line);
      }
    }
    return resultSet;
  }

  private static boolean ignore(String line) {
    if (StringUtils.trimToNull(line) == null || line.startsWith("#")) {
      return true;
    }
    return false;
  }


    /**
   * @param source the source input stream
   * @param encoding the encoding used by the input stream
   * @return
   * @throws UnsupportedEncodingException
   */
  public static LineIterator getLineIterator(InputStream source, String encoding) {
    try {
      return new LineIterator(new BufferedReader(new InputStreamReader(source, encoding)));
    } catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException("Unsupported encoding" + encoding, e);
    }
  }


}

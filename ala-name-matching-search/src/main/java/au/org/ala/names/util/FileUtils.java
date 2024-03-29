/*
 * Copyright (C) 2014 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 */

package au.org.ala.names.util;

import java.io.*;
import java.util.Set;

import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;

/**
 * Some Generic file utilities.
 */
public class FileUtils {


    public static Set<String> streamToSet(InputStream source, Set<String> resultSet, boolean toLowerCase) throws IOException {
        LineIterator lines = getLineIterator(source, "UTF8");
        while (lines.hasNext()) {
            String line = lines.nextLine().trim();
            if (toLowerCase)
                line = line.toLowerCase();
            // ignore comments
            if (!ignore(line)) {
                resultSet.add(line);
            }
        }
        return resultSet;
    }

    private static boolean ignore(String line) {
        return StringUtils.trimToNull(line) == null || line.startsWith("#");
    }


    /**
     * @param source   the source input stream
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

    /**
     * Clear out a directory
     *
     * @param dir The directory to clear
     * @param clearTop Clear this directory as well.
     *
     * @throws IOException If there is a problem
     */
    public static void clear(File dir, boolean clearTop) throws IOException {
         if (dir.isDirectory()) {
             for (File f: dir.listFiles())
                 clear(f, true);
         }
         if (clearTop)
             dir.delete();

    }

    public static File mkTempDir(String prefix, String suffix, File dir) throws IOException {
        File temp = File.createTempFile(prefix, suffix, dir);
        temp.delete();
        temp.mkdirs();
        return temp;
    }

}

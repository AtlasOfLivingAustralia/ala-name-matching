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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.apache.commons.lang3.StringUtils;
import org.gbif.api.vocabulary.NomenclaturalCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Nomenclatural code classification.
 * <p>
 * Used instead of the GBIF NomenclaturalClassifier to allow navigation
 * of fuzzy code matching.
 * </p>
 */
@JsonSerialize(using = NomenclaturalClassifier.Serializer.class)
@JsonDeserialize(using = NomenclaturalClassifier.Deserializer.class)
public class NomenclaturalClassifier implements Comparable<NomenclaturalClassifier> {
    private static final Logger logger = LoggerFactory.getLogger(NomenclaturalClassifier.class);

    private String acronym;
    private String title;
    private URL source;
    private NomenclaturalClassifier parent;
    private NomenclaturalCode code;
    private Set<String> aliases;

    /** The list of instances */
    private final static List<NomenclaturalClassifier> CLASSIFIERS = buildInstances();
    /** The parsing map */
    private final static Map<String, NomenclaturalClassifier> NAMES = buildNames(CLASSIFIERS);
    /** The GBIF code map */
    private final static Map<NomenclaturalCode, NomenclaturalClassifier> CODES = CLASSIFIERS.stream()
            .filter(c -> c.getCode() != null)
            .collect(Collectors.toMap(
                    NomenclaturalClassifier::getCode,
                    c -> c)
            );

    /** The bacterial classifier */
    public static final NomenclaturalClassifier BACTERIAL = find(NomenclaturalCode.BACTERIAL);
    /** The botanical classifier */
    public static final NomenclaturalClassifier BOTANICAL = find(NomenclaturalCode.BOTANICAL);
    /** The cultivar classifier */
    public static final NomenclaturalClassifier CULTIVARS = find(NomenclaturalCode.CULTIVARS);
    /** The virus classifier */
    public static final NomenclaturalClassifier VIRUS = find(NomenclaturalCode.VIRUS);
    /** The zoological classifier */
    public static final NomenclaturalClassifier ZOOLOGICAL = find(NomenclaturalCode.ZOOLOGICAL);


    /**
     * Read the list of instances from a file
     *
     * @return The instance list
     */
    private static List<NomenclaturalClassifier> buildInstances() {
        try {
            Reader r = new InputStreamReader(NomenclaturalClassifier.class.getResourceAsStream("nomenclatural_classifiers.csv"), "UTF-8");
            CSVReader reader = new CSVReaderBuilder(r).withSkipLines(1).build();
            List<NomenclaturalClassifier> classifiers = new ArrayList<>();
            for (String[] row: reader) {
                String acronym = row[0];
                final String p = StringUtils.trimToNull(row[1]);
                NomenclaturalClassifier parent = p == null ? null : classifiers.stream()
                        .filter(c -> c.getAcronym().equals(p))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Can't find parent " + p));
                String title = row[2];
                String s = StringUtils.trimToNull(row[3]);
                URL source = s == null ? null : new URL(s);
                String c = StringUtils.trimToNull(row[4]);
                NomenclaturalCode code = c == null ? null : NomenclaturalCode.valueOf(c);
                Set<String> aliases = IntStream.range(5, row.length).mapToObj(i -> row[i]).collect(Collectors.toSet());
                classifiers.add(new NomenclaturalClassifier(acronym, parent, title, source, code, aliases));
            }
            return classifiers;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to read nomenclatural codes", ex);
        }
    }

    /**
     * Create a parsing map for the classifiers
     *
     * @param classifiers The list of classifiers
     *
     * @return The parsing map
     */
    private static Map<String, NomenclaturalClassifier> buildNames(List<NomenclaturalClassifier> classifiers) {
        Map<String, NomenclaturalClassifier> map = new HashMap<>();
        for (NomenclaturalClassifier classifier: classifiers) {
            map.put(classifier.getAcronym(), classifier);
            if (classifier.getCode() != null)
                map.put(classifier.getCode().name(), classifier);
            for (String alias: classifier.getAliases())
                map.put(alias, classifier);

        }
        return map;
    }

    /**
     * Find a nomenclatural classifier
     *
     * @param code The code
     *
     * @return The classifier, or null for not found
     */
    public static NomenclaturalClassifier find(String code) {
        return NAMES.get(code);
    }

    /**
     * Find a nomenclatural classifier
     *
     * @param code The code
     *
     * @return The classifier, or null for not found
     */
    public static NomenclaturalClassifier find(NomenclaturalCode code) {
        return CODES.get(code);
    }

    /**
     * Construct a new classifier
     *
     * @param acronym The standard acronym for the code
     * @param parent Any parent classification
     * @param title The full title of the classificartion
     * @param source A source URL
     * @param code The matchng GBIF nomenclatural code
     * @param aliases Any aliases
     */
    protected NomenclaturalClassifier(String acronym, NomenclaturalClassifier parent, String title, URL source, NomenclaturalCode code, Set<String> aliases) {
        this.acronym = acronym;
        this.parent = parent;
        this.title = title;
        this.source = source;
        this.code = code;
        this.aliases = aliases;
    }

    /**
     * Get the preferred code acronym.
     *
     * @return The code
     */
    public String getAcronym() {
        return acronym;
    }

    /**
     * Get the full title of the code.
     *
     * @return The title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Get the code source URL
     *
     * @return The source
     */
    public URL getSource() {
        return source;
    }

    /**
     * Get the parent code
     *
     * @return The parent code
     */
    public NomenclaturalClassifier getParent() {
        return parent;
    }

    /**
     * Get the GBIF nomenclatural code
     *
     * @return
     */
    public NomenclaturalCode getCode() {
        return code;
    }

    /**
     * Get any aliases that this code goes under
     *
     * @return
     */
    public Set<String> getAliases() {
        return aliases;
    }

    /**
     * Two classifiers are equal if they have the same acronym.
     *
     * @param o The other object
     *
     * @return True if equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NomenclaturalClassifier that = (NomenclaturalClassifier) o;
        return acronym.equals(that.acronym);
    }

    /**
     * Has based on acronym
     *
     * @return The has code
     */
    @Override
    public int hashCode() {
        return Objects.hash(acronym);
    }

    /**
     * String representation
     *
     * @return The acronym
     */
    @Override
    public String toString() {
        return this.acronym;
    }

    /**
     * Compare acronyms
     *
     * @param o The other classifier
     *
     * @return An ordewring based on the acrobym
     */
    @Override
    public int compareTo(NomenclaturalClassifier o) {
        return this.acronym.compareTo(o.acronym);
    }

    public static class Serializer extends StdSerializer<NomenclaturalClassifier> {
        public Serializer() {
            super(NomenclaturalClassifier.class);
        }

        @Override
        public void serialize(NomenclaturalClassifier value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            if (value != null) {
                gen.writeString(value.getAcronym());
            }
        }
    }

    public static class Deserializer extends StdDeserializer<NomenclaturalClassifier> {
        public Deserializer() {
            super(NomenclaturalClassifier.class);
        }

         @Override
        public NomenclaturalClassifier deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
             String code = p.getValueAsString();
             NomenclaturalClassifier classifier = NomenclaturalClassifier.find(code);
             if (classifier == null)
                 throw new IllegalArgumentException("Unrecognised nomenclatural code " + code);
             return classifier;
        }
    }
}

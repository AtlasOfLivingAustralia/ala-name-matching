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

package au.org.ala.names.search;

import au.org.ala.names.lucene.analyzer.LowerCaseKeywordAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexOrDocValuesQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.NumericUtils;
import org.apache.lucene.util.QueryBuilder;

/**
 * The type of field stored in the lucene index.
 * <p>
 * Used to determine how to store and search for a field.
 * </p>
 */
abstract public class FieldType<T> {
    protected static final ThreadLocal<Analyzer> ANALYZER = ThreadLocal.withInitial(
            () -> LowerCaseKeywordAnalyzer.newInstance()
    );
    protected static final ThreadLocal<QueryBuilder> QUERY_BUILDER = ThreadLocal.withInitial(
            () -> new QueryBuilder(ANALYZER.get())
    );
    protected static final ThreadLocal<org.apache.lucene.document.FieldType> TERM_FIELD_TYPE = ThreadLocal.withInitial(
            () -> {
                org.apache.lucene.document.FieldType ft = new org.apache.lucene.document.FieldType(TextField.TYPE_STORED);
                ft.setOmitNorms(true);
                return ft;
            }
    );

    /** The class of term stored */
    private Class<T> class_;
    /** The name of the field type */
    private String name;

    /**
     * Construct with a name
     *
     * @param name The name
     */
    public FieldType(Class<T> class_, String name) {
        this.class_ = class_;
        this.name = name;
    }

    /**
     * Store a searchable field into a lucene document.
     * <p>
     * This may involve storing multiple lucene fields for range types.
     * </p>
     *
     * @param value The value to store
     * @param name The name of the field
     * @param document The document to add the field to
     */
    abstract public void index(T value, String name, Document document);

    /**
     * Store a non-searchable field into a lucene document.
     * <p>
     * This may involve storing multiple lucene fields for range types.
     * </p>
     *
     * @param value The value to store
     * @param name The name of the field
     * @param document The document to add the field to
     */
    abstract public void store(T value, String name, Document document);

    /**
     * Generate a query for a field of this type.
     *
     * @param value The value to search for
     * @param name The field name
     * @return A query that searches for the value
     */
    abstract public Query search(T value, String name);

    /**
     * Search for a value in a range (inclusive).
     * <p>
     * By default, this throws a {@link UnsupportedOperationException}.
     * Types that have a concept of range can use this to implement a range search.
     * </p>
     *
     * @param lower The lower bound
     * @param upper The upper bound
     * @param name The field name
     *
     * @return A query based on the range
     */
    public Query searchRange(T lower, T upper, String name) {
        throw new UnsupportedOperationException("Field type " + this.name + " does not support ranges");
    }


    /**
     * Store-only field.
     */
        public static final FieldType<String> STORE = new FieldType<String>(String.class,"store") {
        @Override
        public void index(String value, String name, Document document) {
            document.add(new StoredField(name, value));
        }

        @Override
        public void store(String value, String name, Document document) {
            document.add(new StoredField(name, value));
        }

        @Override
        public Query search(String value, String name) {
            throw new UnsupportedOperationException("Store-only field");
        }
    };

    /**
     * An exact identifier.
     * <p>
     * Storage and search is accomplished via extact lookup.
     * </p>
     */
    public static final FieldType<String> IDENTIFIER = new FieldType<String>(String.class,"identifier") {
        @Override
        public void index(String value, String name, Document document) {
            document.add(new StringField(name, value, Field.Store.YES));
        }

        @Override
        public void store(String value, String name, Document document) {
            document.add(new StoredField(name, value));
        }

        @Override
        public Query search(String value, String name) {
            return new TermQuery(new Term(name, value));
        }
    };

    /**
     * A simple term.
     * <p>
     * Storage and search is accomplished via case-insensitive storage and lookup.
     * </p>
     */
    public static final FieldType<String> TERM = new FieldType<String>(String.class, "term") {
        @Override
        public void index(String value, String name, Document document) {
            Field field = new Field(name, value, TERM_FIELD_TYPE.get());
            document.add(field);
        }

        @Override
        public void store(String value, String name, Document document) {
            document.add(new StoredField(name, value));
        }

        @Override
        public Query search(String value, String name) {
            return QUERY_BUILDER.get().createPhraseQuery(name, value);
        }
    };

    /**
     * A tokenisable term.
     * <p>
     * Storage and search is accomplished via case-insensitive tokenisation and search
     * </p>
     */
    public static final FieldType<String> TEXT = new FieldType<String>(String.class, "text") {

        @Override
        public void index(String value, String name, Document document) {
            document.add(new TextField(name, value, Field.Store.YES));
        }

        @Override
        public void store(String value, String name, Document document) {
            document.add(new StoredField(name, value));
        }

        @Override
        public Query search(String value, String name) {
            return QUERY_BUILDER.get().createPhraseQuery(name, value);
        }
    };

    /**
     * A common name.
     * <p>
     * Storage and search is based on a simplified lookup where non alpha-numeric characters are removed
     * and made case insensitive.
     * </p>
     */
    public static final FieldType<String> COMMON = new FieldType<String>(String.class,"common") {
        @Override
        public void index(String value, String name, Document document) {
            value = value.toUpperCase().replaceAll("[^A-Z0-9ÏËÖÜÄÉÈČÁÀÆŒ]", "");
            document.add(new StringField(name, value, Field.Store.YES));
        }

        @Override
        public void store(String value, String name, Document document) {
            document.add(new StoredField(name, value));
        }

        @Override
        public Query search(String value, String name) {
            value = value.toUpperCase().replaceAll("[^A-Z0-9ÏËÖÜÄÉÈČÁÀÆŒ]", "");
            return new TermQuery(new Term(name, value));
        }
    };

    /**
     * An integer term.
     * <p>
     * Storage and search allow range-based queries.
     * </p>
     */
    public static final FieldType<Integer> INTEGER = new FieldType<Integer>(Integer.class, "integer") {
        @Override
        public void index(Integer value, String name, Document document) {
            if (value != null) {
                document.add(new NumericDocValuesField(name, value));
                document.add(new IntPoint(name, value));
            }
            document.add(new StoredField(name, value));
        }

        @Override
        public void store(Integer value, String name, Document document) {
            document.add(new StoredField(name, value));
        }

        @Override
        public Query search(Integer value, String name) {
            Query pq = IntPoint.newExactQuery(name, value);
            Query vq = NumericDocValuesField.newSlowExactQuery(name, value);
            return new IndexOrDocValuesQuery(pq, vq);
        }

        /**
         * Search for a value in a range (inclusive).
         *
         * @param lower The lower bound
         * @param upper The upper bound
         * @param name  The field name
         * @return A query based on the range
         */
        @Override
        public Query searchRange(Integer lower, Integer upper, String name) {
            Query pq = IntPoint.newRangeQuery(name, lower, upper);
            Query vq = NumericDocValuesField.newSlowRangeQuery(name, lower, upper);
            return new IndexOrDocValuesQuery(pq, vq);
        }
    };

    /**
     * A double term.
     * <p>
     * Storage and search allow range-based queries.
     * </p>
     */
    public static final FieldType<Double> DOUBLE = new FieldType<Double>(Double.class, "double") {
        @Override
        public void index(Double value, String name, Document document) {
            if (value != null) {
                document.add(new NumericDocValuesField(name, NumericUtils.doubleToSortableLong(value)));
                document.add(new DoublePoint(name, value));
            }
            document.add(new StoredField(name, value));
        }

        @Override
        public void store(Double value, String name, Document document) {
            document.add(new StoredField(name, value));
        }

        @Override
        public Query search(Double value, String name) {
            Query pq = DoublePoint.newExactQuery(name, value);
            Query vq = NumericDocValuesField.newSlowExactQuery(name, NumericUtils.doubleToSortableLong(value));
            return new IndexOrDocValuesQuery(pq, vq);
        }

        /**
         * Search for a value in a range (inclusive).
         *
         * @param lower The lower bound
         * @param upper The upper bound
         * @param name  The field name
         * @return A query based on the range
         */
        @Override
        public Query searchRange(Double lower, Double upper, String name) {
            Query pq = DoublePoint.newRangeQuery(name, lower, upper);
            Query vq = NumericDocValuesField.newSlowRangeQuery(name,
                    NumericUtils.doubleToSortableLong(lower),
                    NumericUtils.doubleToSortableLong(upper));
            return new IndexOrDocValuesQuery(pq, vq);
        }
    };

}

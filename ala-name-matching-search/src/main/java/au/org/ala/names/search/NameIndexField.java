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
package au.org.ala.names.search;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.WildcardQuery;

/**
 * An Enum for all the fields that are indexed for the name matching.
 *
 * @author Natasha Carter
 */
public enum NameIndexField {
    ID("id", FieldType.IDENTIFIER),
    GUID("guid", FieldType.IDENTIFIER),
    OTHER_GUID("otherGuid", FieldType.IDENTIFIER),
    LEFT("left", FieldType.INTEGER),
    RIGHT("right", FieldType.INTEGER),
    LSID("lsid", FieldType.IDENTIFIER),
    REAL_LSID("reallsid", FieldType.STORE),
    PARENT_ID("parent_id", FieldType.IDENTIFIER),
    DOCUMENT_TYPE("doctype", FieldType.IDENTIFIER),
    ACCEPTED("accepted_lsid", FieldType.IDENTIFIER),
    iS_SYNONYM("is_synonym", FieldType.IDENTIFIER),//whether or not the record is a synonym
    KINGDOM("kingdom", FieldType.TERM),
    KINGDOM_ID("kid", FieldType.STORE),
    PHYLUM("phylum", FieldType.TERM),
    PHYLUM_ID("pid", FieldType.STORE),
    CLASS("class", FieldType.TERM),
    CLASS_ID("cid", FieldType.STORE),
    ORDER("order", FieldType.TERM),
    ORDER_ID("oid", FieldType.STORE),
    FAMILY("family", FieldType.TERM),
    FAMILY_ID("fid", FieldType.STORE),
    GENUS("genus", FieldType.TERM),
    GENUS_ID("gid", FieldType.STORE),
    GENUS_EX("genus_ex", FieldType.TERM), //genus sounds like expression - handles masculine and feminine too.
    SPECIES("species", FieldType.TERM),
    SPECIES_ID("sid", FieldType.STORE),
    SPECIES_EX("specific_ex", FieldType.TERM),// specific epithet sounds like expression
    INFRA_EX("infra_ex", FieldType.TERM),//infra specific epithet sounds like expression
    SPECIFIC("specific", FieldType.TERM),
    INFRA_SPECIFIC("infra", FieldType.TERM),
    NAME("name", FieldType.TEXT),// search name
    OTHER_NAMES("other_names", FieldType.TEXT),// Alternative names
    NAME_CANONICAL("name_canonical", FieldType.TEXT), // Canonical name
    NAME_COMPLETE("name_complete", FieldType.TEXT), // Complete name
    SEARCHABLE_COMMON_NAME("common", FieldType.COMMON),
    COMMON_NAME("common_orig", FieldType.TEXT),
    CONCAT_NAME("concat_name", FieldType.TERM),
    RANK_ID("rank_id", FieldType.INTEGER),
    RANK("rank", FieldType.TERM),
    AUTHOR("author", FieldType.TEXT),
    PHRASE("phrase", FieldType.TEXT),//stores the values of a "phrase" name.  Some more intelligence will be needed when matching these
    VOUCHER("voucher", FieldType.TEXT), //stores a voucher value minus the spaces and fullstops.
    ALA("ala", FieldType.IDENTIFIER), //stores whether or not it is an ALA generated name
    DATASET_ID("dataset_id", FieldType.IDENTIFIER), // The source dataset
    SYNONYM_TYPE("syn_type", FieldType.IDENTIFIER), //stores the type of synonym that it represents
    HOMONYM("homonym", FieldType.IDENTIFIER),
    LANGUAGE("lang", FieldType.IDENTIFIER),
    /* Stores the priority score associated with a taxon */
    PRIORITY("priority", FieldType.INTEGER);

    /** The field name */
    String name;
    /** The field type */
    FieldType type;

    NameIndexField(String name, FieldType type) {
        this.name = name;
        this.type = type;
    }

    public String toString() {
        return name;
    }

    /**
     * Store a value into this field in a document
     *
     * @param value The value
     * @param document The document
     */
    public <T> void store(T value, Document document) {
        if (value != null && value instanceof String) {
            value = (T) StringUtils.trimToNull((String) value);
        }
        if (value == null)
            return;
        this.type.store(value, this.name, document);
    }


    /**
     * Store an indexed value into this field in a document
     *
     * @param value The value
     * @param document The document
     */
    public <T> void index(T value, Document document) {
        if (value != null && value instanceof String) {
            value = (T) StringUtils.trimToNull((String) value);
        }
        if (value == null)
            return;
        this.type.index(value, this.name, document);
    }

    /**
     * Make a query for this field for a value.
     *
     * @param value The value
     *
     * @return A matching query
     */
    public <T> Query search(T value) {
        return this.type.search(value, this.name);
    }

    /**
     * Make a range query for this field for a value.
     *
     * @param lower The lower value (inclusive)
     * @param upper The upper value (inclusive)
     *
     * @return A matching query
     */
    public <T> Query searchRange(T lower, T upper) {
        return this.type.searchRange(lower, upper, this.name);
    }


    /**
     * Make a wildcard query for this field for a value.
     *
     * @param value The value, including "*" for wildcards
     *
     * @return A matching query
     */
    public Query searchWildcard(String value) {
        return new WildcardQuery(new Term(this.name, value));
    }

}

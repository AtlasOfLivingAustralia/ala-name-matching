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

package au.org.ala.names.index.provider;

/**
 * How to match a name or author
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public enum NameMatchType {
    /** Exact match */
    EXACT,
    /** Case and space insensitive */
    INSENSITIVE,
    /** Normalised by GBIF name analysis @see org.gbif.checklistbank.utils.SciNameNormalizer @see org.gbif.checklistbank.authorship.AuthorComparator */
    NORMALISED,
    /** Reguilar expression match */
    REGEX
}

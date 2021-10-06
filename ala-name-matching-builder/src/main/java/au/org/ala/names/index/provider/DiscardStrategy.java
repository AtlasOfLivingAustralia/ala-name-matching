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
 * What do to with names that we are going to discard.
 * <p>
 * Forbidden names, and the like, may be gone from the taxonomy but things like the identifiers and names
 * might need to hang around.
 * This enum gives the various possible strategies.
 * </p>
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2018 Atlas of Living Australia
 */
public enum DiscardStrategy {
    /** Ignore the taxon and discard all information */
    IGNORE,
    /** Preserve the identifier as an identitier for the parent taxon */
    IDENTIFIER_TO_PARENT,
    /** Make the taxon a synonym of the parent taxon */
    SYNONYMISE_TO_PARENT
}

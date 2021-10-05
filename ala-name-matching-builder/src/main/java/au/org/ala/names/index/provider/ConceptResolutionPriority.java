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
 * How to handle differences of opinion about taxon concepts in terms of authorship, etc.
 * <p>
 * When resolving secondary taxon concepts, if there is an additional or filler non-primary concept,
 * then it can be reallocated to the primary concept.
 * </p>
 */
public enum ConceptResolutionPriority {
    /** This provider is an authoratative source of taxon concepts. */
    AUTHORATATIVE,
    /** This provider is an additional source of taxon concepts */
    ADDITIONAL,
    /** This provider is a provider of filler concepts */
    FILLER;
}

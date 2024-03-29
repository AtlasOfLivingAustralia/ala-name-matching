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

import au.org.ala.names.model.NameSearchResult;

import java.util.List;

/**
 * Exception that is thrown when the result is an unresolved
 * homonym
 *
 * @author Natasha
 */
public class HomonymException extends SearchResultException {
    public HomonymException(String msg, List<NameSearchResult> results) {
        this(msg);
        this.results = results;
    }

    public HomonymException(String message) {
        super("Warning an unresolved homonym has been detected. " + message);
        errorType = au.org.ala.names.model.ErrorType.HOMONYM;
    }

    public HomonymException() {
        this("Warning an unresolved homonym has been detected. ");
    }

    public HomonymException(List<NameSearchResult> results) {
        this();
        this.results = results;
    }

    public void setResults(List<NameSearchResult> results) {
        this.results = results;
    }
}

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

import au.org.ala.names.model.ErrorType;
import au.org.ala.names.model.NameSearchResult;

import java.util.List;

/**
 * The generic search result exception that can be thrown during a search.  This exception
 * will be used to wrap any exception that occurs that do not fall into the other categories.
 * @author Natasha
 * @see HomonymException
 */
public class SearchResultException extends Exception {
    protected List<NameSearchResult> results;
    protected ErrorType errorType;

    public SearchResultException(String msg) {
        super(msg);
        errorType = ErrorType.GENERIC;
    }

    public SearchResultException(String msg, List<NameSearchResult> results) {
        this(msg);
        this.results = results;
    }

    public List<NameSearchResult> getResults() {
        return results;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

}

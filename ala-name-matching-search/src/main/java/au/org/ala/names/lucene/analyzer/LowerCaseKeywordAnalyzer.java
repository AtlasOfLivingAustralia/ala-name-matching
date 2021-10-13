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
package au.org.ala.names.lucene.analyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordTokenizerFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * A custom KeywordAnalyzer that converts the text to lowercase before tokenizing
 * the complete string as one token
 *
 * @author Natasha
 */
public final class LowerCaseKeywordAnalyzer  {
    private static final Logger logger = LoggerFactory.getLogger(LowerCaseKeywordAnalyzer.class);

    /**
     * Get an instance of a lower-case keyword analyser.
     *
     * @return The analyser
     */
    public static Analyzer newInstance() {
        try {
            return CustomAnalyzer.builder().withTokenizer(KeywordTokenizerFactory.class).addTokenFilter(LowerCaseFilterFactory.class).build();
        } catch (IOException ex) {
            logger.error("Unable to build analyzer", ex);
            throw new IllegalStateException(ex);
        }
    }
 }

package au.org.ala.names.search;

import au.org.ala.names.model.*;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NameType;
import org.gbif.nameparser.PhraseNameParser;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;
public class APNIandConservationTests {
    private static ALANameSearcher searcher;

    @org.junit.BeforeClass
    public static void init() throws Exception {
        searcher = new ALANameSearcher("/data/lucene/namematching-20230725-3");
    }



}

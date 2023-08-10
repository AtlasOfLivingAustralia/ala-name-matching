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

public class AdditionalUnderstandingTests {
    private static ALANameSearcher searcher;

    @org.junit.BeforeClass
    public static void init() throws Exception {
        searcher = new ALANameSearcher("/data/lucene/namematching-20230329-2");
        //searcher = new ALANameSearcher("/data/lucene/namematching-20210811-5");
    }


    @Test
    public void testReturnedVernacular1()  {
        try {
            String name = "Chenonetta jubata";
            NameSearchResult nsr = searcher.searchForRecord(name);
            assertEquals("https://biodiversity.org.au/afd/taxa/31944e98-202b-4c14-b480-6508740dde58", nsr.getLsid());
            assertEquals("Australian Wood Duck", nsr.getVernacularName());
        } catch (MisappliedException ex) {

        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex);
        }
    }
    @Test
    public void testGenusHomonyms()  {
        try {
            String name = "Stylidium";
            NameSearchResult nsr = searcher.searchForRecord(name);
            assertEquals("https://id.biodiversity.org.au/taxon/apni/51684749", nsr.getLsid());

        } catch (MisappliedException ex) {

        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex);
        }
    }

    @Test
    public void testGenusWithKingdomHomonyms()  {
        try {
            String name = "Stylidium";
            LinnaeanRankClassification classification = new LinnaeanRankClassification();
            classification.setRank("genus");
            classification.setKingdom("Plantae");
            classification.setScientificName(name);
            NameSearchResult nsr = searcher.searchForRecord(classification, true, true, true);;

            assertEquals("https://id.biodiversity.org.au/taxon/apni/51684749", nsr.getLsid());

        } catch (MisappliedException ex) {

        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex);
        }
    }
    @Test
    public void testGenusWithKingdomHomonyms2()  {
        try {
            String name = "Galaxias";
            LinnaeanRankClassification classification = new LinnaeanRankClassification();
            classification.setKingdom("Animalia");
            //classification.setOrder("SALMONIFORMES");
            //classification.setPhylum("CHORDATA");
            classification.setFamily("GALAXIIDAE");
            classification.setGenus(name);
            classification.setScientificName(name);
            NameSearchResult nsr = searcher.searchForRecord(classification, true, true, true);;
        //    List<NameSearchResult> options = searcher.searchForRecords(name, RankType.GENUS,classification, 10,true, true );
            assertEquals("https://biodiversity.org.au/afd/taxa/cf9d20e7-5234-4ff3-94a2-23802357365f", nsr.getLsid());

        } catch (MisappliedException ex) {

        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex);
        }
    }

    @Test
    public void testGenusWithKingdomHomonyms3()  {
        try {
            String name = "Mitrella pulla";
            LinnaeanRankClassification classification = new LinnaeanRankClassification();
            //classification.setKingdom("Animalia");
            //classification.setOrder("SALMONIFORMES");
            //classification.setPhylum("CHORDATA");
            //classification.setFamily("GALAXIIDAE");
            //classification.setGenus(name);
            //classification.setSpecies(name);
            classification.setScientificName(name);
            NameSearchResult nsr = searcher.searchForRecord(classification, true, true, true);;
            //    List<NameSearchResult> options = searcher.searchForRecords(name, RankType.GENUS,classification, 10,true, true );
            assertEquals("https://biodiversity.org.au/afd/taxa/cf9d20e7-5234-4ff3-94a2-23802357365f", nsr.getLsid());

        } catch (MisappliedException ex) {

        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex);
        }
    }

    @Test
    public void testSynonymLocal()  {
        try {
            String name = "Portmacquaria chimaira";
            LinnaeanRankClassification classification = new LinnaeanRankClassification();
            //classification.setKingdom("Animalia");
            //classification.setOrder("SALMONIFORMES");
            //classification.setPhylum("CHORDATA");
            //classification.setFamily("GALAXIIDAE");
           // classification.setGenus(name);
            classification.setScientificName(name);
            NameSearchResult nsr = searcher.searchForRecord(classification, true, true, true);;
            //    List<NameSearchResult> options = searcher.searchForRecords(name, RankType.GENUS,classification, 10,true, true );
            assertEquals("https://biodiversity.org.au/afd/taxa/cf9d20e7-5234-4ff3-94a2-23802357365f", nsr.getLsid());

        } catch (MisappliedException ex) {

        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex);
        }
    }

    @Test
    public void testForMatch(){
        try{
            String name ="Acanthocarpus";
            LinnaeanRankClassification classification = new LinnaeanRankClassification();
            classification.setScientificName(name);
            //classification.setFamily(Ana);
            NameSearchResult nsr = searcher.searchForRecord(classification, true, true, true);;
            //    List<NameSearchResult> options = searcher.searchForRecords(name, RankType.GENUS,classification, 10,true, true );
            assertEquals("https://biodiversity.org.au/afd/taxa/cf9d20e7-5234-4ff3-94a2-23802357365f", nsr.getLsid());

        } catch (MisappliedException ex) {

        } catch (SearchResultException ex) {
            fail("Unexpected search exception " + ex);

        }

    }
}

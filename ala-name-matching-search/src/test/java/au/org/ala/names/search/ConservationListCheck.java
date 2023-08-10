package au.org.ala.names.search;


import au.org.ala.names.model.LinnaeanRankClassification;
import au.org.ala.names.model.NameSearchResult;
import au.org.ala.names.model.RankType;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.CSVWriter;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/*
Test case to generate report on whether conservation and sensitive species are represented in the index.

 */

class ReporterStruct{

    public String scientificName;
    public String sourceLists;
    public boolean existsInCombined;
    public boolean existsInReduced;
    public String reportedLSID = "";
    public String errorMessageInCombined = "";
    public String errorMessageInReduced = "";

    public String[] serialise()
    {
        String[] result = { this.scientificName, this.sourceLists, String.valueOf(this.existsInCombined),String.valueOf(this.existsInReduced),  this.reportedLSID, this.errorMessageInCombined, this.errorMessageInReduced};
        return result;
    }

    public static String[] header = {"scientificName", "sourceLists", "existsInCombined","existsInReduced", "reportedLSID", "errorMessageInCombined", "errorMessageInReduced"};

}

public class ConservationListCheck {

    static ALANameSearcher searcher;
    static ALANameSearcher searcher_reduced;
    @org.junit.BeforeClass
    public static void init() throws Exception {
       searcher = new ALANameSearcher("/data/lucene/namematching-20230329-2");
       searcher_reduced = new ALANameSearcher("/data/lucene/namematching-20230329-NoNZOR");
    }

    @Test
    // Based on Iconic Species Test code.
    public void testConservationSpeciesFile() {
        try {
            CSVParser csvParser = new CSVParserBuilder()
                    .withSeparator(',')
                    .withQuoteChar('"')
                    .withEscapeChar('\\')
                    .build();
            CSVReader reader = new CSVReaderBuilder(new InputStreamReader(this.getClass().getResourceAsStream("conservation-species.csv")))
                    .withCSVParser(csvParser)
                    .withSkipLines(1)
                    .build();
            String[] values;
            int passed = 0, failed = 0;
            // reporter
            // list scientific name, index LSID (if found), Common Name, source lists
            List<ReporterStruct> resultReports = new ArrayList<>();
            values = reader.readNext(); // Read header
            while ((values = reader.readNext()) != null) {
                if (values.length >= 3 && !values[0].equals("scientificName")) { // there's an error with reading the row - TODO Flag this row
                    //System.out.println("Processing " + values.length + " : " + values[0]);
                    String scientificName = values[0];
                    String commonName = StringUtils.trimToNull(values[1]);
                    String sourceLists = StringUtils.trimToNull(values[2]);

                    //construct reporter
                    ReporterStruct rowReporter = new ReporterStruct();
                    rowReporter.scientificName = scientificName;
                    rowReporter.sourceLists = "";
                    rowReporter.existsInCombined = false; // assume the worst
                    rowReporter.existsInReduced = false;
                    //attempt to locate the genus species binomial or genus species subspecies trinomial
                    String search = scientificName;
                    //create the search classification
//                    LinnaeanRankClassification classification = new LinnaeanRankClassification(kingdom, phylum, clazz, order, family, genus, search);
//                    classification.setSpecies(search);
//
//                    if (StringUtils.trimToNull(subspecies) != null) {
//                        search += " " + subspecies;
//                        classification.setScientificName(search);
//                    }

                    try {
                        NameSearchResult result = null;
                        NameSearchResult resultFromReduced = null;
                        try {
                            result = searcher.searchForRecord(search,  null, false);
                            } catch (MisappliedException e)
                            {
                            result = e.getMatchedResult();
                            }
                        try {
                            resultFromReduced = searcher_reduced.searchForRecord(search,  null, false);
                            } catch (MisappliedException e)
                            {
                            resultFromReduced = e.getMatchedResult();
                            }

                        //assertNotNull(search + " could not be found" ,guid);
                        if (result == null) {
                            //System.err.println(search + "(" + scientificName + ") could not be found in index");
                           //
                            // rowReporter.errorMessage = scientificName + " could not be found in index";
                            rowReporter.errorMessageInCombined = "Could not be found in index";
                            failed++;

                        } else {

//                            if (result.getLsid().contains("catalogue")) {
//                                System.err.println(search + " (" + scientificName + ") has a CoL LSID");
//                                rowReporter.errorMessageInCombined = scientificName + " has a CoL LSID";
//                            }

/*                            if (result.isSynonym())
                                result = searcher.searchForRecordByLsid(result.getAcceptedLsid());*/
                            //test to see if the classification matches
/*                            if (!classification.hasIdenticalClassification(result.getRankClassification(), RankType.GENUS) && result.getRankClassification().getGenus() != null) {

                                failed++;
                                //System.err.println(search + "("+commonName+") classification: "+ classification + " does not match " + result.getRankClassification());
                                System.err.println(search + "(" + commonName + ") classifications do not match");

                                printDiff(classification, result.getRankClassification(), result.getLsid(), true);
                            } else*/
                            passed++;
                            rowReporter.reportedLSID = result.getLsid();
                            rowReporter.existsInCombined = true;
                        }
                        if (resultFromReduced == null){
                            rowReporter.errorMessageInReduced = "Could not be found in index";
                            failed++;
                        }else
                        {
                            rowReporter.existsInReduced = true;
                            if (result != null){
                                if (result.getLsid() != resultFromReduced.getLsid()){
                                    rowReporter.errorMessageInReduced = "NOTE: Different LSID";
                                }
                            }
                        }

                        //                System.out.println(commonName + " GUID: " + guid);
                    } catch (SearchResultException e) {
                        failed++;
                        rowReporter.errorMessageInCombined = "Search caused an exception: " + e.getMessage();
                    }
                    resultReports.add(rowReporter);
                }
            }
            // write out report file.
            String filePath = System.getProperty("user.home") + File.separator + "Documents" + File.separator + "conservation-checkreport.csv" ;
            File reportFile = new File(filePath);
            try{
                FileWriter reportWriter = new FileWriter(reportFile);
                CSVWriter writer = new CSVWriter(reportWriter);
                writer.writeNext(ReporterStruct.header);
                for (ReporterStruct row : resultReports){
                    writer.writeNext(row.serialise());
                }
                writer.close();
            }catch (IOException e){
                e.printStackTrace();
            }

            System.out.println("Total names tested: " + (failed + passed) + " passed: " + passed + " failed: " + failed);
            if (failed > 0)
                fail("Test failed.  See other error messaged for details.");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unable to open file. " + e.getMessage());
        }
    }

    @Test
    // Based on Iconic Species Test code.
    public void testGeneraInIndex() {
        try {
            CSVParser csvParser = new CSVParserBuilder()
                    .withSeparator(',')
                    .withQuoteChar('"')
                    .withEscapeChar('\\')
                    .build();
            CSVReader reader = new CSVReaderBuilder(new InputStreamReader(this.getClass().getResourceAsStream("Genus-match-2.csv")))
                    .withCSVParser(csvParser)
                    .withSkipLines(1)
                    .build();
            String[] values;
            int passed = 0, failed = 0;
            // reporter
            // list scientific name, index LSID (if found), Common Name, source lists
            List<ReporterStruct> resultReports = new ArrayList<>();
            values = reader.readNext(); // Read header
            while ((values = reader.readNext()) != null) {
                if (values.length >= 1 && !values[0].equals("scientificName")) { // there's an error with reading the row - TODO Flag this row
                    //System.out.println("Processing " + values.length + " : " + values[0]);
                    String scientificName = values[0];
                    String commonName = "";
                    String sourceLists = "";

                    //construct reporter
                    ReporterStruct rowReporter = new ReporterStruct();
                    rowReporter.scientificName = scientificName;
                    rowReporter.sourceLists = sourceLists;
                    rowReporter.existsInCombined = false; // assume the worst
                    rowReporter.existsInReduced = false;
                    //attempt to locate the genus species binomial or genus species subspecies trinomial
                    String search = scientificName;
                    //create the search classification
                    LinnaeanRankClassification classification = new LinnaeanRankClassification(null, null, null, null, null, null, search);
                    classification.setKingdom("Plantae");



                    try {
                        NameSearchResult result = null;

                        try {
                            result = searcher.searchForRecord(classification, true, true, true);
                        } catch (MisappliedException e)
                        {
                            result = e.getMatchedResult();
                        }


                        //assertNotNull(search + " could not be found" ,guid);
                        if (result == null) {
                            //System.err.println(search + "(" + scientificName + ") could not be found in index");
                            //
                            // rowReporter.errorMessage = scientificName + " could not be found in index";
                            rowReporter.errorMessageInCombined = "Could not be found in index";
                            failed++;

                        } else {

//                            if (result.getLsid().contains("catalogue")) {
//                                System.err.println(search + " (" + scientificName + ") has a CoL LSID");
//                                rowReporter.errorMessageInCombined = scientificName + " has a CoL LSID";
//                            }

/*                            if (result.isSynonym())
                                result = searcher.searchForRecordByLsid(result.getAcceptedLsid());*/
                            //test to see if the classification matches
/*                            if (!classification.hasIdenticalClassification(result.getRankClassification(), RankType.GENUS) && result.getRankClassification().getGenus() != null) {

                                failed++;
                                //System.err.println(search + "("+commonName+") classification: "+ classification + " does not match " + result.getRankClassification());
                                System.err.println(search + "(" + commonName + ") classifications do not match");

                                printDiff(classification, result.getRankClassification(), result.getLsid(), true);
                            } else*/
                            passed++;
                            rowReporter.reportedLSID = result.getLsid();
                            rowReporter.sourceLists = "Plantae";
                            rowReporter.existsInCombined = true;
                        }

                        //                System.out.println(commonName + " GUID: " + guid);
                    } catch (SearchResultException e) {
                        try{
                            NameSearchResult result = null;
                            classification.setKingdom("Animalia");
                            try {
                                result = searcher.searchForRecord(classification, true, true, true);
                            } catch (MisappliedException g)
                            {
                                result = g.getMatchedResult();
                            }
                            if (result == null) {
                                //System.err.println(search + "(" + scientificName + ") could not be found in index");
                                //
                                // rowReporter.errorMessage = scientificName + " could not be found in index";
                                rowReporter.errorMessageInCombined = "Could not be found in index";
                                failed++;

                            } else {

                                passed++;
                                rowReporter.reportedLSID = result.getLsid();
                                rowReporter.sourceLists = "Animalia";
                                rowReporter.existsInCombined = true;
                            }
                        } catch (SearchResultException f) {
                            failed++;
                            rowReporter.errorMessageInCombined = "Search caused an exception: " + f.getMessage();
                        }
                    }
                    resultReports.add(rowReporter);
                }
            }
            // write out report file.
            String filePath = System.getProperty("user.home") + File.separator + "Documents" + File.separator + "Genus-Match-Report.csv" ;
            File reportFile = new File(filePath);
            try{
                FileWriter reportWriter = new FileWriter(reportFile);
                CSVWriter writer = new CSVWriter(reportWriter);
                writer.writeNext(ReporterStruct.header);
                for (ReporterStruct row : resultReports){
                    writer.writeNext(row.serialise());
                }
                writer.close();
            }catch (IOException e){
                e.printStackTrace();
            }

            System.out.println("Total names tested: " + (failed + passed) + " passed: " + passed + " failed: " + failed);
            if (failed > 0)
                fail("Test failed.  See other error messaged for details.");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unable to open file. " + e.getMessage());
        }
    }

}

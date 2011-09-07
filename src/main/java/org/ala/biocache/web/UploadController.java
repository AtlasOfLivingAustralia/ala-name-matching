package org.ala.biocache.web;

import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.biocache.*;
import au.org.ala.util.ParsedRecord;
import au.org.ala.util.AdHocParser;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class UploadController {

    private final static Logger logger = Logger.getLogger(UploadController.class);

    /**
     * Upload a dataset using a POST, returning a UID for this data
     *
     * @return an identifier for this temporary dataset
     */
    @RequestMapping("/upload")
    public String uploadHomePage(){
        return "upload/home";
    }

    /**
     * Expects a request body in JSON
     *
     * @param request
     * @return
     */
    @RequestMapping(value="/parser/areDwcTerms", method = RequestMethod.POST)
    public @ResponseBody boolean areDwcTerms(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ObjectMapper om = new ObjectMapper();
        try {
            InputStream input = request.getInputStream();
            List<String> terms = om.readValue(input, new TypeReference<List<String>>() {});
            input.close();
            String[] termArray = terms.toArray(new String[]{});
            return AdHocParser.areColumnHeaders(termArray);
        } catch(Exception e) {
            logger.error(e.getMessage(),e);
            response.sendError(HttpURLConnection.HTTP_BAD_REQUEST);
            return false;
        }
    }

    @RequestMapping(value="/parser/matchTerms", method = RequestMethod.POST)
    public @ResponseBody String[] guessFieldTypes(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ObjectMapper om = new ObjectMapper();
        try {
            InputStream input = request.getInputStream();
            List<String> terms = om.readValue(input, new TypeReference<List<String>>() {});
            input.close();
            String[] termArray = terms.toArray(new String[]{});
            return AdHocParser.guessColumnHeaders(termArray);
        } catch(Exception e) {
            logger.error(e.getMessage(),e);
            response.sendError(HttpURLConnection.HTTP_BAD_REQUEST);
            return null;
        }
    }

    @RequestMapping(value="/process/adhoc", method = RequestMethod.POST)
    public @ResponseBody ParsedRecord processRecord(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ObjectMapper om = new ObjectMapper();
        try {
            InputStream input = request.getInputStream();
            Map<String,String> record = om.readValue(input, new TypeReference<Map<String,String>>() {});
            input.close();
            String[] headers = AdHocParser.guessColumnHeaders(record.keySet().toArray(new String[]{}));
            return AdHocParser.processLine(headers, record.values().toArray(new String[]{}));
        } catch(Exception e) {
            logger.error(e.getMessage(),e);
            response.sendError(HttpURLConnection.HTTP_BAD_REQUEST);
            return null;
        }
    }

    /**
     * Upload a dataset using a POST, returning a UID for this data
     *
     * @return an identifier for this temporary dataset
     */
    @RequestMapping(value="/upload/post", method = RequestMethod.POST)
    public @ResponseBody Map<String,String> uploadOccurrenceData(HttpServletRequest request) throws Exception {

        //check the request
        String headers = request.getParameter("headers");
        String csvData = request.getParameter("csvData");

        //get a record count
        int lineCount = 0;
        BufferedReader reader = new BufferedReader(new StringReader(csvData));
        while(reader.readLine() != null){
            lineCount++;
        }

        String datasetName = request.getParameter("datasetName");
        String separator = request.getParameter("separator");
        char separatorChar = ',';
        if(separator != null && "TAB".equalsIgnoreCase(separator)){
           separatorChar =  '\t';
        }

        logger.debug("######### Retrieved - separator (original): '" + separator + "'");
        logger.debug("######### Retrieved - separatorChar: '" + separatorChar + "'");

        //PostMethod post = new PostMethod("http://woodfired.ala.org.au:8080/Collectory/ws/tempDataResource");
        PostMethod post = new PostMethod("http://collections.ala.org.au/ws/tempDataResource");
        ObjectMapper mapper = new ObjectMapper();

        UserUpload uu = new UserUpload();
        uu.setNumberOfRecords(lineCount);
        uu.setName(datasetName);

        String json = mapper.writeValueAsString(uu);
        System.out.println("Sending: " + json);

        post.setRequestBody(json);
        HttpClient httpClient = new HttpClient();
        httpClient.executeMethod(post);
        logger.debug("######### Retrieved: " + post.getResponseBodyAsString());
        logger.debug("######### Retrieved: " + post.getResponseHeader("location").getValue());
        logger.debug("######### Data uploaded....");
        String collectoryUrl = post.getResponseHeader("location").getValue();

        String tempUid = collectoryUrl.substring(collectoryUrl.lastIndexOf('/') + 1);
        CSVReader csvReader = new CSVReader(new StringReader(csvData), separatorChar);

        try {
            String[] currentLine = csvReader.readNext();
            boolean firstListAreHeaders = AdHocParser.areColumnHeaders(currentLine);

            String[] headerArray = null;

            if(headers == null){
                headerArray = AdHocParser.guessColumnHeaders(currentLine);
            } else {
                String[] unnormalised = headers.split(",");
                headerArray = new String[unnormalised.length];
                for(int i=0; i<headerArray.length; i++){
                    headerArray[i] = unnormalised[i].trim();
                }
            }

            if(!firstListAreHeaders){
                addRecord(tempUid, currentLine, headerArray);
                currentLine = csvReader.readNext();
            }

            while(currentLine!=null){
                addRecord(tempUid, currentLine, headerArray);
                currentLine = csvReader.readNext();
            }
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            csvReader.close();
        }
        au.org.ala.biocache.Store.index(tempUid);
        logger.debug("######### Temporary UID being returned...." + tempUid);
        Map<String,String> details = new HashMap<String,String>();
        details.put("uid", tempUid);
        return details;
    }

    private void addRecord(String tempUid, String[] currentLine, String[] headers) {
        Map<String,String> map = new HashMap<String, String>();
        for(int i=0; i< headers.length && i< currentLine.length; i++){
            map.put(headers[i], currentLine[i].trim());
        }
        au.org.ala.biocache.Store.insertRecord(tempUid, map, false);
    }
}

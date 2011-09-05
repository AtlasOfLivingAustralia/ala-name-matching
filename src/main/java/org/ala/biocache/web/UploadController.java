package org.ala.biocache.web;

import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.biocache.*;
import au.org.ala.util.ParsedRecord;
import au.org.ala.util.AdHocParser;
import org.ala.biocache.dto.DataUploadParams;
import org.ala.biocache.dto.SearchRequestParams;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import sun.misc.Request;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class UploadController {

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
            return AdHocParser.processLine(record.keySet().toArray(new String[]{}), record.values().toArray(new String[]{}));
        } catch(Exception e) {
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
        String datasetName = request.getParameter("datasetName");

        //PostMethod post = new PostMethod("http://woodfired.ala.org.au:8080/Collectory/ws/tempDataResource");
        PostMethod post = new PostMethod("http://collections.ala.org.au/ws/tempDataResource");
        ObjectMapper mapper = new ObjectMapper();

        UserUpload uu = new UserUpload();
        uu.setName(datasetName);

        String json = mapper.writeValueAsString(uu);
        System.out.println("Sending: " + json);

        post.setRequestBody(json);
        HttpClient httpClient = new HttpClient();
        httpClient.executeMethod(post);
        System.out.println("######### Retrieved: " + post.getResponseBodyAsString());
        System.out.println("######### Retrieved: " + post.getResponseHeader("location").getValue());
        System.out.println("######### Data uploaded....");
        String collectoryUrl= post.getResponseHeader("location").getValue();

        String tempUid = collectoryUrl.substring(collectoryUrl.lastIndexOf('/') + 1);
        CSVReader csvReader = new CSVReader(new StringReader(csvData));

        try {
            String[] currentLine = csvReader.readNext();
            boolean firstListAreHeaders = AdHocParser.areColumnHeaders(currentLine);

            String[] headerArray = null;

            if(headers == null){
                headerArray = AdHocParser.guessColumnHeaders(currentLine);
            } else {
                headerArray = headers.split(",");
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
        System.out.println("######### Temporary UID being returned...." + tempUid);
        Map<String,String> details = new HashMap<String,String>();
        details.put("uid", tempUid);
        return details;
    }

    private void addRecord(String tempUid, String[] currentLine, String[] headers) {
        Map<String,String> map = new HashMap<String, String>();
        for(int i=0; i< headers.length && i< currentLine.length; i++){
            map.put(headers[i], currentLine[i]);
        }
        au.org.ala.biocache.Store.insertRecord(tempUid, map, true);
        au.org.ala.biocache.Store.index(tempUid);
    }
}

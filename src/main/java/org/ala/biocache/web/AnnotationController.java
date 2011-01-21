/**************************************************************************
 *  Copyright (C) 2010 Atlas of Living Australia
 *  All Rights Reserved.
 *
 *  The contents of this file are subject to the Mozilla Public
 *  License Version 1.1 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of
 *  the License at http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  rights and limitations under the License.
 ***************************************************************************/
package org.ala.biocache.web;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ala.biocache.dto.OccurrenceAnnotation;
import org.ala.biocache.dto.OccurrenceAnnotationBody;
import org.ala.biocache.dto.OccurrenceAnnotationUpdate;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.codehaus.jackson.map.ObjectMapper;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.XPath;
import org.dom4j.io.SAXReader;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.HtmlUtils;
/**
 * Occurrences controller for the BioCache
 *
 * FIXME This should be completely refactored to use biocache-store API
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
@Controller
public class AnnotationController {

	/** Logger initialisation */
	private final static Logger logger = Logger.getLogger(AnnotationController.class);

	/** Name of view for an empty search page */
	private final String JSON = "json/annotationJson";

    protected String annoteaServerUrl = "http://annotate.ala.org.au/annotea";
	protected String annotationTemplate = "annotation.vm";

	protected String twitterConsumerKey = "";
	protected String twitterConsumerSecret = "";
	
	protected String twitterTokenFilePath = "/token.txt";
	protected String twitterTokenSecretFilePath = "/tokenSecret.txt";

	private String bitlyLogin = "";
	private String bitlyApiKey ="";

    private String dannoServer = "annotate.ala.org.au";
    private int dannoPort = 80;
    private String dannoUser = "biocache-webapp";
    private String dannoPassword = "biocache";
    
    private int timeoutInMillisec = 10000;

    private boolean enableTwitterSync = true;
    
    /**
     * Constructor to populate fields with values from the portal.properties file.
     * N.B. Because of a class loading order issue, the fields could not be loaded
     * directly from the portal.properties file.
     */
    public AnnotationController() {
	    ResourceBundle rb = ResourceBundle.getBundle("biocache");
	    try {
	        this.annoteaServerUrl = rb.getString("annotationController.annoteaServerUrl");
	        this.dannoServer = rb.getString("annotationController.dannoServer");
	        this.twitterConsumerKey = rb.getString("annotationController.twitterConsumerKey");
            this.twitterConsumerSecret = rb.getString("annotationController.twitterConsumerSecret");
            this.bitlyLogin = rb.getString("annotationController.bitlyLogin");
            this.bitlyApiKey = rb.getString("annotationController.bitlyApiKey");
        } catch (Exception e){
        	logger.warn("Unable to retrieve annotationController.* from portal.properties. Using default values.", e);
        }
    }
    
    
	/**
	 * Retrieve all annotations for a given URI and render data as JSON.
	 *
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
    @RequestMapping(value = "/annotation/retrieveAllAnnotationsForOccurrenceRecord", method = RequestMethod.GET)
	public ModelAndView retrieveAllAnnotationsForOccurrenceRecord(HttpServletRequest request, HttpServletResponse response) throws Exception {
		/*
         * Call danno to retrieve annotations for a given URL
         * url: http://dannoServer/danno/annotea/
         * params: w3c_annotates: occurrenceHtmlUrl
         */
        String pageUrl = ServletRequestUtils.getStringParameter(request, "url");

        if (pageUrl == null) return null;
        String url = annoteaServerUrl + "?w3c_annotates=" + pageUrl;
        logger.debug("annotea url = " + url);
        List<OccurrenceAnnotation> occurrenceAnnotations = getAnnotationsForUrl(url);
        // sort annotations by date so we can ensure replies are processed after their inReplyTo
        Comparator dateComparator = new Comparator() {
            @Override
            public int compare(Object occurrenceAnnotation1, Object occurrenceAnnotation2) {
                Date date1 = ((OccurrenceAnnotation) occurrenceAnnotation1).getDate();
                Date date2 = ((OccurrenceAnnotation) occurrenceAnnotation2).getDate();
                return date1.compareTo(date2);
            }
        };
        Collections.sort(occurrenceAnnotations, dateComparator);

        for (OccurrenceAnnotation oa : occurrenceAnnotations) {
            try {
                String bodyUrl = oa.getBodyUrl();
                oa.setBody(getAnnotationForBodyUrl(bodyUrl));
            } catch (Exception e) {
                logger.error("Problem retrieving annotation "+oa.getBodyUrl()+": "+e.getMessage());
            }
        }

        ModelAndView mav = new ModelAndView(JSON);
        mav.addObject("oas", occurrenceAnnotations);
        response.setHeader("Cache-Control", "no-cache");  // prevent IE from caching results
		return mav;
	}

    /**
	 * Retrieve all annotations for a given annotation (URI) and render data as JSON.
	 *
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
    @RequestMapping(value = "/annotation/retrieveReplyAnnotationsForAnnotation", method = RequestMethod.GET)
	public ModelAndView retrieveReplyAnnotationsForAnnotation(HttpServletRequest request, HttpServletResponse response) throws Exception {
		/*
         * Call danno to retrieve annotations for a given annotation
         * url: http://dannoServer/danno/annotea/
         * params: w3c_annotates: occurrenceHtmlUrl
         */
        String pageUrl = ServletRequestUtils.getStringParameter(request, "url");

        if (pageUrl == null) return null;
        String url = annoteaServerUrl + "?w3c_reply_tree=" + pageUrl;
        logger.debug("annotea url = " + url);
        List<OccurrenceAnnotation> occurrenceAnnotations = getAnnotationsForUrl(url);

        for (OccurrenceAnnotation oa : occurrenceAnnotations) {
            String bodyUrl = oa.getBodyUrl();
            oa.setBody(getAnnotationForBodyUrl(bodyUrl));
        }

        ModelAndView mav = new ModelAndView(JSON);
        mav.addObject("oas", occurrenceAnnotations);
        response.setHeader("Cache-Control", "no-cache");  // prevent IE from caching results
		return mav;
	}

	/**
	 * Save a submitted annotation to the annotea compliant server.
	 *
	 * This takes values from the HTTP POST with the prefix convention of "new." and "old." (e.g. new.latitude  old.latitude)
	 * and adds them to the annotation. This then performs the HTTP POST to Danno.
	 *
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
    @RequestMapping(value = "/annotation/saveAnnotation", method = RequestMethod.POST)
	public ModelAndView saveAnnotation(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
		StringBuffer annotationBody = new StringBuffer();
		String title = "Occurrence Record Annotation";
		String xpath = request.getParameter("xpath");
		String url = request.getParameter("url");
        String comment = HtmlUtils.htmlEscape(request.getParameter("comment"));
        String creator = HtmlUtils.htmlEscape(request.getParameter("creator"));
        String fullName = HtmlUtils.htmlEscape(request.getParameter("creator-name"));
        String email = HtmlUtils.htmlEscape(request.getParameter("creator-email"));
        String issueCode = request.getParameter("atype");
		String occurrenceRecordKey = request.getParameter("recordKey");
        String annotationType = ServletRequestUtils.getStringParameter(request, "type", "Annotation");
        String replyRoot = request.getParameter("rootAnnotation");
        String replyField = request.getParameter("field");
        String dataResourceId = request.getParameter("dataResourceId");
        String dataResourceUid = request.getParameter("dataResourceUid");
        String dataResourceName = request.getParameter("dataResource");
        String type = "change";

		//retrieve old values
		List<String> paramNames = getParamsWithPrefix(request, "old.");
		annotationBody.append("<ala:occurrenceRecordAnnotation>");
        annotationBody.append("<ala:hasOccurrenceRecordId>"+occurrenceRecordKey+"</ala:hasOccurrenceRecordId>");
        annotationBody.append("<ala:hasDataResourceId>"+dataResourceId+"</ala:hasDataResourceId>");
        annotationBody.append("<ala:hasDataResourceUid>"+dataResourceUid+"</ala:hasDataResourceUid>");
        annotationBody.append("<ala:hasDataResourceName>"+dataResourceName+"</ala:hasDataResourceName>");
        annotationBody.append("<ala:hasOccurrenceSection>"+xpath+"</ala:hasOccurrenceSection>");
        annotationBody.append("<ala:hasIssueCode>"+issueCode+"</ala:hasIssueCode>");
        annotationBody.append("<ala:hasComment>"+comment+"</ala:hasComment>");

        Tweeter tw =null;
        for (String paramName: paramNames) {
			String oldValue = request.getParameter("old."+paramName);
			String newValue = HtmlUtils.htmlEscape(request.getParameter("new."+paramName));
			if(StringUtils.isNotEmpty(newValue)){
				addFieldUpdate(annotationBody, paramName, oldValue, newValue);
				if(tw==null){
					tw = createTweeter(url, dataResourceUid);
					if(tw!=null){
						tw.setFieldName(paramName);
						tw.setNewValue(newValue);
						tw.setOldValue(oldValue);
					}
				}
			}
		}

        if (tw==null && StringUtils.isNotEmpty(comment)){
        	tw = createTweeter(url, dataResourceUid); // comment-only annotation
        	if (tw != null) {
                tw.setComment(comment);
            }
            type = "Comment";
        }

        // Name & Email supplied
        String creatorString = creator +" | " + fullName + " | mailto:" + email;

        if ("reply".equals(annotationType)) type = "Comment";

		annotationBody.append("</ala:occurrenceRecordAnnotation>");
		logger.debug("annotationBody: "+annotationBody);
        logger.debug("email address: "+email);
		//create an annotea RDF message
        
        Velocity.setProperty("resource.loader", "class");
        Velocity.setProperty("class.resource.loader.class","org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init();
        
		VelocityContext ctx = new VelocityContext();
		Template t = Velocity.getTemplate("annotation.vm");
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		PrintWriter ow = new PrintWriter(bout, true);
		ctx.put("type", type);
        ctx.put("annotationType", annotationType);  // Annotation or Reply
        ctx.put("replyRoot", replyRoot);  // for replies
		ctx.put("url", url);
		ctx.put("xpath", replyField);  // was xpath
		ctx.put("creator", creatorString);
        //ctx.put("email", email);
		ctx.put("title", title);
		ctx.put("lang", "en");
		//ctx.put("comment", comment);
		ctx.put("body", annotationBody.toString());
		t.merge(ctx, ow);
		ow.flush();

		//submit to Danno
		HttpClient httpClient  = new HttpClient();
        httpClient.getState().setCredentials(
            new AuthScope(dannoServer, dannoPort, "realm"),
            new UsernamePasswordCredentials(dannoUser,dannoPassword)
        );
        PostMethod postMethod = new PostMethod(annoteaServerUrl);
		postMethod.setDoAuthentication( true );
        httpClient.getState().setAuthenticationPreemptive(true);

        String requestBody = bout.toString();
		logger.debug(requestBody);
		postMethod.setRequestBody(requestBody);
		int status = httpClient.executeMethod(postMethod);
        String statusMsg = postMethod.getResponseBodyAsString();
		//logger.debug(postMethod.getResponseBodyAsString());

		logger.debug("danno URL (PostMethod): "+annoteaServerUrl+" (dannoServer:"+dannoServer+")");
        logger.debug("danno status: "+status);
        logger.debug("danno status msg: "+statusMsg);
        logger.debug("END: Annotation submitted to annotea server.");
		postMethod.releaseConnection();
		if (status != HttpServletResponse.SC_CREATED) {
			response.sendError(status, "Annotation server returned an error (see tomcat logs for details).");
		}

        String annotationUrl = getAnnotationUrl(statusMsg);
        logger.debug("annotationUrl: "+annotationUrl);
        response.setStatus(HttpServletResponse.SC_CREATED, "created"); // OK 201
        // output annotationUrl as content so JS client can highlight the added annotation
        response.setContentType("text/plain");
        response.getWriter().println(annotationUrl);
        //submit that tweet!
        if(tw!=null && enableTwitterSync){
            //retrieve the annotation URL
        	tw.setUrlToAnnotation(annotationUrl);
	        Thread th = new Thread(tw);
	        th.run();
        }

        return null;
	}

   /**
	 * Autocomplete AJAX service for JQuery-autocomplete
	 *
	 * @param query
	 * @param response
	 */
	@RequestMapping(value = "/occurrence/terms", method = RequestMethod.GET)
	public void listTerms(
			@RequestParam(value="term", required=false) String query,
			HttpServletResponse response) {

		try {
			OutputStreamWriter os = new OutputStreamWriter(response.getOutputStream());
			//Autocompleter ac = new Autocompleter();
			List<String> terms = new ArrayList<String>();
            terms.add("foo");
            terms.add("bar");
            terms.add("pop");
            // TODO: add method to SearchDao for autcomplete via SOLR
			//terms = ac.suggestTermsFor(query.toLowerCase().trim(), 10);
			// create JSON string using Jackson
			ObjectMapper o = new ObjectMapper();
			String json = o.writeValueAsString(terms);
			response.setContentType("application/json");
			os.write(json);
			os.close();
		} catch (Exception ex) {
			logger.error("Problem running Autocompleter: "+ex.getMessage(), ex);
		}

		return;
	}

	/**
	 * Retrieve the URL for the annotation from the response to the create annotation
	 * request.
	 *
	 * @param responseMessage
	 * @return
	 */
	public String getAnnotationUrl(String responseMessage) throws Exception {
		StringReader sr = new StringReader(responseMessage);
		Document document = parseAnnotationResponse(sr);
		Attribute annotationUrl = (Attribute) document.selectSingleNode( "/rdf:RDF/rdf:Description/@rdf:about" );
		return annotationUrl.getValue();
	}

	/**
	 * Add a field update to the request.
	 *
	 * @param fieldUpdates
	 * @param paramName
	 * @param oldValue
	 * @param newValue
	 */
	private void addFieldUpdate(StringBuffer fieldUpdates, String paramName,
			String oldValue, String newValue) {
		fieldUpdates.append("<ala:hasFieldUpdate>");
		fieldUpdates.append("<ala:fieldUpdate>");
        fieldUpdates.append("<ala:hasFieldName>");
		fieldUpdates.append(paramName);
		fieldUpdates.append("</ala:hasFieldName>");
		fieldUpdates.append("<ala:hasOldValue>");
		fieldUpdates.append(oldValue);
		fieldUpdates.append("</ala:hasOldValue>");
		fieldUpdates.append("<ala:hasNewValue>");
		fieldUpdates.append(newValue);
		fieldUpdates.append("</ala:hasNewValue>");
		fieldUpdates.append("</ala:fieldUpdate>");
		fieldUpdates.append("</ala:hasFieldUpdate>");
	}

    private Tweeter createTweeter( String url, String dataResourceKey) {
        
    	try {
    		Tweeter tw = new Tweeter();
			InputStream tokenStream = getClass().getResourceAsStream(twitterTokenFilePath);
			ObjectInputStream objectInputStream = new ObjectInputStream(tokenStream);
			String twitterToken = (String) objectInputStream.readObject();
			InputStream tokenSecretStream = getClass().getResourceAsStream(twitterTokenSecretFilePath);
			objectInputStream = new ObjectInputStream(tokenSecretStream);
			String twitterTokenSecret = (String) objectInputStream.readObject();
			tw.setTwitterConsumerKey(twitterConsumerKey);
			tw.setTwitterConsumerSecret(twitterConsumerSecret);
			tw.setTwitterToken(twitterToken);
			tw.setTwitterTokenSecret(twitterTokenSecret);
			
			System.out.println("########################### bitlyLogin : "+bitlyLogin);
			System.out.println("########################### bitlyApiKey : "+bitlyApiKey);
			tw.setBitlyLogin(bitlyLogin);
			tw.setBitlyApiKey(bitlyApiKey);
			tw.setUrl(url);
			tw.setDataResourceKey(dataResourceKey);
	        return tw;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
    }

    
    
    
	/**
	 * Retrieve a list of parameters for update.
	 *
	 * @param request
	 * @param prefix
	 * @return
	 */
	private List<String> getParamsWithPrefix(HttpServletRequest request, String prefix) {
		Enumeration<String> paramNames = request.getParameterNames();
		List<String> params = new ArrayList<String>();
		while(paramNames.hasMoreElements()){
			String paramName = paramNames.nextElement();
            logger.debug("param="+paramName);
			if(paramName.startsWith(prefix)){
				params.add(paramName.substring(prefix.length()));
			}
		}
		return params;
	}

	/**
	 * Get the Danno response stream.
	 *
	 * @param url
	 * @return
	 * @throws Exception
	 */
    private InputStream getDannoInputStream(String url) throws Exception {
        logger.debug("Danno request: " + url);
    	HttpClient httpClient = new HttpClient();
        httpClient.getParams().setSoTimeout(timeoutInMillisec);
        httpClient.getState().setCredentials(
            new AuthScope(dannoServer, dannoPort, "realm"),
            new UsernamePasswordCredentials(dannoUser,dannoPassword)
        );
        GetMethod getMethod = new GetMethod(url);
        getMethod.setDoAuthentication(true);
        httpClient.getState().setAuthenticationPreemptive(true);
        httpClient.executeMethod(getMethod);
        if(logger.isDebugEnabled()){
        	logger.debug("Danno response: "+getMethod.getResponseBodyAsString());
        }
        return getMethod.getResponseBodyAsStream();
    }

    /**
     * Get the annotations for the URL.
     *
     * @param url
     * @return
     * @throws Exception
     */
    protected List<OccurrenceAnnotation> getAnnotationsForUrl(String url) throws Exception {
        InputStream queryResponse = getDannoInputStream(url);
        Document document = parseAnnotationResponse(queryResponse);
		return findAnnoBodies(document);
    }

    /**
     * Parse the Annotea response.
     *
     * @param queryResponse
     * @return
     * @throws DocumentException
     */
	public Document parseAnnotationResponse(InputStream queryResponse)
			throws DocumentException {
		return parseAnnotationResponse(new InputStreamReader(queryResponse));
	}

    /**
     * Parse the Annotea response.
     *
     * @param reader
     * @return
     * @throws DocumentException
     */
	public Document parseAnnotationResponse(Reader reader)
			throws DocumentException {
		SAXReader saxReader = new SAXReader();
        Map<String, String> namespaceURIs = new HashMap<String, String>();
		namespaceURIs.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		namespaceURIs.put("j.0", "http://www.w3.org/2000/10/annotationType#");
        namespaceURIs.put("j.1", "http://purl.org/dc/elements/1.0/" );
        namespaceURIs.put("j.2", "http://www.w3.org/2000/10/annotation-ns#" );
        namespaceURIs.put("j.3", "http://www.w3.org/2001/03/thread#" );
        namespaceURIs.put("ala", "http://ala.org.au/2009/annotation/0.1/" );
        namespaceURIs.put("dc",  "http://purl.org/dc/elements/1.1/");
		DocumentFactory.getInstance().setXPathNamespaceURIs(namespaceURIs);
        Document document = saxReader.read(reader);
		return document;
	}

    /**
     *
     * @param document
     * @return
     * @throws ParseException
     */
    public List<OccurrenceAnnotation> findAnnoBodies(Document document) throws ParseException {
        List<OccurrenceAnnotation> annotations = new ArrayList<OccurrenceAnnotation>();
        List<Node> list = document.selectNodes( "/rdf:RDF/rdf:Description" ); // //j.2:body/@rdf:resource
        for (Iterator<Node> iter = list.iterator(); iter.hasNext(); ) {
            Node node = iter.next();
            OccurrenceAnnotation oa = new OccurrenceAnnotation();
            //Node body = node.selectSingleNode( "j.2:body/@rdf:resource" );
            Attribute bodyUrl = (Attribute) node.selectSingleNode( "j.2:body/@rdf:resource" );
            if(bodyUrl!=null){
	            oa.setBodyUrl(bodyUrl.getValue());
                oa.setAnnoteaKey(node.valueOf("@rdf:about"));
	            Attribute pageUrl = (Attribute) node.selectSingleNode( "j.2:annotates/@rdf:resource" );
	            if (pageUrl!=null) oa.setAnnotates(pageUrl.getValue());
	            oa.setCreator(formatCreator(node.valueOf("dc:creator"))); // node.valueOf("j.1:creator/vcard:fn")
	            
	            String createdDateAsString = adjustTimezone(node.valueOf("dc:date"));
	            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	            oa.setDate(df.parse(createdDateAsString));
	            
                Attribute replyUrl = (Attribute) node.selectSingleNode( "j.3:inReplyTo/@rdf:resource" );
	            if (replyUrl!=null) {
                    oa.setInReplyTo(replyUrl.getValue());
                    oa.setReplyField(node.valueOf("j.2:context"));
                }

	            annotations.add(oa);
            }
        }
        return annotations;
    }

    /**
     * Removes ':' from time zone since the SimpleDateFormat parser will not parse dates formatted like,
     * 
     * <p><code>2010-07-05T23:21:33.467+10:00</code></p>
     * 
     * According to Java doc the time zone should be <code>GMT+hh:mm</code> or <code>+hhmm</code>.
     * <p/>
     * @param date
     * @return date string with ':' removed from time zone.
     */
    private String adjustTimezone(String date) {
    	if (date.charAt(26) == ':') {
    		return date.substring(0, date.lastIndexOf(':')) + date.substring(date.lastIndexOf(':') + 1);
    	} else {
    		return date;
    	}
	}

	/**
	 * If creator's email is supplied then return it otherwise return name.  An anonymous user will have no email address.
	 * 
	 * @param creator
	 * @return email or name.
	 */
	private String formatCreator(String creator) {
		String[] creatorParts = creator.split("\\|");
		if(creatorParts.length>1){
			String name = creatorParts[1];
			return name;
		}
		return "";
	}

	/**
     * Parse integer, return null if not integer.
     *
     * @param intAsString
     * @return
     */
    public Integer nullsafeParseInt(String intAsString){
    	try {
    		return Integer.parseInt(intAsString);
    	} catch (Exception e){}
    	return null;
    }

    /**
     * Retrieve the annotation update.
     *
     * @param document
     * @return
     */
    protected OccurrenceAnnotationBody getAnnotationForBodyUrl(String bodyUrl) throws Exception {
    	//retrieve from url
        InputStream queryResponse = getDannoInputStream(bodyUrl);
        //parse DOM
        SAXReader reader = new SAXReader();
        Document document = reader.read(queryResponse);
        //create new annotation
        OccurrenceAnnotationBody oab = new OccurrenceAnnotationBody();
        oab.setOccurrenceId(nullsafeParseInt(document.selectSingleNode("//ala:hasOccurrenceRecordId").getText()));
        oab.setDataResourceId(nullsafeParseInt(document.selectSingleNode("//ala:hasDataResourceId").getText()));
        oab.setSection(document.selectSingleNode("//ala:hasOccurrenceSection").getText());
        oab.setComment(document.selectSingleNode("//ala:hasComment").getText());
        //oab.setIssueType(document.selectSingleNode("//ala:hasIssueCode").getText());

        //get the field update nodes
        XPath xpath = document.createXPath("//ala:hasFieldUpdate");
        Object fieldUpdates = xpath.evaluate(document);

        if(fieldUpdates instanceof Element){
        	Element element = (Element) fieldUpdates;
    		Attribute att = element.attribute("nodeID");
    		String nodeId = att.getText();
            XPath nodeXpath = document.createXPath("//rdf:Description[@rdf:nodeID='"+nodeId+"']");
            Object nodeEl = nodeXpath.evaluate(document);
            if(nodeEl instanceof Element){
            	Element el = (Element) nodeEl;
	            OccurrenceAnnotationUpdate oau = new OccurrenceAnnotationUpdate();
	            oau.setFieldName(el.valueOf("ala:hasFieldName"));
	            oau.setOldValue(el.valueOf("ala:hasOldValue"));
	            oau.setNewValue(el.valueOf("ala:hasNewValue"));
	            oab.addFieldUpdate(oau);
            }
        }

        if(fieldUpdates instanceof List){
        	List<Element> fieldUpdateEls = (List<Element>) fieldUpdates;
        	for(Element el: fieldUpdateEls){
        		Attribute att = el.attribute("nodeID");
        		String nodeId = att.getText();
                XPath nodeXpath = document.createXPath("//rdf:Description[@rdf:nodeID='"+nodeId+"']");
                Object nodeEl = nodeXpath.evaluate(document);
                if(nodeEl instanceof Element){
                	Element element = (Element) nodeEl;
	                OccurrenceAnnotationUpdate oau = new OccurrenceAnnotationUpdate();
	                oau.setFieldName(element.valueOf("ala:hasFieldName"));
	                oau.setOldValue(element.valueOf("ala:hasOldValue"));
	                oau.setNewValue(element.valueOf("ala:hasNewValue"));
	                oab.addFieldUpdate(oau);
                }
        	}
        }
        return oab;
    }

	/**
	 * @param annoteaServerUrl the annoteaServerUrl to set
	 */
	public void setAnnoteaServerUrl(String annoteaServerUrl) {
		this.annoteaServerUrl = annoteaServerUrl;
	}

	/**
	 * @param annotationTemplate the annotationTemplate to set
	 */
	public void setAnnotationTemplate(String annotationTemplate) {
		this.annotationTemplate = annotationTemplate;
	}

    /**
     * @param dannoPassword
     */
    public void setDannoPassword(String dannoPassword) {
        this.dannoPassword = dannoPassword;
    }

    /**
     * @param dannoServer
     */
    public void setDannoServer(String dannoServer) {
        this.dannoServer = dannoServer;
    }

    /**
     * @param dannoUser
     */
    public void setDannoUser(String dannoUser) {
        this.dannoUser = dannoUser;
    }

	/**
	 * @param dannoPort the dannoPort to set
	 */
	public void setDannoPort(int dannoPort) {
		this.dannoPort = dannoPort;
	}

    /**
     * @param timeoutInMillisec the timeoutInMillisec to set
     */
    public void setTimeoutInMillisec(int timeoutInMillisec) {
        this.timeoutInMillisec = timeoutInMillisec;
    }

	/**
	 * @param bitlyLogin the bitlyLogin to set
	 */
	public void setBitlyLogin(String bitlyLogin) {
		this.bitlyLogin = bitlyLogin;
	}

	/**
	 * @param bitlyApiKey the bitlyApiKey to set
	 */
	public void setBitlyApiKey(String bitlyApiKey) {
		this.bitlyApiKey = bitlyApiKey;
	}

	/**
	 * @param enableTwitterSync the enableTwitterSync to set
	 */
	public void setEnableTwitterSync(boolean enableTwitterSync) {
		this.enableTwitterSync = enableTwitterSync;
	}

	/**
	 * @param twitterConsumerKey the twitterConsumerKey to set
	 */
	public void setTwitterConsumerKey(String twitterConsumerKey) {
		this.twitterConsumerKey = twitterConsumerKey;
	}

	/**
	 * @param twitterConsumerSecret the twitterConsumerSecret to set
	 */
	public void setTwitterConsumerSecret(String twitterConsumerSecret) {
		this.twitterConsumerSecret = twitterConsumerSecret;
	}

	/**
	 * @param twitterTokenFilePath the twitterTokenFilePath to set
	 */
	public void setTwitterTokenFilePath(String twitterTokenFilePath) {
		this.twitterTokenFilePath = twitterTokenFilePath;
	}

	/**
	 * @param twitterTokenSecretFilePath the twitterTokenSecretFilePath to set
	 */
	public void setTwitterTokenSecretFilePath(String twitterTokenSecretFilePath) {
		this.twitterTokenSecretFilePath = twitterTokenSecretFilePath;
	}
}

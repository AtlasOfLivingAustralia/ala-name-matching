package org.ala.web.controller;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Base class for high level controllers providing REST style pathing.
 * Support for this pathing will lead to deep linking support within the
 * web application.
 * 
 * It is expected the methods :
 * 1) handleRequest(Map<String, String> properties, HttpServletRequest request, HttpServletResponse response)
 * 2) mapProperties(List<String> urlProperties)
 * will be overidden by most subclasses.
 *
 * The default behaviour is to map values in the url to key value pairs based on Spring configured supportedPatterns. These key value pairs
 * will be added to the HttpServletRequest as attributes (see HttpServletRequest.getAttribute(String)). The view name is constructed using
 * the viewNamePrefix value and the property subViewNameAttribute.
 * 
 * @author dmartin
 */
public class RestController implements Controller {
	
	protected static Log logger = LogFactory.getLog(RestController.class);	
	
	public static final String DEFAULT_SUBVIEW_ATTRIBUTE="view";
	
	/**The view to direct to in the event of an error**/
	protected String defaultView;	
	/**The root of a url. e.g. "/taxonomy/" **/	
	protected String urlRoot = "/";
	/**view name prefix for taxonConcept views. e.g. taxonConcept**/
	protected String viewNamePrefix;	
	/**view name attribute to use to retrieve the subview name. e.g. the names part of the view name species.names **/
	protected String subViewNameAttribute=DEFAULT_SUBVIEW_ATTRIBUTE;	
	/**whether file extensions should be removed from the final property in the url**/
	protected boolean removeExtensions=false;
	/**A list of supported patterns. A pattern is a list of one or more strings**/
	protected List<List<String>> supportedPatterns;
	/**the charset to use to decode the url. Default is UTF-8.**/
	protected String urlCharset = "UTF-8";
	/** A list of supported subview names that can be used */
	protected List<String> supportedSubViews;
	/** Additional properties - that default get added as attributes to the request */
	protected Map<String, String> additionalProperties;
	
	/**
	 * Takes the requested url and splits out the properties in that url. <br>
	 * E.g. "/taxonomy/2324/Summary" where  "taxonomy" is the root will have 2 properties
	 * "2324" and "Summary"
	 * 
	 * @todo replace this implementation with the use of AntPathMatcher and PropertiesMethodNameResolver
	 * 
	 * @see org.springframework.web.servlet.mvc.multiaction.MultiActionController#handleRequestInternal(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public final ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

		String requestURI = request.getRequestURI();
		//get path e.g. /taxonomy/1234/Summary.htm
		String path = requestURI.substring(requestURI.indexOf(urlRoot)+urlRoot.length());
		StringTokenizer tokenizer = new StringTokenizer(path, "/");
		List<String> properties = new ArrayList<String>();
		while(tokenizer.hasMoreTokens()){
			String property = tokenizer.nextToken();
			//need to do each property separately as a value
			///may have a directory separator in it that shouldnt be interpreted
			//as a directory separator within the application
			property = URLDecoder.decode(property, urlCharset);
			//if this is the last property check for an extension
			// and remove if configured to
			if(!tokenizer.hasMoreTokens() && removeExtensions)
				property = FilenameUtils.removeExtension(property);
			properties.add(property);
		}
		//match the retrieved properties to a supported pattern
		Map<String, String> propertyMap = mapUrlProperties(properties);
		if(propertyMap==null)
			redirectToDefaultView();
		return handleRequest(propertyMap, request, response);
	}

	/**
	 * Matches a set of properties to a support pattern. If can not match, returns null to indicate
	 * failure.
	 * 
	 * It is expected that this method will be overidded for more custom behaviour. This basic implementation
	 * just matches the number of arguments.
	 *  
	 * @param urlProperties the properties retrieved from the Url
	 * @return a map of properties that tally with the url properties provided
	 */
	public Map<String, String> mapUrlProperties(List<String> urlProperties){
		if(supportedPatterns!=null){
			for (List<String> supportedPattern : supportedPatterns){
				if(supportedPattern.size()== urlProperties.size()){
					Map<String, String> propertyMap = new HashMap<String, String>();				
					int noOfElements=supportedPattern.size();
					for (int i=0; i<noOfElements; i++){
						propertyMap.put(supportedPattern.get(i), decodeParameter(urlProperties.get(i)));
					}
					return propertyMap;
				}
			}
		}
		//else populate map anyway
		Map<String, String> propertyMap = new HashMap<String, String>();
		int noOfElements=urlProperties.size();
		for (int i=0; i<noOfElements; i++)
			propertyMap.put("property"+i, urlProperties.get(i));
		return propertyMap;
	}
	
	/**
	 * Intended to be overridden by subclasses.
	 * 
	 * Uses the supplied properties to determine the method to invoke or view to direct to.
	 * Adds resolved property names to the request and resolves the view name using the configured
	 * viewNamePrefix and the retrieve subview name.
	 * 
	 * @param properties
	 * @param request
	 * @param response
	 * @return a ModelAndView
	 */
	public ModelAndView handleRequest(Map<String, String> propertiesMap, HttpServletRequest request, HttpServletResponse response) throws Exception{
		addPropertiesToRequest(propertiesMap, request);
		return resolveAndCreateView(propertiesMap, request, true);
	}

	/**
	 * Adds the retrieved properties from the url and mapping to the request as attributes.
	 * @param properties
	 * @param request
	 */
	protected void addPropertiesToRequest(Map<String, String> properties, HttpServletRequest request){
		for (String key: properties.keySet())
			request.setAttribute(key, properties.get(key));
		if(additionalProperties!=null){
			for (String key: additionalProperties.keySet())
				request.setAttribute(key, additionalProperties.get(key));
		}
	}

	/**
	 * Constructs the view name from the view name prefix and sub view name attribute.
	 * Users should override this for more complex view names. Forces the sub view name to be lower
	 * case to allow case insensitivity in the urls.
	 * 
	 * @param properties
	 * @param request
	 * @return ModelAndView with the correct view name
	 */
	protected final ModelAndView resolveAndCreateView(Map<String, String> properties, HttpServletRequest request, boolean validate){
		String subView = properties.get(subViewNameAttribute);
		String viewName = viewNamePrefix;
		if(StringUtils.isNotEmpty(subView)){
			if(validate){
				if(isValidSubview(subView)){
					subView = StringUtils.uncapitalize(subView);
					if(viewName!=null && viewName.length()>0)
						viewName = viewName+"."+subView;
					else
						viewName = subView;
				}
			} else {
				subView = StringUtils.uncapitalize(subView);
				if(viewName!=null && viewName.length()>0)
					viewName = viewName+"."+subView;
				else
					viewName = subView;			}
		}
		if(logger.isDebugEnabled())
			logger.debug("constructed view name: "+viewName);
		ModelAndView mav = new ModelAndView(viewName);		
		return mav;	
	}

	/**
	 * Returns true if the supplied view name is contained with the configured 
	 * list of supported sub views. If the list of supported sub views is empty
	 * true will be returned.
	 * 
	 * @return true if list is valid.
	 */
	protected boolean isValidSubview(String subViewName){
		if(supportedSubViews!=null && !supportedSubViews.isEmpty()){
			return supportedSubViews.contains(subViewName);
		}
		return true;
	}
	
	/**
	 * Redirects to the default view for this controller.
	 * @return ModelAndView for the default view
	 */
	protected ModelAndView redirectToDefaultView(){
		return new ModelAndView(new RedirectView(defaultView, true));
	}
	
	/**
	 * Decodes a parameter - replacing underscores with whitespace.
	 * @param encodedParam
	 * @return decoded parameter value
	 */
	public final static String decodeParameter(String encodedParam){
		if(StringUtils.isEmpty(encodedParam))
			return null;
		encodedParam = encodedParam.trim();
		encodedParam = encodedParam.replaceAll("_", " ");
		return encodedParam; 
	}

	/**
	 * @return the defaultView
	 */
	public String getDefaultView() {
		return defaultView;
	}

	/**
	 * @param defaultView the defaultView to set
	 */
	public void setDefaultView(String defaultView) {
		this.defaultView = defaultView;
	}

	/**
	 * @return the urlRoot
	 */
	public String getUrlRoot() {
		return urlRoot;
	}

	/**
	 * @param urlRoot the urlRoot to set
	 */
	public void setUrlRoot(String urlRoot) {
		this.urlRoot = urlRoot;
	}

	/**
	 * @return the viewNamePrefix
	 */
	public String getViewNamePrefix() {
		return viewNamePrefix;
	}

	/**
	 * @param viewNamePrefix the viewNamePrefix to set
	 */
	public void setViewNamePrefix(String viewNamePrefix) {
		this.viewNamePrefix = viewNamePrefix;
	}

	/**
	 * @return the removeExtensions
	 */
	public boolean isRemoveExtensions() {
		return removeExtensions;
	}

	/**
	 * @param removeExtensions the removeExtensions to set
	 */
	public void setRemoveExtensions(boolean removeExtensions) {
		this.removeExtensions = removeExtensions;
	}

	/**
	 * @return the supportedPatterns
	 */
	public List<List<String>> getSupportedPatterns() {
		return supportedPatterns;
	}

	/**
	 * @param supportedPatterns the supportedPatterns to set
	 */
	public void setSupportedPatterns(List<List<String>> supportedPatterns) {
		this.supportedPatterns = supportedPatterns;
	}

	/**
	 * @return the subViewNameAttribute
	 */
	public String getSubViewNameAttribute() {
		return subViewNameAttribute;
	}

	/**
	 * @param subViewNameAttribute the subViewNameAttribute to set
	 */
	public void setSubViewNameAttribute(String subViewNameAttribute) {
		this.subViewNameAttribute = subViewNameAttribute;
	}

	/**
	 * @param urlCharset the urlCharset to set
	 */
	public void setUrlCharset(String urlCharset) {
		this.urlCharset = urlCharset;
	}

	/**
	 * @param supportedSubViews the supportedSubViews to set
	 */
	public void setSupportedSubViews(List<String> supportedSubViews) {
		this.supportedSubViews = supportedSubViews;
	}

	/**
	 * @param additionalProperties the additionalProperties to set
	 */
	public void setAdditionalProperties(Map<String, String> additionalProperties) {
		this.additionalProperties = additionalProperties;
	}
}
/***************************************************************************
 * Copyright (C) 2009 Atlas of Living Australia
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
 ***************************************************************************/
package org.ala.web.interceptor;

import java.net.URLEncoder;
import java.util.List;
import java.util.Set;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * Simple Interceptor that checks a cookie is available. If not available forwards request to a url supplying
 * the requested url as a parameter.
 *
 * @author dmartin
 */
public class CookieAndSessionCheckInterceptor extends HandlerInterceptorAdapter {

	protected static Log logger = LogFactory.getLog(CookieAndSessionCheckInterceptor.class);
	
	/** The name of the cookie to look for and create if not present */
	protected String cookieName="ALATermsAndConditions";
	/** The url to redirect to if cookie not present */
	protected String baseForwardUrl = "/terms.htm?";
	/** The request key for the url to forward to once cookie is accepted */
	protected String forwardUrlRequestKey = "forwardUrl";
	/** A list of urls to ignore for cookie checks */
	protected List<String> ignoreUrlPatterns;
	/** set of agents allowed to get in cookie free **/
	protected Set<String> robotsAgentsAllowedToBypassCookies;
	/** Switch off cookie check */
	protected boolean switchOffCookieCheck = true;
	
	/**
	 * @see org.springframework.web.servlet.handler.HandlerInterceptorAdapter#preHandle(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.Object)
	 */
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		
		if(switchOffCookieCheck)
			return true;
		
		//check parameter in the session
		if(logger.isDebugEnabled()){
			logger.debug(request.getRequestURL());
			logger.debug(request.getRemoteAddr());
			logger.debug("Checking session...requested url:"+request.getRequestURL());
		}
		if(bypassCookieCheck(request, response)){
			logger.debug("Bypassing cookie check.");
			return true;
		}
		
		Object cookieNameChecked = request.getSession().getAttribute(cookieName);
		if(cookieNameChecked!=null)
			return true;
		
		logger.debug("Checking cookies...");
		Cookie[] cookies = request.getCookies();
		if(cookies!=null){
			for(Cookie cookie: cookies){
				if(cookie.getName().equals(cookieName)){
					request.getSession().setAttribute(cookieName, true);
					return true;
				}
			}
		}
		logger.debug("Redirecting to terms page...");
		//need to redirect to disclaimer page
		StringBuffer requestedUrlBuffer = request.getRequestURL();
		String queryString = request.getQueryString();
		if(StringUtils.isNotEmpty(queryString)){
			requestedUrlBuffer.append('?');
			requestedUrlBuffer.append(queryString);
		}
		String requestedUrl = URLEncoder.encode(requestedUrlBuffer.toString(), "UTF-8");
		response.sendRedirect(request.getContextPath()+baseForwardUrl+forwardUrlRequestKey+"="+requestedUrl);
		return false;
	}
	
	/**
	 * Returns true if the request is for a url pattern that should be ignored.
	 * Or is a robot we like!
	 * @param request
	 * @param response
	 * @return
	 */
	private boolean bypassCookieCheck(HttpServletRequest request, HttpServletResponse response){

		if(ignoreUrlPatterns!=null && !ignoreUrlPatterns.isEmpty()){
			StringBuffer requestedUrlBuffer = request.getRequestURL();
			String queryParam = request.getQueryString();
			String requestURI = requestedUrlBuffer.toString();
			for(String ignorePattern: ignoreUrlPatterns){
				if(requestURI.indexOf(ignorePattern)!=-1 || 
					(queryParam!=null && queryParam.contains(ignorePattern))
				){
					if(logger.isDebugEnabled())
						logger.debug("Bypassing cookie check. URL:"+requestURI+" matches pattern: "+ignorePattern);
					return true;
				}
			}
		}
		
		String userAgent = request.getHeader("User-Agent");
		if(logger.isDebugEnabled()){
			logger.debug("User agent in request: " + userAgent);
		}
		
		if (userAgent != null) {
			for (String agentToIgnore : robotsAgentsAllowedToBypassCookies) {
				if (userAgent.toUpperCase().contains(agentToIgnore.toUpperCase())) {
					if(logger.isDebugEnabled()){
						logger.debug("Bypassing cookie check for agent: " + userAgent);
					}
					return true;
				}
			}
		} else {
			logger.debug("No user agent in request...");
		}
		return false;
	}
	
	/**
	 * @param disclaimerCookieName the disclaimerCookieName to set
	 */
	public void setCookieName(String disclaimerCookieName) {
		this.cookieName = disclaimerCookieName;
	}

	/**
	 * @param disclaimerUrl the disclaimerUrl to set
	 */
	public void setBaseForwardUrl(String disclaimerUrl) {
		this.baseForwardUrl = disclaimerUrl;
	}

	/**
	 * @param forwardUrlRequestKey the forwardUrlRequestKey to set
	 */
	public void setForwardUrlRequestKey(String forwardUrlRequestKey) {
		this.forwardUrlRequestKey = forwardUrlRequestKey;
	}

	/**
	 * @param ignoreUrlPatterns the ignoreUrlPatterns to set
	 */
	public void setIgnoreUrlPatterns(List<String> ignoreUrlPatterns) {
		this.ignoreUrlPatterns = ignoreUrlPatterns;
	}

	/**
	 * @return Returns the robotsAgentsAllowedToBypassCookies.
	 */
	public Set<String> getRobotsAgentsAllowedToBypassCookies() {
		return robotsAgentsAllowedToBypassCookies;
	}

	/**
	 * @param robotsAgentsAllowedToBypassCookies The robotsAgentsAllowedToBypassCookies to set.
	 */
	public void setRobotsAgentsAllowedToBypassCookies(
			Set<String> robotsAgentsAllowedToBypassCookies) {
		this.robotsAgentsAllowedToBypassCookies = robotsAgentsAllowedToBypassCookies;
	}

	/**
	 * @param switchOffCookieCheck the switchOffCookieCheck to set
	 */
	public void setSwitchOffCookieCheck(boolean switchOffCookieCheck) {
		this.switchOffCookieCheck = switchOffCookieCheck;
	}
}
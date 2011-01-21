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

import javax.servlet.http.HttpServletResponse;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Simple proxy controller to proxy requests to other ALA domains and thus overcome the
 * cross-domain restrictions of AJAX.
 * 
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
@Controller
@RequestMapping("/proxy")
public class ProxyController {
    /** Logger initialisation */
	private final static Logger logger = Logger.getLogger(ProxyController.class);
    /** WordPress URL */
    private String WORDPRESS_URL = "http://www.ala.org.au/";

    /**
     * Proxy to WordPress site using page_id URI format
     * 
     * @param pageId
     * @param contentOnly
     * @param response
     * @throws Exception
     */
    @RequestMapping(value = "/wordpress", method = RequestMethod.GET)
    public void getWordPressContentforPageId(
            @RequestParam(value="page_id", required=true) String pageId,
            @RequestParam(value="content-only", required=false) String contentOnly,
            HttpServletResponse response) throws Exception {

        StringBuilder urlString = new StringBuilder(WORDPRESS_URL+"?page_id="+pageId);
        
        if (contentOnly != null && !contentOnly.isEmpty()) {
            urlString.append("&content-only=").append(contentOnly);
        }

        logger.info("proxy URI: "+urlString);

        try {
            String contentAsString = getUrlContentAsString(urlString.toString(), 10000);
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(contentAsString);
        } catch (Exception ex) {
            // send a 500 so ajax client does not display WP not found page
            response.setStatus(response.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(ex.getMessage());
            logger.error("Proxy error: "+ex.getMessage(), ex);
        }
    }

    /**
	 * Retrieve content as String.
	 *
	 * @param url
	 * @return
	 * @throws Exception
	 */
	public static String getUrlContentAsString(String url, int timeoutInMillisec) throws Exception {
		GetMethod gm = null;
        String content = null;

		try {
            HttpConnectionManagerParams cmParams = new HttpConnectionManagerParams();
		    cmParams.setSoTimeout(timeoutInMillisec);
		    cmParams.setConnectionTimeout(timeoutInMillisec);
		    HttpConnectionManager manager = new SimpleHttpConnectionManager();
		    manager.setParams(cmParams);
		    HttpClientParams params = new HttpClientParams();
            params.setContentCharset("UTF-8");
		    HttpClient client = new HttpClient(params, manager);	
			gm = new GetMethod(url);
            gm.setFollowRedirects(true);
            client.executeMethod(gm);
            
            if (gm.getStatusCode() == 200) {
                content = gm.getResponseBodyAsString();
            } else {
                throw new Exception("HTTP request for "+url+" failed. Status code: "+gm.getStatusCode());
            }
        } catch (Exception ex) {
            logger.warn("HTTP connection error: "+ex.getMessage(), ex);
        } finally {
            if (gm != null) {
				logger.debug("Releasing connection");
				gm.releaseConnection();
			}
        }

		return content;
	}
}

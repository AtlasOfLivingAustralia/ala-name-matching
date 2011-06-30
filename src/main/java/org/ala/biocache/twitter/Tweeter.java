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
package org.ala.biocache.twitter;

import java.net.URLEncoder;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.http.AccessToken;


/**
 * Submits a tweet annotation to twitter. Done in a thread so as to not impact
 * the rest of the application.
 * 
 * @author Dave Martin (David.Martin@csiro.au)
 */
public class Tweeter implements Runnable {
	
	protected Logger logger = Logger.getLogger(this.getClass());
	private String fieldName = null;
	private String comment = null;
	private String oldValue = null;
	private String newValue = null;
	private String url = null;
	private String dataResourceUid;
	private String urlToAnnotation = null;
	private String bitlyLogin = null;
	private String bitlyApiKey = null;
	
	//twitter authentication params
	private String twitterConsumerKey = null;
	private String twitterConsumerSecret = null;
	private String twitterToken = null;
	private String twitterTokenSecret = null;
	
	private int timeoutInMillisec = 10000;

	public static final int MAX_CHARACTER_LIMIT = 160;
	
	/**
	 * Runs and attempts to tweet. If there is any sort of communication failure, this will simply
	 * just log the fact. This is a best effort.
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
    	try {
    		//get a bit.ly URL for the update
    		HttpClient hc = new HttpClient();
            hc.getParams().setSoTimeout(timeoutInMillisec);
    		String fullUrl = url+"?annotation="+URLEncoder.encode(urlToAnnotation,"UTF-8");
    		logger.debug("Full URL for bit.ly: "+fullUrl);
    		String bitlyUrl = "http://api.bit.ly/shorten?version=2.0.1&login="+bitlyLogin+"&apiKey="+bitlyApiKey+"&longUrl="+fullUrl;
            logger.debug("bitlyUrl="+bitlyUrl);
    		GetMethod gm = new GetMethod(bitlyUrl);
    		hc.executeMethod(gm);
    		String response = gm.getResponseBodyAsString();
    		logger.debug(response);
    		
    		String shortUrl = "";
    		
    		try {
	    		JSONObject jsonObject = new JSONObject(response);
	    		JSONObject result = jsonObject.getJSONObject("results");
	    		JSONObject resultForUrl = (JSONObject) result.get(url+"?annotation="+urlToAnnotation);
	    		shortUrl = (String) resultForUrl.get("shortUrl");
	    		logger.debug("Retrieved bit.ly URL: "+shortUrl);
    		} catch (Exception e){
    			logger.error(e.getMessage(), e);
    		}
    		
			//careful not to hit the 160 character limit
			int maxTextLength = (MAX_CHARACTER_LIMIT-shortUrl.length());
    		
    		//submit to twitter    		

			StringBuffer sb = new StringBuffer("#ala_"+dataResourceUid+" ");
			if(fieldName!=null){
				sb.append("#");
				sb.append(fieldName);
				sb.append(" ");
				sb.append("updated to \"");
				sb.append(newValue);
				sb.append("\"");
				
				//append old value if we have space
				if(StringUtils.isNotEmpty(oldValue) && (sb.length()<maxTextLength)){
					sb.append(" from \"");
					sb.append(oldValue);
					sb.append("\"");
				}
				sb.append(". ");
			} else if(StringUtils.isNotEmpty(comment)){
				sb.append("\"");
				//minus 4 for the quotes and punctuation
				if(comment.length()>maxTextLength-4){
					sb.append(StringUtils.abbreviate(comment, (maxTextLength-6))+"...");
				} else {
					sb.append(comment);
				}
				sb.append("\". ");
			}
			
			String message = null;

			//abbreviate if we have hit the character limit
			if(sb.length()>maxTextLength){
				message = StringUtils.abbreviate(sb.toString(), (maxTextLength-3))+"...";
			} else {
				message = sb.toString();
			}
			
			AccessToken accessToken = new AccessToken(twitterToken, twitterTokenSecret);
			TwitterFactory factory = new TwitterFactory();
			Twitter twitter = factory.getOAuthAuthorizedInstance(twitterConsumerKey, twitterConsumerSecret, accessToken);
			Status status = twitter.updateStatus(message+shortUrl);
			System.out.println("Successfully updated the status to ["+ status.getText() + "].");
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
	}
	
	/**
	 * @return the logger
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * @param logger the logger to set
	 */
	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	/**
	 * @return the fieldName
	 */
	public String getFieldName() {
		return fieldName;
	}

	/**
	 * @param fieldName the fieldName to set
	 */
	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	/**
	 * @return the oldValue
	 */
	public String getOldValue() {
		return oldValue;
	}

	/**
	 * @param oldValue the oldValue to set
	 */
	public void setOldValue(String oldValue) {
		this.oldValue = oldValue;
	}

	/**
	 * @return the newValue
	 */
	public String getNewValue() {
		return newValue;
	}

	/**
	 * @param newValue the newValue to set
	 */
	public void setNewValue(String newValue) {
		this.newValue = newValue;
	}

	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @param url the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * @return the urlToAnnotation
	 */
	public String getUrlToAnnotation() {
		return urlToAnnotation;
	}

	/**
	 * @param urlToAnnotation the urlToAnnotation to set
	 */
	public void setUrlToAnnotation(String urlToAnnotation) {
		this.urlToAnnotation = urlToAnnotation;
	}

	/**
	 * @return the bitlyLogin
	 */
	public String getBitlyLogin() {
		return bitlyLogin;
	}

	/**
	 * @param bitlyLogin the bitlyLogin to set
	 */
	public void setBitlyLogin(String bitlyLogin) {
		this.bitlyLogin = bitlyLogin;
	}

	/**
	 * @return the bitlyApiKey
	 */
	public String getBitlyApiKey() {
		return bitlyApiKey;
	}

	/**
	 * @param bitlyApiKey the bitlyApiKey to set
	 */
	public void setBitlyApiKey(String bitlyApiKey) {
		this.bitlyApiKey = bitlyApiKey;
	}

	/**
	 * @return the comment
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * @param comment the comment to set
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}

	/**
	 * @return the dataResourceKey
	 */
	public String getDataResourceUid() {
		return dataResourceUid;
	}

	/**
	 * @param dataResourceKey the dataResourceKey to set
	 */
	public void setDataResourceKey(String dataResourceUid) {
		this.dataResourceUid = dataResourceUid;
	}

    /**
     * @return the timeoutInMillisec
     */
    public int getTimeoutInMillisec() {
        return timeoutInMillisec;
    }

    /**
     * @param timeoutInMillisec the timeoutInMillisec to set
     */
    public void setTimeoutInMillisec(int timeoutInMillisec) {
        this.timeoutInMillisec = timeoutInMillisec;
    }


	/**
	 * @return the twitterConsumerKey
	 */
	public String getTwitterConsumerKey() {
		return twitterConsumerKey;
	}


	/**
	 * @param twitterConsumerKey the twitterConsumerKey to set
	 */
	public void setTwitterConsumerKey(String twitterConsumerKey) {
		this.twitterConsumerKey = twitterConsumerKey;
	}


	/**
	 * @return the twitterConsumerSecret
	 */
	public String getTwitterConsumerSecret() {
		return twitterConsumerSecret;
	}


	/**
	 * @param twitterConsumerSecret the twitterConsumerSecret to set
	 */
	public void setTwitterConsumerSecret(String twitterConsumerSecret) {
		this.twitterConsumerSecret = twitterConsumerSecret;
	}


	/**
	 * @return the twitterToken
	 */
	public String getTwitterToken() {
		return twitterToken;
	}


	/**
	 * @param twitterToken the twitterToken to set
	 */
	public void setTwitterToken(String twitterToken) {
		this.twitterToken = twitterToken;
	}


	/**
	 * @return the twitterTokenSecret
	 */
	public String getTwitterTokenSecret() {
		return twitterTokenSecret;
	}


	/**
	 * @param twitterTokenSecret the twitterTokenSecret to set
	 */
	public void setTwitterTokenSecret(String twitterTokenSecret) {
		this.twitterTokenSecret = twitterTokenSecret;
	}

}

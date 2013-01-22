package org.gbif.checklistbank.utils;

import java.io.BufferedReader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
/**
 *
 * Copied this class from Checklist bank code to remove some of the dependencies.
 *
 */
public class SqlUtils {
	protected static final Log log = LogFactory.getLog(SqlUtils.class);

	public static void executeSqlScript(Connection con, InputStream sqlScript) throws Exception{
		BufferedReader d = new BufferedReader(new InputStreamReader(sqlScript));

		//Now read line bye line
		StringBuffer sql = new StringBuffer();
		String thisLine;
		boolean insideBlockComment = false;
		boolean insideFunction = false;
		Statement stmt = con.createStatement();
	    try {
			while ((thisLine = d.readLine()) != null)
			{
			    //Skip comments and empty lines
			    if(thisLine.length() > 0 && thisLine.charAt(0) == '-' || thisLine.length() == 0 ){
			        continue;
			    }

			    // block comment start?
			    if (thisLine.trim().startsWith("/*")){
			    	insideBlockComment=true;
			    }

			    // function start?
			    if (thisLine.toUpperCase().startsWith("CREATE OR REPLACE FUNCTION")){
			    	insideFunction = true;
			    }

			    // block comment end?
			    if (insideBlockComment){
			        if (thisLine.trim().endsWith("*/")){
			        	insideBlockComment = false;
			        }
			        continue;
			    }else if(insideFunction){
			        sql.append(" " + thisLine);
			        if (thisLine.trim().toLowerCase().replace(" immutable","").endsWith("language plpgsql;")){
			        	insideFunction = false;
			            sql.setCharAt(sql.length() - 1, ' '); //Remove the ; since jdbc complains
//			            System.out.println(sql.toString());
			            stmt.execute(sql.toString());
			            sql = new StringBuffer();
			        }
			    }else{
			        sql.append(" " + thisLine);
			        //If one command complete
			        if(sql.charAt(sql.length() - 1) == ';') {
			            sql.setCharAt(sql.length() - 1, ' '); //Remove the ; since jdbc complains
//			            System.out.println(sql.toString());
			            stmt.execute(sql.toString());
			            sql = new StringBuffer();
			        }
			    }
			}
		} catch (Exception e) {
			log.error("Error executing sql script file. Problematic statement: "+sql, e);
			throw e;
		}
	}
}

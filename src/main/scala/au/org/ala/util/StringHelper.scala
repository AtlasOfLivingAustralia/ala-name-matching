package au.org.ala.util

/**
 * ************************************************************************
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
 * *************************************************************************
 */
import java.io._
import java.util.jar.JarFile
import au.org.ala.biocache.DateParser
/**
 * String helper - used as a implicit converter to add additional helper methods to java.io.File
 */
class StringHelper(str: String) {
	
    def fixWidth(width:Int) : String = {
        if(str.length> width) str.substring(0,width)
	    else if (str.length< width) str + List.fill(width - str.length)(" ").mkString
	    else str
    }
    
    def isInt: Boolean = {
        try {
            str.toInt
            true
        } catch {
            case _ => false
        }
    }

    def isFloat: Boolean = {
        try {
            str.toFloat
            true
        } catch {
            case _ => false
        }
    }

    def isDate: Boolean = {
        DateParser.parseDate(str) match {
            case it if !it.isEmpty && DateParser.isValid(it.get) => true
            case _ => false
        }
    }

    def isLatitude: Boolean = {
        try {
            val latitude = str.toFloat
            if (latitude <= 90.0 && latitude >= -90.0) {
                true
            } else {
                false
            }
        } catch {
            case _ => false
        }
    }

    def isLongitude: Boolean = {
        try {
            val longitude = str.toFloat
            if (longitude <= 180.0 && longitude >= -180.0) {
                true
            } else {
                false
            }
        } catch {
            case _ => false
        }
    }
}

/**
 * Define a extensions to java.io.File
 */
object StringHelper {
    implicit def string2helper(str: String) = new StringHelper(str)
}
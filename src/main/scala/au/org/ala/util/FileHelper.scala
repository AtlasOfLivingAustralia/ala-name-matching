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
package au.org.ala.util
import java.io._
import java.io._
import java.util.jar.JarFile
import au.com.bytecode.opencsv.CSVReader
/**
 * File helper - used as a implicit converter to add additional helper methods to java.io.File
 */
class FileHelper(file: File) {

    def write(text: String): Unit = {
        val fw = new FileWriter(file)
        try { fw.write(text) }
        finally { fw.close }
    }

    def foreachLine(proc: String => Unit): Unit = {
        val br = new BufferedReader(new FileReader(file))
        try {
            while (br.ready) {
                proc(br.readLine)
            }
        } finally { br.close }
    }

    def deleteAll: Unit = {
        def deleteFile(dfile: File): Unit = {
            if (dfile.isDirectory) {
                val subfiles = dfile.listFiles
                if (subfiles != null)
                    subfiles.foreach { f => deleteFile(f) }
            }
            dfile.delete
        }
        deleteFile(file)
    }

    def extractZip: Unit = {
        val basename = file.getName.substring(0, file.getName.lastIndexOf("."))
        val todir = new File(file.getParentFile, basename)
        todir.mkdirs()

        println("Extracting " + file + " to " + todir)
        val jar = new JarFile(file)
        val enu = jar.entries
        while (enu.hasMoreElements) {
            val entry = enu.nextElement
            val entryPath =
                if (entry.getName.startsWith(basename)) entry.getName.substring(basename.length)
                else entry.getName

            println("Extracting to " + todir + "/" + entryPath)
            if (entry.isDirectory) {
                new File(todir, entryPath).mkdirs
            } else {
                val istream = jar.getInputStream(entry)
                val ostream = new FileOutputStream(new File(todir, entryPath))
                copyStream(istream, ostream)
                ostream.close
                istream.close
            }
        }
    }

    /**
     * Read this file as a CSV
     */
    def readAsCSV(separator:Char, quotechar:Char, procHdr:(Array[String] => Array[String]), read:((Array[String], Array[String]) => Unit)){
        val reader =  new CSVReader(new FileReader(file), separator, quotechar)
        val rawColumnHdrs = reader.readNext
        val columnHdrs = procHdr(rawColumnHdrs)
        var currentLine = reader.readNext
        while(currentLine != null){
            read(columnHdrs, currentLine)
            currentLine = reader.readNext
        }
    }
    
    private def copyStream(istream: InputStream, ostream: OutputStream): Unit = {
        var bytes = new Array[Byte](1024)
        var len = -1
        while ({ len = istream.read(bytes, 0, 1024); len != -1 })
            ostream.write(bytes, 0, len)
    }
}

/**
 * Define a extensions to java.io.File
 */
object FileHelper {
    implicit def file2helper(file: File) = new FileHelper(file)
}
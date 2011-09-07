<%@ page contentType="text/html" pageEncoding="UTF-8" %><%@
taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"
%><!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="pageName" content="home"/>
        <title>BioCache Upload | Atlas of Living Australia</title>
        <script language="JavaScript" type="text/javascript" src="http://www.ala.org.au/wp-content/themes/ala/scripts/jquery-1.4.2.min.js"></script>
        <style type="text/css">
          table { border-collapse: collapse; }
          th { font-size: 12px; }
          td { font-size: 11px; }
          body { font-family: Arial; font-size: 11px;}
          .unrecognised { background-color: red; }
        </style>
  </head>
  <body>
    <script type="text/javascript">
      function upload(){
        $.post("upload/post", {
          "csvData": $('#csvData').val() },
            function(data){
              $('#uploadFeedback').html('<p>Dataset uploaded. Temporary dataset ID is : <a href="http://localhost:8181/hubs-webapp/occurrences/'+data+'/search?q=">'+data+'</span>.</p>')
            }
        );
      }
    </script>
    <div class="section">
        <h1> Occurrence Data Sandbox </h1>
        <p>This is a sandbox environment that allow users to upload there datasets to view them through ALA tools.</p>

        <h2>1. Paste your Darwin Core CSV data here</h2>
        <textArea id="csvData" name="csvData" rows="15" cols="120"></textArea>

        <h2>2. Initial parsing</h2>



        <h2>3. Interpreted records</h2>


        <h2>4. Load data into Sandbox</h2>
        <input type="button" name="upload" value="Upload" onclick="javascript:upload();"/>
        <div id="uploadFeedback">
        </div>
    </div>
  </body>
</html>
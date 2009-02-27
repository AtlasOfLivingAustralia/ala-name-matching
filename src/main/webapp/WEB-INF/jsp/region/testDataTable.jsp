<%-- 
    Document   : testDataTable
    Created on : Feb 27, 2009, 3:58:12 PM
    Author     : "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
--%>

<%@page contentType="text/html" pageEncoding="MacRoman"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=MacRoman">
        <title>YUI Datatable Test</title>
        <link rel="stylesheet" type="text/css" href="http://yui.yahooapis.com/2.7.0/build/fonts/fonts-min.css" />
        <link rel="stylesheet" type="text/css" href="http://yui.yahooapis.com/2.7.0/build/datatable/assets/skins/sam/datatable.css" />
        <script type="text/javascript" src="http://yui.yahooapis.com/2.7.0/build/yahoo-dom-event/yahoo-dom-event.js"></script>

        <script type="text/javascript" src="http://yui.yahooapis.com/2.7.0/build/connection/connection-min.js"></script>
        <script type="text/javascript" src="http://yui.yahooapis.com/2.7.0/build/json/json-min.js"></script>
        <script type="text/javascript" src="http://yui.yahooapis.com/2.7.0/build/element/element-min.js"></script>
        <script type="text/javascript" src="http://yui.yahooapis.com/2.7.0/build/datasource/datasource-min.js"></script>
        <script type="text/javascript" src="http://yui.yahooapis.com/2.7.0/build/datatable/datatable-min.js"></script>

    </head>
    <body class=" yui-skin-sam">
        <h1>YUI Datatable Test</h1>
        <div id="json"></div>
        
        <script type="text/javascript">
            YAHOO.util.Event.addListener(window, "load", function() {
                YAHOO.example.XHR_JSON = function() {
                    var formatResourceUrl = function(elCell, oRecord, oColumn, sData) {
                        elCell.innerHTML = "<a href='" + oRecord.getData("dataResourceUrl") + "'>" + sData + "</a>";
                    };
                    var formatOccurrencesUrl = function(elCell, oRecord, oColumn, sData) {
                        elCell.innerHTML = "<a href='" + oRecord.getData("occurrencesUrl") + "'>" + sData + "</a>";
                    };
                    var formatGeoreferencedOccurrencesUrl = function(elCell, oRecord, oColumn, sData) {
                        elCell.innerHTML = "<a href='" + oRecord.getData("georeferencedOccurrencesUrl") + "'>" + sData + "</a>";
                    };

                    var myColumnDefs = [
                        {key:"dataResourceName", label:"Dataset", sortable:true, formatter:formatResourceUrl},
                        {key:"occurrences", label:"Occurrences", formatter:formatOccurrencesUrl, sortable:true},
                        {key:"georeferencedOccurrences", label:"Georeferenced Occurrences", formatter:formatGeoreferencedOccurrencesUrl, sortable:true},
                        {key:"basisOfRecord", label:"Basis of Record", sortable:true}
                    ];

                    var myDataSource = new YAHOO.util.DataSource("${pageContext.request.contextPath}/regions/resources/");
                    myDataSource.responseType = YAHOO.util.DataSource.TYPE_JSON;
                    myDataSource.connXhrMode = "queueRequests";
                    myDataSource.responseSchema = {
                        resultsList: "ResultSet.Result",
                        fields: ["dataResourceName","dataResourceUrl",{key:"occurrences",parser:"number"},"occurrencesUrl",{key:"georeferencedOccurrences",parser:"number"},"georeferencedOccurrencesUrl","basisOfRecord"]
                    };

                    var myDataTable = new YAHOO.widget.DataTable("json", myColumnDefs,
                            myDataSource, {initialRequest:"643"});

                    var mySuccessHandler = function() {
                        this.set("sortedBy", null);
                        this.onDataReturnAppendRows.apply(this,arguments);
                    };
                    var myFailureHandler = function() {
                        this.showTableMessage(YAHOO.widget.DataTable.MSG_ERROR, YAHOO.widget.DataTable.CLASS_ERROR);
                        this.onDataReturnAppendRows.apply(this,arguments);
                    };
                    var callbackObj = {
                        success : mySuccessHandler,
                        failure : myFailureHandler,
                        scope : myDataTable
                    };

                    myDataSource.sendRequest("", callbackObj);
                    
                    //myDataSource.sendRequest("query=chinese&zip=94089&results=10&output=json",
                    //        callbackObj);

                    return {
                        oDS: myDataSource,
                        oDT: myDataTable
                    };
                }();
            });

            </script>

    </body>
</html>

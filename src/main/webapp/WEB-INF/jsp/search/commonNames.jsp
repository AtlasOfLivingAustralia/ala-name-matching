<%@ include file="/common/taglibs.jsp"%>
<c:set var="maxClauseCountError">
    <div class="searchError"><span class="searchWarning">Warning:</span> Wildcard
    search exceeded limits, try including more characters in search query. </div>
</c:set>
<div id="twopartheader">
    <h2><spring:message code="blanket.search.commonNames.header" text="Common Names search results for:"/> <strong>${searchString}</strong> </h2>
    <p><a href="${pageContext.request.contextPath}/search/${searchString}"><spring:message code="blanket.search.allResults" text="Back to search results for all pages"/></a></p>
</div>

<div id="YuiSearchResults" class=" yui-skin-sam">
    <h4><spring:message code="blanket.search.commonNames.header" text="Common Names search results for:"/> ${searchString} (total results ${commonNamesTotal})</h4>
    <c:if test="${not empty commonNames}">
    <div id="json"></div>
    <script type="text/javascript">
        //YAHOO.util.Event.addListener(window, "load", function() {
            YAHOO.example.XHR_JSON = function() {
                var formatCommonNameUrl = function(elCell, oRecord, oColumn, sData) {
                    elCell.innerHTML = "<a href='" + oRecord.getData("commonNameUrl") +  "' title='go to species page'>" + sData + "</a>";
                };

                var formatScientificName = function(elCell, oRecord, oColumn, sData) {
                    if (oRecord.getData("rank")=="species" || oRecord.getData("rank")=="subspecies" || oRecord.getData("rank")=="genus") {
                        elCell.innerHTML = "<i>" + sData + "</i>";
                    } else {
                        elCell.innerHTML = sData;
                    }
                };


                var myColumnDefs = [
                    {key:"commonName", label:"Common Name", sortable:true, formatter:formatCommonNameUrl},
                    {key:"scientificName", label:"Scientific Name", formatter:formatScientificName},
                    {key:"rank", label:"Taxon Rank", sortable:true},
                    {key:"kingdom", label:"Kingdom", sortable:true},
                    {key:"score", label:"Score", formatter:"number", sortable:true}
                ];

                var myDataSource = new YAHOO.util.DataSource("${pageContext.request.contextPath}/search/commonNames/${searchString}/json?");
                myDataSource.responseType = YAHOO.util.DataSource.TYPE_JSON;
                //myDataSource.connXhrMode = "queueRequests";
                myDataSource.responseSchema = {
                    resultsList: "result",
                    fields: [{key:"score",parser:"number"},"commonName","commonNameUrl","scientificName","rank","kingdom"],
                    metaFields: {totalRecords: "totalRecords"}
                };

                // DataTable configuration
                var myConfigs = {
                    initialRequest:"sort=score&dir=desc&startIndex=0&results=25",
                    dynamicData: true,
                    sortedBy:{key:"score", dir:"desc"},
                    paginator: new YAHOO.widget.Paginator({ rowsPerPage:25 })
                };

                var myDataTable = new YAHOO.widget.DataTable("json", myColumnDefs, myDataSource, myConfigs); // scrollable:true,height:"150px",
                
                myDataTable.handleDataReturnPayload = function(oRequest, oResponse, oPayload) {
                    oPayload.totalRecords = oResponse.meta.totalRecords;
                    return oPayload;
                }

                return {
                    ds: myDataSource,
                    dt: myDataTable
                };
            }();
        //});

    </script>
    </c:if>
    <c:if test="${fn:contains(commonNamesError,'maxClauseCount')}">${maxClauseCountError}</c:if>
</div>

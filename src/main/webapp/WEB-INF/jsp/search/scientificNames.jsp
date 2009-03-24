<%@ include file="/common/taglibs.jsp"%>
<c:set var="maxClauseCountError">
    <div class="searchError"><span class="searchWarning">Warning:</span> Wildcard
    search exceeded limits, try including more characters in search query. </div>
</c:set>
<div id="twopartheader">
    <h2><spring:message code="blanket.search.scientificNames.header" text="Scientific Names search results for:"/> <strong>${searchString}</strong> </h2>
    <p><a href="${pageContext.request.contextPath}/search/${searchString}"><spring:message code="blanket.search.allResults" text="Back to search results for all pages"/></a></p>
</div>

<div id="YuiSearchResults" class=" yui-skin-sam">
    <h4><spring:message code="blanket.search.scientificNames.header" text="Scientific Names search results for:"/> ${searchString} (total results ${scientificNamesTotal})</h4>
    <c:if test="${not empty scientificNames}">
    <div id="json"></div>
    <script type="text/javascript">
        //YAHOO.util.Event.addListener(window, "load", function() {
            YAHOO.example.XHR_JSON = function() {
                var formatNameUrl = function(elCell, oRecord, oColumn, sData) {
                    var thisData;
                    if (oRecord.getData("rank")=="species" || oRecord.getData("rank")=="subspecies" || oRecord.getData("rank")=="genus") {
                        thisData = "<i>" + sData + "</i>";
                    } else {
                        thisData = sData;
                    }
                    elCell.innerHTML = "<a href='" + oRecord.getData("scientificNameUrl") +  "' title='go to species page'>" + thisData + "</a>";
                };

                var myColumnDefs = [
                    {key:"scientificName", label:"Scientific Name", sortable:true, formatter:formatNameUrl},
                    {key:"author", label:"Author"},
                    {key:"rank", label:"Taxon Rank", sortable:true},
                    {key:"kingdom", label:"Kingdom", sortable:true},
                    {key:"score", label:"Score", formatter:"number", sortable:true}
                ];

                var myDataSource = new YAHOO.util.DataSource("${pageContext.request.contextPath}/search/scientificNames/${searchString}/json?");
                myDataSource.responseType = YAHOO.util.DataSource.TYPE_JSON;
                //myDataSource.connXhrMode = "queueRequests";
                myDataSource.responseSchema = {
                    resultsList: "result",
                    fields: [{key:"score",parser:"number"},"scientificName","scientificNameUrl","author","rank","kingdom"],
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
    <c:if test="${fn:contains(scientificNamesError,'maxClauseCount')}">${maxClauseCountError}</c:if>
</div>

<%@ include file="/common/taglibs.jsp"%>
<div id="twopartheader">
    <h2><spring:message code="blanket.search.localities.header" text="Localities search results for:"/> <strong>${searchString}</strong> </h2>
    <p><a href="${pageContext.request.contextPath}/search/${searchString}"><spring:message code="blanket.search.allResults" text="Back to search results for all pages"/></a></p>
</div>
<c:if test="${not empty localities}">
<div id="YuiSearchResults" class=" yui-skin-sam">
    <h4><spring:message code="blanket.search.localities.header" text="Localities search results for:"/> ${searchString} (total results ${localitiesTotal})</h4>
    <div id="json"></div>
    <script type="text/javascript">
        //YAHOO.util.Event.addListener(window, "load", function() {
            YAHOO.example.XHR_JSON = function() {
                var formatNameUrl = function(elCell, oRecord, oColumn, sData) {
                    elCell.innerHTML = "<a href='" + oRecord.getData("localityUrl") +  "' title='go to localities page'>" + sData + "</a>";
                };

                var myColumnDefs = [
                    {key:"locality", label:"Locality", sortable:true, formatter:formatNameUrl},
                    {key:"state", label:"State"},
                    {key:"postcode", label:"Postcode"},
                    {key:"geoRegion", label:"Geographic Region"},
                    {key:"score", label:"Score", formatter:"number", sortable:true}
                ];

                var myDataSource = new YAHOO.util.DataSource("${pageContext.request.contextPath}/search/localities/${searchString}/json?");
                myDataSource.responseType = YAHOO.util.DataSource.TYPE_JSON;
                //myDataSource.connXhrMode = "queueRequests";
                myDataSource.responseSchema = {
                    resultsList: "result",
                    fields: [{key:"score",parser:"number"},"locality","localityUrl","state","postcode","geoRegion"],
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
</div>
</c:if>
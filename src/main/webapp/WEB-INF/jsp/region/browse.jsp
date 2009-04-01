<%@ include file="/common/taglibs.jsp" %>
<div id="twopartheader">
    <h2><spring:message code="regions.list.main.title"/></h2>
    <ul>
    	<alatag:geoRegionTypeBullet geoRegionType="${geoRegionType}" requestedType="states"/>
        <alatag:geoRegionTypeBullet geoRegionType="${geoRegionType}" requestedType="lga"/>
        <alatag:geoRegionTypeBullet geoRegionType="${geoRegionType}" requestedType="ibra"/>
        <alatag:geoRegionTypeBullet geoRegionType="${geoRegionType}" requestedType="imcra"/>
        <alatag:geoRegionTypeBullet geoRegionType="${geoRegionType}" requestedType="rivers"/>
    </ul>
</div>
<div class="subcontainer yui-skin-sam">
<h3>
    <c:if test="${geoRegionType.name == 'states'}"><spring:message code="regions.list.states.title"/></c:if>
    <c:if test="${geoRegionType.name == 'cities'}"><spring:message code="regions.list.cities.title"/></c:if>
    <c:if test="${geoRegionType.name == 'shires'}"><spring:message code="regions.list.shires.title"/></c:if>
    <c:if test="${geoRegionType.name == 'towns'}"><spring:message code="regions.list.towns.title"/></c:if>
    <c:if test="${geoRegionType.name == 'lga'}"><spring:message code="regions.list.lga.title"/></c:if>
    <c:if test="${geoRegionType.name == 'ibra'}"><spring:message code="regions.list.ibra.title"/></c:if>
    <c:if test="${geoRegionType.name == 'imcra'}"><spring:message code="regions.list.imcra.title"/></c:if>
    <c:if test="${geoRegionType.name == 'rivers'}"><spring:message code="regions.list.rivers.title"/></c:if>
</h3>
<!--
<display:table name="geoRegions" export="false" class="statistics" id="geoRegion">
	<display:column titleKey="regions.list.table.title" class="name">
			<a href="${pageContext.request.contextPath}/regions/${geoRegion.id}"><gbif:capitalize>${geoRegion.name}</gbif:capitalize></a>
	</display:column>
	<display:column titleKey="dataset.list.occurrence.count" class="countrycount">
	  	<c:if test="${geoRegion.occurrenceCount>0}"><a href="${pageContext.request.contextPath}/occurrences/search.htm?<gbif:criterion subject="36" predicate="0" value="${geoRegion.id}" index="0"/>"></c:if><fmt:formatNumber value="${geoRegion.occurrenceCount}" pattern="###,###"/><c:if test="${geoRegion.occurrenceCount>0}"></a></c:if>
	  	(<c:if test="${geoRegion.occurrenceCoordinateCount>0}"><a href="${pageContext.request.contextPath}/occurrences/search.htm?<gbif:criterion subject="36" predicate="0" value="${geoRegion.id}" index="0"/>&<gbif:criterion subject="28" predicate="0" value="0" index="1"/>"></c:if><fmt:formatNumber value="${geoRegion.occurrenceCoordinateCount}" pattern="###,###"/><c:if test="${geoRegion.occurrenceCoordinateCount>0}"></a></c:if>)
	</display:column>	  
	<display:setProperty name="basic.msg.empty_list">
	   No datasets indexed
	</display:setProperty>
	<display:setProperty name="basic.empty.showtable">false</display:setProperty>
</display:table>
-->
<div id="geoRegions"></div>
    <script type="text/javascript">
    YAHOO.example.Data1 =
      <json:object prettyPrint="true">
        <json:array name="results" var="geoRegion" items="${geoRegions}">
          <json:object>
            <json:property name="geoRegion" value="${geoRegion.name}"/>
            <json:property name="geoRegionUrl">
                ${pageContext.request.contextPath}/regions/${geoRegion.id}
            </json:property>
            <json:property name="occurrences"><fmt:formatNumber value="${geoRegion.occurrenceCount}" pattern="###,###"/></json:property>
            <json:property name="occurrencesSort">${geoRegion.occurrenceCount}</json:property>
            <json:property name="occurrencesUrl">
              ${pageContext.request.contextPath}/occurrences/searchWithTable.htm?<gbif:criterion subject="36" predicate="0" value="${geoRegion.id}" index="0"/>
            </json:property>
            <json:property name="georeferencedOccurrences"><fmt:formatNumber value="${geoRegion.occurrenceCoordinateCount}" pattern="###,###"/></json:property>
            <json:property name="georeferencedOccurrencesSort">${geoRegion.occurrenceCoordinateCount}</json:property>
            <json:property name="georeferencedOccurrencesUrl">
              ${pageContext.request.contextPath}/occurrences/searchWithTable.htm?<gbif:criterion subject="36" predicate="0" value="${geoRegion.id}" index="0"/>&<gbif:criterion subject="28" predicate="0" value="0" index="1"/>
            </json:property>
            <json:property name="geoRegionType" value="${geoRegion.geoRegionType.name}"/>
          </json:object>
        </json:array>
      </json:object>
    </script>
    <script type="text/javascript">
    YAHOO.util.Event.addListener(window, "load", function() {
        YAHOO.example.Basic = function() {
            var formatNameUrl = function(elCell, oRecord, oColumn, sData) {
                elCell.innerHTML = "<a href='" + oRecord.getData("geoRegionUrl") +  "' title='View detailed information on this geographic region'>" + sData + "</a>";
            };
            var formatOccurrencesUrl = function(elCell, oRecord, oColumn, sData) {
                elCell.innerHTML = "<a href='" + oRecord.getData("occurrencesUrl") + "' title='View occurrence records for this region'>" + sData + "</a>";
            };
            var formatGeoreferencedOccurrencesUrl = function(elCell, oRecord, oColumn, sData) {
                elCell.innerHTML = "<a href='" + oRecord.getData("georeferencedOccurrencesUrl") + "' title='View georeferenced occurrence records for this region'>" + sData + "</a>";
            };

            var myColumnDefs = [
                {key:"geoRegion", label:"Geographic Region", sortable:true, formatter:formatNameUrl},
                {key:"geoRegionType", label:"Region Type", sortable:true},
                {label:"Occurrences",
                  children: [
                    {key:"occurrences", label:"All", formatter:formatOccurrencesUrl, sortable:true, sortOptions:{field:"occurrencesSort"}},
                    {key:"georeferencedOccurrences", label:"Georeferenced", formatter:formatGeoreferencedOccurrencesUrl, sortable:true, sortOptions:{field:"georeferencedOccurrencesSort"}}
                  ]
                }
            ];

            var myDataSource = new YAHOO.util.DataSource(YAHOO.example.Data1.results);
            myDataSource.responseType = YAHOO.util.DataSource.TYPE_JSARRAY;
            myDataSource.responseSchema = {
                fields: ["geoRegion","geoRegionUrl","occurrences",{key:"occurrencesSort",parser:"number"},"occurrencesUrl",
                         "georeferencedOccurrences",{key:"georeferencedOccurrencesSort",parser:"number"},"georeferencedOccurrencesUrl","geoRegionType"]
            };
            var oConfigs = {
	                sortedBy:{key:"geoRegionType", dir:"asc"},
                    paginator: new YAHOO.widget.Paginator({rowsPerPage: 50}),
	                initialRequest: "results=${fn:length(geoRegions)}"
	        };
            var myDataTable = new YAHOO.widget.DataTable("geoRegions", myColumnDefs, myDataSource, oConfigs);

            return {
                oDS: myDataSource,
                oDT: myDataTable
            };
        }();
    });
    </script>
</div>
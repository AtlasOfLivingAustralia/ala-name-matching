<%@ include file="/common/taglibs.jsp"%>
<div id="twopartheader">
    <h2><spring:message code="blanket.search.header"/> <strong>${searchString}</strong> </h2>
</div>
<table id="results" style="width:100%;">
<tr>
<td id="resultsSummary" style="vertical-align: top; width:20%; padding-top:25px; padding-right :3px;">
	<ul>
        <li><a href="#taxonConcepts">Scientific names (${taxonConceptsTotal})</a></li>
	    <li><a href="#commonNames">Common names (${commonNamesTotal})</a></li>
	    <li><a href="#geoRegions">Geographic Regions (${geoRegionsTotal})</a></li>
	    <li><a href="#localities">Localities (${localitiesTotal})</a></li>
	    <li><a href="#dataResources">Data Resources (${dataResourcesTotal})</a></li>
	    <li><a href="#dataProviders">Data Provider (${dataProvidersTotal})</a></li>
	    <!-- <li><a href="#institutions">Institutions (0)</a></li> -->
	</ul>
</td><!-- resultsSummary -->

<td id="resultsBreakdown" style="vertical-align:top; padding-top:0px;">
<c:if test="${taxonConceptsTotal==0 && commonNamesTotal==0 && geoRegionsTotal==0 && localitiesTotal==0 && dataResourcesTotal==0 && dataProvidersTotal==0}">
    <div class="moreSearchResults">Search for &quot;${searchString}&quot; returned no results</div>
</c:if>
<div class=" yui-skin-sam">
<% /** Taxon Names  */%>
<c:if test="${taxonConceptsTotal>0 || fn:length(taxonConceptsError)>0}">
    <a name="taxonConcepts"></a>
    <h2 class="scNames"><spring:message code="blanket.search.scientific.names.title" text="Scientific names"/></h2>
    <div id="taxonConcepts"></div>
    <script type="text/javascript">
    YAHOO.example.Data1 =
      <json:object prettyPrint="true">
        <json:array name="results" var="taxonConcept" items="${taxonConcepts}">
          <json:object>
            <json:property name="scientificName" value="${taxonConcept.taxonName.canonical}"/>
            <json:property name="scientificNameUrl" value="${pageContext.request.contextPath}/species/${taxonConcept.id}"/>
            <json:property name="rank"><alatag:taxonRankfromInt rankValue="${taxonConcept.rank}"/></json:property>
            <json:property name="family" value="${taxonConcept.familyConcept.taxonName.canonical}"/>
            <json:property name="kingdom" value="${taxonConcept.kingdomConcept.taxonName.canonical}"/>
          </json:object>
        </json:array>
      </json:object>
    </script>
    <script type="text/javascript">
    YAHOO.util.Event.addListener(window, "load", function() { 
        YAHOO.example.Basic = function() {
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
                {key:"scientificName", label:"Scientific Name", formatter:formatNameUrl},
                {key:"rank", label:"Taxon Rank"},
                {key:"family", label:"Family"},
                {key:"kingdom", label:"Kingdom"}
            ];

            var myDataSource = new YAHOO.util.DataSource(YAHOO.example.Data1.results);
            myDataSource.responseType = YAHOO.util.DataSource.TYPE_JSARRAY;
            myDataSource.responseSchema = {
                fields: ["scientificName","scientificNameUrl","rank","family","kingdom"]
            };

            var myDataTable = new YAHOO.widget.DataTable("taxonConcepts", myColumnDefs, myDataSource, {});

            return {
                oDS: myDataSource,
                oDT: myDataTable
            };
        }();
    });
    </script>
    <c:if test="${taxonConceptsTotal>10}">
        <div class="moreSearchResults">
            <a href="${pageContext.request.contextPath}/search/scientificNames/${searchString}">View all scientific names matching &quot;${searchString}&quot;</a>
        </div>
    </c:if>
</c:if>
<c:if test="${fn:contains(taxonConceptsError,'maxClauseCount')}"><div class="searchError"><spring:message code="blanket.search.error.maxClauseCount"/></div></c:if>


<% /** Common Names  */%>
<c:if test="${commonNamesTotal>0 || fn:length(commonNamesError)>0}">
    <a name="commonNames"></a>
    <h2 class="scNames"><spring:message code="blanket.search.common.names.title" text="Common names"/></h2>
    <div id="commonNames"></div>
    <script type="text/javascript">
    YAHOO.example.Data2 =
      <json:object prettyPrint="true">
        <json:array name="results" var="commonName" items="${commonNames}">
          <json:object>
            <json:property name="commonName" value="${commonName.name}"/>
            <json:property name="commonNameUrl" value="${pageContext.request.contextPath}/species/${commonName.taxonConcept.id}/commonName/${commonName.name}"/>
            <json:property name="scientificName" value="${commonName.taxonConcept.taxonName.canonical}"/>
            <json:property name="rank"><alatag:taxonRankfromInt rankValue="${commonName.taxonConcept.rank}"/></json:property>
            <json:property name="kingdom" value="${commonName.taxonConcept.kingdomConcept.taxonName.canonical}"/>
          </json:object>
        </json:array>
      </json:object>
    </script>
    <script type="text/javascript">
    YAHOO.util.Event.addListener(window, "load", function() {
        YAHOO.example.Basic = function() {
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
                {key:"commonName", label:"Common Name", formatter:formatCommonNameUrl},
                {key:"scientificName", label:"Scientific Name", formatter:formatScientificName},
                {key:"rank", label:"Taxon Rank"},
                {key:"kingdom", label:"Kingdom"}
            ];

            var myDataSource = new YAHOO.util.DataSource(YAHOO.example.Data2.results);
            myDataSource.responseType = YAHOO.util.DataSource.TYPE_JSARRAY;
            myDataSource.responseSchema = {
                fields: ["commonName","commonNameUrl","scientificName","rank","kingdom"]
            };

            var myDataTable = new YAHOO.widget.DataTable("commonNames", myColumnDefs, myDataSource, {});

            return {
                oDS: myDataSource,
                oDT: myDataTable
            };
        }();
    });
    </script>
    <c:if test="${commonNamesTotal>10}">
        <div class="moreSearchResults">
            <a href="${pageContext.request.contextPath}/search/commonNames/${searchString}">View all common names matching &quot;${searchString}&quot;</a>
        </div>
    </c:if>
</c:if>
<c:if test="${fn:contains(commonNamesError,'maxClauseCount')}"><div class="searchError"><spring:message code="blanket.search.error.maxClauseCount"/></div></c:if>


<% /** Geo Regions  */%>
<c:if test="${geoRegionsTotal>0 || fn:length(geoRegionsError)>0}">
    <a name="geoRegions"></a>
    <h2 class="scNames">Geographic Regions</h2>
    <div id="geoRegions"></div>
    <script type="text/javascript">
    YAHOO.example.Data3 =
      <json:object prettyPrint="true">
        <json:array name="results" var="geoRegion" items="${geoRegions}">
          <json:object>
            <json:property name="geoRegion" value="${geoRegion.name}"/>
            <json:property name="geoRegionUrl">
                ${pageContext.request.contextPath}/regions/${geoRegion.id}
            </json:property>
            <json:property name="acronym" value="${geoRegion.acronym}"/>
            <json:property name="geoRegionType" value="${geoRegion.geoRegionType.name}"/>
          </json:object>
        </json:array>
      </json:object>
    </script>
    <script type="text/javascript">
    YAHOO.util.Event.addListener(window, "load", function() {
        YAHOO.example.Basic = function() {
            var formatNameUrl = function(elCell, oRecord, oColumn, sData) {
                elCell.innerHTML = "<a href='" + oRecord.getData("geoRegionUrl") +  "' title='go to geographic regions page'>" + sData + "</a>";
            };

            var myColumnDefs = [
                {key:"geoRegion", label:"Geographic Region", formatter:formatNameUrl},
                {key:"acronym", label:"Acronym"},
                {key:"geoRegionType", label:"Region Type"}
            ];

            var myDataSource = new YAHOO.util.DataSource(YAHOO.example.Data3.results);
            myDataSource.responseType = YAHOO.util.DataSource.TYPE_JSARRAY;
            myDataSource.responseSchema = {
                fields: ["geoRegion","geoRegionUrl","acronym","geoRegionType"]
            };

            var myDataTable = new YAHOO.widget.DataTable("geoRegions", myColumnDefs, myDataSource, {});

            return {
                oDS: myDataSource,
                oDT: myDataTable
            };
        }();
    });
    </script>
    <c:if test="${geoRegionsTotal>10}">
        <div class="moreSearchResults">
            <a href="${pageContext.request.contextPath}/search/geoRegions/${searchString}">View all geographic regions matching &quot;${searchString}&quot;</a>
        </div>
    </c:if>
</c:if>
<c:if test="${fn:contains(geoRegionsError,'maxClauseCount')}"><div class="searchError"><spring:message code="blanket.search.error.maxClauseCount"/></div></c:if>

<% /** Localities */%>
<c:if test="${localitiesTotal>0 || fn:length(localitiesError)>0}">
    <a name="localities"></a>
    <h2 class="scNames">Localities</h2>
    <div id="localities"></div>
    <script type="text/javascript">
    YAHOO.example.Data4 =
      <json:object prettyPrint="true">
        <json:array name="results" var="locality" items="${localities}">
          <json:object>
            <json:property name="locality" value="${locality.name}"/>
            <json:property name="localityUrl">
              ${pageContext.request.contextPath}/regions/${locality.geoRegion.id}/locality/${locality.id}?map=google
            </json:property>
            <json:property name="state" value="${locality.state}"/>
            <json:property name="postcode" value="${locality.postcode}"/>
            <json:property name="geoRegion" value="${locality.geoRegion.name}"/>
          </json:object>
        </json:array>
      </json:object>
    </script>
    <script type="text/javascript">
    YAHOO.util.Event.addListener(window, "load", function() {
        YAHOO.example.Basic = function() {
            var formatNameUrl = function(elCell, oRecord, oColumn, sData) {
                elCell.innerHTML = "<a href='" + oRecord.getData("localityUrl") +  "' title='go to localities page'>" + sData + "</a>";
            };

            var myColumnDefs = [
                {key:"locality", label:"Locality", formatter:formatNameUrl},
                {key:"state", label:"State"},
                {key:"postcode", label:"Postcode"},
                {key:"geoRegion", label:"Geographic Region"}
            ];

            var myDataSource = new YAHOO.util.DataSource(YAHOO.example.Data4.results);
            myDataSource.responseType = YAHOO.util.DataSource.TYPE_JSARRAY;
            myDataSource.responseSchema = {
                fields: ["locality","localityUrl","state","postcode","geoRegion"]
            };

            var myDataTable = new YAHOO.widget.DataTable("localities", myColumnDefs, myDataSource, {});

            return {
                oDS: myDataSource,
                oDT: myDataTable
            };
        }();
    });
    </script>
    <c:if test="${localitiesTotal>10}">
        <div class="moreSearchResults">
            <a href="${pageContext.request.contextPath}/search/localities/${searchString}">View all localities matching &quot;${searchString}&quot;</a>
        </div>
    </c:if>
</c:if>
<c:if test="${fn:contains(localitiesError,'maxClauseCount')}"><div class="searchError"><spring:message code="blanket.search.error.maxClauseCount"/></div></c:if>

<% /** Data Resource  */%>
<c:if test="${dataResourcesTotal>0 || fn:length(dataResourcesError)>0}">
    <a name="dataResources"></a>
    <h2 class="scNames">Data Resources</h2>
    <div id="dataResources"></div>
    <script type="text/javascript">
    YAHOO.example.Data5 =
      <json:object prettyPrint="true">
        <json:array name="results" var="dataResource" items="${dataResources}">
          <json:object>
            <json:property name="dataResource" value="${dataResource.name}"/>
            <json:property name="dataResourceUrl">
              ${pageContext.request.contextPath}/datasets/resource/${dataResource.id}
            </json:property>
            <json:property name="occurrences" value="${dataResource.occurrenceCount}"/>
          </json:object>
        </json:array>
      </json:object>
    </script>
    <script type="text/javascript">
    YAHOO.util.Event.addListener(window, "load", function() {
        YAHOO.example.Basic = function() {
            var formatNameUrl = function(elCell, oRecord, oColumn, sData) {
                elCell.innerHTML = "<a href='" + oRecord.getData("dataResourceUrl") +  "' title='go to data resource page'>" + sData + "</a>";
            };

            var myColumnDefs = [
                {key:"dataResource", label:"Data Resource", formatter:formatNameUrl},
                {key:"occurrences", label:"Occurrences Count"}
            ];

            var myDataSource = new YAHOO.util.DataSource(YAHOO.example.Data5.results);
            myDataSource.responseType = YAHOO.util.DataSource.TYPE_JSARRAY;
            myDataSource.responseSchema = {
                fields: ["dataResource","dataResourceUrl","occurrences"]
            };

            var myDataTable = new YAHOO.widget.DataTable("dataResources", myColumnDefs, myDataSource, {});

            return {
                oDS: myDataSource,
                oDT: myDataTable
            };
        }();
    });
    </script>
    <c:if test="${dataResourcesTotal>10}">
        <div class="moreSearchResults">
            <a href="${pageContext.request.contextPath}/search/dataResources/${searchString}">View all data resources matching &quot;${searchString}&quot;</a>
        </div>
    </c:if>
</c:if>
<c:if test="${fn:contains(dataResourcesError,'maxClauseCount')}"><div class="searchError"><spring:message code="blanket.search.error.maxClauseCount"/></div></c:if>

<% /** Data Providers */%>
<c:if test="${dataProvidersTotal>0 || fn:length(dataProvidersError)>0}">
    <a name="dataProviders"></a>
    <h2 class="scNames">Data Providers</h2>
    <div id="dataProviders"></div>
    <script type="text/javascript">
    YAHOO.example.Data6 =
      <json:object prettyPrint="true">
        <json:array name="results" var="dataProvider" items="${dataProviders}">
          <json:object>
            <json:property name="dataProvider" value="${dataProvider.name}"/>
            <json:property name="dataProviderUrl">
              ${pageContext.request.contextPath}/datasets/provider/${dataProvider.id}
            </json:property>
            <json:property name="dataResourceCount" value="${dataProvider.dataResourceCount}"/>
            <json:property name="occurrences" value="${dataProvider.occurrenceCount}"/>
          </json:object>
        </json:array>
      </json:object>
    </script>
    <script type="text/javascript">
    YAHOO.util.Event.addListener(window, "load", function() {
        YAHOO.example.Basic = function() {
            var formatNameUrl = function(elCell, oRecord, oColumn, sData) {
                elCell.innerHTML = "<a href='" + oRecord.getData("dataProviderUrl") +  "' title='go to data provider page'>" + sData + "</a>";
            };

            var myColumnDefs = [
                {key:"dataProvider", label:"Data Providers", formatter:formatNameUrl},
                {key:"dataResourceCount", label:"Resource Count"},
                {key:"occurrences", label:"Occurrences Count"}
            ];

            var myDataSource = new YAHOO.util.DataSource(YAHOO.example.Data6.results);
            myDataSource.responseType = YAHOO.util.DataSource.TYPE_JSARRAY;
            myDataSource.responseSchema = {
                fields: ["dataProvider","dataProviderUrl","dataResourceCount","occurrences"]
            };

            var myDataTable = new YAHOO.widget.DataTable("dataProviders", myColumnDefs, myDataSource, {});

            return {
                oDS: myDataSource,
                oDT: myDataTable
            };
        }();
    });
    </script>
    <c:if test="${dataProvidersTotal>10}">
        <div class="moreSearchResults">
            <a href="${pageContext.request.contextPath}/search/dataProviders/${searchString}">View all data providers matching &quot;${searchString}&quot;</a>
        </div>
    </c:if>
</c:if>
<c:if test="${fn:contains(dataProvidersError,'maxClauseCount')}"><div class="searchError"><spring:message code="blanket.search.error.maxClauseCount"/></div></c:if>
</div>
</td><!-- resultsBreakdown -->
</tr>
</table>
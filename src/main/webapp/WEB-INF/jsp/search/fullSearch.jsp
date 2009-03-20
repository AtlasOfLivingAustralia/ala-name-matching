<%@ include file="/common/taglibs.jsp"%>
<div id="twopartheader">
    <h2><spring:message code="blanket.search.header"/> <strong>${searchString}</strong> </h2>
</div>
<table id="results" style="width:100%;">
<tr>
<td id="resultsSummary" style="vertical-align: top; width:13%; padding-top:30px; padding-right :3px; border-right: 1px solid #CCCCCC;">
	<ul>
        <li><a href="#taxonConcepts">Scientific names (${taxonConceptsTotal})</a></li>
	    <li><a href="#commonNames">Common names (${commonNamesTotal})</a></li>
	    <li><a href="#geoRegions">Geographic Regions (${geoRegionsTotal})</a></li>
	    <li><a href="#localities">Localities (${localitiesTotal})</a></li>
	    <li><a href="#dataResources">Data Resources (${dataResourcesTotal})</a></li>
	    <li><a href="#dataProviders">Data Provider (${dataProvidersTotal})</a></li>
	    <li><a href="#institutions">Institutions (0)</a></li>
	</ul>
</td><!-- resultsSummary -->

<td id="resultsBreakdown" style="vertical-align:top;">

<% /** Taxon Names  */%>
<c:if test="${taxonConceptsTotal>0}">
<a name="taxonConcepts">
    <h2 class="scNames"><spring:message code="blanket.search.scientific.names.title" text="Scientific names"/></h2>
</a>
<table cellspacing="1" width="100%">
    <tbody>
     <c:forEach items="${taxonConcepts}" var="taxonConcept" varStatus="status">
        <tr valign="top">
            <td class="tdColumn2" >
                <p class="column2">
                    <span class="speciesName">
                        <a href="${pageContext.request.contextPath}/species/${taxonConcept.id}">
                            ${taxonConcept.taxonName.canonical} ${taxonConcept.taxonName.author}
                        </a>
                        - ${taxonConcept.kingdomConcept.taxonName.canonical} ${taxonConcept.familyConcept.taxonName.canonical}
                    </span>
                </p>
            </td>
        </tr>
      </c:forEach>
    </tbody>
</table>
<div class="moreSearchResults">
    <a href="${pageContext.request.contextPath}/search/scientificNames/${searchString}">View all scientific names matching &quot;${searchString}&quot;</a>
</div>
</c:if>

<% /** Common Names  */%>
<c:if test="${commonNamesTotal>0}">
<a name="commonNames">
    <h2 class="scNames"><spring:message code="blanket.search.common.names.title" text="Common names"/></h2>
</a>
<table cellspacing="1" width="100%">
    <tbody>
    <c:forEach items="${commonNames}" var="commonName" varStatus="status">
        <tr valign="top">
            <td class="tdColumn2" >
                <p class="column2">
                    <span class="speciesName">
                        <a href="${pageContext.request.contextPath}/species/${commonName.taxonConcept.id}/commonName/${commonName.name}">
                            <gbif:capitalize>${commonName.name}</gbif:capitalize>
                        </a>
                        - Scientific name: ${commonName.taxonConcept.taxonName.canonical}
                        
                        ${commonName.taxonConcept.kingdomConcept.taxonName.canonical}
                    </span>
                </p>
            </td>
        </tr>
    </c:forEach>
    </tbody>
</table>
<div class="moreSearchResults">
    <a href="${pageContext.request.contextPath}/search/commonNames/${searchString}">View all common names matching &quot;${searchString}&quot;</a>
</div>
</c:if>

<% /** Geo Regions  */%>
<c:if test="${geoRegionsTotal>0}">
<a name="geoRegions">
    <h2 class="scNames">Geographic Regions</h2>
</a>
<table cellspacing="1" width="100%">
    <tbody>
     <c:forEach items="${geoRegions}" var="geoRegion" varStatus="status">
        <tr valign="top">
            <td class="tdColumn2" >
                <p class="column2">
                    <span class="speciesName">
                        <a href="${pageContext.request.contextPath}/regions/${geoRegion.id}">
                            <gbif:capitalize>${geoRegion.name}</gbif:capitalize>
                        </a>
                         - <gbif:capitalize>${geoRegion.geoRegionType.name}</gbif:capitalize>
                    </span>
                </p>
            </td>
        </tr>
      </c:forEach>
    </tbody>
</table>
<div class="moreSearchResults">
    <a href="${pageContext.request.contextPath}/search/geoRegions/${searchString}">View all geographic regions matching &quot;${searchString}&quot;</a>
</div>
</c:if>

<% /** Localities  */%>
<c:if test="${localitiesTotal>0}">
<a name="localities">
    <h2 class="scNames">Localities</h2>
</a>
<table cellspacing="1" width="100%">
    <tbody>
     <c:forEach items="${localities}" var="locality" varStatus="status">
        <tr valign="top">
            <td class="tdColumn2" >
               <a href="${pageContext.request.contextPath}/regions/${locality.geoRegion.id}/locality/${locality.id}">${locality.name}</a> - ${locality.geoRegion.name}, ${locality.postcode}
            </td>
        </tr>
      </c:forEach>
    </tbody>
</table>
<div class="moreSearchResults">
    <a href="${pageContext.request.contextPath}/search/localities/${searchString}">View all localities matching &quot;${searchString}&quot;</a>
</div>
</c:if>

<% /** Data Resource  */%>
<c:if test="${dataResourcesTotal>0}">
<a name="dataResources">
    <h2 class="scNames">Data Resources</h2>
</a>
<table cellspacing="1" width="100%">
    <tbody>
     <c:forEach items="${dataResources}" var="dataResource" varStatus="status">
        <tr valign="top">
            <td class="tdColumn2" >
                <p class="column2">
                    <span class="speciesName">
                        <a href="${pageContext.request.contextPath}/datasets/resource/${dataResource.id}">${dataResource.name}</a>
                        - With ${dataResource.occurrenceCount} occurrences
                    </span>
                </p>
            </td>
        </tr>
      </c:forEach>
    </tbody>
</table>
<div class="moreSearchResults">
    <a href="${pageContext.request.contextPath}/search/dataResources/${searchString}">View all data resources matching &quot;${searchString}&quot;</a>
</div>
</c:if>

<% /** Data Providers  */%>
<c:if test="${dataProvidersTotal>0}">
<a name="dataProviders">
    <h2 class="scNames">Data Providers</h2>
</a>
<table cellspacing="1" width="100%">
    <tbody>
     <c:forEach items="${dataProviders}" var="dataProvider" varStatus="status">
        <tr valign="top">
            <td class="tdColumn2" >
                <p class="column2">
                    <span class="speciesName">
                        <a href="${pageContext.request.contextPath}/datasets/provider/${dataProvider.id}">${dataProvider.name}</a>
                         - With ${dataProvider.dataResourceCount} resources
                    </span>
                </p>
            </td>
        </tr>
      </c:forEach>
    </tbody>
</table>
<div class="moreSearchResults">
    <a href="${pageContext.request.contextPath}/search/dataProviders/${searchString}">View all data providers matching &quot;${searchString}&quot;</a>
</div>
</c:if>

</td><!-- resultsBreakdown -->
</tr>
</table>
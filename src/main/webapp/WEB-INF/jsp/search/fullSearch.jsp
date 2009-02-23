<%@ include file="/common/taglibs.jsp"%>
<div id="twopartheader">
    <h2><spring:message code="blanket.search.header"/> <strong>${searchString}</strong> </h2>
</div>
<table id="results" style="width:100%;">
<tr>
<td id="resultsSummary" style="vertical-align: top; width:13%; padding-top:30px; padding-right :3px; border-right: 1px solid #CCCCCC;">
	<ul>
	    <li>Scientific names (${taxonConceptsTotal})</li>
	    <li>Common names (${commonNamesTotal})</li>
	    <li>Geographic Regions (${geoRegionsTotal})</li>
	    <li>Localities (${localitiesTotal})</li>
	    <li>Data Resources (${dataResourcesTotal})</li>
	    <li>Data Provider (${dataProvidersTotal})</li>
	    <li>Institutions (0)</li>
	</ul>
</td><!-- resultsSummary -->

<td id="resultsBreakdown" style="vertical-align:top;">

<% /** Taxon Names  */%>
<c:if test="${taxonConceptsTotal>0}">
<a name="Names2">
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
                    </span>
                </p>
                <p>
                    ${taxonConcept.kingdomConcept.taxonName.canonical} ${taxonConcept.familyConcept.taxonName.canonical}
                </p>
            </td>
        </tr>
      </c:forEach>
    </tbody>
</table>
</c:if>

<% /** Common Names  */%>
<c:if test="${commonNamesTotal>0}">
<a name="Names2">
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
                    </span>
                </p>
            </td>
        </tr>
      </c:forEach>
    </tbody>
</table>
</c:if>

<% /** Geo Regions  */%>
<c:if test="${geoRegionsTotal>0}">
<a name="Names2">
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
                            <gbif:capitalizeFirstChar>${geoRegion.name}</gbif:capitalizeFirstChar>
                        </a>
                         - <gbif:capitalizeFirstChar>${geoRegion.geoRegionType.name}</gbif:capitalizeFirstChar>
                    </span>
                </p>
            </td>
        </tr>
      </c:forEach>
    </tbody>
</table>
</c:if>

<% /** Localities  */%>
<c:if test="${localitiesTotal>0}">
<a name="Names2">
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
</c:if>

<% /** Data Resource  */%>
<c:if test="${dataResourcesTotal>0}">
<a name="Names2">
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
</c:if>

<% /** Data Providers  */%>
<c:if test="${dataProvidersTotal>0}">
<a name="Names2">
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
</c:if>

</td><!-- resultsBreakdown -->
</tr>
</table>
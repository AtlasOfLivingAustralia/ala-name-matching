<%@ include file="/common/taglibs.jsp"%>
<div id="twopartheader">
	<h2>
		<string:capitalize>${taxonConcept.rank}</string:capitalize>: 
		<span class="subject"><gbif:taxonPrint concept="${taxonConcept}"/> <c:if test="${not empty taxonConcept.author}">${taxonConcept.author}</c:if></span>
	</h2>
	<h3>
		<c:choose>
	    <c:when test="${not empty taxonConcept.acceptedTaxonName}">
        synonym for <a href="${pageContext.request.contextPath}/species/${taxonConcept.acceptedConceptKey}">${taxonConcept.acceptedTaxonName}</a>
        <c:choose>
					<c:when test="${not empty commonName}">
						<gbif:capitalize>(${commonName})</gbif:capitalize>
					</c:when>
					<c:when test="${not empty taxonConcept.commonName}">
						<gbif:capitalize>(${taxonConcept.commonName})</gbif:capitalize>
					</c:when>
        </c:choose>
    	</c:when>
			<c:when test="${not empty commonName}">
				<gbif:capitalize>${commonName}</gbif:capitalize>
			</c:when>
			<c:otherwise>
				<gbif:capitalize>${taxonConcept.commonName}</gbif:capitalize>
			</c:otherwise>
		</c:choose>
	</h3>
</div>

<tiles:insert page="hierarchy.jsp"/>

<tiles:insert page="actions.jsp"/>

<tiles:insert page="names.jsp"/>

<tiles:insert page="specimenTypifications.jsp"/>

<tiles:insert page="warnings.jsp"/>

<gbif:isMajorRank concept="${taxonConcept}">
<div class="subcontainer" title="${taxonConcept.taxonName}<c:if test="${not empty commonName}">(${commonName})</c:if> occurrences distribution map">
	<h4><spring:message code="occurrence.overview"/></h4>
	<tiles:insert page="occurrences.jsp"/>
</div><!-- occurrence overview sub container -->	
</gbif:isMajorRank>

<jsp:include page="charts.jsp"/>

<tiles:insert page="images.jsp"/>
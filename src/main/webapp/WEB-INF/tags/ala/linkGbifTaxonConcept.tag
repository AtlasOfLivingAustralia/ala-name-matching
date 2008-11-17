<%@ include file="/common/taglibs.jsp" %>
<%@ attribute name="concept" required="true" rtexprvalue="true" type="org.gbif.portal.dto.taxonomy.BriefTaxonConceptDTO" %>

<c:choose>
    <c:when test="${not empty concept}">
        <gbiftag:taxonLink concept="${concept}"/>
    </c:when>
    <c:otherwise>
        (<spring:message code="taxonomy.not.available"/>)
    </c:otherwise>
</c:choose>
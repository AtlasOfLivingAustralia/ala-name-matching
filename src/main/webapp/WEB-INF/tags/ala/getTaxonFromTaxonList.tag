<%@ include file="/common/taglibs.jsp" %>
<%@ attribute name="concepts" required="true" rtexprvalue="true" type="java.util.List" %>
<%@ attribute name="requestedConcept" required="true" %>
<%@ variable name-given="requestedTaxonRank" variable-class="org.gbif.portal.dto.taxonomy.BriefTaxonConceptDTO" scope="AT_END" %>
<c:forEach items="${concepts}" var="concept">
    <c:if test="${concept.rank == requestedConcept}">
         <c:set var="requestedTaxonRank" value="${concept}"/>
    </c:if>
</c:forEach>

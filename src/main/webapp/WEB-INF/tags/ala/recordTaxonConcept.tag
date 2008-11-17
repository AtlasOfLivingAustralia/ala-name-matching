<%@ include file="/common/taglibs.jsp" %>
<%@ attribute name="rawOccurrenceRecord" required="true" rtexprvalue="true" type="org.gbif.portal.dto.taxonomy.BriefTaxonConceptDTO" %>
<%@ attribute name="subspeciesConcept" required="true" rtexprvalue="true" type="org.gbif.portal.dto.taxonomy.BriefTaxonConceptDTO" %>
<c:if test="${not empty rawOccurrenceRecord.subspecies}">
    <c:choose>
        <c:when test="${rawOccurrenceRecord.subspecies==subspeciesConcept.taxonName}">
            <gbiftag:taxonLink concept="${subspeciesConcept}"/>
        </c:when>
        <c:otherwise>
            ${rawOccurrenceRecord.subspecies} <gbiftag:interpretedTaxon concept="${subspeciesConcept}"/>
        </c:otherwise>
    </c:choose>
</c:if>
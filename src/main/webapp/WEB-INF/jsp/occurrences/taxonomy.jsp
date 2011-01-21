<%@ include file="/common/taglibs.jsp"%>
<table id="taxonomyTable" class="occurrenceTable" cellspacing="0">
    <alatag:occurrenceTableRow messageCode="occurrence.record.scientificName" section="taxonomy" fieldName="scientificName" annotate="true">
        <c:choose>
            <c:when test="${empty occurrenceRecord}">
                <span id="occurrenceRecord-${rawOccurrenceRecord.key}-taxonomy-scientificName">${rawOccurrenceRecord.scientificName}</span>
                <span id="occurrenceRecord-${rawOccurrenceRecord.key}-taxonomy-author">${rawOccurrenceRecord.author}</span>
            </c:when>
            <c:when test="${rawOccurrenceRecord.scientificName==taxonConcept.taxonName && (((empty rawOccurrenceRecord.author) && (empty taxonConcept.author)) || (rawOccurrenceRecord.author==taxonConcept.author))}">
                <gbiftag:taxonLink concept="${taxonConcept}"/>
                <span id="occurrenceRecord-${rawOccurrenceRecord.key}-taxonomy-author">${rawOccurrenceRecord.author}</span>
            </c:when>
            <c:otherwise>
                <span id="occurrenceRecord-${rawOccurrenceRecord.key}-taxonomy-scientificName" class="genera">${rawOccurrenceRecord.scientificName}</span>
                <span id="occurrenceRecord-${rawOccurrenceRecord.key}-taxonomy-author">${rawOccurrenceRecord.author}</span>
                <c:set var="interpretedSciName">
                 <c:if test="${not empty taxonConcept.partnerConceptKey}">
                     <a href="${pageContext.request.contextPath}/species/${taxonConcept.partnerConceptKey}">
                 </c:if>
                 <span class="genera">${taxonConcept.taxonName}</span>
                     <c:if test="${not empty taxonConcept.partnerConceptKey}"></a></c:if>
                     ${partnerConcept.author}
                </c:set>
                <spring:message code="occurrence.record.interpreted.as" arguments="${interpretedSciName}" argumentSeparator="$$$"/>
            </c:otherwise>
        </c:choose>
    </alatag:occurrenceTableRow>
    <alatag:occurrenceTableRow messageCode="" section="taxonomy" 
                               fieldName="heading" annotate="false" headingName="Matched Classification">
        Recorded Classification
    </alatag:occurrenceTableRow>
    <alatag:getTaxonFromTaxonList concepts="${concepts}" requestedConcept="kingdom"/><!-- populates requestedTaxonRank -->
    <alatag:occurrenceTableRow messageCode="taxonrank.kingdom" section="taxonomy"
                               fieldName="kingdom" annotate="true" matchedTaxa="${requestedTaxonRank}">
        <c:if test="${not empty rawOccurrenceRecord.kingdom}">
            <c:choose>
                <c:when test="${rawOccurrenceRecord.kingdom==kingdomConcept.taxonName}">
                    <gbiftag:taxonLink concept="${kingdomConcept}"/>
                </c:when>
                <c:otherwise>
                    <span id="occurrenceRecord-${rawOccurrenceRecord.key}-taxonomy-kingdom">${rawOccurrenceRecord.kingdom}</span>
                     <gbiftag:interpretedTaxon concept="${kingdomConcept}"/>
                </c:otherwise>
            </c:choose>
        </c:if>
    </alatag:occurrenceTableRow>
    <alatag:getTaxonFromTaxonList concepts="${concepts}" requestedConcept="phylum"/><!-- populates requestedTaxonRank -->
    <alatag:occurrenceTableRow messageCode="taxonrank.phylum" section="taxonomy"
                               fieldName="phylum" annotate="true" matchedTaxa="${requestedTaxonRank}">
        <c:if test="${not empty rawOccurrenceRecord.phylum}">
            <c:choose>
                <c:when test="${rawOccurrenceRecord.phylum==phylumConcept.taxonName}">
                    <gbiftag:taxonLink concept="${phylumConcept}"/>
                </c:when>
                <c:otherwise>
                    <span id="occurrenceRecord-${rawOccurrenceRecord.key}-taxonomy-phylum">${rawOccurrenceRecord.phylum}</span>
                    <gbiftag:interpretedTaxon concept="${phylumConcept}"/>
                </c:otherwise>
            </c:choose>
        </c:if>
    </alatag:occurrenceTableRow>
    <alatag:getTaxonFromTaxonList concepts="${concepts}" requestedConcept="class"/><!-- populates requestedTaxonRank -->
    <alatag:occurrenceTableRow messageCode="taxonrank.class" section="taxonomy"
                               fieldName="class" annotate="true" matchedTaxa="${requestedTaxonRank}">
        <c:if test="${not empty rawOccurrenceRecord.bioClass}">
            <c:choose>
                <c:when test="${rawOccurrenceRecord.phylum==classConcept.taxonName}">
                    <gbiftag:taxonLink concept="${classConcept}"/>
                </c:when>
                <c:otherwise>
                    <span id="occurrenceRecord-${rawOccurrenceRecord.key}-taxonomy-class">${rawOccurrenceRecord.bioClass}</span>
                    <gbiftag:interpretedTaxon concept="${classConcept}"/>
                </c:otherwise>
            </c:choose>
        </c:if>
    </alatag:occurrenceTableRow>
    <alatag:getTaxonFromTaxonList concepts="${concepts}" requestedConcept="order" /><!-- populates requestedTaxonRank -->
    <alatag:occurrenceTableRow messageCode="taxonrank.order" section="taxonomy"
                               fieldName="order" annotate="true" matchedTaxa="${requestedTaxonRank}">
        <c:if test="${not empty rawOccurrenceRecord.order}">
            <c:choose>
                <c:when test="${rawOccurrenceRecord.order==orderConcept.taxonName}">
                    <gbiftag:taxonLink concept="${orderConcept}"/>
                </c:when>
                <c:otherwise>
                    <span id="occurrenceRecord-${rawOccurrenceRecord.key}-taxonomy-order">${rawOccurrenceRecord.order}</span>
                    <gbiftag:interpretedTaxon concept="${orderConcept}"/>
                </c:otherwise>
            </c:choose>
        </c:if>
    </alatag:occurrenceTableRow>
    <alatag:getTaxonFromTaxonList concepts="${concepts}" requestedConcept="family" /><!-- populates requestedTaxonRank -->
    <alatag:occurrenceTableRow messageCode="taxonrank.family" section="taxonomy"
                               fieldName="family" annotate="true" matchedTaxa="${requestedTaxonRank}">
        <c:if test="${not empty rawOccurrenceRecord.family}">
            <c:choose>
                <c:when test="${rawOccurrenceRecord.family==familyConcept.taxonName}">
                    <gbiftag:taxonLink concept="${familyConcept}"/>
                </c:when>
                <c:otherwise>
                    <span id="occurrenceRecord-${rawOccurrenceRecord.key}-taxonomy-order">${rawOccurrenceRecord.family}</span>
                    <gbiftag:interpretedTaxon concept="${familyConcept}"/>
                </c:otherwise>
            </c:choose>
        </c:if>
    </alatag:occurrenceTableRow>
    <alatag:getTaxonFromTaxonList concepts="${concepts}" requestedConcept="genus" /><!-- populates requestedTaxonRank -->
    <alatag:occurrenceTableRow messageCode="taxonrank.genus" section="taxonomy"
                               fieldName="genus" annotate="true" matchedTaxa="${requestedTaxonRank}">
        <c:if test="${not empty rawOccurrenceRecord.genus}">
            <c:choose>
                <c:when test="${rawOccurrenceRecord.genus==genusConcept.taxonName}">
                    <gbiftag:taxonLink concept="${genusConcept}"/>
                </c:when>
                <c:otherwise>
                    <span id="occurrenceRecord-${rawOccurrenceRecord.key}-taxonomy-order">${rawOccurrenceRecord.genus}</span>
                    <gbiftag:interpretedTaxon concept="${genusConcept}"/>
                </c:otherwise>
            </c:choose>
        </c:if>
    </alatag:occurrenceTableRow>
    <alatag:getTaxonFromTaxonList concepts="${concepts}" requestedConcept="species" /><!-- populates requestedTaxonRank -->
    <alatag:occurrenceTableRow messageCode="taxonrank.species" section="taxonomy"
                               fieldName="species" annotate="true" matchedTaxa="${requestedTaxonRank}">
        <c:if test="${not empty rawOccurrenceRecord.species}">
            <c:choose>
                <c:when test="${rawOccurrenceRecord.species==speciesConcept.taxonName}">
                    <gbiftag:taxonLink concept="${speciesConcept}"/>
                </c:when>
                <c:otherwise>
                    <span id="occurrenceRecord-${rawOccurrenceRecord.key}-taxonomy-order">${rawOccurrenceRecord.species}</span>
                    <gbiftag:interpretedTaxon concept="${speciesConcept}"/>
                </c:otherwise>
            </c:choose>
        </c:if>
    </alatag:occurrenceTableRow>
    <alatag:getTaxonFromTaxonList concepts="${concepts}" requestedConcept="subspecies" /><!-- populates requestedTaxonRank -->
    <alatag:occurrenceTableRow messageCode="taxonrank.subspecies" section="taxonomy"
                               fieldName="subspecies" annotate="false" matchedTaxa="${requestedTaxonRank}">
        <c:if test="${not empty rawOccurrenceRecord.subspecies}">
            <c:choose>
                <c:when test="${rawOccurrenceRecord.subspecies==subspeciesConcept.taxonName}">
                    <gbiftag:taxonLink concept="${subspeciesConcept}"/>
                </c:when>
                <c:otherwise>
                    <span id="occurrenceRecord-${rawOccurrenceRecord.key}-taxonomy-subspecies">${rawOccurrenceRecord.subspecies}</span>
                    <gbiftag:interpretedTaxon concept="${subspeciesConcept}"/>
                </c:otherwise>
            </c:choose>
        </c:if>
    </alatag:occurrenceTableRow>
    <alatag:getTaxonFromTaxonList concepts="${concepts}" requestedConcept="variety" /><!-- populates requestedTaxonRank -->
    <alatag:occurrenceTableRow messageCode="taxonrank.variety" section="taxonomy"
                               fieldName="variety" annotate="false" matchedTaxa="${requestedTaxonRank}">
        <c:if test="${not empty rawOccurrenceRecord.subspecies}">
            <c:choose>
                <c:when test="${rawOccurrenceRecord.subspecies==varietyConcept.taxonName}">
                    <gbiftag:taxonLink concept="${varietyConcept}"/>
                </c:when>
                <c:otherwise>
                    <span id="occurrenceRecord-${rawOccurrenceRecord.key}-taxonomy-variety">${rawOccurrenceRecord.subspecies}</span> 
                    <gbiftag:interpretedTaxon concept="${varietyConcept}"/>
                </c:otherwise>
            </c:choose>
        </c:if>
    </alatag:occurrenceTableRow>
    <alatag:getTaxonFromTaxonList concepts="${concepts}" requestedConcept="subspecies" /><!-- populates requestedTaxonRank -->
    <alatag:occurrenceTableRow messageCode="specimen.type.status" section="taxonomy"
                               fieldName="typification" annotate="false" matchedTaxa="${requestedTaxonRank}">
        <c:forEach items="${typifications}" var="typification">
           <span id="occurrenceRecord-${rawOccurrenceRecord.key}-taxonomy-typestatus">${typification.typeStatus}</span>
           <c:if test="${not empty typification.scientificName}"><spring:message code="specimen.type.for" arguments="${typification.scientificName}"/></c:if>
        </c:forEach>
    </alatag:occurrenceTableRow>

</table>

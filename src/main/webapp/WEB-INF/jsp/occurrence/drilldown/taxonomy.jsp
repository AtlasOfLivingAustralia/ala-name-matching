<%@ include file="/common/taglibs.jsp"%>
<fieldset>
<p>
	<c:choose>
		<c:when test="${empty occurrenceRecord}">
			<label><spring:message code="occurrence.record.scientificName"/>:</label> ${rawOccurrenceRecord.scientificName} ${rawOccurrenceRecord.author} 
		</c:when>
		<c:when test="${rawOccurrenceRecord.scientificName==taxonConcept.taxonName && (((empty rawOccurrenceRecord.author) && (empty taxonConcept.author)) || (rawOccurrenceRecord.author==taxonConcept.author))}">
			<label><spring:message code="occurrence.record.scientificName"/>:</label> <gbiftag:taxonLink concept="${taxonConcept}"/> ${rawOccurrenceRecord.author} 
		</c:when>
		<c:otherwise>
			<label><spring:message code="occurrence.record.scientificName"/>:</label><span class="genera">${rawOccurrenceRecord.scientificName}</span> ${rawOccurrenceRecord.author} 
			<c:set var="interpretedSciName"><c:if test="${not empty taxonConcept.partnerConceptKey}"><a href="${pageContext.request.contextPath}/species/${taxonConcept.partnerConceptKey}"></c:if><span class="genera">${taxonConcept.taxonName}</span><c:if test="${not empty taxonConcept.partnerConceptKey}"></a></c:if> ${partnerConcept.author}</c:set>
			<spring:message code="occurrence.record.interpreted.as" arguments="${interpretedSciName}" argumentSeparator="$$$"/>
		</c:otherwise>
	</c:choose>
</p>
<%
	/* Nick's attempt at a single table for both GBIF and record-derived classification */
%>
</fieldset>
<c:if test="${!rawOnly && not empty occurrenceRecord}">
<table class="taxonClassificationComparison">
    <tr>
        <th style="width:170px;"></th>
        <th style="min-width:160px;">GBIF Classification</th>
        <th style="min-width:160px;">Recorded Classification</th>
    </tr>
    <!-- Kingdom -->
    <alatag:getTaxonFromTaxonList concepts="${concepts}" requestedConcept="kingdom"/>
    <c:if test="${not empty requestedTaxonRank || not empty rawOccurrenceRecord.kingdom}">
	    <tr> 
	        <td><spring:message code="taxonrank.kingdom"/>:</td>
	        <td> <!-- GBIF-based classification -->
	            <alatag:linkGbifTaxonConcept concept="${requestedTaxonRank}" />
	        </td>
	        <td> <!-- record-based classification -->
	            <c:if test="${not empty rawOccurrenceRecord.kingdom}">
		            <c:choose>
			            <c:when test="${rawOccurrenceRecord.kingdom==kingdomConcept.taxonName}">
			                <gbiftag:taxonLink concept="${kingdomConcept}"/>
			            </c:when>
			            <c:otherwise>
			                ${rawOccurrenceRecord.kingdom} <gbiftag:interpretedTaxon concept="${kingdomConcept}"/>
			            </c:otherwise>
		            </c:choose>
	            </c:if>
	        </td>
	    </tr>
    </c:if>
    <!-- Phylum -->
    <alatag:getTaxonFromTaxonList concepts="${concepts}" requestedConcept="phylum"/>
    <c:if test="${not empty requestedTaxonRank || not empty rawOccurrenceRecord.phylum}">
	    <tr> 
	        <td><spring:message code="taxonrank.phylum"/>:</td>
	        <td> <!-- GBIF-based classification -->
	            <alatag:linkGbifTaxonConcept concept="${requestedTaxonRank}" />
	        </td>
	        <td> <!-- record-based classification -->
	            <c:if test="${not empty rawOccurrenceRecord.phylum}">
	                <c:choose>
	                    <c:when test="${rawOccurrenceRecord.phylum==phylumConcept.taxonName}">
	                        <gbiftag:taxonLink concept="${phylumConcept}"/>
	                    </c:when>
	                    <c:otherwise>
	                        ${rawOccurrenceRecord.phylum} <gbiftag:interpretedTaxon concept="${phylumConcept}"/>
	                    </c:otherwise>
	                </c:choose>
	            </c:if>
	        </td>
	    </tr>
    </c:if>
    <!-- Class -->
    <alatag:getTaxonFromTaxonList concepts="${concepts}" requestedConcept="class"/>
    <c:if test="${not empty requestedTaxonRank || not empty rawOccurrenceRecord.bioClass}">
	    <tr> 
	        <td><spring:message code="taxonrank.class"/>:</td>
	        <td> <!-- GBIF-based classification -->
	            <alatag:linkGbifTaxonConcept concept="${requestedTaxonRank}" />
	        </td>
	        <td> <!-- record-based classification -->
	            <c:if test="${not empty rawOccurrenceRecord.bioClass}">
	                <c:choose>
	                    <c:when test="${rawOccurrenceRecord.phylum==classConcept.taxonName}">
	                        <gbiftag:taxonLink concept="${classConcept}"/>
	                    </c:when>
	                    <c:otherwise>
	                        ${rawOccurrenceRecord.bioClass} <gbiftag:interpretedTaxon concept="${classConcept}"/>
	                    </c:otherwise>
	                </c:choose>
	            </c:if>
	        </td>
	    </tr>
    </c:if>
    <!-- Order -->
    <alatag:getTaxonFromTaxonList concepts="${concepts}" requestedConcept="order"/>
    <c:if test="${not empty requestedTaxonRank || not empty rawOccurrenceRecord.order}">
	    <tr> 
	        <td><spring:message code="taxonrank.order"/>:</td>
	        <td> <!-- GBIF-based classification -->
	            <alatag:linkGbifTaxonConcept concept="${requestedTaxonRank}" />
	        </td>
	        <td> <!-- record-based classification -->
	            <c:if test="${not empty rawOccurrenceRecord.order}">
	                <c:choose>
	                    <c:when test="${rawOccurrenceRecord.order==orderConcept.taxonName}">
	                        <gbiftag:taxonLink concept="${phylumConcept}"/>
	                    </c:when>
	                    <c:otherwise>
	                        ${rawOccurrenceRecord.order} <gbiftag:interpretedTaxon concept="${orderConcept}"/>
	                    </c:otherwise>
	                </c:choose>
	            </c:if>
	        </td>
	    </tr>
    </c:if>
    <!-- Family -->
    <alatag:getTaxonFromTaxonList concepts="${concepts}" requestedConcept="family"/>
    <c:if test="${not empty requestedTaxonRank || not empty rawOccurrenceRecord.family}">
	    <tr> 
	        <td><spring:message code="taxonrank.family"/>:</td>
	        <td> <!-- GBIF-based classification -->
	            <alatag:linkGbifTaxonConcept concept="${requestedTaxonRank}" />
	        </td>
	        <td> <!-- record-based classification -->
	            <c:if test="${not empty rawOccurrenceRecord.family}">
	                <c:choose>
	                    <c:when test="${rawOccurrenceRecord.family==familyConcept.taxonName}">
	                        <gbiftag:taxonLink concept="${familyConcept}"/>
	                    </c:when>
	                    <c:otherwise>
	                        ${rawOccurrenceRecord.family} <gbiftag:interpretedTaxon concept="${familyConcept}"/>
	                    </c:otherwise>
	                </c:choose>
	            </c:if>
	        </td>
	    </tr>
    </c:if>
    <!-- Genus -->
    <alatag:getTaxonFromTaxonList concepts="${concepts}" requestedConcept="genus"/>
    <c:if test="${not empty requestedTaxonRank || not empty rawOccurrenceRecord.genus}">
	    <tr> 
	        <td><spring:message code="taxonrank.genus"/>:</td>
	        <td> <!-- GBIF-based classification -->
	            <alatag:linkGbifTaxonConcept concept="${requestedTaxonRank}" />
	        </td>
	        <td> <!-- record-based classification -->
	            <c:if test="${not empty rawOccurrenceRecord.genus}">
	                <c:choose>
	                    <c:when test="${rawOccurrenceRecord.genus==orderConcept.genusName}">
	                        <gbiftag:taxonLink concept="${genusConcept}"/>
	                    </c:when>
	                    <c:otherwise>
	                        ${rawOccurrenceRecord.genus} <gbiftag:interpretedTaxon concept="${genusConcept}"/>
	                    </c:otherwise>
	                </c:choose>
	            </c:if>
	        </td>
	    </tr>
    </c:if>
    <!-- Species -->
    <alatag:getTaxonFromTaxonList concepts="${concepts}" requestedConcept="species"/>
    <c:if test="${not empty requestedTaxonRank || not empty rawOccurrenceRecord.species}">
	    <tr> 
	        <td><spring:message code="taxonrank.species"/>:</td>
	        <td> <!-- GBIF-based classification -->
	            <i><alatag:linkGbifTaxonConcept concept="${requestedTaxonRank}" /></i>
	        </td>
	        <td> <!-- record-based classification -->
	            <c:if test="${not empty rawOccurrenceRecord.species}">
	                <c:choose>
	                    <c:when test="${rawOccurrenceRecord.species==speciesConcept.taxonName}">
	                        <gbiftag:taxonLink concept="${speciesConcept}"/>
	                    </c:when>
	                    <c:otherwise>
	                        ${rawOccurrenceRecord.species} <gbiftag:interpretedTaxon concept="${speciesConcept}"/>
	                    </c:otherwise>
	                </c:choose>
	            </c:if>
	        </td>
	    </tr>
	</c:if>
    <!-- Sub-Species -->
    <alatag:getTaxonFromTaxonList concepts="${concepts}" requestedConcept="subspecies"/>
    <c:if test="${not empty requestedTaxonRank || not empty rawOccurrenceRecord.subspecies}">
	    <tr> 
	        <td><spring:message code="taxonrank.subspecies"/>:</td>
	        <td> <!-- GBIF-based classification -->
	            <i><alatag:linkGbifTaxonConcept concept="${requestedTaxonRank}" /></i>
	        </td>
	        <td> <!-- record-based classification -->
	            
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
	        </td>
	    </tr>
    </c:if>
    <!-- Variety -->
    <alatag:getTaxonFromTaxonList concepts="${concepts}" requestedConcept="variety"/>
    <c:if test="${not empty requestedTaxonRank || not empty rawOccurrenceRecord.subspecies}">
        <tr> 
            <td><spring:message code="taxonrank.variety"/>:</td>
            <td> <!-- GBIF-based classification -->
                <i><alatag:linkGbifTaxonConcept concept="${requestedTaxonRank}" /></i>
            </td>
            <td> <!-- record-based classification -->
                <c:if test="${not empty rawOccurrenceRecord.subspecies}">
                    <c:choose>
                        <c:when test="${rawOccurrenceRecord.subspecies==varietyConcept.taxonName}">
                            <gbiftag:taxonLink concept="${varietyConcept}"/>
                        </c:when>
                        <c:otherwise>
                            ${rawOccurrenceRecord.subspecies} <gbiftag:interpretedTaxon concept="${varietyConcept}"/>
                        </c:otherwise>
                    </c:choose>
                </c:if>
            </td>
        </tr>
    </c:if>
    <!-- Typification -->
    
    <c:if test="${not empty typifications}">
        <tr> 
            <td><spring:message code="taxonrank.subspecies"/>:</td>
            <td> <!-- GBIF-based classification -->
                <c:forEach items="${concepts}" var="concept">
                    <!--  TODO: put this functionality into a tag -->
                    <c:if test="${concept.rank == 'subspecies'}">
                        <span class="genera"><gbiftag:taxonLink concept="${concept}"/></span>
                    </c:if>
                </c:forEach>
            </td>
            <td> <!-- record-based classification -->
                <c:forEach items="${typifications}" var="typification">
			        <label><spring:message code="specimen.type.status"/>:</label> ${typification.typeStatus} <c:if test="${not empty typification.scientificName}"><spring:message code="specimen.type.for" arguments="${typification.scientificName}"/></c:if>
			        <br/>
			    </c:forEach>
            </td>
        </tr>
    </c:if>
</table>
</c:if> 
<fieldset>
<%
	/* end Nick's table */
%>
<c:if test="null">
	<c:if test="${!rawOnly && not empty occurrenceRecord}">
		<p id="classification"><label for="classification"><spring:message
			code="occurrence.record.gbif.taxonomy" />:</label> <gbif:flattree
			classname="classificationCondensed" concepts="${concepts}"
			selectedConcept="${taxonConcept}" /> <%
 	/*  <div>Concepts: ${concepts[0].rank} = <a href="${pageContext.request.contextPath}/species/${concepts[0].key}">${concepts[0].taxonName}</a></div>
 	   <div> ${concepts}</div> */
 %>
		</p>
	</c:if>

	<c:if test="${not empty rawOccurrenceRecord.kingdom}">
		<p><c:choose>
			<c:when
				test="${rawOccurrenceRecord.kingdom==kingdomConcept.taxonName}">
				<label for="kingdom"><spring:message
					code="taxonrank.kingdom" />:</label>
				<gbiftag:taxonLink concept="${kingdomConcept}" />
			</c:when>
			<c:otherwise>
				<label for="kingdom"><spring:message
					code="taxonrank.kingdom" />:</label> ${rawOccurrenceRecord.kingdom}
			<gbiftag:interpretedTaxon concept="${kingdomConcept}" />
			</c:otherwise>
		</c:choose></p>
	</c:if>

	<c:if test="${not empty rawOccurrenceRecord.phylum}">
		<p><c:choose>
			<c:when test="${rawOccurrenceRecord.phylum==phylumConcept.taxonName}">
				<label for="phylum"><spring:message code="taxonrank.phylum" />:</label>
				<gbiftag:taxonLink concept="${phylumConcept}" />
			</c:when>
			<c:otherwise>
				<label for="phylum"><spring:message code="taxonrank.phylum" />:</label> ${rawOccurrenceRecord.phylum}
			<gbiftag:interpretedTaxon concept="${phylumConcept}" />
			</c:otherwise>
		</c:choose></p>
	</c:if>

	<c:if test="${not empty rawOccurrenceRecord.bioClass}">
		<p><c:choose>
			<c:when
				test="${rawOccurrenceRecord.bioClass==classConcept.taxonName}">
				<label for="class"><spring:message code="taxonrank.class" />:</label>
				<gbiftag:taxonLink concept="${classConcept}" />
			</c:when>
			<c:otherwise>
				<label for="class"><spring:message code="taxonrank.class" />:</label> ${rawOccurrenceRecord.bioClass}
			<gbiftag:interpretedTaxon concept="${classConcept}" />
			</c:otherwise>
		</c:choose></p>
	</c:if>

	<c:if test="${not empty rawOccurrenceRecord.order}">
		<p><c:choose>
			<c:when test="${rawOccurrenceRecord.order==orderConcept.taxonName}">
				<label for="order"><spring:message code="taxonrank.order" />:</label>
				<gbiftag:taxonLink concept="${orderConcept}" />
			</c:when>
			<c:otherwise>
				<label for="order"><spring:message code="taxonrank.order" />:</label> ${rawOccurrenceRecord.order}
			<gbiftag:interpretedTaxon concept="${orderConcept}" />
			</c:otherwise>
		</c:choose></p>
	</c:if>

	<c:if test="${not empty rawOccurrenceRecord.family}">
		<p><c:choose>
			<c:when test="${rawOccurrenceRecord.family==familyConcept.taxonName}">
				<label for="family"><spring:message code="taxonrank.family" />:</label>
				<gbiftag:taxonLink concept="${familyConcept}" />
			</c:when>
			<c:otherwise>
				<label for="family"><spring:message code="taxonrank.family" />:</label> ${rawOccurrenceRecord.family}
			<gbiftag:interpretedTaxon concept="${familyConcept}" />
			</c:otherwise>
		</c:choose></p>
	</c:if>

	<c:if test="${not empty rawOccurrenceRecord.genus}">
		<p><c:choose>
			<c:when test="${rawOccurrenceRecord.genus==genusConcept.taxonName}">
				<label for="genus"><spring:message code="taxonrank.genus" />:</label>
				<gbiftag:taxonLink concept="${genusConcept}" />
			</c:when>
			<c:otherwise>
				<label for="genus"><spring:message code="taxonrank.genus" />:</label> ${rawOccurrenceRecord.genus}
			<gbiftag:interpretedTaxon concept="${genusConcept}" />
			</c:otherwise>
		</c:choose></p>
	</c:if>

	<c:if test="${not empty rawOccurrenceRecord.species}">
		<p><c:choose>
			<c:when
				test="${rawOccurrenceRecord.species==speciesConcept.taxonName}">
				<label><spring:message code="taxonrank.species" />:</label>
				<gbiftag:taxonLink concept="${speciesConcept}" />
			</c:when>
			<c:otherwise>
				<label><spring:message code="taxonrank.species" />:</label> ${rawOccurrenceRecord.species}
			<gbiftag:interpretedTaxon concept="${speciesConcept}" />
			</c:otherwise>
		</c:choose></p>
	</c:if>

	<c:if test="${not empty rawOccurrenceRecord.subspecies}">
		<p><c:choose>
			<c:when
				test="${rawOccurrenceRecord.subspecies==subspeciesConcept.taxonName}">
				<label><spring:message code="taxonrank.subspecies" />:</label>
				<gbiftag:taxonLink concept="${subspeciesConcept}" />
			</c:when>
			<c:otherwise>
				<label><spring:message code="taxonrank.subspecies" />:</label> ${rawOccurrenceRecord.subspecies}
			<gbiftag:interpretedTaxon concept="${subspeciesConcept}" />
			</c:otherwise>
		</c:choose></p>
	</c:if>

	<c:if test="${not empty typifications}">
		<p><c:forEach items="${typifications}" var="typification">
			<label><spring:message code="specimen.type.status" />:</label> ${typification.typeStatus} <c:if
				test="${not empty typification.scientificName}">
				<spring:message code="specimen.type.for"
					arguments="${typification.scientificName}" />
			</c:if>
			<br />
		</c:forEach></p>
	</c:if>

	<c:if
		test="${rawOccurrenceRecord.identifierName!=null || rawOccurrenceRecord.identificationDate!=null}">
		<c:if test="${rawOccurrenceRecord.identifierName!=null}">
			<p><label for="indentifierName"><spring:message
				code="occurrence.record.identifierName" />:</label>
			${rawOccurrenceRecord.identifierName}</p>
		</c:if>
		<c:if test="${rawOccurrenceRecord.identificationDate!=null}">
			<p><label for="indentificationDate"><spring:message
				code="occurrence.record.dateIdentified" />:</label> <fmt:formatDate
				value="${rawOccurrenceRecord.identificationDate}" /></p>
		</c:if>
	</c:if>
</c:if>
</fieldset>	
<%@ include file="/common/taglibs.jsp"%>
<table id="datasetTable" class="occurrenceTable" cellspacing="0">
    <alatag:occurrenceTableRow messageCode="occurrence.record.dataprovider" section="dataset" fieldName="dataProvider" annotate="false">
        <a href="${pageContext.request.contextPath}/datasets/provider/${rawOccurrenceRecord.dataProviderKey}/">
	        <span id="occurrenceRecord-${rawOccurrenceRecord.key}-dataset-dataProvider">${rawOccurrenceRecord.dataProviderName}</span>
	    </a>
    </alatag:occurrenceTableRow>
    <alatag:occurrenceTableRow messageCode="occurrence.record.dataresource" section="dataset" fieldName="dataResource" annotate="false">
        <a href="${pageContext.request.contextPath}/datasets/resource/${rawOccurrenceRecord.dataResourceKey}/">
	        <span id="occurrenceRecord-${rawOccurrenceRecord.key}-dataset-dataResource">${rawOccurrenceRecord.dataResourceName}</span>
	    </a> 
    </alatag:occurrenceTableRow>
    <alatag:occurrenceTableRow messageCode="occurrence.record.institutioncode" section="dataset" fieldName="institutionCode" annotate="true">
        <span id="occurrenceRecord-${rawOccurrenceRecord.key}-dataset-institutionCode">${rawOccurrenceRecord.institutionCode}</span>
	    <c:if test="${not empty institution && not empty institution.name}">
	        (Interpreted as ${institution.name} <a href="http://biocol.org/${institution.lsid}">${institution.lsid}</a>)
	    </c:if>
    </alatag:occurrenceTableRow>
    <alatag:occurrenceTableRow messageCode="occurrence.record.collectioncode" section="dataset" fieldName="collectionCode" annotate="true">
        <c:choose>
            <c:when test="${not empty occurrenceRecord && not empty occurrenceRecord.collectionCode}">
                <span id="occurrenceRecord-${rawOccurrenceRecord.key}-dataset-collectionCode">${occurrenceRecord.collectionCode}</span>
            </c:when>
            <c:otherwise>
                <span id="occurrenceRecord-${rawOccurrenceRecord.key}-dataset-collectionCode">${rawOccurrenceRecord.collectionCode}</span>
            </c:otherwise>
	    </c:choose>
	    <c:if test="${not empty occurrenceRecord && rawOccurrenceRecord.collectionCode!=occurrenceRecord.collectionCode}">
	        <spring:message code="occurrence.record.interpreted.as" arguments="${occurrenceRecord.collectionCode}" argumentSeparator="$$$%%$$%"/>
	    </c:if>
    </alatag:occurrenceTableRow>
    <alatag:occurrenceTableRow messageCode="occurrence.record.identifier" section="dataset" fieldName="catalogueNumber" annotate="true">
        <c:choose>
            <c:when test="${not empty occurrenceRecord && not empty occurrenceRecord.catalogueNumber}">
                <span id="occurrenceRecord-${rawOccurrenceRecord.key}-dataset-catalogueNumber">${occurrenceRecord.catalogueNumber}</span>
            </c:when>
            <c:otherwise>
                <span id="occurrenceRecord-${rawOccurrenceRecord.key}-dataset-catalogueNumber">${fn:escapeXml(rawOccurrenceRecord.catalogueNumber)}</span>
            </c:otherwise>
	    </c:choose>
	    <c:if test="${not empty occurrenceRecord && rawOccurrenceRecord.catalogueNumber!=occurrenceRecord.catalogueNumber}">
	        <spring:message code="occurrence.record.interpreted.as" arguments="${occurrenceRecord.catalogueNumber}" argumentSeparator="$$$%%$$%"/>
	    </c:if>
    </alatag:occurrenceTableRow>
    <alatag:occurrenceTableRow messageCode="basis.of.record" section="dataset" fieldName="basisOfRecord" annotate="true">
        <c:set var="bor">
	        <a
	            <c:choose>
	                <c:when test="${not empty occurrenceRecord.basisOfRecord}">
	                    title="<spring:message code="basis.of.record.description.${occurrenceRecord.basisOfRecord}" text=""/>"
	                </c:when>
	                <c:otherwise>
	                    title="<spring:message code="basis.of.record.description.unknown" text=""/>"
	                </c:otherwise>
	            </c:choose>
	            ><spring:message code="basis.of.record.${occurrenceRecord.basisOfRecord}" text=""/></a>
	    </c:set>
	    <c:choose>
	        <c:when test="${not empty rawOccurrenceRecord.basisOfRecord}">
	            <c:choose>
	                <c:when test="${not empty rawOccurrenceRecord.basisOfRecord && (occurrenceRecord.basisOfRecord!=fn:toLowerCase(rawOccurrenceRecord.basisOfRecord))}">
	                    <span id="occurrenceRecord-${rawOccurrenceRecord.key}-dataset-basisOfRecord">${rawOccurrenceRecord.basisOfRecord}</span>
	                    <c:if test="${not empty occurrenceRecord && not empty bor}">
	                        <spring:message code="occurrence.record.interpreted.as" arguments="${bor}" argumentSeparator="$$"/>
	                    </c:if>
	                </c:when>
	                <c:otherwise>
	                    <span id="occurrenceRecord-${rawOccurrenceRecord.key}-dataset-basisOfRecord">${bor}</span>
	                </c:otherwise>
	            </c:choose>
	        </c:when>
	        <c:otherwise>
	            <c:choose>
	                <c:when test="${occurrenceRecord.basisOfRecord=='unknown'}">
	                    <span id="occurrenceRecord-${rawOccurrenceRecord.key}-dataset-basisOfRecord">${bor}</span>
	                </c:when>
	                <c:when test="${not empty bor}">
	                    <span id="occurrenceRecord-${rawOccurrenceRecord.key}-dataset-basisOfRecord">${bor}</span>
	                    <spring:message code="occurrence.record.inferred"/>
	                </c:when>
	            </c:choose>
	        </c:otherwise>
	    </c:choose>
    </alatag:occurrenceTableRow>
    <c:forEach items="${typifications}" var="typification">
        <alatag:occurrenceTableRow messageCode="specimen.type.status" section="dataset" fieldName="typification" annotate="false">
            <span id="occurrenceRecord-${rawOccurrenceRecord.key}-dataset-typeStatus">${typification.typeStatus}</span>
            <c:if test="${not empty typification.scientificName}">
                <spring:message code="specimen.type.for" arguments="${typification.scientificName}" />
            </c:if>
        </alatag:occurrenceTableRow>
    </c:forEach>
    <c:if test="${rawOccurrenceRecord.identifierName!=null}">
        <alatag:occurrenceTableRow messageCode="occurrence.record.identifierName" section="dataset" fieldName="identifierName" annotate="true">
            <span id="occurrenceRecord-${rawOccurrenceRecord.key}-dataset-identiferName">
                ${rawOccurrenceRecord.identifierName}
            </span>
        </alatag:occurrenceTableRow>
    </c:if>
    <c:if test="${rawOccurrenceRecord.identificationDate!=null}">
        <alatag:occurrenceTableRow messageCode="occurrence.record.dateIdentified" section="dataset" fieldName="identificationDate" annotate="true">
            <span id="occurrenceRecord-${rawOccurrenceRecord.key}-dataset-identificationDate">
                <fmt:formatDate value="${rawOccurrenceRecord.identificationDate}" />
            </span>
        </alatag:occurrenceTableRow>
    </c:if>
    <c:forEach items="${identifierRecords}" var="identifierRecord">
        <alatag:occurrenceTableRow messageCode="identifier.type.${identifierRecord.identifierType}" section="dataset" fieldName="fieldNumber" annotate="true">
            ${identifierRecord.identifier}<br/>
        </alatag:occurrenceTableRow>
    </c:forEach>
    <alatag:occurrenceTableRow messageCode="occurrence.record.collectorName" section="dataset" fieldName="collectorName" annotate="true">
        ${rawOccurrenceRecord.collectorName}
    </alatag:occurrenceTableRow>
    <alatag:occurrenceTableRow messageCode="occurrence.record.dateCollected" section="dataset" fieldName="collectionDate" annotate="true">
        <c:if test="${rawOccurrenceRecord.day!=null}">
            <spring:message code="day"/>:${rawOccurrenceRecord.day}
        </c:if>
        <c:if test="${rawOccurrenceRecord.month!=null}">
            <spring:message code="month"/>:${rawOccurrenceRecord.month}
        </c:if>
        <c:if test="${rawOccurrenceRecord.year!=null}">
            <spring:message code="year"/>:${rawOccurrenceRecord.year}
        </c:if>
        <c:if test="${occurrenceRecord.occurrenceDate!=null}">
            <c:set var="interpretedDate"><fmt:formatDate value="${occurrenceRecord.occurrenceDate}"/></c:set>
            <spring:message code="occurrence.record.interpreted.as" arguments="${interpretedDate}" argumentSeparator="xxx"/>
        </c:if>
    </alatag:occurrenceTableRow>

</table>
<!-- original code is below  -->
<% /*
<fieldset id="occurrenceRecord-${rawOccurrenceRecord.key}-dataset" class="occurrenceFieldset">

    <!-- Data Provider  -->
    <p>
	    <label for="dataprovider"><spring:message code="occurrence.record.dataprovider"/>:</label>
	    <a href="${pageContext.request.contextPath}/datasets/provider/${rawOccurrenceRecord.dataProviderKey}/">
	        <span id="occurrenceRecord-${rawOccurrenceRecord.key}-dataset-dataProvider">${rawOccurrenceRecord.dataProviderName}</span>
	    </a>
    </p>
    
    <!-- Data Resource -->
    <p>
	    <label for="dataresource"><spring:message code="occurrence.record.dataresource"/>:</label>
	    <a href="${pageContext.request.contextPath}/datasets/resource/${rawOccurrenceRecord.dataResourceKey}/">
	        <span id="occurrenceRecord-${rawOccurrenceRecord.key}-dataset-dataResource">${rawOccurrenceRecord.dataResourceName}</span>
	     </a>  
    </p>
    
    <!-- Institution code -->
    <p class="dataset" name="institutionCode">
	    <label for="institutionCode"><spring:message code="occurrence.record.institutioncode"/>:</label>
	    <span id="occurrenceRecord-${rawOccurrenceRecord.key}-dataset-institutionCode">${rawOccurrenceRecord.institutionCode}</span>
	    <c:if test="${not empty institution && not empty institution.name}">
	        (Interpreted as ${institution.name} <a href="http://biocol.org/${institution.lsid}">${institution.lsid}</a>)
	    </c:if>
    </p>
    
    <!-- Collection code -->
    <p class="dataset" name="collectionCode">
	    <label for="collectionCode"><spring:message code="occurrence.record.collectioncode"/>:</label>
	    <c:choose>
	    <c:when test="${not empty occurrenceRecord && not empty occurrenceRecord.collectionCode}">
	    <span id="occurrenceRecord-${rawOccurrenceRecord.key}-dataset-collectionCode">${occurrenceRecord.collectionCode}</span>
	    </c:when>
	    <c:otherwise>
	    <span id="occurrenceRecord-${rawOccurrenceRecord.key}-dataset-collectionCode">${rawOccurrenceRecord.collectionCode}</span>
	    </c:otherwise>
	    </c:choose> 
	    <c:if test="${not empty occurrenceRecord && rawOccurrenceRecord.collectionCode!=occurrenceRecord.collectionCode}">
	        <spring:message code="occurrence.record.interpreted.as" arguments="${occurrenceRecord.collectionCode}" argumentSeparator="$$$%%$$%"/>           
	    </c:if>
    </p>
    
    <!-- Catalogue Number -->
    <p class="dataset" name="catalogueNumber">
	    <label for="identifier"><spring:message code="occurrence.record.identifier"/>:</label>
	    <c:choose>
	    <c:when test="${not empty occurrenceRecord && not empty occurrenceRecord.catalogueNumber}">
	    <span id="occurrenceRecord-${rawOccurrenceRecord.key}-dataset-catalogueNumber">${occurrenceRecord.catalogueNumber}</span>
	    </c:when>
	    <c:otherwise>
	    <span id="occurrenceRecord-${rawOccurrenceRecord.key}-dataset-catalogueNumber">${fn:escapeXml(rawOccurrenceRecord.catalogueNumber)}</span>
	    </c:otherwise>
	    </c:choose>
	    <c:if test="${not empty occurrenceRecord && rawOccurrenceRecord.catalogueNumber!=occurrenceRecord.catalogueNumber}">
	        <spring:message code="occurrence.record.interpreted.as" arguments="${occurrenceRecord.catalogueNumber}" argumentSeparator="$$$%%$$%"/>          
	    </c:if>         
    </p>
    
    <!-- Basis of record -->
    <p class="dataset" name="basisOfRecord">
	    <label for="basisOfRecord"><spring:message code="basis.of.record"/>:</label>
	    <c:set var="bor">
	        <a 
	            <c:choose>
	                <c:when test="${not empty occurrenceRecord.basisOfRecord}">
	                    title="<spring:message code="basis.of.record.description.${occurrenceRecord.basisOfRecord}" text=""/>"
	                </c:when>
	                <c:otherwise>
	                    title="<spring:message code="basis.of.record.description.unknown" text=""/>"
	                </c:otherwise>
	            </c:choose>                                     
	            ><spring:message code="basis.of.record.${occurrenceRecord.basisOfRecord}" text=""/></a>
	    </c:set>
	    <c:choose>
	        <c:when test="${not empty rawOccurrenceRecord.basisOfRecord}">
	            <c:choose>
	                <c:when test="${not empty rawOccurrenceRecord.basisOfRecord && (occurrenceRecord.basisOfRecord!=fn:toLowerCase(rawOccurrenceRecord.basisOfRecord))}">
	                    <span id="occurrenceRecord-${rawOccurrenceRecord.key}-dataset-basisOfRecord">${rawOccurrenceRecord.basisOfRecord}</span>
	                    <c:if test="${not empty occurrenceRecord && not empty bor}">
	                        <spring:message code="occurrence.record.interpreted.as" arguments="${bor}" argumentSeparator="$$"/>
	                    </c:if> 
	                </c:when>
	                <c:otherwise>
	                    <span id="occurrenceRecord-${rawOccurrenceRecord.key}-dataset-basisOfRecord">${bor}</span>
	                </c:otherwise>
	            </c:choose>
	        </c:when>
	        <c:otherwise>
	            <c:choose>
	                <c:when test="${occurrenceRecord.basisOfRecord=='unknown'}">
	                    <span id="occurrenceRecord-${rawOccurrenceRecord.key}-dataset-basisOfRecord">${bor}</span>
	                </c:when>
	                <c:when test="${not empty bor}">
	                    <span id="occurrenceRecord-${rawOccurrenceRecord.key}-dataset-basisOfRecord">${bor}</span> 
	                    <spring:message code="occurrence.record.inferred"/>
	                </c:when>
	            </c:choose>
	        </c:otherwise>
	    </c:choose>
    </p>

    <!-- Type Status -->
    <c:if test="${not empty typifications}">
    <p><c:forEach items="${typifications}" var="typification">
       <label><spring:message code="specimen.type.status" />:</label> 
       <span id="occurrenceRecord-${rawOccurrenceRecord.key}-dataset-typeStatus">${typification.typeStatus}</span> 
       <c:if test="${not empty typification.scientificName}">
           <spring:message code="specimen.type.for" arguments="${typification.scientificName}" />
       </c:if>
       <br />
    </c:forEach></p>
    </c:if>

    <!-- Identification Date -->
    <c:if test="${rawOccurrenceRecord.identifierName!=null || rawOccurrenceRecord.identificationDate!=null}">
        <c:if test="${rawOccurrenceRecord.identifierName!=null}">
            <p class="dataset" name="identiferName">
                <label for="indentifierName"><spring:message code="occurrence.record.identifierName" />:</label>
                <span id="occurrenceRecord-${rawOccurrenceRecord.key}-dataset-identiferName">
                    ${rawOccurrenceRecord.identifierName}
                </span>
            </p>
        </c:if>
        <c:if test="${rawOccurrenceRecord.identificationDate!=null}">
            <p class="dataset" name="identificationDate">
                <label for="indentificationDate"><spring:message code="occurrence.record.dateIdentified" />:</label> 
                <span id="occurrenceRecord-${rawOccurrenceRecord.key}-dataset-identificationDate">
                    <fmt:formatDate value="${rawOccurrenceRecord.identificationDate}" />
                </span>
            </p>
        </c:if>
    </c:if>

    <!-- Identifier Records -->
    <c:if test="${fn:length(identifierRecords) > 1}">
    <p class="dataset" name="fieldNumber">
	    <c:forEach items="${identifierRecords}" var="identifierRecord">
	       <label><spring:message code="identifier.type.${identifierRecord.identifierType}"/>:</label> 
	       ${identifierRecord.identifier}<br/>
	    </c:forEach>
    </p>
    </c:if>
    <!-- Collector -->
    <p class="dataset" name="collectorName">
        <label for="collector"><spring:message code="occurrence.record.collectorName"/>:</label> 
        <span id="occurrenceRecord-${rawOccurrenceRecord.key}-dataset-collectorName">${rawOccurrenceRecord.collectorName}</span>
    </p>

    <!-- Date collected -->
    <p class="dataset" name="collectionDate">
    <label for="dateCollected"><spring:message code="occurrence.record.dateCollected"/>:</label> 
    <c:if test="${rawOccurrenceRecord.day!=null}">
        <spring:message code="day"/>:${rawOccurrenceRecord.day}
    </c:if>
    <c:if test="${rawOccurrenceRecord.month!=null}">
        <spring:message code="month"/>:${rawOccurrenceRecord.month}
    </c:if>
    <c:if test="${rawOccurrenceRecord.year!=null}">
        <spring:message code="year"/>:${rawOccurrenceRecord.year}
    </c:if>     
    <c:if test="${occurrenceRecord.occurrenceDate!=null}">
        <c:set var="interpretedDate"><fmt:formatDate value="${occurrenceRecord.occurrenceDate}"/></c:set>
        <spring:message code="occurrence.record.interpreted.as" arguments="${interpretedDate}" argumentSeparator="xxx"/>    
    </c:if> 
    </p>
    */ %>
    <!-- Image records -->
    <%--<c:if test="${falsenot empty imageRecords}">
    <p> 
        <c:forEach items="${imageRecords}" var="imageRecord">
            <label><spring:message code="image"/>:</label> <c:choose>
                <c:when test="${not empty imageRecord.htmlForDisplay}">
                        ${imageRecord.htmlForDisplay}
                </c:when>
                <c:when test="${not empty imageRecord.url}">
                    <c:choose>
                        <c:when test="${imageRecord.imageType>1}">
                            <c:choose>
                                <c:when test="${fn:endsWith(imageRecord.url, 'mpg') || fn:endsWith(imageRecord.url, 'mpeg') }">
                                    <embed src="${imageRecord.url}" autostart="false" controller="true" controls="console" />
                                </c:when>
                                <c:when test="${imageRecord.url!=null && imageRecord.url!= 'NULL'}">
                                    <a href="${imageRecord.url}"><gbiftag:scaleImage imageUrl="${imageRecord.url}" maxWidth="300" maxHeight="200" addLink="false"/></a>
                                </c:when>                   
                            </c:choose>
                        </c:when>
                        <c:otherwise>
                            <a href="${imageRecord.url}">${imageRecord.url}</a>
                        </c:otherwise>
                    </c:choose> 
                </c:when>       
            </c:choose>
            <br/>
        </c:forEach>
        </p>    
    </c:if>--%>
</fieldset>
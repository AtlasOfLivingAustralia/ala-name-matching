<%@ include file="/common/taglibs.jsp"%>
<fieldset class="occurrenceFieldset">
    <p>
    <label for="dataprovider"><spring:message code="occurrence.record.dataprovider"/>:</label>
    <a href="${pageContext.request.contextPath}/datasets/provider/${rawOccurrenceRecord.dataProviderKey}/">${rawOccurrenceRecord.dataProviderName}</a>
    </p>
    
    <p>
    <label for="dataresource"><spring:message code="occurrence.record.dataresource"/>:</label>
    <a href="${pageContext.request.contextPath}/datasets/resource/${rawOccurrenceRecord.dataResourceKey}/">${rawOccurrenceRecord.dataResourceName}</a>  
    </p>
    
    <p>
    <label for="institutionCode"><spring:message code="occurrence.record.institutioncode"/>:</label>
    ${rawOccurrenceRecord.institutionCode}
    <c:if test="${not empty institution && not empty institution.name}">
        (Interpreted as ${institution.name} <a href="http://biocol.org/${institution.lsid}">${institution.lsid}</a>)
    </c:if>
    </p>
    
    <p>
    <label for="collectionCode"><spring:message code="occurrence.record.collectioncode"/>:</label>
    <c:choose>
    <c:when test="${not empty occurrenceRecord && not empty occurrenceRecord.collectionCode}">
    ${occurrenceRecord.collectionCode}
    </c:when>
    <c:otherwise>
    ${rawOccurrenceRecord.collectionCode}
    </c:otherwise>
    </c:choose> 
    <c:if test="${not empty occurrenceRecord && rawOccurrenceRecord.collectionCode!=occurrenceRecord.collectionCode}">
        <spring:message code="occurrence.record.interpreted.as" arguments="${occurrenceRecord.collectionCode}" argumentSeparator="$$$%%$$%"/>           
    </c:if>     
    </p>
    
    <p>
    <label for="identifier"><spring:message code="occurrence.record.identifier"/>:</label>
    <c:choose>
    <c:when test="${not empty occurrenceRecord && not empty occurrenceRecord.catalogueNumber}">
    ${occurrenceRecord.catalogueNumber}
    </c:when>
    <c:otherwise>
    ${fn:escapeXml(rawOccurrenceRecord.catalogueNumber)}
    </c:otherwise>
    </c:choose>
    <c:if test="${not empty occurrenceRecord && rawOccurrenceRecord.catalogueNumber!=occurrenceRecord.catalogueNumber}">
        <spring:message code="occurrence.record.interpreted.as" arguments="${occurrenceRecord.catalogueNumber}" argumentSeparator="$$$%%$$%"/>          
    </c:if>         
    </p>
    
    <!-- Basis of record -->    
    <p>
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
                    ${rawOccurrenceRecord.basisOfRecord}
                    <c:if test="${not empty occurrenceRecord && not empty bor}">
                        <spring:message code="occurrence.record.interpreted.as" arguments="${bor}" argumentSeparator="$$"/>
                    </c:if> 
                </c:when>
                <c:otherwise>
                    ${bor}
                </c:otherwise>
            </c:choose>
        </c:when>
        <c:otherwise>
            <c:choose>
                <c:when test="${occurrenceRecord.basisOfRecord=='unknown'}">
                    ${bor}
                </c:when>
                <c:when test="${not empty bor}">
                    ${bor} <spring:message code="occurrence.record.inferred"/>
                </c:when>
            </c:choose>
        </c:otherwise>
    </c:choose>
    </p>

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

    <p>
    <c:forEach items="${identifierRecords}" var="identifierRecord">
    <label><spring:message code="identifier.type.${identifierRecord.identifierType}"/>:</label> ${identifierRecord.identifier}<br/>
    </c:forEach>
    </p>

    <!-- Collector -->
    <p>
    <label for="collector"><spring:message code="occurrence.record.collectorName"/>:</label> 
    ${rawOccurrenceRecord.collectorName}
    </p>

    <!-- Date collected -->
    <p>
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
    
    <!-- Image records -->
    <c:if test="${not empty imageRecords}">
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
    </c:if>
</fieldset>
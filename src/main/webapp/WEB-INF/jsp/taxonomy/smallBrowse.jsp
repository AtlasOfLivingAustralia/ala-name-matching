<%@ include file="/common/taglibs.jsp"%>
<div id="taxonomy" class="taxonomyContainer">
    <div id="twopartheader">
        <h2>
        <c:choose>
            <c:when test="${taxonConcept!=null}">
                <spring:message code="taxonomy.browser.classification.of"/> <span class="subject"><string:capitalize>${taxonConcept.rank}</string:capitalize>: <gbif:taxonPrint concept="${taxonConcept}"/></span> ${taxonConcept.author}
            </c:when>
            <c:otherwise>
                <spring:message code="taxonomy.browser.classification"/>
            </c:otherwise>
        </c:choose>
        </h2>
        <c:if test="${dataProvider!=null}">
            <h3><spring:message code="taxonomy.browser.title.accordingto"/>: 
                <c:choose>
                <c:when test="${dataProvider!=null && dataProvider.key==nubProvider.key}">
                    The ALA GIS Portal Classification 
                    (based on <a href="${pageContext.request.contextPath}/datasets/provider/2">Catalogue of Life Annual Checklist 2008</a>, 
                    with provisional additions from specimen and observation data resources)            
                </c:when>
                <c:otherwise>
                    <a href="${pageContext.request.contextPath}/datasets/provider/${dataProvider.key}">${dataProvider.name}</a><c:if test="${dataResource!=null}">: <a href="${pageContext.request.contextPath}/datasets/resource/${dataResource.key}">${dataResource.name}</a></c:if></h3>
                </c:otherwise>  
                </c:choose>
            </h3>   
        </c:if>
    </div><!--twopartheader-->

    <c:if test="${taxonConcept!=null}">
        <div id="furtherActions">
            <h4><spring:message code='actions.for'/> <string:capitalize>${taxonConcept.rank}</string:capitalize>: <gbif:taxonPrint concept="${taxonConcept}"/> ${taxonConcept.author}</h4>
            <table cellspacing="1" class="actionsList">
                <tbody>
                    <c:set var="conceptKey" value="${taxonConcept.isNubConcept ? taxonConcept.key : taxonConcept.partnerConceptKey}"/>
                    <c:if test="${conceptKey!=null}">
                        <gbif:isMajorRank concept="${taxonConcept}">
                            <tr valign="top">
                                <td><b><spring:message code="actions.view"/></b></td>
                                <td>    
                                    <ul class="actionsListInline">
                                        <li>
                                            <a href="${pageContext.request.contextPath}/species/${taxonConcept.isNubConcept ? taxonConcept.key : taxonConcept.partnerConceptKey}"><spring:message code="taxonomy.browser.overview.of"/> <gbif:taxonPrint concept="${taxonConcept}"/></a>
                                        </li>   
                                    </ul>
                                </td>
                            </tr>
                        </gbif:isMajorRank> 
                    </c:if>
                    <tr valign="top">
                        <td><b><spring:message code="actions.explore"/></b></td>
                        <td>    
                            <ul class="actionsListInline">
                                <gbif:isMajorRank concept="${taxonConcept}">
                                    <c:if test="${not empty occurrenceCriteria}">
                                        <li>
                                            <a href="${pageContext.request.contextPath}/occurrences/search.htm?<gbif:criteria criteria="${occurrenceCriteria}"/>"><spring:message code="occurrences.of"/> <gbif:taxonPrint concept="${taxonConcept}"/></a>
                                        </li>                       
                                    </c:if> 
                                </gbif:isMajorRank> 
                                <li>
                                    <a href="${pageContext.request.contextPath}/species/search.htm?<gbif:criteria criteria="${taxonomyCriteria}"/>"><spring:message code="taxonomy.browser.compare.classifications"/> <gbif:taxonPrint concept="${taxonConcept}"/></a>                      
                                </li>                       
                            </ul>
                        </td>
                    </tr>
                    <tr>
                        <td><b><spring:message code="actions.send"/></b></td>
                        <td>    
                            <ul class="actionsListInline">
                                <li>
                                    <a class="feedback" href='javascript:feedback("${pageContext.request.contextPath}/feedback/taxon/${taxonConcept.key}")'><spring:message code="feedback.to.provider.on.classification.link"  arguments="${dataProvider.name}" argumentSeparator="|"/> <gbif:taxonPrint concept="${taxonConcept}"/></a>
                                </li>
                            </ul>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div><!--further actions-->
    </c:if>
    
    <c:choose>
        <c:when test="${dataResource==null || dataResource.sharedTaxonomy}">
            <c:set var="rootUrl" value="/species/browse/provider/${dataProvider.key}"/>     
        </c:when>
        <c:otherwise>
            <c:set var="rootUrl" value="/species/browse/resource/${dataResource.key}"/>     
        </c:otherwise>  
    </c:choose>
    
    <c:choose>
        <c:when test="${not empty concepts}">
            <tiles:insert page="quickTaxonSearch.jsp"/>
            <div id="taxonomytree" class="smalltree">
                <gbif:smallbrowser concepts="${concepts}" rootUrl="${rootUrl}" highestRank="${highestRank}" selectedConcept="${taxonConcept}" markConceptBelowThreshold="${dataProvider.key==nubProvider.key}" messageSource="${messageSource}"/>
            </div>      
        </c:when>
        <c:when test="${dataProvider!=null}">       
            <spring:message code="taxonomy.browser.notree.for"/> ${dataProvider.name}<c:if test="${dataResource!=null}">: ${dataResource.name}</c:if>
        </c:when>   
        <c:otherwise>       
            <spring:message code="taxonomy.browser.notree"/>
        </c:otherwise>      
    </c:choose> 
</div>
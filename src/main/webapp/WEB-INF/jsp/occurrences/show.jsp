<%-- 
    Document   : show
    Created on : Apr 21, 2010, 9:36:39 AM
    Author     : "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
--%>
<%@ page contentType="text/html" pageEncoding="UTF-8" %>
<%@ include file="/common/taglibs.jsp" %>
<!DOCTYPE html>
<html version="HTML+RDFa 1.1" lang="en-US"
      xmlns="http://www.w3.org/1999/xhtml"
      xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
      xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
      xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
      xmlns:dc="http://purl.org/dc/terms/"
      xmlns:dwc="http://rs.tdwg.org/dwc/terms/"
      dir="ltr">
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="geo.position" content="${occurrence.latitude};${occurrence.longitude}"/>
        <c:set var="thisUri" value="${fn:replace(pageContext.request.requestURL, pageContext.request.requestURI, '')}${pageContext.request.contextPath}/occurrences/${id}"/>
        <meta about="${thisUri}" property="dc:creator" content="${collectionInstitution}"/>
        <meta about="${thisUri}" property="dc:date" content="${occurrence.createdDate}T00:00:00+10:00" />
        <meta about="${thisUri}" property="dc:description" content="Atlas of Living Australia occurrence record ${occurrence.id} from ${occurrence.dataProvider} (${occurrence.dataResource})" />
        <title>Occurrence Record: ${id} - ${occurrence.taxonName} | Atlas of Living Australia</title>
        <script type="text/javascript" src="${pageContext.request.contextPath}/static/js/annotation.js"></script>
        <script type="text/javascript" src="${pageContext.request.contextPath}/static/js/jquery.form.js"></script>
        <script type="text/javascript" src="${pageContext.request.contextPath}/static/js/jquery.url.js"></script>
        <script type="text/javascript" src="${pageContext.request.contextPath}/static/js/jquery.cookie.js"></script>
        <script type="text/javascript" src="${pageContext.request.contextPath}/static/js/jquery.simplemodal.js"></script>
        <script type="text/javascript" src="${pageContext.request.contextPath}/static/js/jquery-fancybox/jquery.fancybox-1.3.1.pack.js"></script>
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/static/js/jquery-fancybox/jquery.fancybox-1.3.1.css" media="screen" />
        <link type="text/css" rel="stylesheet" href="${initParam.centralServer}/wp-content/themes/ala/css/basic.css" charset="utf-8">
        <%-- <link type="text/css" rel="stylesheet" href="${initParam.centralServer}/wp-content/themes/ala/css/occurrenceSpecial.css" charset="utf-8"> --%>
        <%-- <link type="text/css" rel="stylesheet" href="${pageContext.request.contextPath}/static/css/record.css" charset="utf-8"> --%>
        <link type="text/css" rel="stylesheet" href="${initParam.centralServer}/wp-content/themes/ala/css/record.css" charset="utf-8">
        <script type="text/javascript">
            // Set some global variables to be used in imported JS files
            occurrenceHtmlUrl = "http://${pageContext.request.serverName}${pageContext.request.contextPath}/occurrences/${occurrence.id}";
            occurrenceJsonUrl = "${pageContext.request.contextPath}/occurrences/${occurrence.id}.json";
            annotationJsonUrl = "${pageContext.request.contextPath}/annotation/retrieveAllAnnotationsForOccurrenceRecord?url="+occurrenceHtmlUrl;
            annotationReplyJsonUrl = "${pageContext.request.contextPath}/annotation/retrieveReplyAnnotationsForAnnotation";
            //saveAnnotationUrl = "${pageContext.request.contextPath}/annotation/saveAnnotation";
            //getAnnotationsUrl =  "<gbif:propertyLoader bundle='portal' property='hostName'/>/danno/annotea/";

            // Jquery Document.onLoad equivalent
            $(document).ready(function() {
                // add record id to body tag
                $('body').attr('id','record');
                // give every second row a class="grey-bg"
                $('table#datasetTable, table#taxonomyTable, table#geospatialTable').each(function(i, el) {
                    $(this).find('tr').each(function(j, tr) {
                        if (j % 2 == 0) {
                            $(this).addClass("grey-bg");
                        }
                    });
                });
                
                // delete record buttons
                $('#deleteButton').fancybox({
                    'href': '#deleteConfirm',
                    'hideOnContentClick' : false,
                    'hideOnOverlayClick': false,
                    'showCloseButton': false,
                    'titleShow' : false
                });
                $('button#cancelDelete').click(function(e) {
                    e.preventDefault();
                    $.fancybox.close();
                });
                

            }); // end document ready
        </script>
    </head>
    <body>
        <div id="header">
            <div id="breadcrumb">
                <a href="${initParam.centralServer}">Home</a>
                <a href="${initParam.centralServer}/explore">Explore</a>
                Occurrence Record ${occurrence.id}
            </div>
            <h1>Occurrence Record: ${occurrence.id} </h1>
            <c:if test="${not empty occurrence.taxonName}">
                <h1 style="font-size: 1.6em">
                    <alatag:formatSciName rankId="${occurrence.rankId}" name="${occurrence.taxonName}"/> ${occurrence.author}
                    <c:if test="${not empty occurrence.commonName}"> | ${occurrence.commonName}</c:if>
                </h1>
            </c:if>
        </div>
        <div id="column-one">
            <div class="section">
                <c:if test="${not empty occurrence}">
                    <c:set var="bieWebappContext" scope="request"><ala:propertyLoader bundle="biocache" property="bieWebappContext"/></c:set>
                    <c:set var="collectionsWebappContext" scope="request"><ala:propertyLoader bundle="biocache" property="collectionsWebappContext"/></c:set> 
                    <div id="debug"></div>
                    <div id="occurrenceDataset" class="occurrenceSection">
                        <h2>Dataset<span class="head-link-right"><a href="#dataset" class="annotationLink" name="modal" title="add your comments">Annotate Dataset</a></span></h2>
                        <table class="occurrenceTable" id="datasetTable">
                            <alatag:occurrenceTableRow annotate="false" section="dataset" fieldCode="dataProvider" fieldName="Data Provider">
                                <c:choose>
                                    <c:when test="${occurrence.dataProviderUid != null && not empty occurrence.dataProviderUid}">
                                        <a href="${collectionsWebappContext}/public/show/${occurrence.dataProviderUid}">
                                            ${occurrence.dataProvider}
                                        </a>
                                    </c:when>
                                    <c:otherwise>
                                        ${occurrence.dataProvider}
                                    </c:otherwise>
                                </c:choose>
                            </alatag:occurrenceTableRow>
                            <alatag:occurrenceTableRow annotate="false" section="dataset" fieldCode="dataResource" fieldName="Data Set">
                                <c:choose>
                                    <c:when test="${occurrence.dataResourceUid != null && not empty occurrence.dataResourceUid}">
                                        <a href="${collectionsWebappContext}/public/show/${occurrence.dataResourceUid}">
                                            ${occurrence.dataResource}
                                        </a>
                                    </c:when>
                                    <c:otherwise>
                                        ${occurrence.dataResource}
                                    </c:otherwise>
                                </c:choose>
                            </alatag:occurrenceTableRow>

                            <c:choose>
                                <c:when test="${occurrence.institutionCodeUid != null && not empty occurrence.institutionCodeUid}">
                                    <alatag:occurrenceTableRow annotate="true" section="dataset" fieldCode="institutionCode" fieldName="Institution">
                                        <a href="${collectionsWebappContext}/public/show/${occurrence.institutionCodeUid}">
                                            ${collectionInstitution}
                                        </a>
                                        <br/>Institution Code: "${occurrence.institutionCode}"
                                    </alatag:occurrenceTableRow>
                                </c:when>
                                <c:otherwise>
                                    <alatag:occurrenceTableRow annotate="true" section="dataset" fieldCode="institutionCode" fieldName="Institution Code">
                                        ${occurrence.institutionCode}
                                    </alatag:occurrenceTableRow>
                                </c:otherwise>
                                <%--
	                           	${occurrence.institutionCode}
	                               <c:if test="${not empty institutionCodeLsid && not empty institutionCodeName}">
	                                   (<a href="${pageContext.request.contextPath}/institutions/${occurrence.institutionCodeLsid}">${occurrence.institutionCodeName}</a>)
	                               </c:if>
	                            --%>                            	                          	
                            </c:choose>
                            <c:choose>
                                <c:when test="${occurrence.collectionCodeUid != null && not empty occurrence.collectionCodeUid}">
                                    <alatag:occurrenceTableRow annotate="true" section="dataset" fieldCode="collectionCode" fieldName="Collection">
                                        <a href="${collectionsWebappContext}/public/show/${occurrence.collectionCodeUid}">
                                            ${collectionName}
                                        </a>
                                        <br/>Collection Code: "${occurrence.collectionCode}"
                                    </alatag:occurrenceTableRow>
                                </c:when>
                                <c:otherwise>
                                    <alatag:occurrenceTableRow annotate="true" section="dataset" fieldCode="collectionCode" fieldName="Collection Code">
                                        ${occurrence.collectionCode}
                                    </alatag:occurrenceTableRow>
                                </c:otherwise>
                            </c:choose>
                            <alatag:occurrenceTableRow annotate="true" section="dataset" fieldCode="catalogueNumber" fieldName="Catalogue Number">                                
                                <c:if test="${not empty occurrence.catalogueNumber}">
                                	${occurrence.catalogueNumber}
                                </c:if>
                                <c:if test="${empty occurrence.catalogueNumber && not empty rawOccurrence.catalogueNumber}">
                                	${rawOccurrence.catalogueNumber}
                                </c:if> 
                                <c:if test="${not empty occurrence.catalogueNumber && not empty rawOccurrence.catalogueNumber && (fn:toLowerCase(occurrence.catalogueNumber) != fn:toLowerCase(rawOccurrence.catalogueNumber))}">
                                    <br/><span class="originalValue">Supplied as: "${rawOccurrence.catalogueNumber}"</span>
                               	</c:if>                            	
                            </alatag:occurrenceTableRow>
                            <alatag:occurrenceTableRow annotate="true" section="dataset" fieldCode="basisOfRecord" fieldName="Basis of Record">
                                <c:if test="${not empty occurrence.basisOfRecord}">
                                	${occurrence.basisOfRecord}
                                </c:if>
                                <c:if test="${empty occurrence.basisOfRecord && not empty rawOccurrence.basisOfRecord}">
                                	${rawOccurrence.basisOfRecord}
                                </c:if>                                
                                <c:if test="${not empty occurrence.basisOfRecord && not empty rawOccurrence.basisOfRecord && (fn:toLowerCase(occurrence.basisOfRecord) != fn:toLowerCase(rawOccurrence.basisOfRecord))}">
                                    <br/><span class="originalValue">Supplied as: "${rawOccurrence.basisOfRecord}"</span>
                               	</c:if>
                            </alatag:occurrenceTableRow>
                            <alatag:occurrenceTableRow annotate="true" section="dataset" fieldCode="occurrenceDate" fieldName="Record Date">
	  							<c:if test="${empty occurrence.occurrenceDate && empty rawOccurrence.eventDate && empty rawOccurrence.eventDate && empty rawOccurrence.year && empty rawOccurrence.month && empty rawOccurrence.day}">date not supplied</c:if>
	  							<fmt:formatDate value="${occurrence.occurrenceDate}" pattern="yyyy-MM-dd"/>
                            	  <c:if test="${not empty occurrence.occurrenceDate}"><br/></c:if>
                            	  <span class="originalValue">
                            	  	<c:choose>
                            	  		<c:when test="${not empty rawOccurrence.eventDate}">
                            	  			Supplied as: "${rawOccurrence.eventDate}"
                            	  		</c:when>
                            	  		<c:when test="${not empty rawOccurrence.year || not empty rawOccurrence.month || not empty rawOccurrence.day}">
                            	  			Supplied as:  "year: ${rawOccurrence.year}, month: ${rawOccurrence.month}, day: ${rawOccurrence.day}"
                            	  		</c:when>
                            	  	</c:choose>
                            	  	</span>
                            </alatag:occurrenceTableRow>
                            <alatag:occurrenceTableRow annotate="true" section="dataset" fieldCode="identifierName" fieldName="Identifier">${occurrence.identifierName}</alatag:occurrenceTableRow>
                            <alatag:occurrenceTableRow annotate="true" section="dataset" fieldCode="identifierDate" fieldName="Identified Date">
                            	<fmt:formatDate value="${occurrence.identifierDate}" pattern="yyyy-MM-dd"/>
                            </alatag:occurrenceTableRow>
                            <alatag:occurrenceTableRow annotate="true" section="dataset" fieldCode="collectorName" fieldName="Collector/observer">
                                <c:if test="${not empty occurrence.collector}">
                                	${occurrence.collector}
                                </c:if>
                                <c:if test="${empty occurrence.collector && not empty rawOccurrence.collectorName}">
                                	${rawOccurrence.collectorName}
                                </c:if>                                 
                                <c:if test="${not empty occurrence.collector && not empty rawOccurrence.collectorName && (fn:toLowerCase(occurrence.collector) != fn:toLowerCase(rawOccurrence.collectorName))}">
                                    <br/><span class="originalValue">Supplied as: "${rawOccurrence.collectorName}"</span>
                               	</c:if>
                            </alatag:occurrenceTableRow>
                            <alatag:occurrenceTableRow annotate="true" section="dataset" fieldCode="typeStatus" fieldName="Type Status">${occurrence.typeStatus}</alatag:occurrenceTableRow>
                            <alatag:occurrenceTableRow annotate="false" section="dataset" fieldCode="taxonomicIssue" fieldName="taxonomic Issue"><c:if test="${occurrence.taxonomicIssue != 0}">${occurrence.taxonomicIssue}</c:if></alatag:occurrenceTableRow>
                        </table>
                    </div>
                    <div id="occurrenceTaxonomy" class="occurrenceSection">
                        <h2>Taxonomy<span class="head-link-right"><a href="#taxonomy" class="annotationLink" name="modal" title="add your comments">Annotate Taxonomy</a></span></h2>
                        <table class="occurrenceTable"  id="taxonomyTable">
                            <alatag:occurrenceTableRow annotate="true" section="taxonomy" fieldCode="scientificName" fieldName="Scientific Name">
                                <c:choose>
                                    <c:when test="${!(fn:containsIgnoreCase(occurrence.taxonName, occurrence.rawTaxonName) || fn:containsIgnoreCase(occurrence.author, occurrence.rawAuthor))}">
                                        <alatag:formatSciName rankId="${occurrence.rankId}" name="${occurrence.rawTaxonName}"/> ${occurrence.rawAuthor}
                                        (interpreted as <c:if test="${not empty occurrence.taxonConceptLsid}"><a href="${bieWebappContext}/species/${occurrence.taxonConceptLsid}"></c:if>
                                            <alatag:formatSciName rankId="${occurrence.rankId}" name="${occurrence.taxonName}"/> ${occurrence.author}<c:if test="${not empty occurrence.taxonConceptLsid}"></a></c:if>)<br/>Original value:"${rawOccurrence.scientificName}"
                                    </c:when>
                                    <c:otherwise>
                                        <c:if test="${not empty occurrence.taxonConceptLsid}">
                                          <a href="${bieWebappContext}/species/${occurrence.taxonConceptLsid}">
                                        </c:if>
                                        <c:if test="${not empty occurrence.taxonName}">
                                        	<alatag:formatSciName rankId="${occurrence.rankId}" name="${occurrence.taxonName}"/>
                                                ${occurrence.author}
                                        </c:if>
                                        <c:if test="${empty occurrence.taxonName && not empty rawOccurrence.scientificName}">                                        	
                                                ${rawOccurrence.scientificName}
                                        </c:if>        
                                        <c:if test="${not empty occurrence.taxonConceptLsid}">
                                          </a>
                                        </c:if>
                                        <c:if test="${not empty occurrence.taxonName && not empty rawOccurrence.scientificName && (fn:toLowerCase(occurrence.taxonName) != fn:toLowerCase(rawOccurrence.scientificName))}">
                                        	<br/><span class="originalValue">Supplied as: "${rawOccurrence.scientificName}"</span>
                                        </c:if>
                                    </c:otherwise>
                                </c:choose>
                            </alatag:occurrenceTableRow>
                            <alatag:occurrenceTableRow annotate="true" section="taxonomy" fieldCode="taxonRank" fieldName="Taxon Rank">
                            	<span style="text-transform: capitalize;">
	                            	<c:if test="${not empty occurrence.rank}">
	                                	${occurrence.rank}
	                                </c:if>
	                                <c:if test="${empty occurrence.rank && not empty rawOccurrence.rank}">
	                                	${rawOccurrence.rank}
	                                </c:if>
                                </span>
                               	<c:if test="${not empty occurrence.rank && not empty rawOccurrence.rank  && (fn:toLowerCase(occurrence.rank) != fn:toLowerCase(rawOccurrence.rank))}">
                               		<br/><span class="originalValue">Supplied as: "${rawOccurrence.rank}"</span>
                               	</c:if>                               	
                            </alatag:occurrenceTableRow>
                            <alatag:occurrenceTableRow annotate="false" section="taxonomy" fieldCode="commonName" fieldName="Common Name">
                                 <c:if test="${not empty occurrence.commonName}">
	                                	${occurrence.commonName}
                                </c:if>
                                <c:if test="${empty occurrence.commonName && not empty rawOccurrence.vernacularName}">
                                	${rawOccurrence.vernacularName}
                                </c:if>
                                 <c:if test="${not empty occurrence.commonName && not empty rawOccurrence.vernacularName && (fn:toLowerCase(occurrence.commonName) != fn:toLowerCase(rawOccurrence.vernacularName))}">
                               		<br/><span class="originalValue">Supplied as: "${rawOccurrence.vernacularName}"</span>
                               	 </c:if>
                            </alatag:occurrenceTableRow>
                            <alatag:occurrenceTableRow annotate="true" section="taxonomy" fieldCode="kingdom" fieldName="Kingdom">
                                <c:if test="${not empty occurrence.kingdom}">
                                	<c:if test="${not empty occurrence.kingdomLsid}"><a href="${bieWebappContext}/species/${occurrence.kingdomLsid}"></c:if>
                                		${occurrence.kingdom}
                                	<c:if test="${not empty occurrence.kingdomLsid}"></a></c:if>
                                </c:if>
                                <c:if test="${empty occurrence.kingdom && not empty rawOccurrence.kingdom}">
                                	${rawOccurrence.kingdom}
                                </c:if>                                 
								<c:if test="${not empty occurrence.kingdom && not empty rawOccurrence.kingdom && (fn:toLowerCase(occurrence.kingdom) != fn:toLowerCase(rawOccurrence.kingdom))}">
                               		<br/><span class="originalValue">Supplied as: "${rawOccurrence.kingdom}"</span>
                               	</c:if>                                
                            </alatag:occurrenceTableRow>
                            <alatag:occurrenceTableRow annotate="true" section="taxonomy" fieldCode="phylum" fieldName="Phylum">
                                <c:if test="${not empty occurrence.phylum}">
                                	<c:if test="${not empty occurrence.phylumLsid}"><a href="${bieWebappContext}/species/${occurrence.phylumLsid}"></c:if>
                                		${occurrence.phylum}
                                	<c:if test="${not empty occurrence.phylumLsid}"></a></c:if>
                                </c:if>
                                <c:if test="${empty occurrence.phylum && not empty rawOccurrence.phylum}">
                                	${rawOccurrence.phylum}
                                </c:if>
                                <c:if test="${not empty occurrence.phylum && not empty rawOccurrence.phylum && (fn:toLowerCase(occurrence.phylum) != fn:toLowerCase(rawOccurrence.phylum))}">
                               		<br/><span class="originalValue">Supplied as: "${rawOccurrence.phylum}"</span>
                               	 </c:if>
                            </alatag:occurrenceTableRow>
                            <alatag:occurrenceTableRow annotate="true" section="taxonomy" fieldCode="class" fieldName="Class">
                                <c:if test="${not empty occurrence.clazz}">
                                	<c:if test="${not empty occurrence.classLsid}"><a href="${bieWebappContext}/species/${occurrence.classLsid}"></c:if>
                                		${occurrence.clazz}
                                	<c:if test="${not empty occurrence.classLsid}"></a></c:if>
                                </c:if>
                                <c:if test="${empty occurrence.clazz && not empty rawOccurrence.klass}">
                                	${rawOccurrence.klass}
                                </c:if>                                
                                <c:if test="${not empty occurrence.clazz && not empty rawOccurrence.klass && (fn:toLowerCase(occurrence.clazz) != fn:toLowerCase(rawOccurrence.klass))}">
                               		<br/><span class="originalValue">Supplied as: "${rawOccurrence.klass}"</span>
                               	 </c:if>
                            </alatag:occurrenceTableRow>
                            <alatag:occurrenceTableRow annotate="true" section="taxonomy" fieldCode="order" fieldName="Order">
                                <c:if test="${not empty occurrence.order}">
                                	<c:if test="${not empty occurrence.orderLsid}"><a href="${bieWebappContext}/species/${occurrence.orderLsid}"></c:if>
                                		${occurrence.order}
                                	<c:if test="${not empty occurrence.familyLsid}"></a></c:if>
                                </c:if>
                                <c:if test="${empty occurrence.order && not empty rawOccurrence.order}">
                                	${rawOccurrence.order}
                                </c:if>                                
                                <c:if test="${not empty occurrence.order && not empty rawOccurrence.order && (fn:toLowerCase(occurrence.order) != fn:toLowerCase(rawOccurrence.order))}">
                               		<br/><span class="originalValue">Supplied as: "${rawOccurrence.order}"</span>
                               	 </c:if>
                            </alatag:occurrenceTableRow>
                            <alatag:occurrenceTableRow annotate="true" section="taxonomy" fieldCode="family" fieldName="Family">
                                <c:if test="${not empty occurrence.family}">
                                	<c:if test="${not empty occurrence.familyLsid}"><a href="${bieWebappContext}/species/${occurrence.familyLsid}"></c:if>
                                		${occurrence.family}
                                	<c:if test="${not empty occurrence.familyLsid}"></a></c:if>
                                </c:if>
                                <c:if test="${empty occurrence.family && not empty rawOccurrence.family}">
                                	${rawOccurrence.family}
                                </c:if>                                
                                <c:if test="${not empty occurrence.family && not empty rawOccurrence.family && (fn:toLowerCase(occurrence.family) != fn:toLowerCase(rawOccurrence.family))}">
                               		<br/><span class="originalValue">Supplied as: "${rawOccurrence.family}"</span>
                               	 </c:if>
                            </alatag:occurrenceTableRow>
                            <alatag:occurrenceTableRow annotate="true" section="taxonomy" fieldCode="genus" fieldName="Genus">
                                <c:if test="${not empty occurrence.genus}">
                                	<c:if test="${not empty occurrence.genusLsid}"><a href="${bieWebappContext}/species/${occurrence.genusLsid}"></c:if>
                                		${occurrence.genus}
                                	<c:if test="${not empty occurrence.genusLsid}"></a></c:if>
                                </c:if>
                                <c:if test="${empty occurrence.genus && not empty rawOccurrence.genus}">
                                	${rawOccurrence.genus}
                                </c:if>                                
                                <c:if test="${not empty occurrence.genus && not empty rawOccurrence.genus && (fn:toLowerCase(occurrence.genus) != fn:toLowerCase(rawOccurrence.genus))}">
                               		<br/><span class="originalValue">Supplied as: "${rawOccurrence.genus}"</span>
                               	 </c:if>
                            </alatag:occurrenceTableRow>
                            <alatag:occurrenceTableRow annotate="true" section="taxonomy" fieldCode="species" fieldName="Species">
                                <c:if test="${not empty occurrence.species}">
                                	<c:if test="${not empty occurrence.speciesLsid}"><a href="${bieWebappContext}/species/${occurrence.speciesLsid}"></c:if>
                                		${occurrence.species}
                                	<c:if test="${not empty occurrence.speciesLsid}"></a></c:if>
                                </c:if>
                                <c:if test="${empty occurrence.species && not empty rawOccurrence.species}">
                                	${rawOccurrence.species}
                                </c:if>                                
                                <c:if test="${not empty occurrence.species && not empty rawOccurrence.species && (fn:toLowerCase(occurrence.species) != fn:toLowerCase(rawOccurrence.species))}">
                               		<br/><span class="originalValue">Supplied as: "${rawOccurrence.species}"</span>
                               	 </c:if>
                            </alatag:occurrenceTableRow>
                        </table>
                    </div>
                    <div id="occurrenceGeospatial" class="occurrenceSection">
                        <h2>Geospatial<span class="head-link-right"><a href="#geospatial" class="annotationLink" name="modal" title="add your comments">Annotate Geospatial</a></span></h2>
                        <table class="occurrenceTable"  id="geospatialTable">
                            <alatag:occurrenceTableRow annotate="true" section="geospatial" fieldCode="country" fieldName="Country">
                            	<c:if test="${not empty occurrence.countryCode}">
                            		<fmt:message key="country.${occurrence.countryCode}"/>
                           		</c:if>
								<c:if test="${empty occurrence.countryCode && not empty rawOccurrence.country}">
                                		<br/><span class="originalValue">Supplied as: "${rawOccurrence.country}"</span>
                               	 </c:if>                           		
                            </alatag:occurrenceTableRow>
                            <alatag:occurrenceTableRow annotate="true" section="geospatial" fieldCode="state" fieldName="State/Province">
                            	<c:if test="${not empty occurrence.state}">
                                	${occurrence.state}
                                </c:if>
                                <c:if test="${empty occurrence.state && not empty rawOccurrence.stateOrProvince}">
                                	${rawOccurrence.stateOrProvince}
                                </c:if>
                            	<c:if test="${not empty occurrence.state && not empty rawOccurrence.stateOrProvince && (fn:toLowerCase(occurrence.state) != fn:toLowerCase(rawOccurrence.stateOrProvince))}">
                                		<br/><span class="originalValue">Supplied as: "${rawOccurrence.stateOrProvince}"</span>
                               	 </c:if>
                           	</alatag:occurrenceTableRow>
                            <alatag:occurrenceTableRow annotate="true" section="geospatial" fieldCode="biogeographicRegion" fieldName="Biogeographic Region">
                            	${occurrence.biogeographicRegion}
                           	</alatag:occurrenceTableRow>
                            <alatag:occurrenceTableRow annotate="true" section="geospatial" fieldCode="locality" fieldName="Place">
                            	${occurrence.place}
                           	</alatag:occurrenceTableRow>
                            <alatag:occurrenceTableRow annotate="true" section="geospatial" fieldCode="locality" fieldName="Locality">
                            	${rawOccurrence.locality}
                           	</alatag:occurrenceTableRow>
                            <alatag:occurrenceTableRow annotate="true" section="geospatial" fieldCode="latitude" fieldName="Latitude">
                                	${rawOccurrence.latitude}
                           	</alatag:occurrenceTableRow>
                            <alatag:occurrenceTableRow annotate="true" section="geospatial" fieldCode="longitude" fieldName="Longitude">
                                	${rawOccurrence.longitude}
                           	</alatag:occurrenceTableRow>
                            <alatag:occurrenceTableRow annotate="false" section="geospatial" fieldCode="coordinatePrecision" fieldName="Coordinate Precision (metres)">
                            	<c:if test="${not empty occurrence.latitude || not empty occurrence.longitude}">
                            		${not empty occurrence.coordinatePrecision ? occurrence.coordinatePrecision : 'Unknown'}
                            	</c:if>
                            </alatag:occurrenceTableRow>
                            <c:if test="${not empty rawOccurrence.generalisedInMetres}">
                            	<alatag:occurrenceTableRow annotate="false" section="geospatial" fieldCode="generalisedInMetres" fieldName="Coordinates generalised">
                            		Due to sensitivity concerns, the coordinates of this record have been generalised to ${rawOccurrence.generalisedInMetres} metres.
                            	</alatag:occurrenceTableRow>
                            </c:if>
                            <alatag:occurrenceTableRow annotate="false" section="geospatial" fieldCode="geodeticDatum" fieldName="Geodetic datum">
                            	${rawOccurrence.geodeticDatum}
                           	</alatag:occurrenceTableRow>
                            <alatag:occurrenceTableRow annotate="false" section="geospatial" fieldCode="occurrenceRemarks" fieldName="Notes">
                            	${rawOccurrence.occurrenceRemarks}
                           	</alatag:occurrenceTableRow>
                            <c:if test="${not empty rawOccurrence.individualCount && rawOccurrence.individualCount>0}">
                            	<alatag:occurrenceTableRow annotate="false" section="geospatial" fieldCode="individualCount" fieldName="Individual count">${rawOccurrence.individualCount}</alatag:occurrenceTableRow>
                            </c:if>
                            <alatag:occurrenceTableRow annotate="false" section="geospatial" fieldCode="citation" fieldName="Citation">
                               	${rawOccurrence.citation}
                            </alatag:occurrenceTableRow>
                        </table>
                    </div>
                    <div id="occurrenceMetadata">
                        First indexed: <fmt:formatDate value="${occurrence.createdDate}" pattern="yyyy-MM-dd"/>
                        <br/>
                        Last indexed:  <fmt:formatDate value="${occurrence.modifiedDate}" pattern="yyyy-MM-dd"/>
                    </div>
                    <jsp:include page="annotateDataset.jsp"/>
                    <jsp:include page="annotateTaxonomy.jsp"/>
                    <jsp:include page="annotateGeospatial.jsp"/>
                    <jsp:include page="annotateReply.jsp"/>
                </c:if>
                <c:if test="${empty occurrence}">
                    <p>The requested record was not found.</p>
                </c:if>
            </div>
        </div><!--close col-one--> 
        <div id="column-two">
            <%-- Allow logged-in user to delete/edit their own observations --%><%-- casUser = ${pageContext.request.userPrincipal.name} | remoteUser = ${pageContext.request.remoteUser} | userId = ${occurrence.userId} --%>
            <c:if test="${not empty pageContext.request.userPrincipal && not empty occurrence.userId && fn:containsIgnoreCase(pageContext.request.userPrincipal.name, occurrence.userId)}">
                <!-- userPrincipal = ${pageContext.request.userPrincipal.name} -->
                <div id="editOccurrence" class="section">
                    <button id="deleteButton" style="font-size: 120%; color: #E8572F; font-weight: bold; padding: 3px 6px 4px 6px;">Delete this record</button>
                </div>
                <div style="display: none">
                    <div id="deleteConfirm">
                        <h2>Are you sure you want to delete this record?</h2>
                        <p>(the record will be permanently deleted)</p>
                        <p>&nbsp;</p>
                        <form action="${pageContext.request.contextPath}/share/sighting/delete" method="post" name="deleteSighting" id="deleteSighting">
                            <input type="hidden" name="sightingId" value="${occurrence.id}"/>
                            <input type="hidden" name="userId" value="${pageContext.request.userPrincipal.name}"/>
                            <input type="submit" name="action" id="sightingDelete" value="Yes, delete this record"/> 
                            <button id="cancelDelete">Cancel</button>
                        </form>
                        
                    </div>
                </div>
            </c:if> 
            <c:if test="${not empty collectionLogo}">
                <div id="logoRecord" class="section">
                    <img src="${collectionLogo}" alt="collection logo"/>
                </div>
            </c:if> 
            <!-- add the occurrence issue descriptions if necessary -->
            <c:set var="noIssues" value="${!((empty occurrence.taxonomicIssue || occurrence.taxonomicIssue==0)  && (empty occurrence.geospatialIssue || occurrence.geospatialIssue==0) && (empty occurrence.otherIssue || occurrence.otherIssue==0))}"/>
            <c:if test="${noIssues}">
                <div id="warnings" class="section">
                    <h2>Possible Issues</h2>
                    <p class="half-padding-bottom">Data validation tools identified the following possible issues:</p>
                    <ul>
                        <alatag:formatGeospatialIssues issuesBit="${occurrence.geospatialIssue}"/>
                        <c:if test="${not empty geospatialIssueText}">
                            <li>Geospatial issues: ${geospatialIssueText}</li>
                        </c:if>
                        <alatag:formatOtherIssues issuesBit="${occurrence.otherIssue}" />
                        <c:if test="${not empty otherIssueText}">
                            <li>Miscellaneous issues: ${otherIssueText}</li>
                        </c:if>
                        <alatag:formatTaxonomicIssues issuesBit="${occurrence.taxonomicIssue}"/>
                        <c:if test="${not empty taxonomicIssueText}">
                            <li>Taxonomic issues: ${taxonomicIssueText}</li>
                        </c:if>
                    </ul>
                    <!-- <p><a href="${initParam.centralServer}/about/media-centre/terms-of-use">What does this mean?</a></p> -->
                </div>
            </c:if>
            <c:if test="${not empty images}">
                <div id="imageRecords" class="section">
                    <h2>Images</h2>
                    <c:forEach items="${images}" var="imageRecord">
                        <div class="imageRecord">
                            <c:choose>
                                <c:when test="${not empty imageRecord.htmlForDisplay}">
                                    <c:if test="${fn:startsWith(imageRecord.htmlForDisplay, 'http://')}">
                                        <a href="${imageRecord.url}"><img src="${imageRecord.htmlForDisplay}"/></a>
                                        <p>
                                            ${imageRecord.description}
                                        </p>
                                    </c:if>
                                </c:when>
                                <c:otherwise>
                                    <c:forEach var="url" items="${fn:split(imageRecord.url, ',')}">
                                        <c:set var="url" value="${fn:trim(url)}"/>
                                        <a href="${url}"><img src="${url}"/></a>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </c:forEach>
                </div>
            </c:if>
            <c:if test="${not empty occurrence.latitude && not empty occurrence.longitude}">
                <div class="section">
                    <script type="text/javascript" src="http://maps.google.com/maps/api/js?sensor=false"></script>
                    <script type="text/javascript">
                        $(document).ready(function() {
                            var latlng = new google.maps.LatLng(${occurrence.latitude}, ${occurrence.longitude});
                            var myOptions = {
                                zoom: 5,
                                center: latlng,
                                scaleControl: true,
                                streetViewControl: false,
                                mapTypeControl: true,
                                mapTypeControlOptions: {
                                    style: google.maps.MapTypeControlStyle.DROPDOWN_MENU,
                                    mapTypeIds: [google.maps.MapTypeId.ROADMAP, google.maps.MapTypeId.HYBRID, google.maps.MapTypeId.TERRAIN ]
                                },
                                mapTypeId: google.maps.MapTypeId.ROADMAP
                            };

                            var map = new google.maps.Map(document.getElementById("occurrenceMap"), myOptions);

                            var marker = new google.maps.Marker({
                                position: latlng,
                                map: map,
                                title:"Occurrence Location"
                            });

                            <c:if test="${not empty occurrence.coordinatePrecision}">
                                // Add a Circle overlay to the map.
                                var radius = parseInt('${occurrence.coordinatePrecision}');
                                circle = new google.maps.Circle({
                                    map: map,
                                    radius: radius, // 3000 km
                                    strokeWeight: 1,
                                    strokeColor: 'white',
                                    strokeOpacity: 0.5,
                                    fillColor: '#2C48A6',
                                    fillOpacity: 0.2
                                });
                                // bind circle to marker
                                circle.bindTo('center', marker, 'position');
                            </c:if>
                        });
                    </script>
                    <h2>Location of record</h2>
                    <div id="occurrenceMap"></div>
                </div>
            </c:if>
        </div><!--close col-two-->
    </body>
</html>

<%-- 
    Document   : sighting
    Created on : Aug 6, 2010, 5:19:21 PM
    Author     : "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ include file="/common/taglibs.jsp" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">
<c:set var="googleKey" scope="request"><ala:propertyLoader bundle="biocache" property="googleKey"/></c:set>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta charset="UTF-8" >
        <title>Your Sightings | Atlas of Living Australia</title>
        <script type="text/javascript" src="${pageContext.request.contextPath}/static/js/date.js"></script>
        <script type="text/javascript" src="${pageContext.request.contextPath}/static/js/jquery.timePicker.js"></script>
        <link rel="stylesheet" type="text/css" media="screen" href="${pageContext.request.contextPath}/static/css/timePicker.css" />
        <script type="text/javascript" src="${pageContext.request.contextPath}/static/js/jquery.datePicker.js"></script>
        <link rel="stylesheet" type="text/css" media="screen" href="${pageContext.request.contextPath}/static/css/datePicker.css" />
        <script type="text/javascript" src="${pageContext.request.contextPath}/static/js/jquery.tooltip.min.js"></script>
        <link rel="stylesheet" type="text/css" media="screen" href="${pageContext.request.contextPath}/static/css/jquery.tooltip.css" />
        <script type="text/javascript" src="http://www.google.com/jsapi?key=${googleKey}"></script>
        <script type="text/javascript">
            <c:if test="${!empty pageContext.request.remoteUser}"><%-- User is logged in --%>
                google.load("maps", "3", {other_params:"sensor=false"});

                $(document).ready(function() {
                    var myOptions = {
                        scaleControl: true,
                        mapTypeControl: true,
                        mapTypeControlOptions: {
                            style: google.maps.MapTypeControlStyle.DROPDOWN_MENU
                        },
                        navigationControl: true,
                        navigationControlOptions: {
                            style: google.maps.NavigationControlStyle.SMALL // DEFAULT
                        },
                        mapTypeId: google.maps.MapTypeId.HYBRID
                    };

                    var map = new google.maps.Map(document.getElementById("mapCanvas"), myOptions);
                    var latlngbounds = new google.maps.LatLngBounds();
                    
                    <c:forEach var="rec" items="${occurrences}" varStatus="status">
                        var latlng_${status.count} = new google.maps.LatLng(${rec.latitude}, ${rec.longitude});
                        var marker_${status.count} = new google.maps.Marker({
                            position: latlng_${status.count},
                            map: map,
                            title:"Occurrence ${rec.id}"
                        });
                        var contentString_${status.count} = "<div>Record Id: <a href='${pageContext.request.contextPath}/occurrences/${rec.id}'>${rec.id}</a></div>" +
                            "<div><a href='http://bie.ala.org.au/species/${rec.taxonConceptLsid}'><alatag:formatSciName name="${rec.taxonName}" rankId="${rec.rankId}"/> (${rec.commonName})</div>";
                        var infowindow_${status.count} = new google.maps.InfoWindow({
                            content: contentString_${status.count}
                        });
                        google.maps.event.addListener(marker_${status.count}, 'click', function() {
                            infowindow_${status.count}.open(map, marker_${status.count});
                        });
                        latlngbounds.extend(latlng_${status.count});
                    </c:forEach>
                    //map.setCenter(latlngbounds.getCenter(), map.getBoundsZoomLevel(latlngbounds));
                    map.fitBounds(latlngbounds);
                });
            </c:if>
        </script>
    </head>
    <body>
        <div id="header">
            <div id="breadcrumb">
                <a href="${initParam.centralServer}">Home</a>
                <a href="${initParam.centralServer}/share">Share</a>
                Your sightings
            </div>
            <h1>Your sightings</h1>
        </div>
        
        <c:choose>
            <c:when test="${!empty pageContext.request.remoteUser}"><%-- User is logged in --%>
                 <c:if test="${empty taxonConceptMap}">
                    <div id="column-one">
                        <div class="section">                
                  	<p>You have not yet shared any sightings of species</p>
					<p style="text-align: left;"><span>Individual sightings can be recorded from the species page.</span></p>
					<ol>
						<li>
							<div style="text-align: left;"><span>Enter the species you have seen in the search box above and open the species page</span></div>
						</li>
						<li>
							<div style="text-align: left;"><span>Click share and select sightings</span></div>
						</li>
						<li>
							<div style="text-align: left;"><span>Complete the details of the sighting.</span></div>
						</li>
					</ol>
					</div>
					</div>
	            </c:if>
                <c:if test="${not empty taxonConceptMap}">
                    <div id="column-one">
                        <div class="section">
                            <h3 style="margin-bottom: 8px;">Total sightings: ${fn:length(occurrences)} <span style="font-size: 80%;">(<a href="${pageContext.request.contextPath}/occurrences/search?q=user_id:${pageContext.request.remoteUser}" style="text-decoration: underline;">view list of records</a>)</span></h3>
                            <c:forEach var="tc" items="${taxonConceptMap}">
                            	<a href="http://bie.ala.org.au/species/${tc.guid}">
                                    <c:if test="${not empty tc.imageThumbnailUrl}">
                                        <img src="${tc.imageThumbnailUrl}" alt="species image thumbnail" style="display: block; float: left; margin-right: 10px;"/>
                                    </c:if>
                                </a>
                                <!-- 
                                <div style="float: left; padding-right: 15px" id="images" class="section">
                                    <img src="${tc.imageThumbnailUrl}" height="85px" alt="species thumbnail"/>
                                </div>
                                 -->
                                <div style="padding: 5px;">
                                	<c:set var="speciesName">
                                		<alatag:formatSciName name="${tc.scientificName}" rankId="${tc.rankId}"/>
                                		<c:if test="${not empty tc.commonName}"> &ndash; ${tc.commonName}</c:if>
                                	</c:set>
                                    <h4>${speciesName}</h4>
                                    Records: ${tc.count} (<a href="${pageContext.request.contextPath}/occurrences/search?q=user_id:${pageContext.request.remoteUser}&fq=taxon_name:${tc.scientificName}">view list of records</a>)
                                    <br/>
                                    <a href="${pageContext.request.contextPath}/share/sighting/${tc.guid}">Record another sighting</a>
                                    <br/>
                                    <a href="http://bie.ala.org.au/species/${tc.guid}">View species page</a>
                                </div>
                                <div style="clear: both; height:5px;"></div>
                            </c:forEach>
                        </div>
                    </div>
                    <div id="column-two">
                        <div class="section">
                            <div id="mapCanvas" style="height: 315px; width: 315px;"></div>
                        </div>
                    </div>
                </c:if>
                <c:if test="${not empty error}">
                    <div class="section">${error}</div>
                </c:if>
            </c:when>
            <c:otherwise><%-- User is NOT logged in --%>
                <jsp:include page="loginMsg.jsp"/>
            </c:otherwise>
        </c:choose>
    </body>
</html>
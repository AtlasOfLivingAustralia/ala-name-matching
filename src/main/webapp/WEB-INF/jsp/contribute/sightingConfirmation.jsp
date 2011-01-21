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
        <title>Share a sighting - Confirmation | Atlas of Living Australia</title>
    </head>
    <body>
        <div id="header">
            <div id="breadcrumb">
                <a href="${initParam.centralServer}">Home</a>
                <a href="${initParam.centralServer}/share">Share</a>
                Share a sighting
            </div>
            <h1>Share a sighting</h1>
        </div>
        
            <c:choose>
                <c:when test="${!empty pageContext.request.remoteUser}"><%-- User is logged in --%>
                    <c:if test="${not empty taxonConcept}">
                        <form name="sighting" id="sighting" action="" method="POST">
                            <div id="column-one">
                            	<c:if test="${not empty taxonConcept.imageThumbnailUrl}">
                                <div style="float: left; padding-right: 15px" id="images" class="section">
                                    <img src="${taxonConcept.imageThumbnailUrl}" height="85px" alt="species thumbnail"/>
                                </div>
                                </c:if>
                                <div style="margin-left: 115px" class="section">
                                    <h2><a href="http://bie.ala.org.au/species/${param.guid}"><alatag:formatSciName name="${taxonConcept.scientificName}" rankId="${taxonConcept.rankId}"/>
                                         <c:if test="${not empty taxonConcept.commonName}"> : ${taxonConcept.commonName}</c:if></a>
                                        <input type="hidden" name="guid" id="sightingGuid" value="${taxonConcept.guid}"/>
                                        <input type="hidden" name="scientificName" id="sightingSciName" value="${taxonConcept.scientificName}"/>
                                        <input type="hidden" name="commonName" id="sightingCommonName" value="${taxonConcept.commonName}"/>
                                        <input type="hidden" name="rank" id="sightingRank" value="${taxonConcept.rank}"/>
                                        <input type="hidden" name="kingdom" id="sightingKingdom" value="${taxonConcept.kingdom}"/>
                                        <input type="hidden" name="family" id="sightingFamily" value="${taxonConcept.family}"/>
                                    </h2>
                                    <fieldset id="sightingInfo">
                                        <p><label for="date">Date</label>
                                            <span>${param.date}</span>
                                            <input type="hidden" name="date" value="${param.date}"/>
                                        </p>
                                        <p><label for="time">Time</label>
                                            <span>${param.time}</span>
                                            <input type="hidden" name="time" value="${param.time}"/>
                                        </p>
                                        <p><label for="number">Number observed</label>
                                            <span>${param.number}</span>
                                            <input type="hidden" name="number" value="${param.number}"/>
                                        </p>
                                        <p><label for="verbatimLocality">Location</label>
                                            <span>${param.verbatimLocality}</span>
                                            <input type="hidden" name="verbatimLocality" value="${param.verbatimLocality}"/>
                                            <input type="hidden" name="locality" value="${param.locality}"/>
                                            <input type="hidden" name="stateProvince" value="${param.stateProvince}"/>
                                            <input type="hidden" name="country" value="${param.country}"/>
                                            <input type="hidden" name="countryCode" value="${param.countryCode}"/>
                                        </p>
                                            <p><label for="latitude">Latitude</label>
                                            <span>${param.latitude}</span>
                                            <input type="hidden" name="latitude" value="${param.latitude}"/>
                                        </p>
                                        <p><label for="longitude">Longitude</label>
                                            <span>${param.longitude}</span>
                                            <input type="hidden" name="longitude" value="${param.longitude}"/>
                                        </p>
                                        <p><label for="coordinateUncertainty">Accuracy</label>
                                            <span>${param.coordinateUncertainty} m</span>
                                            <input type="hidden" name="coordinateUncertainty" value="${param.coordinateUncertainty}"/>
                                        </p>
                                        <p><label for="recordedBy">Observer</label>
                                            <span>${param.recordedBy}</span>
                                            <input type="hidden" id="recordedBy" name="recordedBy" value="${param.recordedBy}"/>
                                            <input type="hidden" id="recordedById" name="recordedById"  value="${param.recordedById}"/>
                                        </p>
                                        <p><label for="notes">Notes</label>
                                            <span>${param.notes}</span>
                                            <input type="hidden" name="notes" value="${param.notes}"/>
                                        </p>
                                        <p id="confirmValues">Is this information correct? Click "Back" to edit or "Submit" to confirm.
                                            <br/>
                                            <span class="asterisk-container"><a href="${initParam.centralServer}/about/media-centre/terms-of-use#TOUContent" title="Terms of Use" style="text-decoration: none">Information submitted is subject to the <u>Atlas Terms of Use</u></a>.</span>
                                        </p>
                                        <p><label for=""><input type="submit" name="action" id="sightingBack" value="< Back"/></label>
                                            <input type="submit" name="action" id="sightingSubmit" value="Submit"/>
                                        </p>
                                    </fieldset>
                                </div>
                            </div>
                            <div id="column-two">
                                <div id="mapConfirm">
                                    <img src="http://maps.google.com/maps/api/staticmap?center=${param.latitude},${param.longitude}&zoom=15&size=315x315&maptype=hybrid&markers=color:red|color:red|label:|${param.latitude},${param.longitude}&sensor=false" alt="Google Map for sighting"/>
                                </div>
                            </div>
                            
                        </form>
                    </c:if>
                    <c:if test="${not empty error}">
                        <div id="column-one">
                            <div class="section">${error}</div>
                        </div>
                    </c:if>
                </c:when>
                <c:otherwise><%-- User is NOT logged in --%>
                    <jsp:include page="loginMsg.jsp"/>
                </c:otherwise>
            </c:choose>
    </body>
</html>
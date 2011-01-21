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
        <title>Share a sighting | Atlas of Living Australia</title>
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
            // Load Google maps via AJAX API
            google.load("maps", "3", {other_params:"sensor=false"});

            var geocoder, zoom, map, marker, circle;

            /**
             * Google geocode function
             */
            function geocodePosition(pos) {
                geocoder.geocode({
                    latLng: pos
                }, function(responses) {
                    if (responses && responses.length > 0) {
                        //console.log("geocoded position", responses[0]);
                        updateMarkerAddress(responses[0].formatted_address, responses[0]);
                    } else {
                        updateMarkerAddress('Cannot determine address at this location.');
                    }
                });
            }

            function updateMarkerStatus(str) {
                document.getElementById('markerStatus').innerHTML = str;
            }

            function updateMarkerPosition(latLng) {
                var rnd = 100000000;
                var lat = Math.round(latLng.lat() * rnd) / rnd; // round to 8 decimal places
                var lng = Math.round(latLng.lng() * rnd) / rnd; // round to 8 decimal places
                $('#markerLatitude').html(lat);
                $('#sightingLatitude').val(lat);
                $('#markerLongitude').html(lng);
                $('#sightingLongitude').val(lng);
            }

            function updateMarkerAddress(str, addressObj) {
                $('#markerAddress').html(str);
                $('#sightingLocation').val(str);
                // update form fields with location parts
                if (addressObj && addressObj.address_components) {
                    var addressComps = addressObj.address_components; // array
                    for (var i = 0; i < addressComps.length; i++) {
                        var name1 = addressComps[i].short_name;
                        var name2 = addressComps[i].long_name;
                        var type = addressComps[i].types[0];
                        // go through each avail option
                        if (type == 'country') {
                            $('input#countryCode').val(name1);
                            $('input#country').val(name2);
                        } else if (type == 'locality') {
                            $('input#locality').val(name2);
                        } else if (type == 'administrative_area_level_1') {
                             $('input#stateProvince').val(name2);
                        }
                    }
                }
            }

            function initialize() {
                var lat = $('input#sightingLatitude').val();
                var lng = $('input#sightingLongitude').val();
                var latLng = new google.maps.LatLng(lat, lng);
                map = new google.maps.Map(document.getElementById('mapCanvas'), {
                    zoom: zoom,
                    center: latLng,
                    mapTypeControl: true,
                    mapTypeControlOptions: {
                        style: google.maps.MapTypeControlStyle.DROPDOWN_MENU
                    },
                    navigationControl: true,
                    navigationControlOptions: {
                        style: google.maps.NavigationControlStyle.SMALL // DEFAULT
                    },
                    mapTypeId: google.maps.MapTypeId.HYBRID
                });
                marker = new google.maps.Marker({
                    position: latLng,
                    title: 'Sighting Location',
                    map: map,
                    draggable: true
                });

                // Add a Circle overlay to the map.
                var radius = parseInt($('#sightingCoordinateUncertainty').val());
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

                // Update current position info.
                updateMarkerPosition(latLng);
                geocodePosition(latLng);

                // Add dragging event listeners.
                google.maps.event.addListener(marker, 'dragstart', function() {
                    updateMarkerAddress('Dragging...');
                });

                google.maps.event.addListener(marker, 'drag', function() {
                    updateMarkerStatus('Dragging...');
                    updateMarkerPosition(marker.getPosition());
                });

                google.maps.event.addListener(marker, 'dragend', function() {
                    updateMarkerStatus('Drag ended');
                    geocodePosition(marker.getPosition());
                    map.panTo(marker.getPosition());
                });
            }

            /**
             * Try to get a lat/long using HTML5 geoloation API
             */
            function attemptGeolocation() {
                // HTML5 GeoLocation
                if (navigator && navigator.geolocation) {
                    //alert("trying to get coords with navigator.geolocation...");  
                    function getPostion(position) {  
                        //alert('coords: '+position.coords.latitude+','+position.coords.longitude);
                        $('#mapCanvas').html('');
                        $('#sightingLatitude').val(position.coords.latitude);
                        $('#sightingLongitude').val(position.coords.longitude);
                        zoom = 15;
                        //codeAddress(true);
                        initialize();
                    }
                    function positionDeclined() {
                        //alert('geolocation request declined or errored');
                        $('#mapCanvas').html('');
                        zoom = 12;
                        initialize();
                    }
                    // Add message to browser - FF needs this as it is not easy to see
                    $('#mapCanvas').html('Waiting for confirmation to use your current location (see browser message at top of window').css('color','red').css('font-size','14px');
                    navigator.geolocation.getCurrentPosition(getPostion, positionDeclined);
                } else if (google.loader && google.loader.ClientLocation) {
                    // Google AJAX API fallback GeoLocation
                    //alert("getting coords using google geolocation");
                    $('#latitude').val(google.loader.ClientLocation.latitude);
                    $('#longitude').val(google.loader.ClientLocation.longitude);
                    zoom = 12;
                   // codeAddress(true);
                   initialize();
                } else {
                    //alert("Client geolocation failed");
                    //codeAddress();
                    zoom = 12;
                    initialize();
                }
            }

            /**
             * Reverse geocode coordinates via Google Maps API
             */
            function codeAddress() {
                var address = $('input#address').val();

                if (geocoder && address) {
                    //geocoder.getLocations(address, addAddressToPage);
                    geocoder.geocode( { 'address': address, region: 'AU'}, function(results, status) {
                        if (status == google.maps.GeocoderStatus.OK) {
                            // geocode was successful
                            var lat = results[0].geometry.location.lat();
                            var lon = results[0].geometry.location.lng();
                            var locationStr = results[0].formatted_address;
                            //console.log("geocoded address", results[0]);
                            updateMarkerAddress(locationStr, results[0]);
                            //$('input#sightingLocation').val(locationStr); // hidden form element
                            //$('#markerAddress').val(locationStr); // visible span
                            $('input#sightingLatitude').val(lat); // hidden form element
                            $('#markerLatitude').val(lat); // visible span
                            $('input#sightingLongitude').val(lon); // hidden form element
                            $('#markerLongitude').val(lon); // visible span
                            initialize();
                        } else {
                            alert("Geocode was not successful for the following reason: " + status);
                        }
                    });
                }
            }

            /**
             * Geocode location via Google Maps API
             */
            function addAddressToPage(response) {
                //map.clearOverlays();
                if (!response || response.Status.code != 200) {
                    alert("Sorry, we were unable to geocode that address");
                } else {
                    var location = response.Placemark[0];
                    var lat = location.Point.coordinates[1]
                    var lon = location.Point.coordinates[0];
                    var locationStr = response.Placemark[0].address;
                    //if ($('input#location').val() == "") {
                    //    $('input#address').val(locationStr);
                    //}
                    $('input#sightingLocation').val(locationStr); // hidden form element
                    $('#markerAddress').val(locationStr); // visible span
                    $('input#sightingLatitude').val(lat); // hidden form element
                    $('#markerLatitude').val(lat); // visible span
                    $('input#sightingLongitude').val(lon); // hidden form element
                    $('#markerLongitude').val(lon); // visible span
                    loadMap();
                }
            }

            function updateTitleAttr(rad) {
                $('#sightingCoordinateUncertainty').attr('title', 'A measure of the accuracy of the location coordinates. E.g. +/- '+rad+'m');
                $('#sightingCoordinateUncertainty').tooltip({track: true, extraClass: "toolTip" })
            }

            /**
             * Document onLoad event using JQuery
             */
            $(document).ready(function() {
                // geocoding
                geocoder = new google.maps.Geocoder();
                //geocoder.setBaseCountryCode("AU"); // v2 API only
                attemptGeolocation();

                // insert server-side geocoded coords if present
                var lat = "${latitude}";
                var lng = "${longitude}";
                var inputLatLng = "";

                if (lat && lng) {
                    $('input#sightingLatitude').val(lat); // hidden form element
                    $('#markerLatitude').val(lat); // visible span
                    $('input#sightingLongitude').val(lng); // hidden form element
                    $('#markerLongitude').val(lng); // visible span
                }
                // populate date if a new entry
                var sightingDate = "${param.date}";
                if (sightingDate.length == 0) {
                    $('#sightingDate').datePicker({startDate:'01/01/1996'}).val(new Date().asString()).trigger('change');
                }

                // populate time if a new entry
                var sightingTime = "${param.time}";
                if (sightingTime.length == 0) {
                    var date = new Date();
                    var hour = date.getHours();
                    if (hour < 10) hour = "0" + hour;
                    var minutes = date.getMinutes();
                    if (minutes < 10) minutes = "0" + minutes;
                    $('#sightingTime').val(hour+":"+minutes);
                }

                $("#sightingTime").timePicker({
                    //startTime: new Date(),
                    //leftOffset: 85,
                    step: 15
                });
                
                // trigger Google geolocation search on search button
                $('#locationSearch').click(function (e) {
                     e.preventDefault(); // ignore the href text - used for data
                     codeAddress();
                });

                // trigger submit on Next button click
                $('#sightingSubmit').click(function (e) {
                     e.preventDefault(); // ignore the href text - used for data
                     $('form#sighting').submit();
                });

                // catch the enter key and trigger location search
                $(window).keydown(function(e){
                     if (e.which == 13) {
                         //alert('Enter key pressed');
                         e.preventDefault(); // ignore the href text - used for data
                         codeAddress();
                         return false;
                     }
                     
                });

               $('#sightingCoordinateUncertainty').change(function(e){
                   var rad = parseInt($(this).val());
                   circle.setRadius(rad);
                   updateTitleAttr(rad);
               })

                // set observer field based on user login
                var recordedBy = "${param.recordedBy}";
                if (!recordedBy) {
                    $('input#recordedBy').val("<ala:userName/>");
                }

                // set height of inner div
                var h = $('#locationBlock').height();
                $('#location').height(h-20); // $('#locationBlock').height
                // tooltip for location input help
                $('.locationInput').attr('title', 'Use the map controls (right) to set the location');
                //$('#sightingCoordinateUncertainty').attr('title', 'A measure of the accuracy of the location coordinates. E.g. +/- '+$('#sightingCoordinateUncertainty').val()+'m');
                updateTitleAttr($('#sightingCoordinateUncertainty').val());
                $('#sightingAddress label').attr('title', 'You can search for a street address, place of interest, postcode or GPS coordinates (lat, lon)');
                $('.locationInput, #sightingAddress label').tooltip({track: true, extraClass: "toolTip" });

                // make some input fields appears to be not editable unless clicked on
//                $('#sightingLocation, #sightingLatitude, #sightingLongitude').focus(function(e) {
//                    $(this).removeClass('transparentBorder');
//                }).blur(function(e) {
//                    $(this).addClass('transparentBorder');
//                });
            });
            </c:if>
        </script>
    </head>
    <body>
        <div id="header">
            <div id="breadcrumb">
                <a href="${initParam.centralServer}">Home</a>
                <a href="${initParam.centralServer}/share">Share</a>
                Share a Sighting
            </div>
            <h1>Share a Sighting</h1>
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
                                <h2><a href="http://bie.ala.org.au/species/${taxonConcept.guid}"><alatag:formatSciName name="${taxonConcept.scientificName}" rankId="${taxonConcept.rankId}"/>
                                    <c:if test="${not empty taxonConcept.commonName}"> : ${taxonConcept.commonName}</c:if>
                                    </a>
                                    <input type="hidden" name="guid" id="sightingGuid" value="${param.guid}"/>
                                </h2>
                                <fieldset id="sightingInfo">
                                    <p><label for="date">Date</label>
                                        <input type="text" id="sightingDate" name="date" size="20" value="${param.date}"/>
                                        <span class="hint">(DD/MM/YYYY)</span>
                                    </p>
                                    <p><label for="time">Time</label>
                                        <input type="text" id="sightingTime" name="time" size="20" value="${param.time}"/>
                                        <span class="hint">(HH:MM)</span>
                                    </p>
                                    <p><label for="number">Number observed</label>
                                        <select id="sightingNumber" name="number">
                                            <c:forEach var="option" begin="1" end="100" step="1">
                                                <option<c:if test="${option == param.number}"> selected</c:if>>${option}</option>
                                            </c:forEach>
                                        </select>
                                    </p>
                                    <p><label for="verbatimLocality">Location</label>
                                        <!-- <span id="markerAddress" class="locationInput"></span> -->
                                        <input type="text" size="50" id="sightingLocation" class="transparentBorderX" name="verbatimLocality" value="${param.verbatimLocality}"/>
                                        <input type="hidden" id="locality" name="locality" value="${param.locality}"/>
                                        <input type="hidden" id="stateProvince" name="stateProvince" value="${param.stateProvince}"/>
                                        <input type="hidden" id="country" name="country" value="${param.country}"/>
                                        <input type="hidden" id="countryCode" name="countryCode" value="${param.countryCode}"/>
                                    </p>
                                        <p><label for="latitude">Latitude</label>
                                        <span id="markerLatitude" class="locationInput" style="display: none"></span>
                                        <input type="text" id="sightingLatitude" class="transparentBorderX" name="latitude" value="${param.latitude}"/>
                                    </p>
                                    <p><label for="longitude">Longitude</label>
                                        <span id="markerLongitude" class="locationInput" style="display: none"></span>
                                        <input type="text" id="sightingLongitude" class="transparentBorderX" name="longitude" value="${param.longitude}"/>
                                    </p>
                                    <p><label for="coordinateUncertainty">Coordinate Uncertainty</label>
                                        <select id="sightingCoordinateUncertainty" name="coordinateUncertainty">
                                            <c:set var="selected">
                                                <c:choose>
                                                    <c:when test="${not empty param.coordinateUncertainty}">
                                                        ${param.coordinateUncertainty}
                                                    </c:when>
                                                    <c:otherwise>
                                                        20
                                                    </c:otherwise>
                                                </c:choose>
                                            </c:set>
                                            <c:set var="values">1,5,10,20,50,100,500,1000</c:set>
                                            <c:forEach var="option" items="${fn:split(values,',')}">
                                                <option<c:if test="${option == selected}"> selected</c:if>>${option}</option>
                                            </c:forEach>
                                        </select>
                                        <span class="hint">metres</span>
                                    </p>
                                    <p><label for="recordedBy">Observer</label>
                                        <input type="text" id="recordedBy" name="recordedBy" size="30" value="${param.recordedBy}"/>
                                        <input type="hidden" id="recordedById" name="recordedById"  value="${pageContext.request.remoteUser}"/>
                                    </p>
                                    <p><label for="notes" style="vertical-align: top">Notes</label>
                                        <textarea id="sightingNotes" name="notes" cols="30" rows="5">${param.notes}</textarea>
                                        <span id="notes" class="hint">(E.g. weather conditions, observed behaviour, <i>etc.</i>)</span>
                                    </p>
                                    <p><label for="action"></label>
                                        <input type="button" id="sightingSubmit" value="Next >"/>
                                        <input type="hidden" name="action"  value="Next >"/>
                                    </p>
                                </fieldset>
                            </div>
                        </div>
                        <div id="column-two">
                            <div class="section">
                                <div id="sightingAddress">
                                    <label for="address">Enter the location or address of sighting: </label>
                                    <input name="address" id="address" size="38" value="${address}"/>
                                    <input id="locationSearch" type="button" value="Search"/>
                                </div>
                                <div id="mapCanvas"></div>
                                <div style="font-size: 90%; margin-top: 5px;"><b>Hints:</b> click and drag
                                    the marker pin to fine-tune the location coordinates. Using a GPS?
                                    Then you enter the coordinates as &quot;latitude, longitude&quot; in the search bar above the map.</div>
                                <div style="display: none">
                                    <div id="markerAddress"></div>
                                    <div id="markerStatus"></div>
                                    <div id="info"></div>
                                </div>
                            </div>
                        </div>
                    </form>
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
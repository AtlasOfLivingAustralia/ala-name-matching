    <%--
        Document   : show
        Created on : Apr 21, 2010, 9:36:39 AM
        Author     : "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
    --%>
    <%@ page contentType="text/html" pageEncoding="UTF-8" %>
    <%@ include file="/common/taglibs.jsp" %>
    <!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">
    <c:set var="googleKey" scope="request"><ala:propertyLoader bundle="biocache" property="googleKey"/></c:set>
    <html>
        <head>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
            <title>Explore Your Area | Atlas of Living Australia</title>
            <script type="text/javascript" src="http://www.google.com/jsapi?key=${googleKey}"></script>
            <script type="text/javascript" src="${pageContext.request.contextPath}/static/js/jquery-ui-1.8.4.custom.min.js"></script>
            <link type="text/css" rel="stylesheet" href="${initParam.centralServer}/wp-content/themes/ala/css/biocache-theme/jquery-ui-1.8.custom.css" charset="utf-8">
            <script type="text/javascript" src="${pageContext.request.contextPath}/static/js/jquery.ba-hashchange.min.js"></script>
            <script type="text/javascript" src="${pageContext.request.contextPath}/static/js/explore/yourAreaMap.js"></script>
            <script type="text/javascript" src="${pageContext.request.contextPath}/static/js/jquery.qtip-1.0.0.min.js"></script>
            <script type="text/javascript">
                // Global variables for Openlayers
                var contextPath = "${pageContext.request.contextPath}";
                var zoom = ${zoom};
                var radius = ${radius};
                var speciesPageUrl = "${speciesPageUrl}";

                //make the taxa and rank global variable so that they can be used in the download
                var taxa = [];
                taxa[0] ="*";
                var rank ="*";

                var zoomForRadius = {
                    1000: 14,
                    5000: 12,
                    10000: 11
                };

                var radiusForZoom = {
                    11: 10,
                    12: 5,
                    14: 1
                };
                
                // Load Google maps via AJAX API
                google.load("maps", "3", {other_params:"sensor=false"});

                // all mapping JS code moved to /js/explore/yourAreaMap.js


                /**
                 * Document onLoad event using JQuery
                 */
                $(document).ready(function() {
                    // re-call (skin) JS function to tweak with search input
                    greyInitialValues();
                    // instantiate GClientGeocoder
                    //geocoder = new GClientGeocoder(); //new google.maps.Geocoder();
                    geocoder = new google.maps.Geocoder();

                    // onMouseOver event on Group items
                    $('#taxa-level-0 tbody tr').hover(
                        function() {
                            $(this).addClass('hoverRow');
                        },
                        function() {
                            $(this).removeClass('hoverRow');
                        }
                    );
                    // add title attribute for tooltip
                    $('#taxa-level-0 tbody tr').attr('title', 'display on map');
                    // catch the link on the taxon groups table
                    $('#taxa-level-0 tbody tr').click(function(e) {
                        e.preventDefault(); // ignore the href text - used for data
                        taxa = $(this).find('a.taxonBrowse').attr('href'); // $(this+' a.taxonBrowse').attr('href');
                        //console.log("taxon: "+taxon);
                        rank = $(this).find('a.taxonBrowse').attr('id');
                        //taxa = []; // array of taxa
                        //taxa = (taxon.indexOf("|") > 0) ? taxon.split("|") : taxon;
                        $('#taxa-level-0 tr').removeClass("activeRow"); 
                        $(this).addClass("activeRow"); 
                        $('#taxa-level-1 tbody tr').addClass("activeRow"); 
                        // load records layer on map
                        //console.log('about to run: loadRecordsLayer()');
                        if (map) loadRecordsLayer();
                        // AJAX...
                        var uri = "${pageContext.request.contextPath}/explore/species.json";
                        var params = {
                            latitude: $('#latitude').val(),
                            longitude: $('#longitude').val(),
                            radius: $('#radius').val(),
                            taxa: taxa,
                            rank: rank
                        };
                        //var params = "?latitude=${latitude}&longitude=${longitude}&radius=${radius}&taxa="+taxa+"&rank="+rank;
                        $('#taxaDiv').html('[loading...]');
                        $.getJSON(uri, params, function(data, status) {
                            // process JSON data from request
                            if (status == "success") processSpeciesJsonData(data);
                        });
                    });

                    // By default action on page load - show the all species group (simulate a click)
                    //$('#taxa-level-0 tbody td:first').click();

                    // register click event on "Search" button"
                    $('input#locationSearch').click(
                        function(e) {
                            e.preventDefault(); // ignore the href text - used for data
                            codeAddress();
                        }
                    );

                    // Register onChange event on radius drop-down - will re-submit form
                    $('select#radius').change(
                        function(e) {
                            radius = parseInt($(this).val());
                            var radiusInMetres = radius * 1000;
                            circle.setRadius(radiusInMetres);
                            zoom = zoomForRadius[radiusInMetres];
                            map.setZoom((zoom)?zoom:12);
                            updateMarkerPosition(marker.getPosition()); // so bookmarks is updated
                            //loadRecordsLayer();
                            LoadTaxaGroupCounts();
                        }
                    );

                    // Dynamically set height of #taxaDiv (to match containing div height)
                    var tableHeight = $('#taxa-level-0').height();
                    //$('#rightList table').height(tableHeight+2);
                    $('.tableContainer').height(tableHeight+8);
                    var tbodyHeight = $('#taxa-level-0 tbody').height();
                    $('#rightList tbody').height(tbodyHeight);
                    
                    // register click event on download button
                    $("button#download").click(
                        function(e){
                            e.preventDefault();
                            // trigger dialog box
                            $("#dialog-confirm").dialog('open');
                        }
                    );

                    // Configure Dialog box for Download button (JQuery UI)
                    $("#dialog-confirm").dialog({
                        resizable: false,
                        modal: true,
                        autoOpen: false,
                        buttons: {
                            'Download File': function() {
                                var downloadUrl ="${pageContext.request.contextPath}/explore/download?latitude="+$('#latitude').val()+"&longitude="+$('#longitude').val()+"&radius="+$('#radius').val()+"&taxa=*&rank=*";
                                window.location.replace(downloadUrl);
                                $(this).dialog('close');
                            },
                            Cancel: function() {
                                $(this).dialog('close');
                            }
                        }
                    });

                    // trigger ajax to load counts for taxa groups (left column)
                    LoadTaxaGroupCounts();

                    // Handle back button and saved URLs
                    // hash coding: #lat|lng|zoom
                    var url = escape(window.location.hash.replace( /^#/, '')); // escape used to prevent injection attacks

                    if (url) {
                        var hashParts = url.split("%7C"); // note escaped version of |
                        //console.log("url hash = ", url, coords);
                        if (hashParts.length == 3) {
                            zoom = parseInt(hashParts[2]); // set global var
                            radius = radiusForZoom[zoom];  // set global var
                            $('select#radius').val(radius); // update drop-down widget
                            updateMarkerPosition(new google.maps.LatLng(hashParts[0], hashParts[1]));
                            LoadTaxaGroupCounts();
                            loadMap();
                        } else {
                            attemptGeolocation();
                        }
                    } else {
                        //console.log("url not set, geolocating...");
                        attemptGeolocation();
                    }

                    // catch the link for "View all records"
                    $('#viewAllRecords').click(function(e) {
                        e.preventDefault();
                        var params = "q=taxon_name:*|"+$('#latitude').val()+"|"+$('#longitude').val()+"|"+$('#radius').val();
                        document.location.href = contextPath +'/occurrences/searchByArea?' + params;
                    });

                    // Tooltip for matched location
                    $('#addressHelp').qtip({
                        content: {
                            url: '${pageContext.request.contextPath}/proxy/wordpress',
                            data: { 'page_id': 16658, 'content-only': 1},
                            method: 'get',
                            title: {
                               text: 'About the matched address',
                               button: 'Close'
                            },
                            text: '<img src="${pageContext.request.contextPath}/static/images/loading.gif" alt="" class="no-rounding"/>'
                        },
                        position: {
                            corner: {
                                target: 'bottomRight',
                                tooltip: 'topLeft'
                            }
                        },
                        style: {
                            width: 450,
                            padding: 8,
                            background: '#f0f0f0',
                            color: 'black',
                            textAlign: 'left',
                            border: {
                                width: 4,
                                radius: 5,
                                color: '#E66542'// '#E66542' '#DD3102'
                            },
                            tip: 'topLeft',
                            name: 'light' // Inherit the rest of the attributes from the preset light style
                        },
                        show: { effect: { type: 'slide', length: 300 } },
                        hide: { fixed: true, effect: { type: 'slide', length: 300 }, when: { event: 'unfocus' }}
                    }).bind('click', function(event){ event.preventDefault(); return false;});
                }); // End: $(document).ready() function
            </script>
        </head>
        <body>
            <div id="header">
                <div id="breadcrumb">
                    <a href="${initParam.centralServer}">Home</a>
                    <a href="${initParam.centralServer}/explore">Explore</a>
                    Your Area
                </div>
                <h1>Explore Your Area</h1>
            </div>
            <div id="column-one" class="full-width">
                <div class="section">
                    <div>
                        <div id="mapOuter" style="width: 400px; height: 450px; float:right;">
                            <div id="mapCanvas" style="width: 400px; height: 430px;"></div>
                            <div style="font-size:11px;width:400px;color: black;" class="show-80">
                                <table id="cellCountsLegend">
                                    <tr>
                                        <td style="background-color:#333; color:white; text-align:right;">Records:&nbsp;</td>
                                        <td style="width:60px;background-color:#ffff00;">1&ndash;9</td>
                                        <td style="width:60px;background-color:#ffcc00;">10&ndash;49</td>
                                        <td style="width:60px;background-color:#ff9900;">50&ndash;99</td>
                                        <td style="width:60px;background-color:#ff6600;">100&ndash;249</td>
                                        <td style="width:60px;background-color:#ff3300;">250&ndash;499</td>
                                        <td style="width:60px;background-color:#cc0000;">500+</td>
                                    </tr>
                                </table>
                            </div>
                        </div>
                        <div id="left-col">
                            <form name="searchForm" id="searchForm" action="" method="GET">
                                <div id="locationInput">
                                    <h2>Enter your location or address</h2>
                                    <div id="searchHints">E.g. a street address, place name, postcode or GPS coordinates (as lat, long)</div>
                                    <input name="address" id="address" size="50" value="${address}"/>
                                    <input id="locationSearch" type="submit" value="Search"/>
                                    <input type="hidden" name="latitude" id="latitude" value="${latitude}"/>
                                    <input type="hidden" name="longitude" id="longitude" value="${longitude}"/>
                                    <input type="hidden" name="location" id="location" value="${location}"/>
                                </div>
                                <div id="locationInfo">
                                    <c:if test="${true || not empty location}">
                                        <p>
                                            Showing records for: <span id="markerAddress">${location}</span>
                                            &nbsp;&nbsp;&nbsp;
                                            <a href="#" id="addressHelp" style="text-decoration: none"><span class="help-container">&nbsp;</span></a>
                                        </p>
                                    </c:if>
                                    <table id="locationOptions">
                                        <tbody>
                                            <tr>
                                                <td>Display records in a
                                                    <select id="radius" name="radius">
                                                        <option value="1" <c:if test="${radius eq '1.0'}">selected</c:if>>1</option>
                                                        <option value="5" <c:if test="${radius eq '5.0'}">selected</c:if>>5</option>
                                                        <option value="10" <c:if test="${radius eq '10.0'}">selected</c:if>>10</option>
                                                    </select> km radius <!--<input type="submit" value="Reload"/>--></td>
                                                <td><img src="${pageContext.request.contextPath}/static/css/images/database_go.png" alt="search list icon" style="margin-bottom:-3px;" class="no-rounding"><a href="#" id="viewAllRecords">View all occurrence records</a></td>
                                                <td><button id="download" title="Download a list of all species (tab-delimited file)">Download</button></td>
                                            </tr>
                                        </tbody>
                                    </table>
                                    <div id="dialog-confirm" title="Continue with download?" style="display: none">
                                        <p><span class="ui-icon ui-icon-alert" style="float:left; margin:0 7px 20px 0;"></span>You are about to
                                            download a list of species found within a <span id="rad"></span> km radius of <code>${location}</code>.<br/>
                                            Format: tab-delimited text file (called data.xls)</p>
                                    </div>
                                </div>
                                <div id="taxaBox">
                                    <div id="rightList" class="tableContainer">
                                        <table>
                                            <thead class="fixedHeader">
                                                <tr>
                                                    <th>&nbsp;</th>
                                                    <th>Species</th>
                                                    <th>Records</th>
                                                </tr>
                                            </thead>
                                            <tbody class="scrollContent">
                                            </tbody>
                                        </table>
                                    </div>
                                    <div id="leftList">
                                        <table id="taxa-level-0">
                                            <thead>
                                                <tr>
                                                    <th>Group</th>
                                                    <th>Count</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                <c:forEach var="tg" items="${taxaGroups}">
                                                    <c:set var="indent">
                                                        <c:choose>
                                                            <c:when test="${tg.parentGroup == null}"></c:when>
                                                            <c:when test="${tg.parentGroup == 'ALL_LIFE'}">indent</c:when>
                                                            <c:otherwise>indent2</c:otherwise>
                                                        </c:choose>
                                                    </c:set>
                                                    <tr>
                                                        <td class="${indent}"><a href="${fn:join(tg.taxa, "|")}" id="${tg.rank}" title="${tg.label}" class="taxonBrowse">${tg.label}</a>
                                                        <td></td>
                                                    </tr>
                                                </c:forEach>
                                            </tbody>
                                        </table>
                                    </div>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
        </body>
    </html>

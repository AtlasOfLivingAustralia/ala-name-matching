<%--
    Document   : list
    Created on : Apr 21, 2010, 9:36:39 AM
    Author     : "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
--%>
<%@ page contentType="text/html" pageEncoding="UTF-8" %>
<%@ include file="/common/taglibs.jsp" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="pageName" content="species"/>
        <script type="text/javascript" src="${pageContext.request.contextPath}/static/js/jquery.query.js"></script>
        <script type="text/javascript" src="${pageContext.request.contextPath}/static/js/jquery-ui-1.8.custom.min.js"></script>
        <script type="text/javascript" src="${pageContext.request.contextPath}/static/js/jquery.simplemodal.js"></script>
        <script type="text/javascript" src="${pageContext.request.contextPath}/static/js/jquery.oneshowhide.js"></script>
        <script type="text/javascript" src="${pageContext.request.contextPath}/static/js/openlayers/OpenLayers.js"></script>
        <link type="text/css" rel="stylesheet" href="${initParam.centralServer}/wp-content/themes/ala/css/biocache-theme/jquery-ui-1.8.custom.css" charset="utf-8">
        <link type="text/css" rel="stylesheet" href="${initParam.centralServer}/wp-content/themes/ala/css/basic.css" charset="utf-8">
        <!--[if lt IE 7]>
        <link type='text/css' href='${initParam.centralServer}/wp-content/themes/ala/css/basic_ie.css' rel='stylesheet' media='screen' />
        <![endif]-->
        <title><c:choose><c:when test="${not empty entityQuery}">${entityQuery}</c:when><c:otherwise>${queryJsEscaped}</c:otherwise></c:choose> | Occurrence Search | Atlas of Living Australia</title>
        <link rel="stylesheet" href="${initParam.centralServer}/wp-content/themes/ala/css/bie.css" type="text/css" media="screen" charset="utf-8"/>
        <script type="text/javascript">
            /* Openlayers vars */
            var lon = 133;
            var lat = -27;
            var zoom = 4;
            var map, vectorLayer, selectControl, selectFeature, loadingControl;

            /* Openlayers map */
            function loadMap() {
                map = new OpenLayers.Map('pointsMap',{controls: []});
                //add controls
                map.addControl(new OpenLayers.Control.Attribution());
                var panel = new OpenLayers.Control.Panel();
                // control to display the "loading" graphic
                loadingControl = new OpenLayers.Control.Button({displayClass: "loadingVector", trigger: dummyFunction});
                panel.addControls([loadingControl]);
                map.addControl(panel);
                // hide the loading graphic
                loadingControl.moveTo(new OpenLayers.Pixel(-1000, -1000));
                baseLayer = new OpenLayers.Layer.WMS( "OpenLayers WMS",
                "http://labs.metacarta.com/wms/vmap0",
                {layers: 'basic'} );
                map.addLayer(baseLayer);
                map.setCenter(new OpenLayers.LonLat(lon, lat), zoom);
                // reload vector layer on zoom event
                map.events.register('zoomend', map, function (e) {
                    loadVectorLayer();
                });

                loadVectorLayer(); // load data via GeoJSON

            }

            function dummyFunction() {
                alert('button');
            }

            /**
             * Load occurrence points/clusters via GeoJSON request
             */
            function loadVectorLayer() {
                if (vectorLayer != null) {
                    vectorLayer.destroy();
                }

                var myStyles = new OpenLayers.StyleMap({
                    "default": new OpenLayers.Style({
                        fillColor: "${'${color}'}",//"#ffcc66",
                        //fillColor: "#D75A25",
                        //strokeColor: "${'${color}'}",
                        fillOpacity: 0.7,
                        graphicZIndex: 1,
                        strokeWidth: 0
                    })
                });
                
                var legend = '<table id="cellCountsLegend" class="show-70"><tr><td style="background-color:#333; color:white; text-align:right;">Record counts:&nbsp;</td><td style="width:60px;background-color:#ffff00;">1&ndash;9</td><td style="width:60px;background-color:#ffcc00;">10&ndash;49</td><td style="width:60px;background-color:#ff9900;">50&ndash;99</td><td style="width:60px;background-color:#ff6600;">100&ndash;249</td><td style="width:60px;background-color:#ff3300;">250&ndash;499</td><td style="width:60px;background-color:#cc0000;">500+</td></tr></table>';

                vectorLayer  = new OpenLayers.Layer.Vector("Occurrences", {
                    styleMap: myStyles,
                    attribution: legend,
                    //strategies: [new OpenLayers.Strategy.BBOX()], // new OpenLayers.Strategy.Fixed(),new OpenLayers.Strategy.BBOX()
                    protocol: new OpenLayers.Protocol.HTTP({
                        format: new OpenLayers.Format.GeoJSON()
                    })
                });

                map.addLayer(vectorLayer);
                
                // trigger lading of GeoJSON
                reloadData();
            }

            /* load features via ajax call */
            function reloadData() {
                // show loading graphic
                loadingControl.moveTo(new OpenLayers.Pixel(270, 220));
                // url vars
                var geoJsonUrl = "${pageContext.request.contextPath}/geojson/cells"; //+"&zoom=4&callback=?";
                var zoomLevel = map.getZoom();
                var paramString = "q=${query}&zoom="+zoomLevel+"&type=${type}&fq=${fn:join(facetQuery, '&fq=')}";
                // JQuery GET
                $.get(geoJsonUrl, paramString, dataRequestHandler);
            }

            /* handler for loading features */
            function dataRequestHandler(data) {
                // clear existing
                vectorLayer.destroyFeatures();
                // parse returned json
                var features = new OpenLayers.Format.GeoJSON().read(data);
                // add features to map
                vectorLayer.addFeatures(features);
                // hide the "loading" graphic
                loadingControl.moveTo(new OpenLayers.Pixel(-1000, -1000));
                
                // add select control
                if (selectControl != null) {
                    map.removeControl(selectControl);
                    selectControl.destroy();
                    selectControl = null;
                }

                selectControl = new OpenLayers.Control.SelectFeature(vectorLayer, {
                    //hover: true,
                    onSelect: onFeatureSelect,
                    onUnselect: onFeatureUnselect
                });

                map.addControl(selectControl);
                selectControl.activate();
            }


            function onPopupClose(evt) {
                selectControl.unselect(selectedFeature);
            }

            function onFeatureSelect(feature) {
                selectedFeature = feature;
                popup = new OpenLayers.Popup.FramedCloud("cellPopup", feature.geometry.getBounds().getCenterLonLat(),
                    null, "<div style='font-size:12px; color: #222;'>Number of records: " + feature.attributes.count, // +
                    //"<br /><a href=''>View records in this area</a> " + feature.geometry.getBounds().toBBOX() + "</div>",
                    null, true, onPopupClose);
                feature.popup = popup;
                map.addPopup(popup);
            }

            function onFeatureUnselect(feature) {
                map.removePopup(feature.popup);
                feature.popup.destroy();
                feature.popup = null;
            }

            function destroyMap() {
                if (map != null) {
                    map.destroy();
                    $("#pointsMap").html('');
                }
            }

            function checkRegexp(o,regexp,n) {

                if ( o.val().length>0 &&!( regexp.test( o.val() ) ) ) {
                    o.addClass('ui-state-error');
                    alert(n);
                    return false;
                } else {
                    return true;
                }
            }

            // Jquery Document.onLoad equivalent
            $(document).ready(function() {
                var facetLinksSize = $("ul#subnavlist li").size();

                if (facetLinksSize == 0) {
                    // Hide an empty facet link list
                    $("#facetBar > h4").hide();
                    $("#facetBar #navlist").hide();
                }
                /* Accordion widget */
                var icons = {
                    header: "ui-icon-circle-arrow-e",
                    headerSelected: "ui-icon-circle-arrow-s"
                };

//                $("#accordion").accordion({
//                    icons: icons,
//                    collapsible: true,
//                    autoHeight: false
//                });

                $("#toggle").button().toggle(function() {
                    $("#accordion").accordion("option", "icons", false);
                }, function() {
                    $("#accordion").accordion("option", "icons", icons);
                });

                $("select#sort").change(function() {
                    var val = $("option:selected", this).val();
                    reloadWithParam('sort',val);
                });
                $("select#dir").change(function() {
                    var val = $("option:selected", this).val();
                    reloadWithParam('dir',val);
                });

                //$("#searchButtons > button").button();
                $("#searchButtons > button#download").click(function() {
                    $("#dialog-confirm").dialog('open');
                });

                // Configure Dialog box for Download button (JQuery UI)
                $("#dialog-confirm").dialog({
                    resizable: true,
                    modal: true,
                    autoOpen: false,
                    width: 375,
                    buttons: {
                        'Download File': function() {
                            var email = $("#email");
                            email.removeClass('ui-state-error');
                            //Only allow empty or valid email addresses
                            // From jquery.validate.js (by joern), contributed by Scott Gonzalez: http://projects.scottsplayground.com/email_address_validation/
                            if(checkRegexp(email, /^((([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+(\.([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+)*)|((\x22)((((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(([\x01-\x08\x0b\x0c\x0e-\x1f\x7f]|\x21|[\x23-\x5b]|[\x5d-\x7e]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(\\([\x01-\x09\x0b\x0c\x0d-\x7f]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]))))*(((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(\x22)))@((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.?$/i,"Invalid email format. (eg. name@host.com)")){
                                var lat = "${latitude}";
                                var lon = "${longitude}";
                                var rad = "${radius}";
                                var reason = $("#reason").val();
                                if(typeof reason == "undefined")
                                    reason="";
                                var downloadUrl = "${pageContext.request.contextPath}/occurrences/download?q=${query}&fq=${fn:join(facetQuery, '&fq=')}&type=${type}&email="+email.val()+"&reason="+encodeURIComponent(reason)+"&file="+$("#filename").val();
                                if (lat && lon && rad) {
                                    rad = parseInt(rad);
                                    downloadUrl = downloadUrl + "&lat=" + lat + "&lon=" + lon +"&rad=" + rad;
                                }
                                window.location.replace(downloadUrl);
                                $(this).dialog('close');
                            }
                        },
                        Cancel: function() {
                            $(this).dialog('close');
                            $("#email").removeClass('ui-state-error');
                        }
                    }
                });

                $('button#showMap').click(function (e) {
                    //window.location.replace("#searchResults");
                    $("#pointsMap").show();
                    loadMap();
                    $('#pointsMap').modal();

                });

                // more/fewer search option links
                $("#refineMore a").click(function(e) {
                    e.preventDefault();
                    $("#accordion").slideDown();
                    $("#refineLess").show('slow');
                    $("#refineMore").hide('slow');
                });
                $("#refineLess a").click(function(e) {
                    e.preventDefault();
                    $("#accordion").slideUp();
                    $("#refineLess").hide('slow');
                    $("#refineMore").show('slow');
                });

                // Remove hanging border inside search results
                $("p.occurrenceResultRow :last-child").css('border-right','none');

                // listeners for sort widgets
                $("select#sort").change(function() {
                    var val = $("option:selected", this).val();
                    reloadWithParam('sort',val);
                });
                $("select#dir").change(function() {
                    var val = $("option:selected", this).val();
                    reloadWithParam('dir',val);
                });
                $("select#per-page").change(function() {
                    var val = $("option:selected", this).val();
                    reloadWithParam('pageSize',val);
                });

                // add show/hide links to facets
                $('#subnavlist ul').oneShowHide({
                    numShown: 4,
                    showText : '+ show More',
                    hideText : '- show Less',
                    className: 'showHide'
                });
                
            });

            /**
             * Catch sort drop-down and build GET URL manually
             */
            function reloadWithParam1(paramName, paramValue) {
                var paramList = [];
                var q = $.getQueryParam('q'); //$.query.get('q')[0];
                var fqList = $.getQueryParam('fq'); //$.query.get('fq');
                var sort = $.getQueryParam('sort');
                var dir = $.getQueryParam('dir');
                // add query param
                if (q != null) {
                    paramList.push("q=" + q);
                }
                // add filter query param
                if (fqList != null) {
                    paramList.push("fq=" + fqList.join("&fq="));
                }
                // add sort param if already set
                if (paramName != 'sort' && sort != null) {
                    paramList.push('sort' + "=" + sort);
                }
                // add the triggered param
                if (paramName != null && paramValue != null) {
                    if (paramName == 'sort') {
                        paramList.push(paramName + "=" +paramValue);
                    } else if (paramName == 'dir' && !(sort == null || sort == 'score')) {
                        paramList.push(paramName + "=" +paramValue);
                    }
                }

                window.location.replace(window.location.pathname + '?' + paramList.join('&'));
            }

            /**
             * Catch sort drop-down and build GET URL manually
             */
            function reloadWithParam(paramName, paramValue) {
                var paramList = [];
                var q = $.getQueryParam('q'); //$.query.get('q')[0];
                var fqList = $.getQueryParam('fq'); //$.query.get('fq');
                var sort = $.getQueryParam('sort');
                var dir = $.getQueryParam('dir');
                // add query param
                if (q != null) {
                    paramList.push("q=" + q);
                }
                // add filter query param
                if (fqList != null) {
                    paramList.push("fq=" + fqList.join("&fq="));
                }
                // add sort param if already set
                if (paramName != 'sort' && sort != null) {
                    paramList.push('sort' + "=" + sort);
                }

                if (paramName != null && paramValue != null) {
                    paramList.push(paramName + "=" +paramValue);
                }

                //alert("params = "+paramList.join("&"));
                //alert("url = "+window.location.pathname);
                window.location.replace(window.location.pathname + '?' + paramList.join('&'));
            }


            function removeFacet(facet) {
                var q = $.getQueryParam('q'); //$.query.get('q')[0];
                var fqList = $.getQueryParam('fq'); //$.query.get('fq');
                var paramList = [];
                if (q != null) {
                    paramList.push("q=" + q);
                }
                //alert("this.facet = "+facet+"; fqList = "+fqList.join('|'));

                if (fqList instanceof Array) {
                    //alert("fqList is an array");
                    for (var i in fqList) {
                        var thisFq = decodeURI(fqList[i]); //.replace(':[',':'); // for dates to work
                        //alert("fq = "+thisFq + " || facet = "+facet);
                        if (thisFq.indexOf(facet) != -1) {  // if(str1.indexOf(str2) != -1){
                            //alert("removing fq: "+fqList[i]);
                            fqList.splice(fqList.indexOf(fqList[i]),1);
                        }
                    }
                } else {
                    //alert("fqList is NOT an array");
                    if (decodeURI(fqList) == facet) {
                        fqList = null;
                    }
                }
                //alert("(post) fqList = "+fqList.join('|'));
                if (fqList != null) {
                    paramList.push("fq=" + fqList.join("&fq="));
                }

                window.location.replace(window.location.pathname + '?' + paramList.join('&'));
            }

            // jQuery getQueryParam Plugin 1.0.0 (20100429)
            // By John Terenzio | http://plugins.jquery.com/project/getqueryparam | MIT License
            // Adapted by Nick dos Remedios to handle multiple params with same name - return a list
            (function ($) {
                // jQuery method, this will work like PHP's $_GET[]
                $.getQueryParam = function (param) {
                    // get the pairs of params fist
                    var pairs = location.search.substring(1).split('&');
                    var values = [];
                    // now iterate each pair
                    for (var i = 0; i < pairs.length; i++) {
                        var params = pairs[i].split('=');
                        if (params[0] == param) {
                            // if the param doesn't have a value, like ?photos&videos, then return an empty srting
                            //return params[1] || '';
                            values.push(params[1]);
                        }
                    }

                    if (values.length > 0) {
                        return values;
                    } else {
                        //otherwise return undefined to signify that the param does not exist
                        return undefined;
                    }

                };
            })(jQuery);

            function changeSort(el) {
                var fqList = $.query.get('fq');
                $("#searchForm").submit();
                window.location.replace(url);
            }
        </script>
    </head>
    <body>
        <div id="header">
            <div id="breadcrumb">
                <a href="${initParam.centralServer}">Home</a>
                <a href="${initParam.centralServer}/explore">Explore</a>
                Occurrence Records
            </div>
            <div id="searchButtons">
                <button id="download" title="Download all ${totalHits} results as XLS (tab-delimited) file">Download</button>
                <c:if test="${!fn:contains(entityQuery, 'km of point')}"><%-- Don't display buttons on searchByArea version of page --%>
                    <button id="showMap" title="Display a small map showing points for records">View as Map</button>
                </c:if>
            </div>
            <div>
                <h1>Occurrence Records</h1>
            </div>
            <div id="dialog-confirm" title="Download Occurrences" style="display:none">
               	<p id="termsOfUseDownload">
                    By downloading this content you are agreeing to use it in accordance with the Atlas
                      <a href="http://www.ala.org.au/about/terms-of-use/#TOUusingcontent">Terms of Use</a>
                    and individual <a href=" http://www.ala.org.au/support/faq/#q29">Data Provider Terms</a>.
                    <br/>
                    Please provide the following optional details before downloading:
                </p>
                <form id="downloadForm">
                    <fieldset>
                        <p><label for="email">Email</label>
                            <input type="text" name="email" id="email" value="${pageContext.request.remoteUser}" size="30"  /></p>
                        <p><label for="filename">File Name</label>
                            <input type="text" name="filename" id="filename" value="data" size="30"  /></p>
                        <p><label for="reason" style="vertical-align: top">Download Reason</label>
                            <textarea name="reason" rows="5" cols="30" id="reason"  ></textarea></p>
                    </fieldset>
                </form>
            </div>
        </div><!--close header-->
        <div id="refine-results" class="section no-margin-top">
            <h2>Refine results</h2>
            <h3><strong><fmt:formatNumber value="${searchResult.totalRecords}" pattern="#,###,###"/></strong> results
                returned for <strong>
                <c:choose>
                    <c:when test="${not empty entityQuery}">
                        ${entityQuery}
                    </c:when>
                    <c:otherwise>
                        Search: <a href="?q=${queryJsEscaped}">${queryJsEscaped}</a><a name="searchResults">&nbsp;</a>
                    </c:otherwise>
                </c:choose>
                </strong></h3>
        </div>
        <div id="searchResults">
            <div id="facets">
                <div id="accordion"  style="display:block;">
                    <c:if test="${not empty query}">
                        <c:set var="queryParam">q=<c:out value="${param['q']}" escapeXml="true"/><c:if
                                test="${not empty param.fq}">&fq=${fn:join(paramValues.fq, "&fq=")}</c:if></c:set>
                    </c:if>
                    <c:if  test="${not empty facetMap}">
                        <h3><span class="FieldName">Current Filters</span></h3>
                        <div id="subnavlist">
                            <ul style="padding-left: 24px;">
                                <c:forEach var="item" items="${facetMap}">
                                    <li style="text-indent: -12px; text-transform: none;">
                                        <c:set var="closeLink">&nbsp;[<b><a href="#" onClick="removeFacet('${item.key}:${item.value}'); return false;" style="text-decoration: none" title="remove">X</a></b>]</c:set>
                                        <fmt:message key="facet.${item.key}"/>:
                                        <c:choose>
                                            <c:when test="${fn:containsIgnoreCase(item.key, 'month')}">
                                                <b><fmt:message key="month.${item.value}"/></b>${closeLink}
                                            </c:when>
                                            <c:when test="${fn:containsIgnoreCase(item.key, 'occurrence_date') && fn:startsWith(item.value, '[*')}">
                                                <c:set var="endYear" value="${fn:substring(item.value, 6, 10)}"/><b>Before ${endYear}</b>${closeLink}
                                            </c:when>
                                            <c:when test="${fn:containsIgnoreCase(item.key, 'occurrence_date') && fn:endsWith(item.value, '*]')}">
                                                <c:set var="startYear" value="${fn:substring(item.value, 1, 5)}"/><b>After ${startYear}</b>${closeLink}
                                            </c:when>
                                            <c:when test="${fn:containsIgnoreCase(item.key, 'occurrence_date') && fn:endsWith(item.value, 'Z]')}">
                                                <c:set var="startYear" value="${fn:substring(item.value, 1, 5)}"/><b>${startYear} - ${startYear + 10}</b>${closeLink}
                                            </c:when>
                                            <c:otherwise>
                                                <b><fmt:message key="${item.value}"/></b>${closeLink}
                                            </c:otherwise>
                                        </c:choose>
                                    </li>
                                </c:forEach>
                            </ul>
                        </div>
                    </c:if>
                    <c:forEach var="facetResult" items="${searchResult.facetResults}">
                        <c:if test="${fn:length(facetResult.fieldResult) > 1 && empty facetMap[facetResult.fieldName]}"> <%-- || not empty facetMap[facetResult.fieldName] --%>
                            <h3><span class="FieldName"><fmt:message key="facet.${facetResult.fieldName}"/></span></h3>
                            <div id="subnavlist">
                                <ul>
                                    <c:set var="lastElement" value="${facetResult.fieldResult[fn:length(facetResult.fieldResult)-1]}"/>
                                    <c:if test="${lastElement.label eq 'before' && lastElement.count > 0}">
                                        <li><c:set var="firstYear" value="${fn:substring(facetResult.fieldResult[0].label, 0, 4)}"/>
                                            <a href="?${queryParam}&fq=${facetResult.fieldName}:[* TO ${facetResult.fieldResult[0].label}]">Before ${firstYear}</a>
                                            (<fmt:formatNumber value="${lastElement.count}" pattern="#,###,###"/>)
                                        </li>
                                    </c:if>
                                    <c:forEach var="fieldResult" items="${facetResult.fieldResult}" varStatus="vs">
                                        <c:if test="${fieldResult.count > 0}">
                                            <c:set var="dateRangeTo"><c:choose><c:when test="${vs.last || facetResult.fieldResult[vs.count].label=='before'}">*</c:when><c:otherwise>${facetResult.fieldResult[vs.count].label}</c:otherwise></c:choose></c:set>
                                            <c:choose>
                                                <c:when test="${not empty facetMap[facetResult.fieldName] && fn:contains(facetMap[facetResult.fieldName], fieldResult.label)}"> <%-- fieldResult.label == facetMap[facetResult.fieldName] --%>
                                                    <%-- catch an "active" fq search and provide option to clear it --%>
                                                    <%--<li><a href="#" onClick="removeFacet('${facetResult.fieldName}:${fieldResult.label}'); return false;" class="facetCancelLink">&lt; Any <fmt:message key="facet.${facetResult.fieldName}"/></a><br/>
                                                        <b>
                                                            <c:choose>
                                                                <c:when test="${fn:containsIgnoreCase(facetResult.fieldName, 'month')}"><fmt:message key="month.${fieldResult.label}"/></c:when>
                                                                <c:when test="${fn:containsIgnoreCase(facetResult.fieldName, 'occurrence_date') && fn:endsWith(fieldResult.label, 'Z')}">
                                                                    <c:set var="startYear" value="${fn:substring(fieldResult.label, 0, 4)}"/>${startYear} - ${startYear + 10}
                                                                </c:when>
                                                                <c:otherwise><fmt:message key="${fieldResult.label}"/></c:otherwise>
                                                            </c:choose>

                                                        </b></li>--%>
                                                </c:when>
                                                <c:when test="${fn:containsIgnoreCase(facetResult.fieldName, 'occurrence_date') && fn:endsWith(fieldResult.label, 'Z')}">
                                                    <li><c:set var="startYear" value="${fn:substring(fieldResult.label, 0, 4)}"/>
                                                        <a href="?${queryParam}&fq=${facetResult.fieldName}:[${fieldResult.label} TO ${dateRangeTo}]">${startYear} - ${startYear + 10}</a>
                                                        (<fmt:formatNumber value="${fieldResult.count}" pattern="#,###,###"/>)</li>
                                                </c:when>
                                                <c:when test="${fn:endsWith(fieldResult.label, 'before')}"><%-- skip, otherwise gets inserted at bottom, not top of list --%></c:when>
                                                <c:when test="${fn:containsIgnoreCase(facetResult.fieldName, 'month')}">
                                                    <li><a href="?${queryParam}&fq=${facetResult.fieldName}:${fieldResult.label}"><fmt:message key="month.${not empty fieldResult.label ? fieldResult.label : 'unknown'}"/></a>
                                                        (<fmt:formatNumber value="${fieldResult.count}" pattern="#,###,###"/>)</li>
                                                </c:when>
                                                <c:otherwise>
                                                    <li><a href="?${queryParam}&fq=${facetResult.fieldName}:${fieldResult.label}"><fmt:message key="${not empty fieldResult.label ? fieldResult.label : 'unknown'}"/></a>
                                                        (<fmt:formatNumber value="${fieldResult.count}" pattern="#,###,###"/>)</li>
                                                </c:otherwise>
                                            </c:choose>
                                        </c:if>
                                    </c:forEach>
                                </ul>
                            </div>
                        </c:if>
                    </c:forEach>
                </div>
            </div><!--facets-->
            <div class="solrResults">
                <div id="dropdowns">
                    <div id="resultsStats">
                        <label for="per-page">Results per page</label>
                        <select id="per-page" name="per-page">
                            <c:set var="pageSizeVar">
                                <c:choose>
                                    <c:when test="${not empty param.pageSize}">${param.pageSize}</c:when>
                                    <c:otherwise>20</c:otherwise>
                                </c:choose>
                            </c:set>
                            <option value="10" <c:if test="${pageSizeVar eq '10'}">selected</c:if>>10</option>
                            <option value="20" <c:if test="${pageSizeVar eq '20'}">selected</c:if>>20</option>
                            <option value="50" <c:if test="${pageSizeVar eq '50'}">selected</c:if>>50</option>
                            <option value="100" <c:if test="${pageSizeVar eq '100'}">selected</c:if>>100</option>
                        </select>
                    </div>
                    <div id="sortWidget">
                        Sort by
                        <select id="sort" name="sort">
                            <option value="score" <c:if test="${param.sort eq 'score'}">selected</c:if>>best match</option>
                            <option value="taxon_name" <c:if test="${param.sort eq 'taxon_name'}">selected</c:if>>scientific name</option>
                            <option value="common_name" <c:if test="${param.sort eq 'common_name'}">selected</c:if>>common name</option>
                            <!--                            <option value="rank">rank</option>-->
                            <option value="occurrence_date" <c:if test="${param.sort eq 'occurrence_date'}">selected</c:if>>record date</option>
                            <option value="record_type" <c:if test="${param.sort eq 'record_type'}">selected</c:if>>record type</option>
                        </select>
                        Sort order
                        <select id="dir" name="dir">
                            <option value="asc" <c:if test="${param.dir eq 'asc'}">selected</c:if>>normal</option>
                            <option value="desc" <c:if test="${param.dir eq 'desc'}">selected</c:if>>reverse</option>
                        </select>

                    </div><!--sortWidget-->
                </div><!--drop downs-->
                <div class="results">
                    <c:forEach var="occurrence" items="${searchResult.occurrences}">
                        <h4>Record: <a href="${occurrence.id}" class="occurrenceLink">${occurrence.id}</a> &mdash;
                            <span style="text-transform: capitalize">${occurrence.rank}</span>: <span class="occurrenceNames"><alatag:formatSciName rankId="${occurrence.rankId}" name="${occurrence.taxonName}"/></span>
                            <c:if test="${not empty occurrence.commonName}"> | <span class="occurrenceNames">${occurrence.commonName}</span></c:if>
                        </h4>
                        <p class="occurrenceResultRow">
                            <c:if test="${not empty occurrence.dataResource}"><span style="text-transform: capitalize;"><strong class="resultsLabel">Dataset:</strong> ${occurrence.dataResource}</span></c:if>
                            <c:if test="${not empty occurrence.basisOfRecord}"><span style="text-transform: capitalize;"><strong class="resultsLabel">Record type:</strong> ${occurrence.basisOfRecord}</span></c:if>
                            <c:if test="${not empty occurrence.occurrenceDate}"><span style="text-transform: capitalize;"><strong class="resultsLabel">Record date:</strong> <fmt:formatDate value="${occurrence.occurrenceDate}" pattern="yyyy-MM-dd"/></span></c:if>
                            <c:if test="${not empty occurrence.states}"><span style="text-transform: capitalize;"><strong class="resultsLabel">State:</strong> <fmt:message key="region.${occurrence.states[0]}"/></span></c:if>
                        </p>
                    </c:forEach>
                </div><!--close results-->
                <div id="searchNavBar">
                    <alatag:searchNavigationLinks totalRecords="${searchResult.totalRecords}" startIndex="${searchResult.startIndex}"
                         lastPage="${lastPage}" pageSize="${searchResult.pageSize}"/>
                </div>
            </div><!--solrResults-->
            <div id="pointsMap"></div>
            <div id="busyIcon" style="display:none;"><img src="${pageContext.request.contextPath}/static/css/images/wait.gif" alt="busy/spinning icon" /></div>
        </div>
    </body>
</html>
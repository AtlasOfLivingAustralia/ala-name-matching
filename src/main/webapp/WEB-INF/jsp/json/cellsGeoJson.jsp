<%@ include file="/common/taglibs.jsp"%><%
/*
 * GeoJSON representation of "distinct" points as polygons "cells" for a given occurrence search
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
*/
%><%@ page contentType="application/json; charset=UTF-8" %>
<c:if test="${not empty param['callback']}">${param['callback']}(</c:if>
<json:object prettyPrint="false">
    <json:property name="type" value="FeatureCollection"/>
    <json:array name="features" var="cell" items="${cells}">
        <json:object>
            <json:property name="type" value="Feature"/>
            <json:object name="geometry">
                <json:property name="type" value="Polygon"/>
                <json:array name="coordinates">
                    <json:array var="coordCells" items="${cell.coordinateCells}">
                        <json:array var="coord" items="${coordCells}">
                            <json:property value="${coord}"/>
                        </json:array>
                    </json:array>
                </json:array>
            </json:object>
            <json:object name="properties">
                 <json:property name="type" value="${cell.type.label}"/>
                 <json:property name="count" value="${cell.count}"/>
                 <json:property name="color"><c:if 
                         test="${cell.count < 10}">#ffff00</c:if><c:if
                         test="${cell.count >= 10 && cell.count < 50}">#ffcc00</c:if><c:if
                         test="${cell.count >= 50 && cell.count < 100}">#ff9900</c:if><c:if
                         test="${cell.count >= 100 && cell.count < 250}">#ff6600</c:if><c:if
                         test="${cell.count >= 250 && cell.count < 500}">#ff3300</c:if><c:if
                         test="${cell.count >= 500}">#cc0000</c:if></json:property>
            </json:object>
        </json:object>
    </json:array>
</json:object>
<c:if test="${not empty param['callback']}">)</c:if>
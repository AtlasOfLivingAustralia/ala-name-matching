<%@ include file="/common/taglibs.jsp"%><%
/*
 * GeoJSON representation of "distinct" points for a given occurrence search
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
*/
%><%@ page contentType="application/json; charset=UTF-8" %>
<c:if test="${not empty param['callback']}">${param['callback']}(</c:if>
<json:object prettyPrint="false">
    <json:property name="type" value="FeatureCollection"/>
    <json:array name="features" var="point" items="${points}">
        <json:object>
            <json:property name="type" value="Feature"/>
            <json:object name="geometry">
                <json:property name="type" value="Point"/>
                <json:array name="coordinates" var="coord" items="${point.coordinates}">
                    <json:property value="${coord}"/>
                </json:array>
            </json:object>
            <json:object name="properties">
                 <json:property name="type" value="${point.type.label}"/>
                 <json:property name="count" value="${point.count}"/>
                 <json:property name="color"><c:if 
                         test="${point.count < 10}">#ffff00</c:if><c:if
                         test="${point.count >= 10 && point.count < 50}">#ffcc00</c:if><c:if
                         test="${point.count >= 50 && point.count < 100}">#ff9900</c:if><c:if
                         test="${point.count >= 100 && point.count < 250}">#ff6600</c:if><c:if
                         test="${point.count >= 250 && point.count < 500}">#ff3300</c:if><c:if
                         test="${point.count >= 500}">#cc0000</c:if></json:property>
            </json:object>
        </json:object>
    </json:array>
</json:object>
<c:if test="${not empty param['callback']}">)</c:if>
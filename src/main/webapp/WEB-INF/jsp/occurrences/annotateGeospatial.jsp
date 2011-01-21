<%@ include file="/common/taglibs.jsp"%>
<div class="boxes">
    <div id="geospatial" class="window">
    	<h2>Geospatial Annotation</h2>
<c:choose>
<c:when test="${empty pageContext.request.userPrincipal}">
	<jsp:include page="annotateAuth.jsp"/>
</c:when>
<c:otherwise>
        <form action="${pageContext.request.contextPath}/annotation/saveAnnotation" method="post" name="geospatialForm" id="geospatialForm">
        <table>
          <thead>
            <tr>
              <th>&nbsp;</th>
              <th>Current</th>
              <th>Suggested</th>
            </tr>
          </thead>
          <tbody>
          <tr>
            <td>State/Territory<input type="hidden" name="4" value="state"/></td>
            <td><input class="oldValues" name="old.state" type="text" readonly="true" value=""/></td>
            <td><select class="newValues" name="new.state" id="state">
                <option></option>
                <option>New South Wales</option>
                <option>Queensland</option>
                <option>South Australia</option>
                <option>Tasmania</option>
                <option>Victoria</option>
                <option>Western Australia</option>
                <option>Australian Capital Territory</option>
                <option>Northern Territory</option>
              </select></td>
          </tr>
          <tr>
            <td valign="top">Locality<input type="hidden" name="3" value="locality"/></td>
            <td><textarea class="oldValues" name="old.locality" readonly="true" rows="3" cols="20"></textarea></td>
            <td><textarea class="newValues" name="new.locality" id="locality" rows="3" cols="20"></textarea></td>
          </tr>
          <tr>
            <td>Latitude (in degrees)<input type="hidden" name="1" value="latitude"/></td>
            <td><input class="oldValues" name="old.latitude" type="text" readonly="true" value=""/></td>
            <td><input class="newValues" name="new.latitude" type="text" id="latitude" /></td>
          </tr>
          <tr>
            <td>Longitude (in degrees)<input type="hidden" name="2" value="longitude"/></td>
            <td><input class="oldValues" name="old.longitude" type="text" readonly="true" value=""/></td>
            <td><input class="newValues" name="new.longitude" type="text" id="longitude" /></td>
          </tr>
          <jsp:include page="annotateCommon1.jsp"/>
          </tbody>
        </table>
        <jsp:include page="annotateCommon2.jsp"><jsp:param name="section" value="geospatial" /></jsp:include>
        </form>
</c:otherwise>
</c:choose>
    </div>
</div>

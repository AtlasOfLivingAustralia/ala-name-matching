<%@ include file="/common/taglibs.jsp"%>
<div class="boxes">
    <div id="dataset" class="window">
    	<h2>Dataset Annotation</h2>
<c:choose>
<c:when test="${empty pageContext.request.userPrincipal}">
	<jsp:include page="annotateAuth.jsp"/>
</c:when>
<c:otherwise>
        <form action="${pageContext.request.contextPath}/annotation/saveAnnotation" method="post" name="datasetForm" id="datasetForm">
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
            <td>Institution code<input name="1" type="hidden" value="institution-name"/></td>
            <td><input class="oldValues" name="old.institutionCode" type="text" readonly="true" value=""/></td>
            <td><input class="newValues" name="new.institutionCode" type="text" id="institutionCode" /></td>
          </tr>
          <tr>
            <td>Collection code<input name="2" type="hidden" value="collection-code"/></td>
            <td><input class="oldValues" name="old.collectionCode" type="text" readonly="true" value=""/></td>
            <td><input class="newValues" name="new.collectionCode" type="text" id="collectionCode" /></td>
          </tr>
          <tr>
            <td>Catalogue No<input name="3" type="hidden" value="catalog-no"/></td>
            <td><input class="oldValues" name="old.catalogueNumber" type="text" readonly="true" value=""/></td>
            <td><input class="newValues" name="new.catalogueNumber" type="text" id="catalogueNumber" /></td>
          </tr>
          <tr>
            <td>Basis of record<input name="4" type="hidden" value="basis-of-record"/></td>
            <td><input class="oldValues" name="old.basisOfRecord" type="text" readonly="true" value=""/></td>
            <td><select name="new.basisOfRecord" id="basisOfRecord" class="newValues">
                <option></option>
                <option>unknown</option>
                <option>observation</option>
                <option>specimen</option>
                <option>living</option>
                <option>germplasm</option>
                <option>fossil</option>
                <option>literature</option>
              </select>
            </td>
          </tr>
          <tr>
            <td>Identifier name<input name="5" type="hidden" value="identifier-name"/></td>
            <td><input class="oldValues" name="old.identifierName" type="text" readonly="true" value=""/></td>
            <td><input class="newValues" name="new.identifierName" type="text" id="identifierName" /></td>
          </tr>
          <tr>
            <td>Date identified (yyyy-mm-dd)<input name="6" type="hidden" value="date-identified"/></td>
            <td><input class="oldValues" name="old.identificationDate" type="text" readonly="true" value=""/></td>
            <td><input class="newValues" name="new.identificationDate" type="text" id="identificationDate" /></td>
          </tr>
          <tr>
            <td>Field number<input name="7" type="hidden" value="field-number"/></td>
            <td><input class="oldValues" name="old.fieldNumber" type="text" readonly="true" value=""/></td>
            <td><input class="newValues" name="new.fieldNumber" type="text" id="fieldNumber" /></td>
          </tr>
          <tr>
            <td>Collector name<input name="8" type="hidden" value="collector-name"/></td>
            <td><input class="oldValues" name="old.collectorName" type="text" readonly="true" value=""/></td>
            <td><input class="newValues" name="new.collectorName" type="text" id="collectorName" /></td>
          </tr>
          <tr>
            <td>Date collected (yyyy-mm-dd)<input name="9" type="hidden" value="date-collected"/></td>
            <td><input class="oldValues" name="old.collectionDate" type="text" readonly="true" value=""/></td>
            <td><input class="newValues" name="new.collectionDate" type="text" id="collectionDate" /></td>
          </tr>
          <jsp:include page="annotateCommon1.jsp"/>
          </tbody>
        </table>
          <jsp:include page="annotateCommon2.jsp"><jsp:param name="section" value="dataset" /></jsp:include>
        </form>
</c:otherwise>
</c:choose>
    </div>
    <!-- Mask to cover the whole screen -->
    <div id="mask"></div>
</div>

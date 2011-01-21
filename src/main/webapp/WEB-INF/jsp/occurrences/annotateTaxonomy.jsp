<%@ include file="/common/taglibs.jsp"%>
<div class="boxes">
    <div id="taxonomy" class="window">
    	<h2>Taxonomy Annotation</h2>
<c:choose>
<c:when test="${empty pageContext.request.userPrincipal}">
	<jsp:include page="annotateAuth.jsp"/>
</c:when>
<c:otherwise>
        <form action="${pageContext.request.contextPath}/annotation/saveAnnotation" method="post" name="taxonomyForm" id="taxonomyForm">
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
            <td>Scientific name (excluding authorship)<input name="1" type="hidden" value="scientific-name"/></td>
            <td><input class="oldValues" name="old.scientificName" type="text" readonly="true" value=""/></td>
            <td><input class="newValues" name="new.scientificName" type="text" id="scientificName" /></td>
          </tr>
          <tr>
            <td>Author<input name="2" type="hidden" value="author"/></td>
            <td><input class="oldValues" name="old.author" type="text" readonly="true" value=""/></td>
            <td><input class="newValues" name="new.author" type="text" id="v" /></td>
          </tr>
          <tr>
            <td>Kingdom<input name="3" type="hidden" value="kingdom"/></td>
            <td><input class="oldValues" name="old.kingdom" type="text" readonly="true" value=""/></td>
            <td><select class="newValues" name="new.kingdom" id="kingdom">
                <option></option>
                <option>Animalia</option>
                <option>Archaea</option>
                <option>Bacteria</option>
                <option>Chromista</option>
                <option>Fungi</option>
                <option>Plantae</option>
                <option>Protozoa</option>
                <option>Viruses</option>
              </select></td>
          </tr>
          <tr>
            <td>Phylum<input name="4" type="hidden" value="phylum"/></td>
            <td><input class="oldValues" name="old.phylum" type="text" readonly="true" value=""/></td>
            <td><input class="newValues" name="new.phylum" type="text" id="phylum" /></td>
          </tr>
          <tr>
            <td>Class<input name="5" type="hidden" value="class"/></td>
            <td><input class="oldValues" name="old.class" type="text" readonly="true" value=""/></td>
            <td><input class="newValues" name="new.class" type="text" id="class" /></td>
          </tr>
          <tr>
            <td>Order<input name="6" type="hidden" value="order"/></td>
            <td><input class="oldValues" name="old.order" type="text" readonly="true" value=""/></td>
            <td><input class="newValues" name="new.order" type="text" id="order" /></td>
          </tr>
          <tr>
            <td>Family<input name="7" type="hidden" value="family"/></td>
            <td><input class="oldValues" name="old.family" type="text" readonly="true" value=""/></td>
            <td><input class="newValues" name="new.family" type="text" id="family" /></td>
          </tr>
          <tr>
            <td>Genus<input name="8" type="hidden" value="genus"/></td>
            <td><input class="oldValues" name="old.genus" type="text" readonly="true" value=""/></td>
            <td><input class="newValues" name="new.genus" type="text" id="genus" /></td>
          </tr>
          <tr>
            <td>Species<input name="9" type="hidden" value="species"/></td>
            <td><input class="oldValues" name="old.species" type="text" readonly="true" value=""/></td>
            <td><input class="newValues" name="new.species" type="text" id="species" /></td>
          </tr>
          <jsp:include page="annotateCommon1.jsp"/>
          </tbody>
        </table>
        <jsp:include page="annotateCommon2.jsp"><jsp:param name="section" value="taxonomy" /></jsp:include>
        </form>
</c:otherwise>
</c:choose>
    </div>
</div>

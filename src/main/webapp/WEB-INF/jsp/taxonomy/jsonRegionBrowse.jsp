<%@ include file="/common/taglibs.jsp"%><% 
/*  
 * JSON data format adapted from example YOU DataTable
 * 
 * http://developer.yahoo.com/yui/examples/datatable/dt_xhrjson.html
 * 
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
*/ 
%><json:object prettyPrint="true">
  <json:object name="ResultSet">
    <json:array name="Result" var="regionConcept" items="${regionConcepts}">
        <json:object>
          <json:property name="label" value="${regionConcept.taxonConceptName} (${regionConcept.rankName})"/>
          <json:property name="id" value="${regionConcept.taxonConceptId}"/>
        </json:object>
    </json:array>
  </json:object>
</json:object>
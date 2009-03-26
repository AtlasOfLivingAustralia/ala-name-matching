<%@ include file="/common/taglibs.jsp"%>
<h3><spring:message code="blanket.search"/></h3>
<form name="quickSearchForm" method="get" onsubmit="return submitQuickSearch(false);" action="${pageContext.request.contextPath}/search/blanketSearch.htm">
	<fieldset>
		<input id="query" type="search" name="keyword" <c:if test="${not empty searchString}">value="${searchString}"</c:if> placeholder="<spring:message code="blanket.search.placeholder"/>" autosave="gbif.blanketsearch" results="5" tabindex="1"/>
		<a href="javascript:submitFromLinkQuickSearch();" id="go"><spring:message code="blanket.search.go"/></a>
	</fieldset>	
</form>
<script type="text/javascript">
	//document.getElementById("searchQuery").focus();
	
	function submitQuickSearch(formSubmit){
		//check for empty value
		var textValue = document.getElementById('query').value;
		if(textValue!=null && textValue.length>0){
			if(formSubmit)
				document.quickSearchForm.submit();
			return true;
		}
		return false;
	}
	
	function submitFromLinkQuickSearch(){
		//check for empty value
		var textValue = document.getElementById('query').value;
		if(textValue!=null && textValue.length>0){
				document.quickSearchForm.submit();
		}
	}	
	
</script>
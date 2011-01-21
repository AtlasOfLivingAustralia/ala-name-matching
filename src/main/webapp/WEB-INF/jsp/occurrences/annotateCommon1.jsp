<%@ include file="/common/taglibs.jsp"%>
<c:set var="occurrenceHtmlUrl" value="http://${pageContext.request.serverName}${pageContext.request.serverPort!=80 ? ':' : ''}${pageContext.request.serverPort!=80 ? pageContext.request.serverPort : ''}${pageContext.request.contextPath}/occurrences/${occurrence.id}"/>
<tr>
    <td valign="top">Comment<input name="10" type="hidden" value="descr"/></td>
    <td colspan="2"><textarea name="comment" class="comment newValues" rows="6" cols="45"></textarea></td>
</tr>
<tr>
<td></td><td></td><td><ala:loginStatus/></td>
</tr>
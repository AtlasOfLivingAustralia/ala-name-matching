<%@ include file="/common/taglibs.jsp"%>
<%@ attribute name="rankValue" required="true" rtexprvalue="true" type="java.lang.Integer"%>

<%
	java.lang.String rankName = org.ala.web.util.RankFacet.getForId(rankValue).getRank();
	if(rankName!=null)
		out.print(rankName);
	else
		out.print(rankValue);
%>
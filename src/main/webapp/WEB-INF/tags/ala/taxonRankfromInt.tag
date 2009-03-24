<%@ include file="/common/taglibs.jsp"%>
<%@ attribute name="rankValue" required="true" rtexprvalue="true" type="java.lang.Integer"%>

<% // TODO put this in a proper tag class (not JSP)
if (rankValue != null) {
    org.ala.web.util.RankFacet rank = org.ala.web.util.RankFacet.getForId(rankValue);
    java.lang.String rankName = (rank != null) ? rank.getRank() : null;
	if (rankName != null) out.print(rankName);
	else out.print(rankValue);
}
%>
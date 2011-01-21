<%@ include file="/common/taglibs.jsp"%>
<%@ attribute name="issuesBit" required="true" rtexprvalue="true" type="java.lang.Integer" %>
<%
	if (issuesBit==0) {
		request.setAttribute("otherIssueText",null);
	} else {
		//no support in JSP EL for bit-wise operators has resulted in this....
		request.setAttribute("otherIssueText", 
								(((issuesBit & 0x01)==0) ? "" : ("missing catalogue number" + (issuesBit > 0x01 ? "; " : "")))
								+ (((issuesBit & 0x02)==0) ? "" : ("basis of record not known" + (issuesBit > 0x03 ? "; " : "")))
								+ (((issuesBit & 0x04)==0) ? "" : ("supplied date invalid" + (issuesBit > 0x07 ? "; " : "")))
								+ (((issuesBit & 0x08)==0) ? "" : "country inferred from coordinates"));
	}
%>
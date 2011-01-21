<%@ include file="/common/taglibs.jsp"%>
<%@ attribute name="issuesBit" required="true" rtexprvalue="true" type="java.lang.Integer" %>
<%
	if (issuesBit==0) {
		request.setAttribute("taxonomicIssueText",null);
	} else {
		//no support in JSP EL for bit-wise operators has resulted in this....
		request.setAttribute("taxonomicIssueText", 
								(((issuesBit & 0x01)==0) ? "" : ("scientific name not validly formed" + (issuesBit > 0x01 ? "; " : "")))
								+ (((issuesBit & 0x02)==0) ? "" : ("kingdom not known for record" + (issuesBit > 0x03 ? "; " : "")))
								+ (((issuesBit & 0x04)==0) ? "" : "supplied name ambiguous"));
	}
%>

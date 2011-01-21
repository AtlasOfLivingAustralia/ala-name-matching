<%@ include file="/common/taglibs.jsp"%>
<%@ attribute name="issuesBit" required="true" rtexprvalue="true" type="java.lang.Integer" %>
<%

try{
	if (issuesBit==0) {
		request.setAttribute("geospatialIssueText",null);
	} else {
		//no support in JSP EL for bit-wise operators has resulted in this....
		request.setAttribute("geospatialIssueText", 
								(((issuesBit & 0x01)==0) ? "" : ("latitude probably negated" + (issuesBit > 0x01 ? "; " : "")))
								+ (((issuesBit & 0x02)==0) ? "" : ("longitude probably negated" + (issuesBit > 0x03 ? "; " : "")))
								+ (((issuesBit & 0x04)==0) ? "" : ("latitude and longitude probably transposed" + (issuesBit > 0x07 ? "; " : "")))
								+ (((issuesBit & 0x08)==0) ? "" : ("coordinates supplied as (0.0, 0.0)" + (issuesBit > 0x0F ? "; " : "")))
								+ (((issuesBit & 0x10)==0) ? "" : ("supplied coordinates out of range" + (issuesBit > 0x1F ? "; " : "")))
								+ (((issuesBit & 0x20)==0) ? "" : "coordinates fall outside specified country"	)
					);
	}
} catch(Exception e){
	e.printStackTrace();
	
}
%>
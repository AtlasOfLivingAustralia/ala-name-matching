<%
StringBuffer sb = new StringBuffer();
java.util.Enumeration keys = request.getAttributeNames();
while (keys.hasMoreElements()) {
	String attributeName = (String) keys.nextElement();
	Object next = request.getAttribute(attributeName);
    //sb.append(attributeName+" = "+next.toString());	
	//System.out.println(next.getClass());
	if (next instanceof org.springframework.web.context.support.XmlWebApplicationContext) {
		sb.append(attributeName);
		sb.append(" || (getNamespace = ");
		sb.append(((org.springframework.web.context.support.XmlWebApplicationContext)next).getNamespace());
		sb.append(")");
	}
	sb.append("<br/>");
}
out.print(sb.toString());
%>
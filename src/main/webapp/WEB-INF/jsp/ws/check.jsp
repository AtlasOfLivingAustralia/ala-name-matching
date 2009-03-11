<%@ include file="/common/taglibs.jsp"%>
<div id="twopartheader">
    <h2>Your webservice - what we found out</h2>
</div>
<div>

<p>
<label>Name</label>
${dataResource.name}<br/>
<label>Description</label>
${dataResource.description}<br/>
<label>Address</label>
${dataResource.address}<br/>
</p>

<c:forEach items="${agents}" var="agent">
    <p>
	    <label>Name</label>
		${agent.name}<br/>
		<label>Email</label>
		${agent.email}<br/>
    </p>
</c:forEach>
</div>
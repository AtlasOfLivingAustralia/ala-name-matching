<%@ include file="/common/taglibs.jsp"%><%
/*
 * JSON data representation of occurrence record annotation
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
*/
%><json:object prettyPrint="true">
    <json:array name="annotation" var="oa" items="${oas}">
        <json:object>
            <json:property name="annotates" value="${oa.annotates}"/>
            <json:property name="annoteaKey" value="${oa.annoteaKey}"/>
            <json:property name="inReplyTo" value="${oa.inReplyTo}"/>
            <json:property name="replyField" value="${oa.replyField}"/>
            <json:property name="creator" value="${oa.creator}"/>
            <json:property name="date"><fmt:formatDate value="${oa.date}" pattern="dd MMMM yyyy HH:mm"/></json:property>
            <json:property name="occurrenceId" value="${oa.body.occurrenceId}"/>
            <json:property name="dataResourceId" value="${oa.body.dataResourceId}"/>
            <json:property name="section" value="${oa.body.section}"/>
            <json:property name="comment" value="${oa.body.comment}"/>
            <json:array name="fieldUpdateSet" var="fieldUpdate" items="${oa.body.fieldUpdates}">
                <json:object>
                    <json:property name="fieldName" value="${fieldUpdate.fieldName}"/>
                    <json:property name="oldValue" value="${fieldUpdate.oldValue}"/>
                    <json:property name="newValue" value="${fieldUpdate.newValue}"/>
                </json:object>
            </json:array>
        </json:object>
    </json:array>
</json:object>

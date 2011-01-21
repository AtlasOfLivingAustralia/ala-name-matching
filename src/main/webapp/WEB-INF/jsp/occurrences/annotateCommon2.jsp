<%@ include file="/common/taglibs.jsp"%>
        <input type="hidden" name="lim" value="9" id="tn" />
        <input type="hidden" name="act" value="2" id="act"/>
        <input type="hidden" name="xpath" value="${param.section}" id="xpath"/>
        <input type="hidden" name="url" value="http://${pageContext.request.serverName}${pageContext.request.contextPath}/occurrences/${occurrence.id}" size="128"/>
        <input type="hidden" name="recordKey" value="${occurrence.id}" id="recordid"/>
        <input type="hidden" name="dataResourceId" value="${rawOccurrenceRecord.dataResourceId}" id="dataResourceId"/>
        <input type="hidden" name="dataResourceUid" value="${occurrence.dataResourceUid}" id="dataResourceUid"/>
        <input type="hidden" name="dataResource" value="${occurrence.dataResource}" id="dataResource"/>
        <input type="hidden" name="collectionUid" value="${occurrence.collectionCodeUid}" id="collectionCodeUid"/>
        <input type="hidden" name="institutionUid" value="${occurrence.institutionCodeUid}" id="institutionCodeUid"/>
        <input type="hidden" name="ref" value="" size="128"/>
        <input type="hidden" name="token" value="/tmp/TUEb3WK"/>
        <div class="submitButtons">
            <span class="btn"><input type="submit" name="submit" value="Submit"></span>
            <span class="btn"><input type="button" value="Cancel" class="close"></span>
        </div>
        <div class="errorMsg">&nbsp;</div>
        <div class="loading"><img src="${pageContext.request.contextPath}/static/css/images/wait.gif" alt="loading data..."/></div>
        <div class='message'><span id="msgTextGeospatial" class="msgText"></span><input type="button" value="Close" class="finish"></div>
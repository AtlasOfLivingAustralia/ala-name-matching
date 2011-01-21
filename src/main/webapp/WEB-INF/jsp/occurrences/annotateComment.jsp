<%@ include file="/common/taglibs.jsp"%>
<div class="boxes">
    <div id="comment" class="window">
        <form action="${pageContext.request.contextPath}/annotation/saveAnnotation" method="post" name="geospatialForm" id="geospatialForm">
        <h4 style="margin:0 0 10px 0">General Comment</h4>
        <table>
          <thead>
            <th>&nbsp;</th>
            <th>&nbsp;</th>
            <th>&nbsp;</th>
          </thead>
          <jsp:include page="annotateCommon1.jsp"/>
        </table>
        <input type="hidden" name="zz" class="newValues" value="needed for form validation"/>
        <jsp:include page="annotateCommon2.jsp"/>
        </form>
    </div>
    <!-- Mask to cover the whole screen
    <div id="mask"></div> -->
</div>

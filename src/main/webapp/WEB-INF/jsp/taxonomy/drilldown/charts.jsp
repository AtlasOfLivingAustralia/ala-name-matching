<%@ include file="/common/taglibs.jsp"%>
<c:if test="${not empty chartData}">
    <div id="charts" style="">
        <script type="text/javascript" src="${pageContext.request.contextPath}/javascript/swfobject.js"></script>
        <script type="text/javascript">
            var flashvars1 = {
                path: escape("${pageContext.request.contextPath}/charts/"),
                settings_file: escape("${pageContext.request.contextPath}/charts/year_settings.xml"),
                chart_data: "${chartData.year}",
                preloader_color: "#999999"
            };

            swfobject.embedSWF("${pageContext.request.contextPath}/charts/amxy.swf", "chart1", "80%", "400", "9.0.0", "expressInstall.swf", flashvars1 );

            var flashvars2 = {
                    path: escape("${pageContext.request.contextPath}/charts/"),
                    settings_file: escape("${pageContext.request.contextPath}/charts/month_settings.xml"),
                    chart_data: "${chartData.month}",
                    preloader_color: "#999999"
                };

            swfobject.embedSWF("${pageContext.request.contextPath}/charts/amcolumn.swf", "chart2", "80%", "400", "9.0.0", "expressInstall.swf", flashvars2 );

            var flashvars3 = {
                    path: escape("${pageContext.request.contextPath}/charts/"),
                    settings_file: escape("${pageContext.request.contextPath}/charts/names_settings.xml"),
                    chart_data: "${chartData.basis_of_record}",
                    preloader_color: "#999999"
                };

            swfobject.embedSWF("${pageContext.request.contextPath}/charts/ampie.swf", "chart3", "80%", "400", "9.0.0", "expressInstall.swf", flashvars3 );

        </script>
        <h4>Data Breakdown Charts</h4>

        <h5>By Year</h5>
        <div id="chart1"></div>
        <h5>By Month</h5>
        <div id="chart2"></div>
        <h5>By Basis of Record</h5>
        <div id="chart3"></div>
    </div>
    </c:if>
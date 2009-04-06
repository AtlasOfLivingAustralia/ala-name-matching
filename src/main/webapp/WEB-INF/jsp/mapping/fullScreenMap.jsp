<%@ include file="/common/taglibs.jsp"%><html xmlns="http://www.w3.org/1999/xhtml">
    <head>
        <title>${param['name']} - Full screen mapping - ALA GIS Portal</title>
        <style type="text/css">
            body {margin: 0;}
            #map {width: 100%;height: 100%;}
        </style>
        <jsp:include page="/WEB-INF/jsp/mapping/headcontent.jsp"/>
        <script type="text/javascript">
          var entityId = '${param['id']}';
          var entityType = '${param['type']}';
          var entityName = '${param['name']}';
          /*
          var minLongitude = '${param['minx']}';
          var minLatitude = '${param['miny']}';
          var maxLongitude = '${param['maxx']}';
          var maxLatitude = '${param['maxy']}';
          */
        </script>
        <link rel="shortcut icon" href="${pageContext.request.contextPath}/favicon.ico" type="image/x-icon" /> 
    </head>
    <body>
        <div id="content">
            <jsp:include page="openlayer.jsp"/>
        </div>
        <script type="text/javascript">
           zoomToBounds();
        </script>
    </body>
</html>
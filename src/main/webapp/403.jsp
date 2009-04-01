<%@ page language="java" isErrorPage="true" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
  "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">


<html>
  <head>
    <title>ALA - Access Denied</title>
    <style>
      body {
        margin: 0px;
        padding: 0px;
        font: 0.81em Verdana, Arial, Helvetica, sans-serif;
      }
      h1 {
        background: url("${pageContext.request.contextPath}/skins/ala/images/ALA-web-banner-2.jpg") no-repeat;
        height: 80px;
        color: white;
        font-size: 14pt;
        padding-left: 680px;
        padding-top: 40px;
        margin: 0px;
      }
      h2 {
        font-size: 14pt;
        margin: 0px;
      }
      h3 {
        font-size: 12pt;
        margin: 0px;
      }
      #page {
        margin-left: 20px;
      }
      a {
        color: #006600;
      }
      #content {
        padding:30px;
      }
    </style>
  </head>
  <body>
    <h1>Access Denied</h1>
    
    <div id="content">
      <h2>Access Denied</h2>
      <p>
      This might be because:<br/>
      <ul>
        <li>You dont have access privileges to view this page</li>      
        <li>You may have typed the web address incorrectly. Please check the address and spelling ensuring that it does not contain capital letters or spaces</li>
      </ul>
     </p>
     <p>To report this please send an email supplying the url for this page to 
        <a href="mailto:info@ala.org.au">info@ala.org.au</a>.
     </p> 
     <p>
          <a href="${pageContext.request.contextPath}">Click here</a> to continue.
     </p>      
     </giv>   
  </body>
</html>
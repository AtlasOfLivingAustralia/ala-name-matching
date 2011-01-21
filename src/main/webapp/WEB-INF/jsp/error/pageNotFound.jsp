<%-- 
    Document   : pageNotFound
    Created on : Apr 7, 2010, 12:26:36 PM
    Author     : "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Page Not Found</title>
        <script type="text/javascript">
            $(document).ready(function() {
                $(".showHideLink").toggle(function() {
                        $("#stackTrace").slideDown();
                    }, function() {
                        $("#stackTrace").slideUp();
                    }
                 );
            });
        </script>
    </head>
    <body>
        <h2>Page Not Found</h2>
        <p>The page you requested is no longer available at this URL. Please try searching or navigating to it from our <a href="/">home page</a>.</p>
        <p></p>
        <p><a href="#" class="showHideLink">Show/Hide Stacktrace</a></p>
        <div id="stackTrace" style="display: none; font-family: monospace;"><%
        try {
                // The Servlet spec guarantees this attribute will be available
                Throwable exception = (Throwable) request.getAttribute("javax.servlet.error.exception");

                if (exception != null) {
                        if (exception instanceof ServletException) {
                                // It's a ServletException: we should extract the root cause
                                ServletException sex = (ServletException) exception;
                                Throwable rootCause = sex.getRootCause();
                                if (rootCause == null)
                                        rootCause = sex;
                                out.println("** Root cause is: "+ rootCause.getMessage());
                                rootCause.printStackTrace(new java.io.PrintWriter(out));
                        }
                        else {
                                // It's not a ServletException, so we'll just show it
                                exception.printStackTrace(new java.io.PrintWriter(out));
                        }
                }
                else  {
                out.println("No error information available");
                }

                // Display cookies
                out.println("\nCookies:\n");
                Cookie[] cookies = request.getCookies();
                if (cookies != null) {
                for (int i = 0; i < cookies.length; i++) {
                        out.println(cookies[i].getName() + "=[" + cookies[i].getValue() + "]");
                        }
                }

        } catch (Exception ex) {
                ex.printStackTrace(new java.io.PrintWriter(out));
        }
        %>
        </div>
        <p/>
        <br/>
    </body>
</html>

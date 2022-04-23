<%--@elvariable id="user" type="net.testudobank.User"--%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
         pageEncoding="ISO-8859-1" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:genericpage>
    <jsp:attribute name="header">
        <title>Welcome Page</title>
          <style type="text/css">
              label {
                  display: inline-block;
                  width: 200px;
                  margin: 5px;
                  text-align: left;
              }

              input[type=text], input[type=password], select {
                  width: 200px;
              }

              input[type=radio] {
                  display: inline-block;
                  margin-left: 45px;
              }

              input[type=checkbox] {
                  display: inline-block;
                  margin-right: 190px;
              }

              button {
                  padding: 10px;
                  margin: 10px;
              }
          </style>
    </jsp:attribute>
    <jsp:body>
        <div class="container text-center">
            <h2>Welcome to Testudo Bank!</h2>
            <img src="https://fanapeel.com/wp-content/uploads/logo_-university-of-maryland-terrapins-testudo-turtle-hold-red-white-m.png"
                 style="float:left;width:100px;height:100px;">
        </div>
    </jsp:body>
</t:genericpage>
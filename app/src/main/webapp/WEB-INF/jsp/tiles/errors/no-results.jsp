<%--@elvariable id="searchDescription" type="java.lang.String"--%>

<%@ page pageEncoding="UTF-8" contentType="text/html;charset=UTF-8" %>
<%@ page isErrorPage="true" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>


<div class="row margin-top-xxlarge">
    <div class="small-6 small-centered columns">
        <h4>Sorry, we could not find results matching your search criteria:</h4>
        <h5><c:out value="${searchDescription}"/></h5>
    </div>
</div>


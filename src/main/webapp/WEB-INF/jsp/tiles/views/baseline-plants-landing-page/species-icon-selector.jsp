<%--@elvariable id="species" type="String"--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<c:choose>
    <c:when test="${species.matches('(?i)anolis carolinensis')}">
        <c:set var="speciesIconCode" value="7"/>
        <c:set var="speciesColorCode" value="blue"/>
    </c:when>
    <c:when test="${species.matches('(?i)arabidopsis.*')}">
        <c:set var="speciesIconCode" value="B"/>
        <c:set var="speciesColorCode" value="green"/>
    </c:when>
    <c:when test="${species.matches('(?i)beta vulgaris.*')}">
        <c:set var="speciesIconCode" value="B"/>
        <c:set var="speciesColorCode" value="green"/>
    </c:when>
    <c:when test="${species.matches('(?i)bos taurus')}">
        <c:set var="speciesIconCode" value="C"/>
        <c:set var="speciesColorCode" value="red"/>
    </c:when>
    <c:when test="${species.matches('(?i)brachypodium distachyon')}">
        <c:set var="speciesIconCode" value="%"/>
        <c:set var="speciesColorCode" value="green"/>
    </c:when>
    <c:when test="${species.matches('(?i)brassica.*')}">
        <c:set var="speciesIconCode" value="B"/>
        <c:set var="speciesColorCode" value="green"/>
    </c:when>
    <c:when test="${species.matches('(?i)caenorhabditis elegans')}">
        <c:set var="speciesIconCode" value="W"/>
        <c:set var="speciesColorCode" value="blue"/>
    </c:when>
    <c:when test="${species.matches('(?i)chlamydomonas reinhardtii')}">
        <c:set var="speciesIconCode" value="Y"/>
        <c:set var="speciesColorCode" value="green"/>
    </c:when>
    <c:when test="${species.matches('(?i)chlorocebus sabaeus')}">
        <c:set var="speciesIconCode" value="r"/>
        <c:set var="speciesColorCode" value="red"/>
    </c:when>
    <c:when test="${species.matches('(?i)danio rerio')}">
        <c:set var="speciesIconCode" value="Z"/>
        <c:set var="speciesColorCode" value="blue"/>
    </c:when>
    <c:when test="${species.matches('(?i)drosophila melanogaster')}">
        <c:set var="speciesIconCode" value="F"/>
        <c:set var="speciesColorCode" value="blue"/>
    </c:when>
    <c:when test="${species.matches('(?i)equus caballus')}">
        <c:set var="speciesIconCode" value="h"/>
        <c:set var="speciesColorCode" value="red"/>
    </c:when>
    <c:when test="${species.matches('(?i)gallus gallus')}">
        <c:set var="speciesIconCode" value="k"/>
        <c:set var="speciesColorCode" value="red"/>
    </c:when>
    <c:when test="${species.matches('(?i)glycine max')}">
        <c:set var="speciesIconCode" value="^"/>
        <c:set var="speciesColorCode" value="green"/>
    </c:when>
    <c:when test="${species.matches('(?i)gorilla gorilla')}">
        <c:set var="speciesIconCode" value="G"/>
        <c:set var="speciesColorCode" value="red"/>
    </c:when>
    <c:when test="${species.matches('(?i)homo sapiens')}">
        <c:set var="speciesIconCode" value="H"/>
        <c:set var="speciesColorCode" value="red"/>
    </c:when>
    <c:when test="${species.matches('(?i)hordeum vulgare.*')}">
        <c:set var="speciesIconCode" value="5"/>
        <c:set var="speciesColorCode" value="green"/>
    </c:when>
    <c:when test="${species.matches('(?i)macaca mulatta')}">
        <c:set var="speciesIconCode" value="r"/>
        <c:set var="speciesColorCode" value="red"/>
    </c:when>
    <c:when test="${species.matches('(?i)monodelphis domestica')}">
        <c:set var="speciesIconCode" value="9"/>
        <c:set var="speciesColorCode" value="red"/>
    </c:when>
    <c:when test="${species.matches('(?i)mus musculus')}">
        <c:set var="speciesIconCode" value="M"/>
        <c:set var="speciesColorCode" value="red"/>
    </c:when>
    <c:when test="${species.matches('(?i)musa acuminata.*')}">
        <c:set var="speciesIconCode" value="P"/>
        <c:set var="speciesColorCode" value="green"/>
    </c:when>
    <c:when test="${species.matches('(?i)oryctolagus cuniculus')}">
        <c:set var="speciesIconCode" value="t"/>
        <c:set var="speciesColorCode" value="red" />
    </c:when>
    <c:when test="${species.matches('(?i)oryza sativa.*')}">
        <c:set var="speciesIconCode" value="6"/>
        <c:set var="speciesColorCode" value="green"/>
    </c:when>
    <c:when test="${species.matches('(?i)ovis aries')}">
        <c:set var="speciesIconCode" value="x"/>
        <c:set var="speciesColorCode" value="red"/>
    </c:when>
    <c:when test="${species.matches('(?i)pan paniscus')}">
        <c:set var="speciesIconCode" value="i"/>
        <c:set var="speciesColorCode" value="red"/>
    </c:when>
    <c:when test="${species.matches('(?i)pan troglodytes')}">
        <c:set var="speciesIconCode" value="i"/>
        <c:set var="speciesColorCode" value="red"/>
    </c:when>
    <c:when test="${species.matches('(?i)papio anubis')}">
        <c:set var="speciesIconCode" value="8"/>
        <c:set var="speciesColorCode" value="red"/>
    </c:when>
    <c:when test="${species.matches('(?i)populus trichocarpa')}">
        <c:set var="speciesIconCode" value="P"/>
        <c:set var="speciesColorCode" value="green"/>
    </c:when>
    <c:when test="${species.matches('(?i)rattus norvegicus')}">
        <c:set var="speciesIconCode" value="R"/>
        <c:set var="speciesColorCode" value="red"/>
    </c:when>
    <c:when test="${species.matches('(?i)schistosoma mansoni')}">
        <c:set var="speciesIconCode" value="W"/>
        <c:set var="speciesColorCode" value="blue"/>
    </c:when>
    <c:when test="${species.matches('(?i)setaria italica')}">
        <c:set var="speciesIconCode" value="%"/>
        <c:set var="speciesColorCode" value="green"/>
    </c:when>
    <c:when test="${species.matches('(?i)solanum lycopersicum')}">
        <c:set var="speciesIconCode" value=")"/>
        <c:set var="speciesColorCode" value="green"/>
    </c:when>
    <c:when test="${species.matches('(?i)sorghum bicolor')}">
        <c:set var="speciesIconCode" value="P"/>
        <c:set var="speciesColorCode" value="green"/>
    </c:when>
    <c:when test="${species.matches('(?i)sus scrofa')}">
        <c:set var="speciesIconCode" value="p"/>
        <c:set var="speciesColorCode" value="red"/>
    </c:when>
    <c:when test="${species.matches('(?i)triticum aestivum')}">
        <c:set var="speciesIconCode" value="5"/>
        <c:set var="speciesColorCode" value="green"/>
    </c:when>
    <c:when test="${species.matches('(?i)vitis vinifera')}">
        <c:set var="speciesIconCode" value="O"/>
        <c:set var="speciesColorCode" value="green"/>
    </c:when>
    <c:when test="${species.matches('(?i)xenopus.*')}">
        <c:set var="speciesIconCode" value="f"/>
        <c:set var="speciesColorCode" value="blue"/>
    </c:when>
    <c:when test="${species.matches('(?i)zea mays')}">
        <c:set var="speciesIconCode" value="c"/>
        <c:set var="speciesColorCode" value="green" />
    </c:when>
    <c:otherwise>
        <c:set var="speciesIconCode" value="❔"/>
        <c:set var="speciesColorCode" value="grey" />
    </c:otherwise>
</c:choose>

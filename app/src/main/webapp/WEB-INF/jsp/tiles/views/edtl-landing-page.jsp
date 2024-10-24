<%--@elvariable id="experimentAccessionsBySpecies" type="com.google.common.collect.SortedSetMultimap<String, String>"--%>
<%--@elvariable id="experimentLinks" type="java.util.Map<String, Integer>"--%>
<%--@elvariable id="experimentDisplayNames" type="java.util.Map<String, String>"--%>

<%@ page pageEncoding="UTF-8" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%--<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/baseline_plant-experiments.css">--%>

<div class="row">
  <h3>European Diagnostic Transcriptomic Library</h3>

  <div class="media-object">
    <div class="media-object-section hide-for-small-onl">
      <img src="${pageContext.request.contextPath}/resources/images/experiments-summary/edtl.png" alt="EDTL logo" style="height: 7em">
    </div>
    <div class="media-object-section">
      <p>
        The European Diagnostic Transcriptomic Library (EDTL) is being developed as a curated database of whole blood
        RNA expression data from human subjects with a wide range of infectious and inflammatory conditions. It will be
        a resource for the discovery and validation of diagnostic gene expression signatures of human disease.
        Stringent diagnostic classification criteria have been applied to create reference standard groups of subjects
        with different diseases.
      </p>
      <p>
        The EDTL will initially present data arising from the <a href="https://www.diamonds2020.eu/">DIAMONDS project
        (Diagnosis and Management of Febrile Illness using RNA Personalised Molecular Signature Diagnosis)</a>. Over
        time it will expand to include data from associated studies.
      </p>
      <p>
        This project has received funding from the European Union’s Horizon 2020 research and innovation programme
        under grant agreement No. 848196.
      </p>
    </div>
  </div>
</div>

<div class="row">
  <div class="small-12 columns">
    <h3>Baseline experiments</h3>

    <ul>
      <li>
        <a href="${pageContext.request.contextPath}/experiments/E-MTAB-11671">RNA-Sequencing of whole blood samples from children with a range of acute febrile illnesses</a>
      </li>
      <li>
        <a href="${pageContext.request.contextPath}/experiments/E-CURD-146">Diagnosis of multisystem inflammatory syndrome in children (MIS-C) by a whole-blood transcriptional signature</a>
      </li>
    </ul>
  </div>
</div>

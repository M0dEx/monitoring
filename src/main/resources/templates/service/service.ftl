<#-- @ftlvariable name="serviceDescription" type="eu.m0dex.monitoring.backend.rest.resources.Services.ServiceDescription" -->
<#import "../layout.ftl" as layout>
<@layout.base>
<div class="container d-flex flex-column text-center mt-5">
    <div class="row justify-content-center">
        <div class="col-auto">
            <h1>${serviceDescription.displayName}</h1>
        </div>
    </div>
    <div class="row justify-content-center">
        <p>URL: <a href="${serviceDescription.url}">${serviceDescription.url}</a></p>
    </div>
    <div class="row justify-content-center">
        <p>Method: ${serviceDescription.method}</p>
    </div>
    <div class="row justify-content-center">
        <p>Check interval: ${serviceDescription.intervalMillis} ms</p>
    </div>
</div>
</@layout.base>
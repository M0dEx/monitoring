<#-- @ftlvariable name="serviceDescription" type="eu.m0dex.monitoring.backend.rest.resources.Services.ServiceDescription" -->
<#-- @ftlvariable name="serviceHistory" type="kotlin.collections.List<eu.m0dex.monitoring.database.schema.Status>" -->
<#-- @ftlvariable name="from" type="kotlinx.datetime.Instant" -->
<#-- @ftlvariable name="to" type="kotlinx.datetime.Instant" -->
<#import "../layout.ftl" as layout>
<@layout.base>
<div class="container d-flex flex-column mt-5">
    <div class="row justify-content-center">
        <div class="col-auto text-center">
            <h1>History of ${serviceDescription.displayName}</h1>
            <h1>${from.toString()} - ${to.toString()}</h1>
        </div>
    </div>
    <div class="row justify-content-center">
        <div class="col-auto">
            <table class="table table-dark table-hover align-middle">
                <tr>
                    <th>Status</th>
                    <th>Response code</th>
                    <th>Latency</th>
                    <th>Fail reason</th>
                    <th>Timestamp</th>
                </tr>
                <#list serviceHistory as status>
                    <tr>
                        <td>
                            <#if status.online>
                                <#assign spanClass = "badge text-bg-success">
                                <#assign spanText = "Online">
                            <#else>
                                <#assign spanClass = "badge text-bg-danger">
                                <#assign spanText = "Offline">
                            </#if>
                            <span class="${spanClass}">${spanText}</span>
                        </td>
                        <td>
                            <#if status.responseCode??>
                                ${status.responseCode}
                            <#else>
                                N/A
                            </#if>
                        </td>
                        <td>
                            <#if status.latency??>
                                ${status.latency} ms
                            <#else>
                                N/A
                            </#if>
                        </td>
                        <td>
                            <#if status.failReason??>
                                ${status.failReason}
                            <#else>
                                N/A
                            </#if>
                        </td>
                        <td>${status.timestamp.toString()}</td>
                    </tr>
                </#list>
            </table>
        </div>
    </div>
</div>
</@layout.base>
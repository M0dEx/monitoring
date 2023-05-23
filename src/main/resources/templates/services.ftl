<#-- @ftlvariable name="services" type="kotlin.collections.List<eu.m0dex.monitoring.backend.gui.resources.ServicesGui.ServiceStatus>" -->
<#import "layout.ftl" as layout>
<@layout.base>
<div class="container d-flex flex-column mt-5">
    <div class="row justify-content-center">
        <div class="col-auto">
            <h1>Status of FIT CTU services</h1>
        </div>
    </div>
    <div class="row justify-content-center">
        <div class="col-auto">
            <table class="table table-dark table-hover align-middle">
                <tr>
                    <th>Service</th>
                    <th>Uptime (24 hours)</th>
                    <th>Status (24 hours)</th>
                </tr>
                <#list services as service>
                    <tr>
                        <td>
                            <#if !service.status?? || !service.status.online?? || !service.status.timestamp??>
                                <#assign spanClass = "badge text-bg-secondary">
                                <#assign spanText = "Unknown">
                                <#assign spanTooltip = "N/A">
                            <#elseif service.status.online>
                                <#assign spanClass = "badge text-bg-success">
                                <#assign spanText = "Online">
                                <#assign spanTooltip = service.status.timestamp.toString()>
                            <#else>
                                <#assign spanClass = "badge text-bg-danger">
                                <#assign spanText = "Offline">
                                <#assign spanTooltip = service.status.timestamp.toString()>
                            </#if>
                            <span class="${spanClass}" data-bs-toggle="tooltip" data-bs-placement="top" data-bs-title="${spanTooltip}">${spanText}</span>
                            <b data-bs-toggle="tooltip" data-bs-html="true" data-bs-title="URL: ${service.description.url}<br/>Method: ${service.description.method}<br/>Interval: ${service.description.intervalMillis} ms">${service.description.displayName}</b>
                        </td>
                        <td>${service.uptime.uptime * 100} %</td>
                        <td>
                            <svg width="320" height="34">
                                <#list service.history as status>
                                    <#if status.onlineCount == 0 && status.offlineCount == 0>
                                        <#assign color = "#6c757d">
                                    <#elseif status.uptime == 1>
                                        <#assign color = "#198754">
                                    <#elseif status.uptime gt 0>
                                        <#assign color = "#ffc107">
                                    <#else>
                                        <#assign color = "#dc3545">
                                    </#if>
                                    <a href="/services/${service.description.name}/history?from=${status.from.toString()}&to=${status.to.toString()}">
                                        <rect height="34" width="10" rx="4" ry="4" x="${status?index * 13}" y="0" fill="${color}" data-bs-toggle="tooltip" data-bs-html="true" data-bs-title="Uptime: ${status.uptime * 100} %<br/>Failed checks: ${status.offlineCount}<br/>${status.from.toString()}<br/>${status.to.toString()}">
                                        </rect>
                                    </a>
                                </#list>
                            </svg>
                        </td>
                    </tr>
                </#list>
            </table>
        </div>
    </div>
</div>
</@layout.base>

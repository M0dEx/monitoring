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
            <table class="table table-dark">
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
                            <a href="/services/${service.description.name}">${service.description.displayName}</a>
                        </td>
                        <td>${service.uptime.uptime * 100} %</td>
                        <td>
                            <svg width="320" height="34">
                                <#list service.history as status>
                                    <#if status.onlineCount == 0 && status.offlineCount == 0>
                                        <#assign color = "gray">
                                    <#elseif status.uptime == 1>
                                        <#assign color = "green">
                                    <#elseif status.uptime gt 0>
                                        <#assign color = "yellow">
                                    <#else>
                                        <#assign color = "red">
                                    </#if>
                                    <a href="/services/${service.description.name}/history?from=${status.from.toString()}&to=${status.to.toString()}">
                                        <rect height="34" width="10" x="${status?index * 13}" y="0" fill="${color}" data-bs-toggle="tooltip" data-bs-placement="top" data-bs-title="${status.from.toString()} - ${status.to.toString()}">
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

<#-- @ftlvariable name="services" type="kotlin.collections.List<eu.m0dex.monitoring.backend.gui.resources.ServicesGui.ServiceStatus>" -->
<#import "layout.ftl" as layout>
<@layout.base>
<div class="row justify-content-center">
    <div class="col-auto justify-content-center">
        <table class="table table-dark">
            <tr>
                <th>Service</th>
                <th>Uptime (24 hours)</th>
                <th>Status (24 hours)</th>
            </tr>
            <#list services as service>

                <tr>
                    <td><a href="/services/${service.description.name}">${service.description.displayName}</a></td>
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
                                <rect height="34" width="10" x="${status?index * 13}" y="0" fill="${color}" data-html="true">
                                    <title>
                                        ${status.from.toString()} - ${status.to.toString()}
                                    </title>
                                </rect>
                            </#list>
                        </svg>
                    </td>
                </tr>
            </#list>
        </table>
    </div>
</div>
</@layout.base>

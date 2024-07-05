<#-- $This file is distributed under the terms of the license in LICENSE$ -->

<#-- Custom object property statement view for faux property "editor of". See the PropertyConfig.n3 file for details.

     This template must be self-contained and not rely on other variables set for the individual page, because it
     is also used to generate the property statement during a deletion.
 -->

<#import "lib-sequence.ftl" as s>
<#import "lib-datetime.ftl" as dt>

<@showMaintainership statement />

<#-- Use a macro to keep variable assignments local; otherwise the values carry over to the
     next statement -->
<#macro showMaintainership statement>
    <#local softwareTitle>
        <#if statement.software??>
            <a href="${profileUrl(statement.uri("software"))}"  title="${i18n().software_name}">${statement.softwareName}</a>
        <#else>
            <#-- This shouldn't happen, but we must provide for it -->
            <a href="${profileUrl(statement.uri("maintainership"))}" title="${i18n().missing_software}">${i18n().missing_software}</a>
        </#if>
    </#local>

    ${softwareTitle} <@dt.yearSpan "${statement.dateTime!}" />
</#macro>

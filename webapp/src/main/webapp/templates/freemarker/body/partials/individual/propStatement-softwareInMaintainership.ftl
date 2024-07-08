<#-- $This file is distributed under the terms of the license in LICENSE$ -->

<#-- Custom object property statement view for faux property "editors". See the PropertyConfig.n3 file for details.

     This template must be self-contained and not rely on other variables set for the individual page, because it
     is also used to generate the property statement during a deletion.
 -->

<#import "lib-sequence.ftl" as s>
<@showMaintainership statement />

<#-- Use a macro to keep variable assignments local; otherwise the values carry over to the
     next statement -->
<#macro showMaintainership statement>
    <#if statement.person??>
        <a href="${profileUrl(statement.uri("person"))}" title="${i18n().maintainer_name}">${statement.personName}</a>
    <#else>
        <#-- This shouldn't happen, but we must provide for it -->
        <a href="${profileUrl(statement.uri("maintainership"))}" title="${i18n().missing_maintainer}">${i18n().missing_maintainer}</a>
    </#if>
</#macro>

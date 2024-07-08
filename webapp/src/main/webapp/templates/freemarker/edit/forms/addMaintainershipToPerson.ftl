<#-- $This file is distributed under the terms of the license in LICENSE$ -->

<#-- this is in request.subject.name -->

<#-- leaving this edit/add mode code in for reference in case we decide we need it -->

<#import "lib-vivo-form.ftl" as lvf>

<#assign subjectName=""/>
<#assign orgLabel="mysteryOrgLabel"/>

<#--Retrieve certain edit configuration information-->
<#assign editMode = editConfiguration.pageData.editMode />
<#assign htmlForElements = editConfiguration.pageData.htmlForElements />

<#--The blank sentinel indicates what value should be put in a URI when no autocomplete result has been selected.
If the blank value is non-null or non-empty, n3 editing for an existing object will remove the original relationship
if nothing is selected for that object-->
<#assign blankSentinel = "" />
<#if editConfigurationConstants?has_content && editConfigurationConstants?keys?seq_contains("BLANK_SENTINEL")>
	<#assign blankSentinel = editConfigurationConstants["BLANK_SENTINEL"] />
</#if>

<#--This flag is for clearing the label field on submission for an existing object being selected from autocomplete.
Set this flag on the input acUriReceiver where you would like this behavior to occur. -->
<#assign flagClearLabelForExisting = "flagClearLabelForExisting" />

<#--Retrieve variables needed-->
<#assign softwareTypeValue = lvf.getFormFieldValue(editSubmission, editConfiguration, "softwareType")/>
<#assign softwareLabelValue = lvf.getFormFieldValue(editSubmission, editConfiguration, "softwareLabel") />
<#assign softwareLabelDisplayValue = lvf.getFormFieldValue(editSubmission, editConfiguration, "softwareLabelDisplay") />
<#assign existingSoftwareValue = lvf.getFormFieldValue(editSubmission, editConfiguration, "existingSoftware") />

<#--If edit submission exists, then retrieve validation errors if they exist-->
<#if editSubmission?has_content && editSubmission.submissionExists = true && editSubmission.validationErrors?has_content>
	<#assign submissionErrors = editSubmission.validationErrors/>
</#if>

<#if editMode == "edit" || editMode == "repair">
        <#assign titleVerb="${i18n().edit_capitalized}">
        <#assign submitButtonText="${i18n().save_changes}">
        <#assign disabledVal="disabled">
<#else>
        <#assign titleVerb="${i18n().create_capitalized}">
        <#assign submitButtonText="${i18n().create_entry}">
        <#assign disabledVal=""/>
</#if>

<#assign requiredHint = "<span class='requiredHint'> *</span>" />
<#assign yearHint     = "<span class='hint'>(${i18n().year_hint_format})</span>" />


<h2>${titleVerb}&nbsp;${i18n().maintainer_of_entry} ${editConfiguration.subjectName}</h2>

<#--Display error messages if any-->
<#if submissionErrors?has_content>
    <#if orgLabelDisplayValue?has_content >
        <#assign orgLabelValue = orgLabelDisplayValue />
    </#if>
    <section id="error-alert" role="alert">
        <img src="${urls.images}/iconAlert.png" width="24" height="24" alert="${i18n().error_alert_icon}" />
        <p>
        <#--below shows examples of both printing out all error messages and checking the error message for a specific field-->
         <#if lvf.submissionErrorExists(editSubmission, "softwareType")>
 	        ${i18n().select_software_type}
        </#if>
         <#if lvf.submissionErrorExists(editSubmission, "softwareLabel")>
 	        ${i18n().select_a_software_name}
        </#if>
        </p>
    </section>
</#if>

<@lvf.unsupportedBrowser urls.base />

<section id="addMaintainershipToPerson" role="region">

    <form id="addMaintainershipToPerson" class="customForm noIE67" action="${submitUrl}"  role="add/edit maintainership">


    <p class="inline">
        <label for="softType">${i18n().software_type_capitalized} ${requiredHint}</label>
        <#assign softTypeOpts = editConfiguration.pageData.softwareType />
        <select id="typeSelector" name="softwareType" acGroupName="software">
            <#list softTypeOpts?keys as key>
                <#if softwareTypeValue = key>
                    <option value="${key}"  selected >${softTypeOpts[key]}</option>
                <#else>
                    <option value="${key}">${softTypeOpts[key]}</option>
                </#if>
            </#list>
        </select>
    </p>

    <p>
        <label for="relatedIndLabel">${i18n().software_name_capitalized} ${requiredHint}</label>
        <input class="acSelector" size="50"  type="text" id="relatedIndLabel" name="softwareLabel" acGroupName="software" value="${softwareLabelValue}"  />
        <input class="display" type="hidden" id="softwareDisplay" acGroupName="software" name="softwareLabelDisplay" value="${softwareLabelDisplayValue}">
    </p>

    <div class="acSelection" acGroupName="software">
        <p class="inline">
            <label>${i18n().selected_software}:</label>
            <span class="acSelectionInfo"></span>
            <a href="" class="verifyMatch"  title="${i18n().verify_match_capitalized}">(${i18n().verify_match_capitalized}</a> ${i18n().or}
            <a href="#" class="changeSelection" id="changeSelection">${i18n().change_selection})</a>
        </p>
        <input class="acUriReceiver" type="hidden" id="softwareUri" name="existingSoftware" value="${existingSoftwareValue}" ${flagClearLabelForExisting}="true" />
    </div>


  	<#--End draw elements-->
    <input type="hidden" id="editKey" name="editKey" value="${editKey}"/>
    <p class="submit">
         <input type="submit" id="submit" value="${submitButtonText}"/><span class="or"> ${i18n().or} </span>
         <a class="cancel" href="${cancelUrl}" title="${i18n().cancel_title}">${i18n().cancel_link}</a>
     </p>

    <p id="requiredLegend" class="requiredHint">* ${i18n().required_fields}</p>

</form>


<script type="text/javascript">
var customFormData  = {
    acUrl: '${urls.base}/autocomplete?tokenize=true&stem=true',
    editMode: '${editMode}',
    acTypes: {software: 'http://purl.obolibrary.org/obo/ERO_0000071'},
    defaultTypeName: 'software',
    baseHref: '${urls.base}/individual?uri=',
    blankSentinel: '${blankSentinel}',
    flagClearLabelForExisting: '${flagClearLabelForExisting}',
    subjectName: '${editConfiguration.subjectName}'
};
var i18nStrings = {
    selectAnExisting: '${i18n().select_an_existing?js_string}',
    selectAnExistingOrCreateNewOne: '${i18n().select_an_existing_or_create_a_new_one?js_string}',
    selectedString: '${i18n().selected?js_string}'
};

</script>

</section>

${stylesheets.add('<link rel="stylesheet" href="${urls.base}/js/jquery-ui/css/smoothness/jquery-ui-1.12.1.css" />')}
${stylesheets.add('<link rel="stylesheet" href="${urls.base}/templates/freemarker/edit/forms/css/customForm.css" />')}
${stylesheets.add('<link rel="stylesheet" href="${urls.base}/templates/freemarker/edit/forms/css/customFormWithAutocomplete.css" />')}


${scripts.add('<script type="text/javascript" src="${urls.base}/js/jquery-ui/js/jquery-ui-1.12.1.min.js"></script>',
             '<script type="text/javascript" src="${urls.base}/js/customFormUtils.js"></script>',
             '<script type="text/javascript" src="${urls.base}/js/extensions/String.js"></script>',
             '<script type="text/javascript" src="${urls.base}/js/browserUtils.js"></script>',
             '<script type="text/javascript" src="${urls.base}/js/jquery_plugins/jquery.bgiframe.pack.js"></script>',
             '<script type="text/javascript" src="${urls.base}/templates/freemarker/edit/forms/js/customFormWithAutocomplete.js"></script>')}



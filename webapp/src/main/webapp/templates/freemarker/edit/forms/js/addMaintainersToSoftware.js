/* $This file is distributed under the terms of the license in LICENSE$ */

var addMaintainerForm = {

    /* *** Initial page setup *** */

    onLoad: function() {

        if (this.disableFormInUnsupportedBrowsers()) {
            return;
        }
        this.mixIn();
        this.initObjects();
        this.initPage();
    },

    disableFormInUnsupportedBrowsers: function() {
        var disableWrapper = $('#ie67DisableWrapper');

        // Check for unsupported browsers only if the element exists on the page
        if (disableWrapper.length) {
            if (vitro.browserUtils.isIELessThan8()) {
                disableWrapper.show();
                $('.noIE67').hide();
                return true;
            }
        }
        return false;
    },

    mixIn: function() {
        // Mix in the custom form utility methods
        $.extend(this, vitro.customFormUtils);

        // Get the custom form data from the page
        $.extend(this, customFormData);

        // Get the i18n variables from the page
        $.extend(this, i18nStrings);
    },

    // On page load, create references for easy access to form elements.
    // NB These must be assigned after the elements have been loaded onto the page.
    initObjects: function() {

        this.form = $('#addMaintainerForm');
        this.showFormButtonWrapper = $('#showAddForm');
        this.showFormButton = $('#showAddFormButton');
        this.removeMaintainershipLinks = $('a.remove');
        //this.undoLinks = $('a.undo');
        this.submit = this.form.find(':submit');
        this.cancel = this.form.find('.cancel');
        this.acSelector = this.form.find('.acSelector');
        this.labelField = $('#label');
        this.firstNameField = $('#firstName');
        this.middleNameField = $('#middleName');
        this.lastNameField = $('#lastName');
        this.lastNameLabel = $('label[for=lastName]');
        this.personUriField = $('#personUri');
        this.firstNameWrapper = this.firstNameField.parent();
        this.middleNameWrapper = this.middleNameField.parent();
        this.lastNameWrapper = this.lastNameField.parent();
        this.selectedMaintainer = $('#selectedMaintainer');
        this.selectedMaintainerName = $('#selectedMaintainerName');
        this.acHelpTextClass = 'acSelectorWithHelpText';
        this.verifyMatch = this.form.find('.verifyMatch');
        this.personSection = $('section#personFields');
        this.personLink = $('a#personLink');
        this.returnLink = $('a#returnLink');
    },

    // Initial page setup. Called only at page load.
    initPage: function() {

        this.initMaintainershipData();

        // Show elements hidden by CSS for the non-JavaScript-enabled version.
        // NB The non-JavaScript version of this form is currently not functional.
        this.removeMaintainershipLinks.show();

        //this.undoLinks.hide();

        this.bindEventListeners();

        this.initAutocomplete();

        this.initElementData();

        this.initMaintainerDD();

        if (this.findValidationErrors()) {
            this.initFormAfterInvalidSubmission();
        } else {
            this.initMaintainerListOnlyView();
        }
    },


    /* *** Set up the various page views *** */

   // This initialization is done only on page load, not when returning to maintainer list only view
   // after hitting 'cancel.'
   initMaintainerListOnlyView: function() {

        if ($('.maintainership').length) {  // make sure we have at least one maintainer
            // Reorder maintainers on page load so that previously unranked maintainers get a rank. Otherwise,
            // when we add a new maintainer, it will get put ahead of any previously unranked maintainers, instead
            // of at the end of the list. (It is also helpful to normalize the data before we get started.)
            this.reorderMaintainers();
        }
        this.showMaintainerListOnlyView();
   },

    // This view shows the list of existing maintainers and hides the form.
    // There is a button to show the form. We do this on page load, and after
    // hitting 'cancel' from full view.
    showMaintainerListOnlyView: function() {
        this.hideForm();
        this.showFormButtonWrapper.show();
    },

    // View of form after returning from an invalid submission. On this form,
    // validation errors entail that we were entering a new person, so we show
    // all the fields straightaway.
    initFormAfterInvalidSubmission: function() {
        this.initForm();
        this.showFieldsForNewPerson();
    },

    // Initial view of add maintainer form. We get here by clicking the show form button,
    // or by cancelling out of an autocomplete selection.
    initFormView: function() {

        this.initForm();

        // There's a conflict bewteen the last name fields .blur event and the cancel
        // button's click. So display the middle and first names along with the last name tlw72
        //this.hideFieldsForNewPerson();

        // This shouldn't be needed, because calling this.hideFormFields(this.lastNameWrapper)
        // from showSelectedMaintainer should do it. However, it doesn't work from there,
        // or in the cancel action, or if referring to this.lastNameField. None of those work,
        // however.
        $('#lastName').val('');
        // Set the initial autocomplete help text in the acSelector field.
        this.addAcHelpText(this.acSelector);

        return false;

    },

    // Form initialization common to both a 'clean' form view and when
    // returning from an invalid submission.
    initForm: function() {

        // Hide the button that shows the form
        this.showFormButtonWrapper.hide();

        this.hideSelectedPerson();

        this.cancel.unbind('click');
        this.cancel.bind('click', function() {
            addMaintainerForm.showMaintainerListOnlyView();
            addMaintainerForm.setMaintainerType("person");
            return false;
        });

        // Reset the last name field. It had been hidden if we selected an maintainer from
        // the autocomplete field.
        this.lastNameWrapper.show();
        this.showFieldsForNewPerson();

        // Show the form
        this.form.show();
        //this.lastNameField.focus();
    },

    hideSelectedPerson: function() {
        this.selectedMaintainer.hide();
        this.selectedMaintainerName.html('');
        this.personUriField.val('');
    },

    showFieldsForNewPerson: function() {
        this.firstNameWrapper.show();
        this.middleNameWrapper.show();
    },

    hideFieldsForNewPerson: function() {
        this.hideFields(this.firstNameWrapper);
        this.hideFields(this.middleNameWrapper);
    },

    /* *** Ajax initializations *** */

    /* Autocomplete */
    initAutocomplete: function() {

        // Make cache a property of this so we can access it after removing
        // an maintainer.
        this.acCache = {};
        this.setAcFilter();
        var $acField = this.lastNameField;
        var urlString = addMaintainerForm.acUrl + addMaintainerForm.personUrl + addMaintainerForm.tokenize;
        var authType = "person";

        $acField.autocomplete({
            minLength: 2,
            source: function(request, response) {
                if (request.term in addMaintainerForm.acCache) {
                    // console.log('found term in cache');
                    response(addMaintainerForm.acCache[request.term]);
                    return;
                }
                // console.log('not getting term from cache');

                // If the url query params are too long, we could do a post
                // here instead of a get. Add the exclude uris to the data
                // rather than to the url.
                $.ajax({
                    url: urlString,
                    dataType: 'json',
                    data: {
                        term: request.term
                    },
                    complete: function(xhr, status) {
                        // Not sure why, but we need an explicit json parse here. jQuery
                        // should parse the response text and return a json object.
                        var results = jQuery.parseJSON(xhr.responseText),
                            filteredResults = addMaintainerForm.filterAcResults(results);
                        addMaintainerForm.acCache[request.term] = filteredResults;
                        response(filteredResults);
                    }

                });
            },
            // Select event not triggered in IE6/7 when selecting with enter key rather
            // than mouse. Thus form is disabled in these browsers.
            // jQuery UI bug: when scrolling through the ac suggestions with up/down arrow
            // keys, the input element gets filled with the highlighted text, even though no
            // select event has been triggered. To trigger a select, the user must hit enter
            // or click on the selection with the mouse. This appears to confuse some users.
            select: function(event, ui) {
                addMaintainerForm.showSelectedMaintainer(ui,authType);
            }
        });

    },

    initElementData: function() {
        this.verifyMatch.data('baseHref', this.verifyMatch.attr('href'));
    },

    setAcFilter: function() {
        this.acFilter = [];

        $('.maintainership').each(function() {
            var uri = $(this).data('maintainerUri');
            addMaintainerForm.acFilter.push(uri);
         });
    },

    removeMaintainerFromAcFilter: function(maintainer) {
        var index = $.inArray(maintainer, this.acFilter);
        if (index > -1) { // this should always be true
            this.acFilter.splice(index, 1);
        }
    },

    filterAcResults: function(results) {
        var filteredResults = [];
        if (!this.acFilter.length) {
            return results;
        }
        $.each(results, function() {
            if ($.inArray(this.uri, addMaintainerForm.acFilter) == -1) {
                // console.log("adding " + this.label + " to filtered results");
                filteredResults.push(this);
            }
            else {
                // console.log("filtering out " + this.label);
            }
        });
        return filteredResults;
    },

    // After removing an maintainership, selectively clear matching autocomplete
    // cache entries, else the associated maintainer will not be included in
    // subsequent autocomplete suggestions.
    clearAcCacheEntries: function(name) {
        name = name.toLowerCase();
        $.each(this.acCache, function(key, value) {
            if (name.indexOf(key) == 0) {
                delete addMaintainerForm.acCache[key];
            }
        });
    },

    // Action taken after selecting an maintainer from the autocomplete list
    showSelectedMaintainer: function(ui,authType) {

        if ( authType == "person" ) {
            this.personUriField.val(ui.item.uri);
            this.selectedMaintainer.show();

            // Transfer the name from the autocomplete to the selected maintainer
            // name display, and hide the last name field.
            this.selectedMaintainerName.html(ui.item.label);
            // NB For some reason this doesn't delete the value from the last name
            // field when the form is redisplayed. Thus it's done explicitly in initFormView.
            this.hideFields(this.lastNameWrapper);
            // These get displayed if the selection was made through an enter keystroke,
            // since the keydown event on the last name field is also triggered (and
            // executes first). So re-hide them here.
            this.hideFieldsForNewPerson();
            this.personLink.attr('href', this.verifyMatch.data('baseHref') + ui.item.uri);
        }

        // Cancel restores initial form view
        this.cancel.unbind('click');
        this.cancel.bind('click', function() {
            addMaintainerForm.initFormView();
            addMaintainerForm.setMaintainerType(authType);
            return false;
        });
    },

    /* Drag-and-drop */
    initMaintainerDD: function() {

        var maintainershipList = $('#dragDropList'),
            maintainerships = maintainershipList.children('li');

        if (maintainerships.length < 2) {
            return;
        }

        $('.maintainerNameWrapper').each(function() {
            $(this).attr('title', addMaintainerForm.maintainerNameWrapperTitle);
        });

        maintainershipList.sortable({
            cursor: 'move',
            update: function(event, ui) {
                addMaintainerForm.reorderMaintainers(event, ui);
            }
        });
    },

    // Reorder maintainers. Called on page load and after maintainer drag-and-drop and remove.
    // Event and ui parameters are defined only in the case of drag-and-drop.
    reorderMaintainers: function(event, ui) {
        var maintainerships = $('li.maintainership').map(function(index, el) {
            return $(this).data('maintainershipUri');
        }).get();

        $.ajax({
            url: addMaintainerForm.reorderUrl,
            data: {
                predicate: addMaintainerForm.rankPredicate,
                individuals: maintainerships
            },
            traditional: true, // serialize the array of individuals for the server
            dataType: 'json',
            type: 'POST',
            success: function(data, status, request) {
                var pos;
                $('.maintainership').each(function(index){
                    pos = index + 1;
                    // Set the new position for this element. The only function of this value
                    // is so we can reset an element to its original position in case reordering fails.
                    addMaintainerForm.setPosition(this, pos);
                });
                // Set the form rank field value.
                $('#rank').val(pos + 1);
            },
            error: function(request, status, error) {
                // ui is undefined on page load and after an maintainership removal.
                if (ui) {
                    // Put the moved item back to its original position.
                    // Seems we need to do this by hand. Can't see any way to do it with jQuery UI. ??
                    var pos = addMaintainerForm.getPosition(ui.item),
                        nextpos = pos + 1,
                        maintainerships = $('#dragDropList'),
                        next = addMaintainerForm.findMaintainership('position', nextpos);

                    if (next.length) {
                        ui.item.insertBefore(next);
                    }
                    else {
                        ui.item.appendTo(maintainerships);
                    }

                    alert(addMaintainerForm.reorderMaintainersAlert);
                }
            }
        });
    },

    // On page load, associate data with each maintainership element. Then we don't
    // have to keep retrieving data from or modifying the DOM as we manipulate the
    // maintainerships.
    initMaintainershipData: function() {
        $('.maintainership').each(function(index) {
            $(this).data(maintainershipData[index]);

            // RY We might still need position to put back an element after reordering
            // failure. Rank might already have been reset? Check.
            // We also may need position to implement undo links: we want the removed maintainership
            // to show up in the list, but it has no rank.
            $(this).data('position', index+1);
        });
    },

    getPosition: function(maintainership) {
        return $(maintainership).data('position');
    },

    setPosition: function(maintainership, pos) {
        $(maintainership).data('position', pos);
    },

    findMaintainership: function(key, value) {
        var matchingMaintainership = $(); // if we don't find one, return an empty jQuery set

        $('.maintainership').each(function() {
            var maintainership = $(this);
            if ( maintainership.data(key) === value ) {
                matchingMaintainership = maintainership;
                return false; // stop the loop
            }
        });

        return matchingMaintainership;
    },


    /* *** Event listeners *** */

    bindEventListeners: function() {

        this.showFormButton.click(function() {
            addMaintainerForm.initFormView();
            return false;
        });

        this.form.submit(function() {
            // NB Important JavaScript scope issue: if we call it this way, this = addMaintainerForm
            // in prepareSubmit. If we do this.form.submit(this.prepareSubmit); then
            // this != addMaintainerForm in prepareSubmit.
            $selectedObj = addMaintainerForm.form.find('input.acSelector');
            addMaintainerForm.deleteAcHelpText($selectedObj);
			addMaintainerForm.prepareSubmit();
        });

        this.lastNameField.blur(function() {
            // Cases where this event should be ignored:
            // 1. personUri field has a value: the autocomplete select event has already fired.
            // 2. The last name field is empty (especially since the field has focus when the form is displayed).
            // 3. Autocomplete suggestions are showing.
            if ( addMaintainerForm.personUriField.val() || !$(this).val() || $('ul.ui-autocomplete li.ui-menu-item').length ) {
                return;
            }
            addMaintainerForm.onLastNameChange();
        });

        this.personLink.click(function() {
            window.open($(this).attr('href'), 'verifyMatchWindow', 'width=640,height=640,scrollbars=yes,resizable=yes,status=yes,toolbar=no,menubar=no,location=no');
            return false;
        });

    	this.acSelector.focus(function() {
        	addMaintainerForm.deleteAcHelpText(this);
    	});

    	this.acSelector.blur(function() {
        	addMaintainerForm.addAcHelpText(this);
    	});

        // When hitting enter in last name field, show first and middle name fields.
        // NB This event fires when selecting an autocomplete suggestion with the enter
        // key. Since it fires first, we undo its effects in the ac select event listener.
        this.lastNameField.keydown(function(event) {
            if (event.which === 13) {
                addMaintainerForm.onLastNameChange();
                return false; // don't submit form
            }
        });

        this.removeMaintainershipLinks.click(function() {
            addMaintainerForm.removeMaintainership(this);
            return false;
        });

    },

    prepareSubmit: function() {
        var firstName,
            middleName,
            lastName,
            name;

        // If selecting an existing person, don't submit name fields
        if (this.personUriField.val() != '' ) {
            this.firstNameField.attr('disabled', 'disabled');
            this.middleNameField.attr('disabled', 'disabled');
            this.lastNameField.attr('disabled', 'disabled');
        }
        else {
            firstName = this.firstNameField.val();
            middleName = this.middleNameField.val();
            lastName = this.lastNameField.val();

            name = lastName;
            if (firstName) {
                name += ', ' + firstName;
            }
            if (middleName) {
                name += ' ' + middleName;
            }

            this.labelField.val(name);
        }

    },

    onLastNameChange: function() {
        this.showFieldsForNewPerson();
        this.firstNameField.focus();
        // this.fixNames();
    },

    removeMaintainership: function(link) {
        // RY Upgrade this to a modal window

        maintainerName = $(link).prev().children().text();

        var removeLast = false,
            message = addMaintainerForm.removeMaintainershipMessage + '\n\n' + maintainerName + ' ?\n\n';
        if (!confirm(message)) {
            return false;
        }

        if ( addMaintainerForm.showFormButtonWrapper.is(':visible') ) {
            addMaintainerForm.returnLink.hide();
            $('img#indicatorOne').removeClass('hidden');
            addMaintainerForm.showFormButton.addClass('disabledSubmit');
            addMaintainerForm.showFormButton.attr('disabled','disabled');
        }
        else {
            addMaintainerForm.cancel.hide();
            $('img#indicatorTwo').removeClass('hidden');
            addMaintainerForm.submit.addClass('disabledSubmit');
            addMaintainerForm.submit.attr('disabled','disabled');
        }

        if ($(link)[0] === $('.remove:last')[0]) {
            removeLast = true;
        }

        $.ajax({
            url: $(link).attr('href'),
            type: 'POST',
            data: {
                deletion: $(link).parents('.maintainership').data('maintainershipUri')
            },
            dataType: 'json',
            context: link, // context for callback
            complete: function(request, status) {
                var maintainership,
                    maintainerUri;

                if (status === 'success') {

                    maintainership = $(this).parents('.maintainership');

                    // Clear autocomplete cache entries matching this maintainer's name, else
                    // autocomplete will be retrieved from the cache, which excludes the removed maintainer.
                    addMaintainerForm.clearAcCacheEntries(maintainership.data('maintainerName'));

                    // Remove this maintainer from the acFilter so it is included in autocomplete
                    // results again.
                    addMaintainerForm.removeMaintainerFromAcFilter(maintainership.data('maintainerUri'));

                    maintainership.fadeOut(400, function() {
                        var numMaintainers;

                        // For undo link: add to a deletedMaintainerships array

                        // Remove from the DOM
                        $(this).remove();

                        // Actions that depend on the maintainer having been removed from the DOM:
                        numMaintainers = $('.maintainership').length; // retrieve the length after removing maintainership from the DOM

                        // If removed item not last, reorder to remove any gaps
                        if (numMaintainers > 0 && ! removeLast) {
                            addMaintainerForm.reorderMaintainers();
                        }

                        // If fewer than two maintainers remaining, disable drag-drop
                        if (numMaintainers < 2) {
                            addMaintainerForm.disableMaintainerDD();
                        }

                        if ( $('img#indicatorOne').is(':visible') ) {
                            $('img#indicatorOne').fadeOut(100, function() {
                                $(this).addClass('hidden');
                            });

                            addMaintainerForm.returnLink.fadeIn(100, function() {
                                $(this).show();
                            });
                            addMaintainerForm.showFormButton.removeClass('disabledSubmit');
                            addMaintainerForm.showFormButton.attr('disabled',false);
                        }
                        else {
                            $('img#indicatorTwo').fadeOut(100, function() {
                                 $(this).addClass('hidden');
                             });

                             addMaintainerForm.cancel.fadeIn(100, function() {
                                 $(this).show();
                             });
                             addMaintainerForm.submit.removeClass('disabledSubmit');
                             addMaintainerForm.submit.attr('disabled',false);
                        }
                    });

                } else {
                    alert(addMaintainerForm.removeMaintainershipAlert);

                }
            }
        });
    },

    // Disable DD and associated cues if only one maintainer remains
    disableMaintainerDD: function() {
        var maintainerships = $('#dragDropList'),
            maintainerNameWrapper = $('.maintainerNameWrapper');

        maintainerships.sortable({ disable: true } );

        // Use class dd rather than jQuery UI's class ui-sortable, so that we can remove
        // the class if there's fewer than one maintainer. We don't want to remove the ui-sortable
        // class, in case we want to re-enable DD without a page reload (e.g., if implementing
        // adding an maintainer via Ajax request).
        maintainerships.removeClass('dd');

        maintainerNameWrapper.removeAttr('title');
    },

    // RY To be implemented later.
    toggleRemoveLink: function() {
        // when clicking remove: remove the maintainer, and change link text to 'undo'
        // when clicking undo: add the maintainer back, and change link text to 'remove'
    },

	// Set the initial help text in the lastName field and change the class name.
	addAcHelpText: function(selectedObj) {
        var typeText;
        if ( $(selectedObj).attr('id') == "lastName" ) {
            typeText = addMaintainerForm.maintainerTypeText;
        }

        if (!$(selectedObj).val()) {
			$(selectedObj).val(addMaintainerForm.helpTextSelect + " " + typeText + " " + addMaintainerForm.helpTextAdd)
						   .addClass(this.acHelpTextClass);
		}
	},

	deleteAcHelpText: function(selectedObj) {
	    if ($(selectedObj).hasClass(this.acHelpTextClass)) {
	            $(selectedObj).val('')
	                          .removeClass(this.acHelpTextClass);
	    }
	},

    // we need to set the correct class names for fields like the acSelector, acSelection, etc.
    // as well as clear and disable fields, call other functions ...
	setMaintainerType: function(authType) {
        if ( authType == "person" ) {
	        this.personSection.show();
            this.acSelector.addClass("acSelector");
            this.personRadio.prop('checked', true);  // needed for reset when cancel button is clicked
	        this.selectedMaintainer.addClass("acSelection");
	        this.selectedMaintainerName.addClass("acSelectionInfo");
	        this.personLink.addClass("verifyMatch");
	        this.acSelector.attr('disabled', false);
	        this.firstNameField.attr('disabled', false);
	        this.middleNameField.attr('disabled', false);
	        this.lastNameField.attr('disabled', false);

	        addMaintainerForm.addAcHelpText(this.acSelector);
	        addMaintainerForm.initAutocomplete();
	    }
    }
};

$(document).ready(function() {
    addMaintainerForm.onLoad();
});

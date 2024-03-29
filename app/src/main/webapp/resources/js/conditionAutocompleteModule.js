"use strict";

var conditionAutocompleteModule = (function ($) {

    function initConditionAutocomplete (element, onChange) {

        var autoCompleteFixSet = function() {
            $(this).attr('arrayExpressAutocomplete', 'off');
        };

        var autoCompleteFixUnset = function() {
            $(this).removeAttr('arrayExpressAutocomplete');
        };

        $(element)
            // don't navigate away from the field on tab when selecting an item
            .bind( 'keydown', function( event ) {
                if (event.keyCode === $.ui.keyCode.TAB &&
                    $(this).data('ui-autocomplete').menu.active ) {
                    event.preventDefault();
                }
            })
            .jsonTagEditor({
                autocomplete: {
                    plugin: 'arrayExpressAutocomplete',
                    urlOrData:  'https://www.ebi.ac.uk/biostudies/api/v1/autocomplete/keywords',
                    matchContains: false,
                    selectFirst: false,
                    scroll: true,
                    max: 50,
                    requestTreeUrl: 'https://www.ebi.ac.uk/biostudies/api/v1/autocomplete/efotree',
                    width: 300
                },
                onChange: onChange,
                placeholder: 'Enter condition query...',
                maxLength: 100,
                maxTagLength: 20
            })
            .focus(autoCompleteFixSet)
            .blur(autoCompleteFixUnset)
            .removeAttr('arrayExpressAutocomplete');

    }

    return {
        init: initConditionAutocomplete
    };

})(jQuery);


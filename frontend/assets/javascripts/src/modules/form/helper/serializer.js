define(['src/utils/helper'], function (utilsHelper) {

    /**
     * serialize form elements
     * this is enables other properties to be mixed in. In the case of stripe we add in the payment token
     * @param elems
     * @param mixin
     * @returns {{}}
     */
    var serializer = function (elems, mixin) {
        var data = {};

        if (mixin) { utilsHelper.extend(data, mixin); }

        elems.filter(function (elem) {
            return elem.name !== '' && elem.type && (elem.type !== 'checkbox' && elem.type !== 'radio' || elem.checked);
        }).map(function (elem) {
            data[elem.name] = elem.value;
        });

        return data;
    };

    return serializer;
});
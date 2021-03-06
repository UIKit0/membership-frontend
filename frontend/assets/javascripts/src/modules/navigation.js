define(['src/utils/user'], function (userUtil) {

    var MENU_ICON = '.js-menu-icon';
    var NAVIGATION ='.js-global-nav';
    var MEMBERS_AREA = '.js-members-area';
    var ACTIVE_CLASS = 'is-active';
    var HIDDEN_CLASS = 'is-hidden';

    function addListeners(menuEl, navigationEl) {
        menuEl.addEventListener('click', function(event) {
            event.preventDefault();
            menuEl.classList.toggle(ACTIVE_CLASS);
            navigationEl.classList.toggle(ACTIVE_CLASS);
        });
    }

    function showMembersArea() {
        var membersAreaLink = document.querySelector(MEMBERS_AREA);
        if (userUtil.getUserFromCookie() && membersAreaLink) {
            membersAreaLink.classList.remove(HIDDEN_CLASS);
        }
    }

    function init() {
        var menuEl = document.querySelector(MENU_ICON),
            navigationEl = document.querySelector(NAVIGATION);
        if (menuEl) {
            addListeners(menuEl, navigationEl);
            showMembersArea();
        }
    }

    return {
        init: init
    };

});

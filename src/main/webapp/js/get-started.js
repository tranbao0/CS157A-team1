// opens get-started dialog as modal right after sign-up, browser then keeps focus inside and Esc closes it
(function () {
  'use strict';

  var dialog = document.getElementById('get-started');
  // showModal is missing only in very old browsers, there dialog just stays closed and Profile page still works
  if (dialog !== null && typeof dialog.showModal === 'function') {
    dialog.showModal();
  }
})();

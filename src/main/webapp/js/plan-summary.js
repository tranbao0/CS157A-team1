// plan card in catalog rail and My plan page remember minimized warnings, rail card also reloads after each add
(function () {
  'use strict';

  var PREFIX = 'coursecraft-warn-';

  // reads saved state, localStorage can throw in some private browsing modes so failure means open
  function isMinimized(key) {
    try {
      return window.localStorage.getItem(PREFIX + key) === 'closed';
    } catch (err) {
      return false;
    }
  }

  // saves state, nothing breaks when browser blocks localStorage, warning just opens again next time
  function saveMinimized(key, minimized) {
    try {
      if (minimized) {
        window.localStorage.setItem(PREFIX + key, 'closed');
      } else {
        window.localStorage.removeItem(PREFIX + key);
      }
    } catch (err) {
      // does nothing on purpose, see comment above
    }
  }

  // closes every warning student minimized before, server always sends them open
  function restore() {
    var warnings = document.querySelectorAll('.plan-warn[data-warn-key]');
    for (var i = 0; i < warnings.length; i++) {
      if (isMinimized(warnings[i].getAttribute('data-warn-key'))) {
        warnings[i].open = false;
      }
    }
  }

  // toggle fires on <details> when it opens or closes, it does not bubble so listen in capture phase
  document.addEventListener('toggle', function (e) {
    var details = e.target;
    if (!details.classList || !details.classList.contains('plan-warn')) {
      return;
    }
    saveMinimized(details.getAttribute('data-warn-key'), !details.open);
  }, true);

  // plan-add.js sends plan-changed after each add, so fetch fresh card HTML from server
  document.addEventListener('plan-changed', function () {
    fetch('plan/summary').then(function (response) {
      // keeps old card on error, page still works
      if (!response.ok) {
        return null;
      }
      return response.text();
    }).then(function (html) {
      var box = document.getElementById('plan-summary');
      if (html === null || box === null) {
        return;
      }
      // HTML comes from plan-summary.jsp, which escapes every database value with c:out
      box.innerHTML = html;
      restore();
    }).catch(function () {
      // keeps old card when network fails, next page load fixes it
    });
  });

  restore();
})();

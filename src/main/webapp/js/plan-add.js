// + Plan buttons send row's course and session to POST /plan/add, page does not reload
(function () {
  'use strict';

  // removes busy look, runs on every answer and on network failure
  function stopBusy(btn) {
    btn.classList.remove('is-busy');
    btn.removeAttribute('aria-busy');
  }

  // course is in plan now, so other sections of same course show In plan like after page reload
  function markOtherSections(btn) {
    var courseId = btn.getAttribute('data-course-id');
    var all = document.querySelectorAll('.js-plan-add');
    for (var i = 0; i < all.length; i++) {
      if (all[i] !== btn && all[i].getAttribute('data-course-id') === courseId) {
        all[i].disabled = true;
        all[i].classList.add('is-added');
        all[i].textContent = 'In plan';
      }
    }
  }

  function add(btn) {
    // body is form-encoded, so servlet reads it with getParameter like normal form
    var body = new URLSearchParams();
    body.append('courseId', btn.getAttribute('data-course-id'));
    body.append('sessionId', btn.getAttribute('data-session-id'));

    // button stays busy until server answers, so double click cannot add twice
    btn.disabled = true;
    btn.classList.add('is-busy');
    btn.setAttribute('aria-busy', 'true');
    window.hideToast();

    // URL has no leading slash, so /coursecraft/catalog.jsp sends to /coursecraft/plan/add
    fetch('plan/add', {
      method: 'POST',
      headers: { 'Accept': 'application/json' },
      body: body
    }).then(function (response) {
      stopBusy(btn);

      // sign-in keeps current page and query after session ends
      if (response.status === 401) {
        // pages sit one path part below context, so last slash ends context
        var path = window.location.pathname;
        var next = path.substring(path.lastIndexOf('/')) + window.location.search;
        window.location.href = 'login?next=' + encodeURIComponent(next);
        return;
      }

      if (response.ok) {
        btn.classList.add('is-added');
        btn.textContent = 'Added';
        markOtherSections(btn);
        // plan-summary.js listens for this and reloads plan card
        document.dispatchEvent(new CustomEvent('plan-changed'));
      } else {
        refreshAvailability();
      }

      // 200, 400, 409 and 422 come with JSON message from PlanAddServlet, 500 is HTML error page
      // 200 message tells term's new unit total and part-time or petition warning
      var type = response.headers.get('Content-Type');
      if (type !== null && type.indexOf('application/json') === 0) {
        return response.json().then(function (data) {
          window.showToast(data.message, response.ok ? 'ok' : 'error');
        });
      }
      window.showToast('Could not add course, try again.', 'error');
    }).catch(function () {
      // fetch only lands here when request never got answer
      stopBusy(btn);
      refreshAvailability();
      window.showToast('Could not reach server, check connection and try again.', 'error');
    });
  }

  document.addEventListener('submit', function (event) {
    var form = event.target.closest('.plan-action__form');
    if (!form) { return; }
    event.preventDefault();
    var button = form.querySelector('.js-plan-add');
    if (!button.disabled && button.getAttribute('aria-disabled') !== 'true') { add(button); }
  });
  var refreshVersion = 0;
  function refreshAvailability() {
    var version = ++refreshVersion;
    var activeId = null;
    var active = document.activeElement.closest('[data-plan-action]');
    if (active) { activeId = active.id; }
    document.querySelectorAll('.js-plan-add').forEach(function (button) { button.disabled = true; });
    fetch(window.location.href, { cache: 'no-store', headers: { 'Accept': 'text/html' } })
      .then(function (response) {
        if (!response.ok) { throw new Error('Could not check prerequisites'); }
        return response.text();
      }).then(function (html) {
        if (version !== refreshVersion) { return; }
        var page = new DOMParser().parseFromString(html, 'text/html');
        var actions = Array.from(document.querySelectorAll('[data-plan-action]'));
        if (actions.some(function (action) { return !page.getElementById(action.id); })) {
          throw new Error('Plan actions are missing');
        }
        actions.forEach(function (action) { action.replaceWith(page.getElementById(action.id)); });
        if (activeId) {
          var replacement = document.getElementById(activeId);
          var control = replacement && replacement.querySelector('button:not(:disabled), [tabindex="0"]');
          if (control) { control.focus({ preventScroll: true }); }
        }
      }).catch(function () {
        if (version !== refreshVersion) { return; }
        document.querySelectorAll('[data-plan-action]').forEach(function (action) {
          var button = action.querySelector('.js-plan-add');
          if (!button || button.classList.contains('is-added')) { return; }
          var reason = action.querySelector('.plan-action__reason');
          if (!reason) {
            reason = document.createElement('p');
            reason.className = 'plan-action__reason';
            reason.id = action.id + '-reason';
            reason.tabIndex = 0;
            action.appendChild(reason);
          }
          button.disabled = true;
          button.setAttribute('aria-disabled', 'true');
          button.setAttribute('aria-describedby', reason.id);
          reason.textContent = 'Could not check prerequisites. Reload the page to try again.';
        });
      });
  }
  document.addEventListener('plan-changed', refreshAvailability);
  window.addEventListener('pageshow', function (event) {
    if (event.persisted) { refreshAvailability(); }
  });
  window.addEventListener('focus', refreshAvailability);
})();

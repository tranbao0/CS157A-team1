// + Plan buttons send row's course and session to POST /plan/add, page does not reload
(function () {
  'use strict';

  // button works again after refusal, and gets focus back that it lost while it was disabled
  function giveBack(btn) {
    btn.disabled = false;
    if (document.activeElement === document.body) {
      btn.focus();
    }
  }

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
        giveBack(btn);
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
      giveBack(btn);
      window.showToast('Could not reach server, check connection and try again.', 'error');
    });
  }

  document.addEventListener('click', function (e) {
    // closest() walks up from clicked element, so click on button text still finds button
    var btn = e.target.closest('.js-plan-add');
    if (btn !== null && !btn.disabled) {
      add(btn);
    }
  });
})();

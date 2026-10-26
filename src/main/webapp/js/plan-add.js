// + Plan buttons send row's course and session to POST /plan/add, page does not reload
(function () {
  'use strict';

  // puts text in status area under page, role="status" makes screen readers read it out loud
  function showMessage(text) {
    var box = document.getElementById('plan-message');
    if (box !== null) {
      box.textContent = text;
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
    showMessage('');

    // URL has no leading slash, so /coursecraft/catalog.jsp sends to /coursecraft/plan/add
    fetch('plan/add', {
      method: 'POST',
      headers: { 'Accept': 'application/json' },
      body: body
    }).then(function (response) {
      stopBusy(btn);

      // sign-in ran out, login page brings student back to catalog after
      if (response.status === 401) {
        window.location.href = 'login';
        return;
      }

      if (response.ok) {
        btn.classList.add('is-added');
        btn.textContent = 'Added';
        markOtherSections(btn);
        // plan-summary.js listens for this and reloads plan card
        document.dispatchEvent(new CustomEvent('plan-changed'));
      } else {
        btn.disabled = false;
      }

      // 200, 400, 409 and 422 come with JSON message from PlanAddServlet, 500 is HTML error page
      // 200 message tells term's new unit total and part-time or petition warning
      var type = response.headers.get('Content-Type');
      if (type !== null && type.indexOf('application/json') === 0) {
        return response.json().then(function (data) {
          showMessage(data.message);
        });
      }
      showMessage('Could not add course, try again.');
    }).catch(function () {
      // fetch only lands here when request never got answer
      stopBusy(btn);
      btn.disabled = false;
      showMessage('Could not reach server, check connection and try again.');
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

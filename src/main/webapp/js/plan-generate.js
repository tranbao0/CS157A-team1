(function () {
  'use strict';

  var form = document.getElementById('plan-generate');
  var preview = document.getElementById('plan-preview-status');
  var button = form.querySelector('button');

  form.addEventListener('submit', function (event) {
    event.preventDefault();
    button.disabled = true;
    preview.textContent = '';
    fetch(form.action, {
      method: 'POST',
      headers: { 'Accept': 'application/json' }
    }).then(function (response) {
      if (response.status === 401) {
        window.location.href = 'login?next=' + encodeURIComponent('/plan');
        return;
      }
      if (!response.ok) {
        throw new Error('Could not generate plan.');
      }
      return response.json().then(function (data) {
        if (data.terms.length === 0) {
          preview.textContent = 'No courses to add yet.';
        }
      });
    }).catch(function () {
      preview.textContent = 'Could not generate plan. Try again.';
    }).finally(function () {
      button.disabled = false;
    });
  });
})();

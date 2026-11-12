// Completion edits post the saved course list and search results through the transcript form.
(function () {
  'use strict';
  var page = document.querySelector('.progress-page, .completed-panel');
  if (!page) { return; }
  var edit = document.getElementById('progress-edit');
  var save = document.getElementById('progress-save');
  var help = document.getElementById('progress-edit-help');
  if (!edit || !save || !help) { return; }
  var checks = Array.from(page.querySelectorAll('.progress-check'));
  var initial = checks.map(function (input) { return input.checked; });
  page.classList.add('has-progress-script');
  edit.hidden = false;
  save.hidden = true;
  help.hidden = true;

  function editing(enabled) {
    page.classList.toggle('is-editing', enabled);
    edit.setAttribute('aria-pressed', String(enabled));
    edit.textContent = enabled ? 'Done editing' : 'Edit completed courses';
    help.hidden = !enabled;
  }
  edit.addEventListener('click', function () {
    var enabled = edit.getAttribute('aria-pressed') !== 'true';
    editing(enabled);
    var fold = document.getElementById('completed-courses');
    if (fold && enabled) { fold.open = true; }
  });
  checks.forEach(function (input) {
    input.addEventListener('change', function () {
      var dirty = checks.some(function (check, index) { return check.checked !== initial[index]; });
      save.hidden = !dirty;
      save.classList.toggle('btn--primary', dirty);
    });
  });
  var completedFold = document.getElementById('completed-courses');
  if (completedFold && window.location.hash === '#completed-courses') { completedFold.open = true; }
}());

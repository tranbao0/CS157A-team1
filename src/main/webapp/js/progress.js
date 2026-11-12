// Completion edits use the course's existing row and the transcript save form.
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
  page.querySelectorAll('[data-edit-completed]').forEach(function (link) {
    link.addEventListener('click', function (event) {
      event.preventDefault();
      editing(true);
      var input = document.getElementById('complete-' + link.dataset.editCompleted);
      var fold = input.closest('details');
      if (fold) { fold.open = true; }
      input.focus();
      input.scrollIntoView({ block: 'center' });
    });
  });
  var completedFold = document.getElementById('completed-courses');
  if (completedFold && window.location.hash === '#completed-courses') { completedFold.open = true; }
  page.querySelectorAll('[data-edit-course]').forEach(function (link) {
    link.addEventListener('click', function () {
      var row = document.getElementById('progress-course-' + link.dataset.editCourse);
      if (!row) { return; }
      editing(true);
      var fold = row.closest('details');
      if (fold) { fold.open = true; }
      window.requestAnimationFrame(function () {
        row.querySelector('.progress-check').focus();
        row.scrollIntoView({ block: 'center' });
      });
    });
  });
}());

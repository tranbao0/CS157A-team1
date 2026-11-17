(function () {
  'use strict';

  var form = document.getElementById('plan-generate');
  var status = document.getElementById('plan-preview-status');
  var results = document.getElementById('plan-preview-results');
  var button = form.querySelector('button');

  function element(tag, className, text) {
    var node = document.createElement(tag);
    node.className = className;
    if (text !== undefined && text !== null) {
      node.textContent = text;
    }
    return node;
  }

  function courseRow(course) {
    var row = element('li', 'tx-row');
    var label = element('div', 'tx-label tx-label--static');
    label.appendChild(element('span', 'tx-code', course.code));
    if (course.title) {
      label.appendChild(element('span', 'tx-title', course.title));
    }
    label.appendChild(element('span', 'tx-meta', course.unitsText + ' units'));
    row.appendChild(label);
    return row;
  }

  function unitWarning(term) {
    var warning = element('details', 'plan-warn plan-warn--' + term.unitWarningKind);
    warning.open = true;
    var summary = element('summary', 'plan-warn__summary');
    summary.appendChild(element('span', 'plan-warn__title', term.unitWarningTitle));
    var toggle = element('span', 'plan-warn__toggle');
    toggle.appendChild(element('span', 'plan-warn__hide', 'Minimize'));
    toggle.appendChild(element('span', 'plan-warn__show', 'Show'));
    summary.appendChild(toggle);
    warning.appendChild(summary);
    warning.appendChild(element('p', 'plan-warn__text', term.unitWarningText));
    return warning;
  }

  function showTerm(term) {
    var section = element('section', 'plan-preview-term');
    var heading = element('div', 'tx-section__head');
    var title = element('h2', 'tx-section__title', term.label);
    title.id = 'preview-term-' + term.termId;
    section.setAttribute('aria-labelledby', title.id);
    heading.appendChild(title);
    heading.appendChild(element('span', 'tx-group__count', term.unitsText + ' units'));
    section.appendChild(heading);
    if (term.unitWarningTitle) {
      section.appendChild(unitWarning(term));
    }
    var list = element('ul', 'tx-list');
    for (var i = 0; i < term.entries.length; i++) {
      var row = courseRow(term.entries[i]);
      row.appendChild(element('span', 'plan-preview-new', 'New'));
      list.appendChild(row);
    }
    section.appendChild(list);
    results.appendChild(section);
  }

  function showUnplaced(courses) {
    var section = element('section', 'plan-preview-term');
    var title = element('h2', 'tx-section__title', 'Could not place');
    title.id = 'preview-unplaced';
    section.setAttribute('aria-labelledby', title.id);
    section.appendChild(title);
    var list = element('ul', 'tx-list');
    for (var i = 0; i < courses.length; i++) {
      var row = courseRow(courses[i]);
      row.querySelector('.tx-label').appendChild(element('span', 'tx-meta', courses[i].reason));
      list.appendChild(row);
    }
    section.appendChild(list);
    results.appendChild(section);
  }

  function showPreview(data) {
    for (var i = 0; i < data.terms.length; i++) {
      showTerm(data.terms[i]);
    }
    if (data.unplaced.length > 0) {
      showUnplaced(data.unplaced);
    }
    if (data.terms.length === 0 && data.unplaced.length === 0) {
      status.textContent = 'No courses to add yet.';
    }
  }

  form.addEventListener('submit', function (event) {
    event.preventDefault();
    button.disabled = true;
    status.textContent = '';
    results.replaceChildren();
    results.setAttribute('aria-busy', 'true');
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
      return response.json().then(showPreview);
    }).catch(function () {
      status.textContent = 'Could not generate plan. Try again.';
    }).finally(function () {
      button.disabled = false;
      results.removeAttribute('aria-busy');
    });
  });
})();

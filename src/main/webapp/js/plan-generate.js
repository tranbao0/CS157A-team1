(function () {
  'use strict';

  var form = document.getElementById('plan-generate');
  var status = document.getElementById('plan-preview-status');
  var results = document.getElementById('plan-terms');
  var restoreNodes = [];
  // page can show at /plan and /plan/remove, so every target is an absolute url from the page
  var saveUrl = form.dataset.saveUrl;
  var planUrl = form.dataset.planUrl;
  var loginUrl = form.dataset.loginUrl + '?next=' + encodeURIComponent('/plan');
  var button = form.querySelector('button');
  var actions = document.getElementById('plan-preview-actions');
  var saveButton = document.getElementById('plan-save');
  var cancelButton = document.getElementById('plan-cancel');
  // proposals the preview shows, each has courseId, sessionId and suggested
  var proposals = [];

  function element(tag, className, text) {
    var node = document.createElement(tag);
    node.className = className;
    if (text !== undefined && text !== null) {
      node.textContent = text;
    }
    return node;
  }

  function markPreview(node) {
    node.dataset.previewAdded = '';
    return node;
  }

  function detachSaved(node) {
    var place = document.createComment('');
    node.replaceWith(place);
    restoreNodes.push(function () {
      place.replaceWith(node);
    });
    return place;
  }

  function undoPreview() {
    results.querySelectorAll('[data-preview-added]').forEach(function (node) {
      node.remove();
    });
    restoreNodes.forEach(function (restore) {
      restore();
    });
    restoreNodes = [];
    proposals = [];
    actions.hidden = true;
    window.CourseCraftWarnings.sync();
  }

  function courseRow(course, isNew) {
    var row = element('li', 'tx-row');
    var label = element('div', 'tx-label tx-label--static');
    var name = label;
    if (isNew) {
      name = element('a', 'course-link progress-course-link');
      name.href = results.dataset.courseUrl + '?id=' + encodeURIComponent(course.courseId);
      label.appendChild(name);
    }
    name.appendChild(element('span', 'tx-code', course.code));
    if (course.title) {
      name.appendChild(element('span', 'tx-title', course.title));
    }
    label.appendChild(element('span', 'tx-meta', course.unitsText + ' units'));
    row.appendChild(label);
    if (isNew) {
      row.classList.add('saved-course');
      markPreview(row);
      row.appendChild(element('span', 'plan-preview-new', course.suggested ? 'Suggested' : 'New'));
    }
    return row;
  }

  function unitWarning(term) {
    var warning = element('details', 'plan-warn plan-warn--' + term.unitWarningKind);
    warning.dataset.warnKey = term.unitWarningKey;
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

  function createTerm(term) {
    var section = markPreview(element('section', 'saved-term'));
    section.id = 'plan-term-' + term.termId;
    section.dataset.planTerm = term.termId;
    section.dataset.termOrder = term.termOrder;
    section.dataset.previewTerm = '';
    var heading = element('div', 'tx-section__head');
    var title = element('h2', 'tx-section__title', term.label);
    title.id = 'plan-title-' + term.termId;
    section.setAttribute('aria-labelledby', title.id);
    heading.appendChild(title);
    heading.appendChild(element('span', 'tx-group__count'));
    section.appendChild(heading);
    section.appendChild(element('ul', 'tx-list'));
    return section;
  }

  function showTerm(term) {
    var section = document.getElementById('plan-term-' + term.termId);
    if (!section) {
      section = createTerm(term);
      var neighbors = results.querySelectorAll('[data-term-order]');
      var next = Array.from(neighbors).find(function (neighbor) {
        return Number(neighbor.dataset.termOrder) > term.termOrder;
      });
      results.insertBefore(section, next || null);
    }
    var oldCount = section.querySelector('.tx-group__count');
    var count = markPreview(element('span', 'tx-group__count', term.unitsText + ' units'));
    if (term.gradTerm) {
      count.textContent += ' · Expected graduation';
    }
    if (section.hasAttribute('data-preview-term')) {
      oldCount.replaceWith(count);
    } else {
      detachSaved(oldCount).before(count);
    }
    var list = section.querySelector('.tx-list');
    var warning = section.querySelector('.plan-warn--part-time, .plan-warn--petition');
    if (warning) {
      detachSaved(warning);
    }
    if (term.unitWarningTitle) {
      section.insertBefore(markPreview(unitWarning(term)), section.querySelector('.plan-warn--depends') || list);
    }
    for (var i = 0; i < term.entries.length; i++) {
      list.appendChild(courseRow(term.entries[i], true));
    }
  }

  function updateTermLinks() {
    var empty = results.querySelector('.tx-empty');
    if (empty) {
      detachSaved(empty);
    }
    var nav = results.querySelector('.plan-term-links');
    if (!nav) {
      nav = markPreview(element('nav', 'plan-term-links'));
      nav.setAttribute('aria-label', 'Plan terms');
      results.prepend(nav);
    }
    var sections = results.querySelectorAll('[data-plan-term]');
    var next = null;
    for (var i = sections.length - 1; i >= 0; i--) {
      var section = sections[i];
      var href = '#' + section.id;
      if (section.hasAttribute('data-preview-term')) {
        var link = markPreview(element('a', 'chip', section.querySelector('h2').textContent));
        link.href = href;
        nav.insertBefore(link, next);
        next = link;
      } else {
        next = nav.querySelector('a[href="' + href + '"]');
      }
    }
  }

  function showUnplaced(courses) {
    var section = markPreview(element('section', 'plan-preview-term'));
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
      for (var j = 0; j < data.terms[i].entries.length; j++) {
        proposals.push(data.terms[i].entries[j]);
      }
    }
    if (data.terms.length > 0) {
      updateTermLinks();
    }
    if (data.unplaced.length > 0) {
      showUnplaced(data.unplaced);
    }
    if (data.terms.length === 0 && data.unplaced.length === 0) {
      status.textContent = 'No courses to add yet.';
    }
    // Cancel shows for any preview, Save only when there is something to save
    actions.hidden = data.terms.length === 0 && data.unplaced.length === 0;
    saveButton.hidden = proposals.length === 0;
    window.CourseCraftWarnings.sync();
  }

  // buttons stay off while save runs, so double click sends one request
  function setBusy(busy) {
    saveButton.disabled = busy;
    cancelButton.disabled = busy;
    button.disabled = busy;
    if (busy) {
      results.setAttribute('aria-busy', 'true');
    } else {
      results.removeAttribute('aria-busy');
    }
  }

  function savePlan() {
    // one entry per shown proposal, as courseId:sessionId:suggested
    var body = new URLSearchParams();
    for (var i = 0; i < proposals.length; i++) {
      var item = proposals[i];
      body.append('entry', item.courseId + ':' + item.sessionId + ':' + (item.suggested ? 'true' : 'false'));
    }
    setBusy(true);
    status.textContent = '';
    fetch(saveUrl, {
      method: 'POST',
      headers: { 'Accept': 'application/json' },
      body: body
    }).then(function (response) {
      if (response.status === 401) {
        window.location.href = loginUrl;
        return;
      }
      if (response.ok) {
        // plan page shows saved courses as normal rows with Remove buttons
        window.location.href = planUrl;
        return;
      }
      if (response.status === 409) {
        // plan changed since preview, so old list is no good
        return response.json().then(function (data) {
          undoPreview();
          status.textContent = data.message;
          setBusy(false);
        });
      }
      throw new Error('Could not save plan.');
    }).catch(function () {
      status.textContent = 'Could not save plan. Try again.';
      setBusy(false);
    });
  }

  saveButton.addEventListener('click', savePlan);

  cancelButton.addEventListener('click', function () {
    undoPreview();
    status.textContent = '';
  });

  form.addEventListener('submit', function (event) {
    event.preventDefault();
    button.disabled = true;
    status.textContent = '';
    undoPreview();
    results.setAttribute('aria-busy', 'true');
    fetch(form.action, {
      method: 'POST',
      headers: { 'Accept': 'application/json' }
    }).then(function (response) {
      if (response.status === 401) {
        window.location.href = loginUrl;
        return;
      }
      if (!response.ok) {
        throw new Error('Could not generate plan.');
      }
      return response.json().then(showPreview);
    }).catch(function () {
      undoPreview();
      status.textContent = 'Could not generate plan. Try again.';
    }).finally(function () {
      button.disabled = false;
      results.removeAttribute('aria-busy');
    });
  });
})();

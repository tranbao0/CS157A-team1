// The rail shares its space between the active plan and saved completed courses.
(function () {
  'use strict';
  var rail = document.getElementById('catalog-rail');
  if (!rail) { return; }
  var list = rail.querySelector('.catalog-rail-tabs');
  var tabs = Array.from(list.querySelectorAll('[data-rail-tab]'));
  list.setAttribute('role', 'tablist');
  tabs.forEach(function (tab) {
    tab.setAttribute('role', 'tab');
    tab.removeAttribute('aria-current');
    var panel = document.getElementById(tab.getAttribute('aria-controls'));
    panel.setAttribute('role', 'tabpanel');
    panel.tabIndex = 0;
  });

  function select(tab, focus, updateUrl) {
    tabs.forEach(function (item) {
      var selected = item === tab;
      item.setAttribute('aria-selected', String(selected));
      item.tabIndex = selected ? 0 : -1;
      document.getElementById(item.getAttribute('aria-controls')).hidden = !selected;
    });
    var value = tab.dataset.railTab;
    var field = document.querySelector('.search-form input[name="rail"]');
    if (field) { field.value = value; }
    if (updateUrl) {
      var url = new URL(window.location.href);
      if (value === 'completed') { url.searchParams.set('rail', 'completed'); }
      else { url.searchParams.delete('rail'); }
      window.history.replaceState(null, '', url);
    }
    if (focus) { tab.focus({ preventScroll: true }); }
  }

  tabs.forEach(function (tab, index) {
    tab.addEventListener('click', function (event) {
      event.preventDefault();
      select(tab, true, true);
    });
    tab.addEventListener('keydown', function (event) {
      var next;
      if (event.key === 'ArrowRight') { next = (index + 1) % tabs.length; }
      else if (event.key === 'ArrowLeft') { next = (index + tabs.length - 1) % tabs.length; }
      else if (event.key === 'Home') { next = 0; }
      else if (event.key === 'End') { next = tabs.length - 1; }
      else if (event.key === ' ' || event.key === 'Enter') { next = index; }
      else { return; }
      event.preventDefault();
      select(tabs[next], true, true);
    });
  });
  document.querySelectorAll('[data-open-completed]').forEach(function (link) {
    link.addEventListener('click', function (event) {
      event.preventDefault();
      select(tabs[1], true, true);
      rail.scrollIntoView({ block: 'start' });
    });
  });
  var active = tabs.find(function (tab) {
    return !document.getElementById(tab.getAttribute('aria-controls')).hidden;
  });
  select(active || tabs[0], false, false);
}());

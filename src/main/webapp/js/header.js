/*
 * Site header, same rules as landing page: slides away going down, comes back going up.
 * Also keeps --site-header-h on <html> equal to header's real height, so things stuck
 * under it (catalog's right column) know how far down to sit while it shows.
 */
(function () {
  'use strict';

  var header = document.querySelector('.site-header');
  if (!header) { return; }

  var root = document.documentElement;
  var lastY = window.scrollY;

  function show(visible) {
    if (visible) {
      header.classList.remove('is-hidden');
    } else {
      header.classList.add('is-hidden');
    }
  }

  function measure() {
    root.style.setProperty('--site-header-h', header.offsetHeight + 'px');
  }

  window.addEventListener('scroll', function () {
    var y = window.scrollY;
    var moved = y - lastY;
    lastY = y;

    // small moves are jitter, only hide once header itself is scrolled past
    if (moved > 6 && y > header.offsetHeight) { show(false); }
    if (moved < -6) { show(true); }
  }, { passive: true });

  // keyboard user tabbing into hidden header needs to see where focus went
  header.addEventListener('focusin', function () { show(true); });

  // header height changes with screen width and wrapped chips
  if ('ResizeObserver' in window) {
    new ResizeObserver(measure).observe(header);
  }
  measure();
}());

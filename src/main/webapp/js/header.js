/*
 * Site header, same rules as landing page: slides away going down, comes back going up.
 * Also keeps --site-header-h on <html> equal to header's real height, so things stuck
 * under it (catalog's right column) know how far down to sit while it shows.
 * Catalog also gets --rail-stack-h, CSS needs stack height to work out how far to push it.
 */
(function () {
  'use strict';

  var header = document.querySelector('.site-header');
  if (!header) { return; }

  var root = document.documentElement;
  // only catalog has rail stack, other pages skip that measurement
  var stack = document.querySelector('.rail__stack');
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
    if (stack) {
      root.style.setProperty('--rail-stack-h', stack.offsetHeight + 'px');
    }
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

  // header height changes with screen width and wrapped chips, stack grows when plan card gets new term
  if ('ResizeObserver' in window) {
    var observer = new ResizeObserver(measure);
    observer.observe(header);
    if (stack) {
      observer.observe(stack);
    }
  }
  measure();

  /*
   * CSS glides only once html has .is-measured. Two frames wait until browser
   * has drawn page with real sizes, so first measurement itself never animates.
   */
  window.requestAnimationFrame(function () {
    window.requestAnimationFrame(function () {
      root.classList.add('is-measured');
    });
  });
}());

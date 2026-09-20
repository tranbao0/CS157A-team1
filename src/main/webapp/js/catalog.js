/* Risk rationale popover. One element, reused across rows. */
(function () {
  'use strict';

  var GAP = 8;             // px between the button and the popover
  var EDGE = 16;           // px minimum distance from the viewport edge

  var popover = null;
  var titleEl = null;
  var bodyEl = null;
  var activeBtn = null;

  function ensurePopover() {
    if (popover) { return popover; }

    popover = document.createElement('div');
    popover.className = 'popover';
    popover.id = 'risk-popover';
    popover.hidden = true;

    titleEl = document.createElement('span');
    titleEl.className = 'popover__title';

    bodyEl = document.createElement('span');
    bodyEl.className = 'popover__body';

    popover.appendChild(titleEl);
    popover.appendChild(bodyEl);
    document.body.appendChild(popover);
    return popover;
  }

  function place(btn) {
    var anchor = btn.getBoundingClientRect();
    var box = popover.getBoundingClientRect();

    // Below the button, or above when tight.
    var top = anchor.bottom + GAP;
    if (top + box.height > window.innerHeight - EDGE) {
      top = Math.max(EDGE, anchor.top - box.height - GAP);
    }

    // Right-align, then clamp to the viewport.
    var left = anchor.right - box.width;
    left = Math.min(left, window.innerWidth - box.width - EDGE);
    left = Math.max(EDGE, left);

    popover.style.top = Math.round(top) + 'px';
    popover.style.left = Math.round(left) + 'px';
  }

  function open(btn) {
    ensurePopover();

    // textContent, never innerHTML: scraped data.
    titleEl.textContent = btn.getAttribute('data-risk-title') || 'High risk';
    bodyEl.textContent = btn.getAttribute('data-risk-body') || '';

    popover.hidden = false;
    place(btn);

    btn.setAttribute('aria-expanded', 'true');
    btn.setAttribute('aria-controls', 'risk-popover');
    activeBtn = btn;
  }

  function close(returnFocus) {
    if (!activeBtn) { return; }

    popover.hidden = true;
    activeBtn.setAttribute('aria-expanded', 'false');
    activeBtn.removeAttribute('aria-controls');

    if (returnFocus) { activeBtn.focus(); }
    activeBtn = null;
  }

  document.addEventListener('click', function (e) {
    var btn = e.target.closest ? e.target.closest('.risk-info') : null;

    if (btn) {
      e.preventDefault();
      if (activeBtn === btn) { close(false); } else { close(false); open(btn); }
      return;
    }

    if (activeBtn && !popover.contains(e.target)) { close(false); }
  });

  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape' && activeBtn) { close(true); }
  });

  // Layout shift invalidates placement.
  window.addEventListener('resize', function () { close(false); });
  window.addEventListener('scroll', function () { close(false); }, true);
}());

/*
 * Shared message box (WEB-INF/jspf/toast.jspf). Other scripts call showToast(text, kind) and hideToast().
 * "ok" message leaves by itself, longer text stays longer, timer waits while pointer or focus is inside.
 * "error" message stays until student closes it or next message takes its place.
 */
(function () {
  'use strict';

  var box = document.getElementById('toast');
  var timer;
  var leaveTimer;
  var duration = 0;
  var hovered = false;
  var leaving = false;
  // element that had focus when message came up, focus goes back there on close
  var opener = null;

  function line(kind) {
    return box.querySelector(kind === 'error' ? '.toast__text[role="alert"]' : '.toast__text[role="status"]');
  }

  function clearTimer() {
    window.clearTimeout(timer);
  }

  // box stays in page so screen readers keep listening, it just loses its text and its look
  function finishLeave() {
    if (leaving) {
      window.clearTimeout(leaveTimer);
      box.classList.remove('is-shown', 'is-leaving');
      line('ok').textContent = '';
      line('error').textContent = '';
      leaving = false;
    }
  }

  function hideToast() {
    if (!box) { return; }
    clearTimer();
    if (!box.classList.contains('is-shown') || leaving) { return; }
    // close button is about to go away, so focus moves back before that
    if (box.contains(document.activeElement) && opener !== null && document.contains(opener)) {
      opener.focus();
    }
    leaving = true;
    box.classList.add('is-leaving');
    // transitionend does not fire with reduced motion or in hidden tab, timeout covers those
    leaveTimer = window.setTimeout(finishLeave, 300);
  }

  function startTimer() {
    clearTimer();
    if (box.classList.contains('is-shown') && !leaving && duration > 0 && !hovered && !box.contains(document.activeElement)) {
      timer = window.setTimeout(hideToast, duration);
    }
  }

  function showToast(text, kind) {
    if (!box) { return; }
    if (!text) { hideToast(); return; }
    clearTimer();
    window.clearTimeout(leaveTimer);
    leaving = false;
    box.classList.remove('is-leaving');
    if (kind !== 'error') { kind = 'ok'; }

    if (!box.contains(document.activeElement)) {
      opener = document.activeElement;
    }
    box.classList.remove('toast--ok', 'toast--error');
    box.classList.add('toast--' + kind);
    // other line empties first, so only one message shows and gets read
    line(kind === 'error' ? 'ok' : 'error').textContent = '';
    line(kind).textContent = text;
    box.classList.add('is-shown');

    hovered = box.matches(':hover');
    if (kind === 'error') {
      duration = 0;
    } else {
      duration = Math.min(10000, Math.max(4000, text.length * 60));
    }
    startTimer();
  }

  window.showToast = showToast;
  window.hideToast = hideToast;
  if (!box) { return; }

  box.querySelector('.toast__close').addEventListener('click', hideToast);
  box.addEventListener('transitionend', function (event) {
    if (event.target === box && event.propertyName === 'opacity') { finishLeave(); }
  });
  box.addEventListener('pointerenter', function () { hovered = true; clearTimer(); });
  box.addEventListener('pointerleave', function () { hovered = false; startTimer(); });
  box.addEventListener('focusin', clearTimer);
  // focus is still on old element inside focusout, so timer check waits one tick
  box.addEventListener('focusout', function () { window.setTimeout(startTimer, 0); });
  document.addEventListener('keydown', function (event) {
    // defaultPrevented means something else on page already used this Escape
    if (event.key === 'Escape' && !event.defaultPrevented && box.classList.contains('is-shown')) { hideToast(); }
  });

  // message that server put in page gets same timer, and setting text again makes screen reader read it
  if (box.classList.contains('is-shown')) {
    if (box.classList.contains('toast--error')) {
      showToast(line('error').textContent, 'error');
    } else {
      showToast(line('ok').textContent, 'ok');
    }
  }
})();

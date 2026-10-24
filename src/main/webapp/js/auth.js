/*
 * Switches between sign-in and sign-up forms on auth.jsp without loading new page,
 * so road art in background keeps playing. Links still have real hrefs,
 * without JavaScript they just load /login or /register like normal.
 */
(function () {
  'use strict';

  var card = document.querySelector('.auth');
  if (!card) { return; }

  var panes = {
    login: card.querySelector('[data-auth-pane="login"]'),
    register: card.querySelector('[data-auth-pane="register"]')
  };

  // server already hid one pane, so visible one is starting mode
  var currentMode = 'login';
  if (panes.login.hidden) {
    currentMode = 'register';
  }

  // Back and Forward only bring back URL, so mode comes from path
  function modeFromUrl() {
    if (window.location.pathname.endsWith('/register')) {
      return 'register';
    }
    return 'login';
  }

  // fade-in class only needs to stay until it plays once
  card.addEventListener('animationend', function (event) {
    event.target.classList.remove('auth__pane--enter');
  });

  function show(mode) {
    if (mode === currentMode) { return; }

    var oldPane = panes[currentMode];
    var newPane = panes[mode];

    // email typed in one form carries over, so student does not type it twice
    var oldEmail = oldPane.querySelector('input[name="email"]');
    var newEmail = newPane.querySelector('input[name="email"]');
    if (newEmail.value === '') {
      newEmail.value = oldEmail.value;
    }

    // error came from submitting old form, it means nothing on other one
    var error = oldPane.querySelector('[data-auth-error]');
    if (error) {
      error.remove();
    }

    // both forms are same height, so card holds still and only content fades in
    oldPane.hidden = true;
    newPane.hidden = false;
    newPane.classList.add('auth__pane--enter');

    currentMode = mode;
    document.title = newPane.getAttribute('data-auth-title');

    // keyboard and screen reader users land on new heading instead of hidden link
    newPane.querySelector('h1').focus();
  }

  card.addEventListener('click', function (event) {
    var link = event.target.closest('[data-auth-switch]');
    if (!link) { return; }

    // ctrl, shift or middle click still opens link in new tab or window
    if (event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) {
      return;
    }

    event.preventDefault();
    history.pushState(null, '', link.href);
    show(link.getAttribute('data-auth-switch'));
  });

  window.addEventListener('popstate', function () {
    show(modeFromUrl());
  });
}());

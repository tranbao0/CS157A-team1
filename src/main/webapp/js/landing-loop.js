/*
 * Landing page scrolls on forever going down. Two copies of hero and features follow real ones.
 * Once user stops scrolling inside a copy, page jumps to same spot in real content, which looks
 * same, so jump is invisible and never cuts off a scroll in progress. Stopping near a copy's title
 * while going down glides it softly into place first. Scrolling up never snaps.
 * Header is separate from loop: slides away going down, comes back going up.
 */
(function () {
  'use strict';

  var header = document.querySelector('.topbar');
  var hero = document.querySelector('.hero');
  var features = document.querySelector('.features');
  var main = document.querySelector('main');
  if (!header || !hero || !features || !main) { return; }

  var COPIES = 2;
  var reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)');

  var firstCopyHero = null;
  var insertAfter = main;
  for (var c = 0; c < COPIES; c++) {
    var copy = document.createElement('div');
    copy.className = 'loop-copy';
    copy.setAttribute('aria-hidden', 'true');
    var heroCopy = hero.cloneNode(true);
    copy.appendChild(heroCopy);
    copy.appendChild(features.cloneNode(true));

    // copies are for looks only, keyboard users stay in real content
    var focusables = copy.querySelectorAll('a, button');
    for (var i = 0; i < focusables.length; i++) {
      focusables[i].setAttribute('tabindex', '-1');
    }

    insertAfter.parentNode.insertBefore(copy, insertAfter.nextSibling);
    insertAfter = copy;
    if (firstCopyHero === null) { firstCopyHero = heroCopy; }
  }

  var lastY = window.scrollY;
  var goingDown = false;
  var snapTarget = null;
  var skipNextScroll = false;
  var idleTimer = null;

  // distance from real hero to first copy hero, one full loop
  function cycle() {
    return firstCopyHero.getBoundingClientRect().top - hero.getBoundingClientRect().top;
  }

  // inside a copy: jump to same spot in real content
  function wrap() {
    var length = cycle();
    var mapped = window.scrollY;
    while (mapped >= length - 1) {
      mapped = mapped - length;
    }
    if (mapped < 0) { mapped = 0; }
    if (Math.round(mapped) === Math.round(window.scrollY)) { return; }
    skipNextScroll = true;
    window.scrollTo({ top: mapped, behavior: 'instant' });
    lastY = window.scrollY;
  }

  // stopped going down near a copy's title: glide it to where real title sits at top of page
  function trySnap() {
    var length = cycle();
    var y = window.scrollY;
    var loops = Math.round(y / length);
    if (loops < 1) { return false; }

    var target = loops * length;
    var ahead = target - y;
    var screen = window.innerHeight;
    // title still coming up (ahead > 0) or just passed (ahead < 0)
    if (ahead > screen * 0.45 || ahead < -screen * 0.3) { return false; }
    if (Math.abs(ahead) < 2) { return false; }

    snapTarget = target;
    // title is landing back in place, bring header back with it
    showHeader(true);
    var behavior = 'smooth';
    if (reduceMotion.matches) { behavior = 'instant'; }
    window.scrollTo({ top: target, behavior: behavior });
    return true;
  }

  function onIdle() {
    if (snapTarget !== null) {
      // glide finished, now hop back to real content
      snapTarget = null;
      wrap();
      return;
    }
    if (goingDown && trySnap()) { return; }
    wrap();
  }

  function showHeader(show) {
    if (show) {
      header.classList.remove('is-hidden');
    } else {
      header.classList.add('is-hidden');
    }
  }

  window.addEventListener('scroll', function () {
    var y = window.scrollY;
    var moved = y - lastY;
    lastY = y;

    // own instant jump, not user movement
    if (skipNextScroll) {
      skipNextScroll = false;
      return;
    }

    // glide is page moving on its own, keep header and direction as they were
    if (snapTarget === null) {
      if (moved > 0) { goingDown = true; }
      if (moved < 0) { goingDown = false; }

      if (moved > 6 && y > header.offsetHeight) { showHeader(false); }
      if (moved < -6) { showHeader(true); }
    }

    // wait for scrolling to settle before snapping or jumping
    clearTimeout(idleTimer);
    idleTimer = setTimeout(onIdle, 180);
  }, { passive: true });

  /*
   * Background art tells hero's story and costs main-thread time every frame.
   * Pause it while no hero (real one or loop copy) is on screen, play on once one comes back.
   */
  var art = document.querySelector('.pov');
  if (art && 'IntersectionObserver' in window) {
    var heroes = document.querySelectorAll('.hero');
    var heroObserver = new IntersectionObserver(function (entries) {
      /*
       * each hero keeps own on-screen flag, first report covers all heroes at once.
       * Hero counts only when 10% of it shows, edge just touching screen reports as intersecting too.
       */
      for (var e = 0; e < entries.length; e++) {
        if (entries[e].intersectionRatio >= 0.1) {
          entries[e].target.setAttribute('data-on-screen', '1');
        } else {
          entries[e].target.removeAttribute('data-on-screen');
        }
      }
      var heroesOnScreen = document.querySelectorAll('.hero[data-on-screen]').length;
      if (heroesOnScreen > 0) {
        art.classList.remove('is-paused');
      } else {
        art.classList.add('is-paused');
      }
    }, { threshold: 0.1 });
    for (var h = 0; h < heroes.length; h++) {
      heroObserver.observe(heroes[h]);
    }
  }

  // browser may restore old scroll position inside a copy after reload
  wrap();
}());

/* Transcript import. Reads a saved MySJSU unofficial transcript page in the browser,
   finds each course with a C- or better, and posts those courses as completed.
   Also reads the student's major from the transcript's "Plan:" line and posts it when it is new. */
(function () {
  'use strict';

  // C- or better. CR (credit) also counts, SJSU only gives it for C- level work or better.
  var PASSING = ['A+', 'A', 'A-', 'B+', 'B', 'B-', 'C+', 'C', 'C-', 'CR'];
  // every grade the transcript can show, so a failed or withdrawn row is still read as graded
  var GRADES = PASSING.concat(['D+', 'D', 'D-', 'F', 'NC', 'W', 'WU', 'I', 'IC', 'RD', 'RP', 'AU', 'NG']);

  var SUBJECT = /^[A-Z][A-Z0-9]{1,4}$/;    // CS, MATH, BUS1
  var NUMBER = /^\d{1,3}[A-Z]{0,3}$/;      // 46A, 157A, 100W
  var NUMERIC = /^\d+(\.\d+)?$/;           // units and grade points

  var BLOCK = /^(ADDRESS|ARTICLE|BLOCKQUOTE|BR|DD|DIV|DL|DT|FIELDSET|FORM|H[1-6]|HR|LI|OL|P|PRE|SECTION|TABLE|TBODY|THEAD|TFOOT|TR|UL)$/;
  var CELL = /^(TD|TH)$/;

  var fileInput = document.getElementById('tx-file');
  var status = document.getElementById('tx-import-status');
  var form = document.getElementById('import-form');
  var dataEl = document.getElementById('tx-courses');
  if (!fileInput || !form || !dataEl) { return; }

  var courses = JSON.parse(dataEl.textContent);
  var majorsEl = document.getElementById('tx-majors');
  var majors = majorsEl ? JSON.parse(majorsEl.textContent) : [];
  var currentMajor = majorsEl ? majorsEl.getAttribute('data-current') : '';
  var byCode = {};
  courses.forEach(function (c) { byCode[normalize(c.code)] = c; });

  function normalize(code) {
    return String(code).toUpperCase().replace(/\s+/g, '');
  }

  function say(text) {
    status.textContent = text;
  }

  // Page text, one line per row. A table row stays on one line even when its cells hold divs,
  // so "CS 146 ... 3.000 3.000 A 12.000" reads the same whether the page is a table or a <pre>.
  function textLines(html) {
    var doc = new DOMParser().parseFromString(html, 'text/html');
    var out = [];

    function walk(node, inCell) {
      if (node.nodeType === 3) {
        out.push(node.nodeValue);
        return;
      }
      if (node.nodeType !== 1) { return; }
      var tag = node.tagName;
      if (tag === 'SCRIPT' || tag === 'STYLE' || tag === 'NOSCRIPT') { return; }
      // MySJSU nests the whole report in one outer cell, one row per line, so a table inside a cell starts lines again
      if (tag === 'TABLE' || tag === 'TR') { inCell = false; }
      var breaks = !inCell && BLOCK.test(tag);
      if (breaks) { out.push('\n'); }
      if (CELL.test(tag)) { out.push(' '); }
      for (var child = node.firstChild; child; child = child.nextSibling) {
        walk(child, inCell || CELL.test(tag));
      }
      if (CELL.test(tag)) { out.push(' '); }
      if (breaks) { out.push('\n'); }
    }

    walk(doc.body || doc.documentElement, false);
    return out.join('').replace(/ /g, ' ').split(/\r?\n/);
  }

  // The grade is the last grade-looking token that comes right after a units number and is followed
  // only by numbers (grade points). That skips titles ending in a letter, like "Programming in C",
  // and in-progress rows, which have units but no grade.
  function gradeOf(tokens, from) {
    for (var j = tokens.length - 1; j > from; j--) {
      if (!NUMERIC.test(tokens[j - 1]) || GRADES.indexOf(tokens[j]) < 0) { continue; }
      var tail = tokens.slice(j + 1);
      if (tail.every(function (t) { return NUMERIC.test(t); })) { return tokens[j]; }
    }
    return null;
  }

  // Major, from the "Plan:" (or "Major:") line under each term. The last one is the most recent,
  // so a student who changed majors gets the new one. Minors and "Sub-Plan:" lines are skipped.
  var PLAN = /(?:^|\s)(?:Plan|Major)\s*:\s*(.+)$/i;
  var DEGREE = /^(BS|BA|BFA|BM|BSN|MS|MA|MBA|PHD)$/;
  var FILLER = /^(MAJOR|DEGREE|UNDERGRADUATE|UNDERGRAD)$/;

  function planOf(lines) {
    var plan = null;
    lines.forEach(function (line) {
      var m = PLAN.exec(line.replace(/\s+/g, ' ').trim());
      if (m && !/\bminor\b/i.test(m[1])) { plan = m[1].trim(); }
    });
    return plan;
  }

  // "Computer Science, BS" -> {core: "COMPUTER SCIENCE", degree: "BS"}
  function splitMajor(text) {
    var words = String(text).toUpperCase().replace(/[^A-Z0-9 ]+/g, ' ').trim().split(/\s+/);
    var core = [], degree = null;
    words.forEach(function (w) {
      if (DEGREE.test(w)) { degree = w; } else if (!FILLER.test(w)) { core.push(w); }
    });
    return { core: core.join(' '), degree: degree };
  }

  // Catalog major whose name matches the plan. Same name is required; when the transcript
  // names a degree too it must agree, so "Mathematics BA" never picks a BS major.
  function matchMajor(plan) {
    if (!plan) { return null; }
    var want = splitMajor(plan);
    if (!want.core) { return null; }
    var hits = majors.filter(function (m) {
      var have = splitMajor(m.name);
      return have.core === want.core && (!want.degree || !have.degree || have.degree === want.degree);
    });
    return hits.length === 1 ? hits[0] : null;
  }

  // Course code -> passed, from every line that names a course and shows a grade.
  // A repeated course counts as passed if any attempt passed.
  function parse(lines) {
    var found = {};
    lines.forEach(function (line) {
      var tokens = line.trim().split(/\s+/);
      for (var i = 0; i + 1 < tokens.length; i++) {
        if (!SUBJECT.test(tokens[i]) || !NUMBER.test(tokens[i + 1])) { continue; }
        var grade = gradeOf(tokens, i + 1);
        if (grade) {
          var code = tokens[i] + tokens[i + 1];
          found[code] = found[code] || PASSING.indexOf(grade) >= 0;
          break;
        }
      }
    });
    return found;
  }

  function readFile(file) {
    return new Promise(function (resolve, reject) {
      var reader = new FileReader();
      reader.onload = function () { resolve(reader.result); };
      reader.onerror = function () { reject(reader.error); };
      reader.readAsText(file);
    });
  }

  function addField(name, value) {
    var input = document.createElement('input');
    input.type = 'hidden';
    input.name = name;
    input.value = value;
    form.appendChild(input);
  }

  fileInput.addEventListener('change', function () {
    var files = Array.prototype.slice.call(fileInput.files);
    if (!files.length) { return; }
    say('Reading transcript...');

    Promise.all(files.map(readFile)).then(function (pages) {
      var found = {};
      var plan = null;
      pages.forEach(function (html) {
        var lines = textLines(html);
        var one = parse(lines);
        Object.keys(one).forEach(function (code) { found[code] = found[code] || one[code]; });
        plan = planOf(lines) || plan;
      });

      // only send the major when it is new, so re-importing never resets a chosen catalog year
      var major = matchMajor(plan);
      var newMajor = major && String(major.id) !== currentMajor ? major : null;
      if (newMajor) { addField('major', newMajor.id); }

      var graded = Object.keys(found);
      if (!graded.length && newMajor) {
        say('Saving your major...');
        form.submit();
        return;
      }
      if (!graded.length) {
        // the page saved from MySJSU is only a frame, the grades sit in a second file next to it
        say('No graded courses found in that file. If you used "Save as", also pick '
          + 'SJ_SA_CUSTOM.SJ_TR_SS_TSCRPT.html from the "_files" folder saved next to it.');
        fileInput.value = '';
        return;
      }

      var matched = [];
      graded.forEach(function (code) {
        var course = byCode[code];
        if (found[code] && course) { matched.push(course); }
      });
      if (!matched.length && newMajor) {
        say('Saving your major...');
        form.submit();
        return;
      }
      if (!matched.length) {
        say('Found ' + graded.length + ' graded courses, but none with a C- or better are in the CourseCraft catalog.');
        fileInput.value = '';
        return;
      }

      var added = 0;
      matched.forEach(function (course) {
        addField('shown', course.id);
        addField('completed', course.id);
        if (!course.passed) { added++; }
      });
      form.elements.imported.value = String(added);
      say('Saving ' + matched.length + ' completed courses...');
      form.submit();
    }).catch(function () {
      say('Could not read that file. Try saving the transcript page again.');
      fileInput.value = '';
    });
  });
})();

<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- landing page is JSP, not plain HTML, so shared top bar knows if student is signed in --%>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>CourseCraft</title>
<%@ include file="/WEB-INF/jspf/head.jspf" %>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/landing.css">
</head>
<body class="page-landing">

<a class="skip-link" href="#main">Skip to main content</a>

<!-- decorative background, storyboard: typing on laptop, zoom into screen, dot climbs one road past course stops,
     road keeps going as squiggle while camera rushes up, dot runs on up last dashed stretch into diploma, light bursts, loops -->
<svg class="pov" viewBox="0 0 800 600" preserveAspectRatio="xMidYMid slice" aria-hidden="true" focusable="false">
  <defs>
    <radialGradient id="pov-disc" cx="50%" cy="45%" r="50%">
      <stop class="pov__stop--disc" offset="0"/>
      <stop class="pov__stop--disc-2" offset=".65"/>
      <stop class="pov__stop--surface" offset="1" stop-opacity="0"/>
    </radialGradient>
    <linearGradient id="pov-lid" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="#1D2C4C"/>
      <stop class="pov__stop--ink" offset="1"/>
    </linearGradient>
    <linearGradient id="pov-screen" x1="0" y1="0" x2="1" y2="1">
      <stop class="pov__stop--surface" offset="0"/>
      <stop offset="1" stop-color="#F4F7FB"/>
    </linearGradient>
    <linearGradient id="pov-deck" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="#E4E8EE"/>
      <stop offset="1" stop-color="#D6DCE4"/>
    </linearGradient>
    <linearGradient id="pov-desk" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="#FFF6DF"/>
      <stop offset="1" stop-color="#FAEDCB"/>
    </linearGradient>
    <linearGradient id="pov-gold" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="#FFD866"/>
      <stop class="pov__stop--amber" offset=".5"/>
      <stop offset="1" stop-color="#F2B530"/>
    </linearGradient>
    <linearGradient id="pov-paper" x1="0" y1="0" x2="0" y2="1">
      <stop class="pov__stop--surface" offset="0"/>
      <stop class="pov__stop--canvas" offset="1"/>
    </linearGradient>
    <linearGradient id="pov-roll" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="#FFF6DF"/>
      <stop class="pov__stop--surface" offset=".5"/>
      <stop offset="1" stop-color="#F3E6C4"/>
    </linearGradient>
    <linearGradient id="pov-ray" x1="0" y1="1" x2="0" y2="0">
      <stop class="pov__stop--amber" offset="0" stop-opacity=".6"/>
      <stop class="pov__stop--amber" offset="1" stop-opacity="0"/>
    </linearGradient>
    <radialGradient id="pov-backlight">
      <stop offset="0" stop-color="#FFE7A3" stop-opacity=".95"/>
      <stop class="pov__stop--amber" offset=".45" stop-opacity=".35"/>
      <stop class="pov__stop--amber" offset="1" stop-opacity="0"/>
    </radialGradient>
    <radialGradient id="pov-flash">
      <stop offset="0" stop-color="#FFF4CC"/>
      <stop class="pov__stop--amber" offset=".45" stop-opacity=".55"/>
      <stop class="pov__stop--amber" offset="1" stop-opacity="0"/>
    </radialGradient>
    <radialGradient id="pov-shock-ring">
      <stop offset=".55" stop-color="#FFE7A3" stop-opacity="0"/>
      <stop offset=".8" stop-color="#FFE7A3" stop-opacity=".7"/>
      <stop offset="1" stop-color="#FFE7A3" stop-opacity="0"/>
    </radialGradient>
    <linearGradient id="pov-band" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="#F6CE62"/>
      <stop offset=".5" stop-color="#EBB233"/>
      <stop offset="1" stop-color="#DDA226"/>
    </linearGradient>
    <linearGradient id="pov-band-shade" x1="0" y1="0" x2="1" y2="0">
      <stop offset="0" stop-color="#8A5B0A" stop-opacity=".28"/>
      <stop offset=".22" stop-color="#8A5B0A" stop-opacity="0"/>
      <stop offset=".78" stop-color="#8A5B0A" stop-opacity="0"/>
      <stop offset="1" stop-color="#8A5B0A" stop-opacity=".28"/>
    </linearGradient>
    <linearGradient id="pov-point" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="#F6CE62"/>
      <stop offset="1" stop-color="#E6AA2A"/>
    </linearGradient>
    <filter id="pov-soft" x="-30%" y="-30%" width="160%" height="160%">
      <feGaussianBlur stdDeviation="6"/>
    </filter>
    <radialGradient id="pov-glow">
      <stop class="pov__stop--amber" offset="0" stop-opacity=".55"/>
      <stop class="pov__stop--amber" offset="1" stop-opacity="0"/>
    </radialGradient>
    <radialGradient id="pov-floor-shadow">
      <stop class="pov__stop--ink" offset="0" stop-opacity=".1"/>
      <stop class="pov__stop--ink" offset="1" stop-opacity="0"/>
    </radialGradient>
    <filter id="pov-paper-shadow" x="-20%" y="-20%" width="140%" height="150%">
      <feDropShadow class="pov__shadow-ink" dx="0" dy="14" stdDeviation="14" flood-opacity=".09"/>
    </filter>
    <clipPath id="pov-display-clip">
      <rect x="219" y="114" width="362" height="190" rx="6"/>
    </clipPath>
    <mask id="pov-sweep-mask" maskUnits="userSpaceOnUse" x="-100" y="-1500" width="1000" height="2100">
      <path class="pov__sweep" d="M 190 520 C 330 520, 460 525, 540 500 C 600 480, 600 395, 540 385 C 440 370, 330 375, 260 365 C 200 355, 200 255, 260 245 C 350 230, 460 235, 540 225 C 600 215, 600 115, 540 105 C 440 92, 330 95, 260 85 C 200 75, 200 -25, 260 -35 C 340 -48, 470 -45, 520 -70 C 570 -100, 560 -170, 470 -190 C 340 -220, 260 -230, 260 -300 C 260 -370, 520 -370, 520 -440 C 520 -500, 300 -505, 300 -565 C 300 -615, 470 -620, 470 -670 C 470 -710, 340 -715, 340 -755 C 340 -790, 430 -795, 420 -830 C 410 -880, 380 -940, 400 -1000 C 420 -1060, 400 -1120, 400 -1180 L 400 -1395" pathLength="1"/>
    </mask>
  </defs>

  <g class="pov__frame">
    <g class="pov__backdrop">
      <circle cx="400" cy="270" r="330" fill="url(#pov-disc)"/>
      <circle class="pov__ring" cx="400" cy="270" r="250"/>
      <circle class="pov__ring" cx="400" cy="270" r="175"/>
      <g class="pov__motes">
        <circle class="pov__mote pov__mote--a pov__mote--t0" cx="150" cy="120" r="4"/>
        <circle class="pov__mote pov__mote--b pov__mote--t1" cx="640" cy="90" r="5"/>
        <circle class="pov__mote pov__mote--c pov__mote--t0" cx="705" cy="225" r="3"/>
        <circle class="pov__mote pov__mote--b pov__mote--t1" cx="108" cy="262" r="5"/>
        <circle class="pov__mote pov__mote--a pov__mote--t0" cx="560" cy="58" r="3"/>
        <circle class="pov__mote pov__mote--c pov__mote--t1" cx="262" cy="72" r="4"/>
        <circle class="pov__mote pov__mote--a pov__mote--t0" cx="724" cy="330" r="4"/>
        <circle class="pov__mote pov__mote--c pov__mote--t1" cx="82" cy="176" r="3"/>
        <circle class="pov__mote pov__mote--b pov__mote--t0" cx="470" cy="34" r="4"/>
      </g>
    </g>

    <!-- laptop seen straight on from your seat, whole group zooms into screen -->
    <g class="pov__laptop">
      <rect x="-400" y="330" width="1600" height="700" fill="url(#pov-desk)"/>
      <rect class="pov__desk-edge" x="-400" y="330" width="1600" height="3"/>
      <ellipse cx="400" cy="462" rx="320" ry="18" fill="url(#pov-floor-shadow)"/>
      <path d="M 205 318 L 595 318 L 690 452 L 110 452 Z" fill="url(#pov-deck)"/>
      <path class="pov__lip" d="M 110 452 L 690 452 L 690 460 Q 690 466 682 466 L 118 466 Q 110 466 110 460 Z"/>
      <g class="pov__keys">
          <rect class="pov__key-edge" x="265.7" y="336.0" width="23.3" height="17" rx="3"/><rect class="pov__key" x="265.7" y="334" width="23.3" height="17" rx="3"/>
          <rect class="pov__key-edge" x="292.9" y="336.0" width="23.3" height="17" rx="3"/><rect class="pov__key" x="292.9" y="334" width="23.3" height="17" rx="3"/>
          <rect class="pov__key-edge" x="320.2" y="336.0" width="23.3" height="17" rx="3"/><rect class="pov__key" x="320.2" y="334" width="23.3" height="17" rx="3"/><rect class="pov__key-lit pov__key--f1" x="320.2" y="334" width="23.3" height="17" rx="3"/>
          <rect class="pov__key-edge" x="347.5" y="336.0" width="23.3" height="17" rx="3"/><rect class="pov__key" x="347.5" y="334" width="23.3" height="17" rx="3"/>
          <rect class="pov__key-edge" x="374.7" y="336.0" width="23.3" height="17" rx="3"/><rect class="pov__key" x="374.7" y="334" width="23.3" height="17" rx="3"/>
          <rect class="pov__key-edge" x="402.0" y="336.0" width="23.3" height="17" rx="3"/><rect class="pov__key" x="402.0" y="334" width="23.3" height="17" rx="3"/>
          <rect class="pov__key-edge" x="429.3" y="336.0" width="23.3" height="17" rx="3"/><rect class="pov__key" x="429.3" y="334" width="23.3" height="17" rx="3"/>
          <rect class="pov__key-edge" x="456.5" y="336.0" width="23.3" height="17" rx="3"/><rect class="pov__key" x="456.5" y="334" width="23.3" height="17" rx="3"/><rect class="pov__key-lit pov__key--f4" x="456.5" y="334" width="23.3" height="17" rx="3"/>
          <rect class="pov__key-edge" x="483.8" y="336.0" width="23.3" height="17" rx="3"/><rect class="pov__key" x="483.8" y="334" width="23.3" height="17" rx="3"/>
          <rect class="pov__key-edge" x="511.1" y="336.0" width="23.3" height="17" rx="3"/><rect class="pov__key" x="511.1" y="334" width="23.3" height="17" rx="3"/>
          <rect class="pov__key-edge" x="246.5" y="363.0" width="27.1" height="17" rx="3"/><rect class="pov__key" x="246.5" y="361" width="27.1" height="17" rx="3"/>
          <rect class="pov__key-edge" x="277.6" y="363.0" width="27.1" height="17" rx="3"/><rect class="pov__key" x="277.6" y="361" width="27.1" height="17" rx="3"/>
          <rect class="pov__key-edge" x="308.7" y="363.0" width="27.1" height="17" rx="3"/><rect class="pov__key" x="308.7" y="361" width="27.1" height="17" rx="3"/>
          <rect class="pov__key-edge" x="339.8" y="363.0" width="27.1" height="17" rx="3"/><rect class="pov__key" x="339.8" y="361" width="27.1" height="17" rx="3"/><rect class="pov__key-lit pov__key--f6" x="339.8" y="361" width="27.1" height="17" rx="3"/>
          <rect class="pov__key-edge" x="370.9" y="363.0" width="27.1" height="17" rx="3"/><rect class="pov__key" x="370.9" y="361" width="27.1" height="17" rx="3"/>
          <rect class="pov__key-edge" x="402.0" y="363.0" width="27.1" height="17" rx="3"/><rect class="pov__key" x="402.0" y="361" width="27.1" height="17" rx="3"/>
          <rect class="pov__key-edge" x="433.1" y="363.0" width="27.1" height="17" rx="3"/><rect class="pov__key" x="433.1" y="361" width="27.1" height="17" rx="3"/><rect class="pov__key-lit pov__key--f2" x="433.1" y="361" width="27.1" height="17" rx="3"/>
          <rect class="pov__key-edge" x="464.2" y="363.0" width="27.1" height="17" rx="3"/><rect class="pov__key" x="464.2" y="361" width="27.1" height="17" rx="3"/>
          <rect class="pov__key-edge" x="495.3" y="363.0" width="27.1" height="17" rx="3"/><rect class="pov__key" x="495.3" y="361" width="27.1" height="17" rx="3"/>
          <rect class="pov__key-edge" x="526.4" y="363.0" width="27.1" height="17" rx="3"/><rect class="pov__key" x="526.4" y="361" width="27.1" height="17" rx="3"/>
          <rect class="pov__key-edge" x="227.4" y="390.0" width="30.9" height="17" rx="3"/><rect class="pov__key" x="227.4" y="388" width="30.9" height="17" rx="3"/>
          <rect class="pov__key-edge" x="262.3" y="390.0" width="30.9" height="17" rx="3"/><rect class="pov__key" x="262.3" y="388" width="30.9" height="17" rx="3"/>
          <rect class="pov__key-edge" x="297.2" y="390.0" width="30.9" height="17" rx="3"/><rect class="pov__key" x="297.2" y="388" width="30.9" height="17" rx="3"/>
          <rect class="pov__key-edge" x="332.1" y="390.0" width="30.9" height="17" rx="3"/><rect class="pov__key" x="332.1" y="388" width="30.9" height="17" rx="3"/>
          <rect class="pov__key-edge" x="367.1" y="390.0" width="30.9" height="17" rx="3"/><rect class="pov__key" x="367.1" y="388" width="30.9" height="17" rx="3"/><rect class="pov__key-lit pov__key--f3" x="367.1" y="388" width="30.9" height="17" rx="3"/>
          <rect class="pov__key-edge" x="402.0" y="390.0" width="30.9" height="17" rx="3"/><rect class="pov__key" x="402.0" y="388" width="30.9" height="17" rx="3"/>
          <rect class="pov__key-edge" x="436.9" y="390.0" width="30.9" height="17" rx="3"/><rect class="pov__key" x="436.9" y="388" width="30.9" height="17" rx="3"/>
          <rect class="pov__key-edge" x="471.9" y="390.0" width="30.9" height="17" rx="3"/><rect class="pov__key" x="471.9" y="388" width="30.9" height="17" rx="3"/><rect class="pov__key-lit pov__key--f2" x="471.9" y="388" width="30.9" height="17" rx="3"/>
          <rect class="pov__key-edge" x="506.8" y="390.0" width="30.9" height="17" rx="3"/><rect class="pov__key" x="506.8" y="388" width="30.9" height="17" rx="3"/>
          <rect class="pov__key-edge" x="541.7" y="390.0" width="30.9" height="17" rx="3"/><rect class="pov__key" x="541.7" y="388" width="30.9" height="17" rx="3"/>
          <rect class="pov__key-edge" x="208.2" y="417.0" width="34.8" height="17" rx="3"/><rect class="pov__key" x="208.2" y="415" width="34.8" height="17" rx="3"/>
          <rect class="pov__key-edge" x="247.0" y="417.0" width="34.8" height="17" rx="3"/><rect class="pov__key" x="247.0" y="415" width="34.8" height="17" rx="3"/>
          <rect class="pov__key-edge" x="285.7" y="417.0" width="34.8" height="17" rx="3"/><rect class="pov__key" x="285.7" y="415" width="34.8" height="17" rx="3"/>
          <rect class="pov__key-edge" x="324.5" y="417.0" width="34.8" height="17" rx="3"/><rect class="pov__key" x="324.5" y="415" width="34.8" height="17" rx="3"/><rect class="pov__key-lit pov__key--f5" x="324.5" y="415" width="34.8" height="17" rx="3"/>
          <rect class="pov__key-edge" x="363.2" y="417.0" width="34.8" height="17" rx="3"/><rect class="pov__key" x="363.2" y="415" width="34.8" height="17" rx="3"/>
          <rect class="pov__key-edge" x="402.0" y="417.0" width="34.8" height="17" rx="3"/><rect class="pov__key" x="402.0" y="415" width="34.8" height="17" rx="3"/>
          <rect class="pov__key-edge" x="440.8" y="417.0" width="34.8" height="17" rx="3"/><rect class="pov__key" x="440.8" y="415" width="34.8" height="17" rx="3"/>
          <rect class="pov__key-edge" x="479.5" y="417.0" width="34.8" height="17" rx="3"/><rect class="pov__key" x="479.5" y="415" width="34.8" height="17" rx="3"/>
          <rect class="pov__key-edge" x="518.3" y="417.0" width="34.8" height="17" rx="3"/><rect class="pov__key" x="518.3" y="415" width="34.8" height="17" rx="3"/>
          <rect class="pov__key-edge" x="557.0" y="417.0" width="34.8" height="17" rx="3"/><rect class="pov__key" x="557.0" y="415" width="34.8" height="17" rx="3"/>
      </g>
      <rect x="205" y="100" width="390" height="220" rx="16" fill="url(#pov-lid)"/>
      <rect class="pov__lid-rim" x="211" y="103" width="378" height="4" rx="2"/>
      <circle class="pov__webcam" cx="400" cy="108" r="2"/>
      <rect x="219" y="114" width="362" height="190" rx="6" fill="url(#pov-screen)"/>
      <g clip-path="url(#pov-display-clip)">
        <!-- inverse of zoom, so after zoom screen content lines up 1:1 with page -->
        <g transform="translate(400 209) scale(.294118) translate(-400 -300)">
          <g class="pov__camera">
            <g transform="translate(0 -1848)">
              <g transform="translate(400 300)">
                <circle class="pov__backlight" r="300" fill="url(#pov-backlight)"/>
                <g class="pov__rays" filter="url(#pov-soft)"><g class="pov__rays-spin"><path d="M -9 0 L 9 0 L 0 -460 Z" fill="url(#pov-ray)" transform="rotate(0.0)"/><path d="M -5 0 L 5 0 L 0 -340 Z" fill="url(#pov-ray)" transform="rotate(22.5)"/><path d="M -9 0 L 9 0 L 0 -460 Z" fill="url(#pov-ray)" transform="rotate(45.0)"/><path d="M -5 0 L 5 0 L 0 -340 Z" fill="url(#pov-ray)" transform="rotate(67.5)"/><path d="M -9 0 L 9 0 L 0 -460 Z" fill="url(#pov-ray)" transform="rotate(90.0)"/><path d="M -5 0 L 5 0 L 0 -340 Z" fill="url(#pov-ray)" transform="rotate(112.5)"/><path d="M -9 0 L 9 0 L 0 -460 Z" fill="url(#pov-ray)" transform="rotate(135.0)"/><path d="M -5 0 L 5 0 L 0 -340 Z" fill="url(#pov-ray)" transform="rotate(157.5)"/><path d="M -9 0 L 9 0 L 0 -460 Z" fill="url(#pov-ray)" transform="rotate(180.0)"/><path d="M -5 0 L 5 0 L 0 -340 Z" fill="url(#pov-ray)" transform="rotate(202.5)"/><path d="M -9 0 L 9 0 L 0 -460 Z" fill="url(#pov-ray)" transform="rotate(225.0)"/><path d="M -5 0 L 5 0 L 0 -340 Z" fill="url(#pov-ray)" transform="rotate(247.5)"/><path d="M -9 0 L 9 0 L 0 -460 Z" fill="url(#pov-ray)" transform="rotate(270.0)"/><path d="M -5 0 L 5 0 L 0 -340 Z" fill="url(#pov-ray)" transform="rotate(292.5)"/><path d="M -9 0 L 9 0 L 0 -460 Z" fill="url(#pov-ray)" transform="rotate(315.0)"/><path d="M -5 0 L 5 0 L 0 -340 Z" fill="url(#pov-ray)" transform="rotate(337.5)"/></g></g>
              </g>
            </g>

            <%-- id lets "you" dot follow this path, see .pov__you --%>
            <path id="pov-road" class="pov__road" d="M 190 520 C 330 520, 460 525, 540 500 C 600 480, 600 395, 540 385 C 440 370, 330 375, 260 365 C 200 355, 200 255, 260 245 C 350 230, 460 235, 540 225 C 600 215, 600 115, 540 105 C 440 92, 330 95, 260 85 C 200 75, 200 -25, 260 -35 C 340 -48, 470 -45, 520 -70 C 570 -100, 560 -170, 470 -190 C 340 -220, 260 -230, 260 -300 C 260 -370, 520 -370, 520 -440 C 520 -500, 300 -505, 300 -565 C 300 -615, 470 -620, 470 -670 C 470 -710, 340 -715, 340 -755 C 340 -790, 430 -795, 420 -830 C 410 -880, 380 -940, 400 -1000 C 420 -1060, 400 -1120, 400 -1180 L 400 -1395"/>
            <g class="pov__todo">
              <circle cx="584.9" cy="442.8" r="11"/>
              <circle cx="216.4" cy="319.4" r="11"/>
              <circle cx="578.5" cy="195.2" r="11"/>
              <circle cx="215.2" cy="30.0" r="11"/>
            </g>
            <g class="pov__labels">
              <text x="560" y="447" text-anchor="end">CS 46A</text>
              <text x="242" y="324" text-anchor="start">CS 46B</text>
              <text x="554" y="200" text-anchor="end">CS 146</text>
              <text x="241" y="35" text-anchor="start">CS 157A</text>
            </g>
            <g mask="url(#pov-sweep-mask)">
              <path class="pov__trail-edge" d="M 190 520 C 330 520, 460 525, 540 500 C 600 480, 600 395, 540 385 C 440 370, 330 375, 260 365 C 200 355, 200 255, 260 245 C 350 230, 460 235, 540 225 C 600 215, 600 115, 540 105 C 440 92, 330 95, 260 85 C 200 75, 200 -25, 260 -35 C 340 -48, 470 -45, 520 -70 C 570 -100, 560 -170, 470 -190 C 340 -220, 260 -230, 260 -300 C 260 -370, 520 -370, 520 -440 C 520 -500, 300 -505, 300 -565 C 300 -615, 470 -620, 470 -670 C 470 -710, 340 -715, 340 -755 C 340 -790, 430 -795, 420 -830 C 410 -880, 380 -940, 400 -1000 C 420 -1060, 400 -1120, 400 -1180 L 400 -1395"/>
              <path class="pov__trail" d="M 190 520 C 330 520, 460 525, 540 500 C 600 480, 600 395, 540 385 C 440 370, 330 375, 260 365 C 200 355, 200 255, 260 245 C 350 230, 460 235, 540 225 C 600 215, 600 115, 540 105 C 440 92, 330 95, 260 85 C 200 75, 200 -25, 260 -35 C 340 -48, 470 -45, 520 -70 C 570 -100, 560 -170, 470 -190 C 340 -220, 260 -230, 260 -300 C 260 -370, 520 -370, 520 -440 C 520 -500, 300 -505, 300 -565 C 300 -615, 470 -620, 470 -670 C 470 -710, 340 -715, 340 -755 C 340 -790, 430 -795, 420 -830 C 410 -880, 380 -940, 400 -1000 C 420 -1060, 400 -1120, 400 -1180 L 400 -1395"/>
            </g>
            <circle class="pov__start" cx="190" cy="520" r="8"/>
            <g transform="translate(584.9 442.8)"><g class="pov__done pov__done--1"><circle r="13"/><path d="M -5 .5 l 3.5 3.5 l 7 -7.5"/></g></g>
              <g transform="translate(216.4 319.4)"><g class="pov__done pov__done--2"><circle r="13"/><path d="M -5 .5 l 3.5 3.5 l 7 -7.5"/></g></g>
              <g transform="translate(578.5 195.2)"><g class="pov__done pov__done--3"><circle r="13"/><path d="M -5 .5 l 3.5 3.5 l 7 -7.5"/></g></g>
              <g transform="translate(215.2 30.0)"><g class="pov__done pov__done--4"><circle r="13"/><path d="M -5 .5 l 3.5 3.5 l 7 -7.5"/></g></g>

            <g transform="translate(0 -1848)">
              <g class="pov__paper">
                <rect x="240" y="150" width="320" height="300" rx="4" fill="url(#pov-paper)" filter="url(#pov-paper-shadow)"/>
                <rect class="pov__paper-border" x="258" y="172" width="284" height="256" rx="3"/>
                <text class="pov__degree" x="400" y="262" text-anchor="middle">BACHELOR’S</text>
                <text class="pov__degree-sub" x="404" y="292" text-anchor="middle">DEGREE</text>
                <rect class="pov__line-text" x="300" y="322" width="200" height="6" rx="3"/>
                <rect class="pov__line-text" x="284" y="342" width="232" height="6" rx="3"/>
                <rect class="pov__line-text" x="332" y="362" width="136" height="6" rx="3"/>
                <g transform="translate(494 396)">
                  <path class="pov__ribbon" d="M -8 6 L -12 24 L -6 20 L -3 27 L 0 9 Z"/>
                  <path class="pov__ribbon" d="M 8 6 L 12 24 L 6 20 L 3 27 L 0 9 Z"/>
                  <circle r="15" fill="url(#pov-gold)"/>
                  <circle class="pov__seal-ring" r="10"/>
                </g>
                <rect x="224" y="136" width="352" height="22" rx="11" fill="url(#pov-roll)"/>
                <ellipse class="pov__roll-end" cx="226" cy="147" rx="5" ry="11"/>
                <ellipse class="pov__roll-end" cx="574" cy="147" rx="5" ry="11"/>
                <rect x="224" y="442" width="352" height="22" rx="11" fill="url(#pov-roll)"/>
                <ellipse class="pov__roll-end" cx="226" cy="453" rx="5" ry="11"/>
                <ellipse class="pov__roll-end" cx="574" cy="453" rx="5" ry="11"/>
              </g>
              <g transform="translate(400 100)">
                <circle class="pov__crown-glow" r="74" fill="url(#pov-glow)"/>
                <g class="pov__crown-bob">
                  <!-- back half of crown: darker copy slides left behind band, inner rim shows inside of ring -->
                  <g class="pov__crown-back-points">
                    <g class="pov__facet-back pov__back--1"><path class="pov__point-back" d="M -9 12 L 0 -24 L 9 12 Z"/><circle class="pov__pearl-back" cy="-26" r="4"/></g>
                    <g class="pov__facet-back pov__back--2"><path class="pov__point-back" d="M -9 12 L 0 -24 L 9 12 Z"/><circle class="pov__pearl-back" cy="-26" r="4"/></g>
                    <g class="pov__facet-back pov__back--3"><path class="pov__point-back" d="M -9 12 L 0 -24 L 9 12 Z"/><circle class="pov__pearl-back" cy="-26" r="4"/></g>
                    <g class="pov__facet-back pov__back--4"><path class="pov__point-back" d="M -9 12 L 0 -24 L 9 12 Z"/><circle class="pov__pearl-back" cy="-26" r="4"/></g>
                    <g class="pov__facet-back pov__back--5"><path class="pov__point-back" d="M -9 12 L 0 -24 L 9 12 Z"/><circle class="pov__pearl-back" cy="-26" r="4"/></g>
                    <g class="pov__facet-back pov__back--6"><path class="pov__point-back" d="M -9 12 L 0 -24 L 9 12 Z"/><circle class="pov__pearl-back" cy="-26" r="4"/></g>
                  </g>
                  <rect class="pov__band-inner" x="-47" y="4" width="94" height="9" rx="3"/>
                  <!-- front half: points and gems ride right across band, squeezed near edges -->
                  <g class="pov__crown-points">
                    <g class="pov__facet pov__facet--1"><path class="pov__point" d="M -9 12 L 0 -24 L 9 12 Z"/><circle class="pov__pearl" cy="-26" r="4"/></g>
                    <g class="pov__facet pov__facet--2"><path class="pov__point" d="M -9 12 L 0 -24 L 9 12 Z"/><circle class="pov__pearl" cy="-26" r="4"/></g>
                    <g class="pov__facet pov__facet--3"><path class="pov__point" d="M -9 12 L 0 -24 L 9 12 Z"/><circle class="pov__pearl" cy="-26" r="4"/></g>
                    <g class="pov__facet pov__facet--4"><path class="pov__point" d="M -9 12 L 0 -24 L 9 12 Z"/><circle class="pov__pearl" cy="-26" r="4"/></g>
                    <g class="pov__facet pov__facet--5"><path class="pov__point" d="M -9 12 L 0 -24 L 9 12 Z"/><circle class="pov__pearl" cy="-26" r="4"/></g>
                    <g class="pov__facet pov__facet--6"><path class="pov__point" d="M -9 12 L 0 -24 L 9 12 Z"/><circle class="pov__pearl" cy="-26" r="4"/></g>
                  </g>
                  <rect x="-50" y="10" width="100" height="20" rx="4" fill="url(#pov-band)"/>
                  <rect class="pov__band-light" x="-46" y="12.5" width="92" height="2" rx="1"/>
                  <g class="pov__crown-gems">
                    <g class="pov__facet pov__facet--1"><ellipse class="pov__gem" cy="20" rx="4.5" ry="4"/></g>
                    <g class="pov__facet pov__facet--2"><ellipse class="pov__gem" cy="20" rx="4.5" ry="4"/></g>
                    <g class="pov__facet pov__facet--3"><ellipse class="pov__gem" cy="20" rx="4.5" ry="4"/></g>
                    <g class="pov__facet pov__facet--4"><ellipse class="pov__gem" cy="20" rx="4.5" ry="4"/></g>
                    <g class="pov__facet pov__facet--5"><ellipse class="pov__gem" cy="20" rx="4.5" ry="4"/></g>
                    <g class="pov__facet pov__facet--6"><ellipse class="pov__gem" cy="20" rx="4.5" ry="4"/></g>
                  </g>
                  <rect x="-50" y="10" width="100" height="20" rx="4" fill="url(#pov-band-shade)"/>
                </g>
              </g>
              <g class="pov__sparkles">
                <g transform="translate(296 118) scale(1.0)"><path class="pov__sparkle pov__sparkle--1" d="M 0 -10 Q 1.6 -1.6 10 0 Q 1.6 1.6 0 10 Q -1.6 1.6 -10 0 Q -1.6 -1.6 0 -10 Z"/></g>
                <g transform="translate(512 92) scale(0.8)"><path class="pov__sparkle pov__sparkle--2" d="M 0 -10 Q 1.6 -1.6 10 0 Q 1.6 1.6 0 10 Q -1.6 1.6 -10 0 Q -1.6 -1.6 0 -10 Z"/></g>
                <g transform="translate(590 214) scale(0.6)"><path class="pov__sparkle pov__sparkle--3" d="M 0 -10 Q 1.6 -1.6 10 0 Q 1.6 1.6 0 10 Q -1.6 1.6 -10 0 Q -1.6 -1.6 0 -10 Z"/></g>
                <g transform="translate(212 250) scale(0.55)"><path class="pov__sparkle pov__sparkle--4" d="M 0 -10 Q 1.6 -1.6 10 0 Q 1.6 1.6 0 10 Q -1.6 1.6 -10 0 Q -1.6 -1.6 0 -10 Z"/></g>
                <g transform="translate(590 380) scale(0.5)"><path class="pov__sparkle pov__sparkle--2" d="M 0 -10 Q 1.6 -1.6 10 0 Q 1.6 1.6 0 10 Q -1.6 1.6 -10 0 Q -1.6 -1.6 0 -10 Z"/></g>
                <g transform="translate(226 150) scale(0.45)"><path class="pov__sparkle pov__sparkle--3" d="M 0 -10 Q 1.6 -1.6 10 0 Q 1.6 1.6 0 10 Q -1.6 1.6 -10 0 Q -1.6 -1.6 0 -10 Z"/></g>
              </g>
            </g>

            <!-- flash in front of paper, starts where dot hits bottom roll -->
            <g transform="translate(0 -1848)">
              <g transform="translate(400 453)">
                <circle class="pov__shock" r="120" fill="url(#pov-shock-ring)"/>
                <circle class="pov__flash" r="90" fill="url(#pov-flash)"/>
              </g>
            </g>

            <g class="pov__you">
              <g class="pov__you-move">
                <circle class="pov__you-halo" r="24"/>
                <circle class="pov__you-ring" r="12"/>
                <circle class="pov__you-dot" r="7.5"/>
              </g>
            </g>
          </g>
        </g>
        <path class="pov__glare" d="M 470 114 L 515 114 L 430 304 L 385 304 Z"/>
      </g>
      <g class="pov__arm">
        <g class="pov__reach pov__reach--left">
          <path class="pov__noodle" d="M 30 660 C 60 520, 150 430, 222 392"/>
          <g class="pov__hand pov__hand--left"><circle class="pov__skin" cx="230" cy="388" r="30"/></g>
        </g>
      </g>
      <g class="pov__arm" transform="translate(800 0) scale(-1 1)">
        <g class="pov__reach pov__reach--right">
          <path class="pov__noodle" d="M 30 660 C 60 520, 150 430, 222 392"/>
          <g class="pov__hand pov__hand--right"><circle class="pov__skin" cx="230" cy="388" r="30"/></g>
        </g>
      </g>
    </g>

    <g class="pov__speed">
      <path class="pov__streak pov__streak--amber pov__streak--o0" stroke-width="7" d="M 120 -80 V 680"/>
      <path class="pov__streak pov__streak--blue pov__streak--o1" stroke-width="4" d="M 178 -80 V 680"/>
      <path class="pov__streak pov__streak--ink pov__streak--o2" stroke-width="9" d="M 240 -80 V 680"/>
      <path class="pov__streak pov__streak--amber pov__streak--o3" stroke-width="3" d="M 300 -80 V 680"/>
      <path class="pov__streak pov__streak--blue pov__streak--o0" stroke-width="6" d="M 352 -80 V 680"/>
      <path class="pov__streak pov__streak--ink pov__streak--o1" stroke-width="7" d="M 410 -80 V 680"/>
      <path class="pov__streak pov__streak--amber pov__streak--o2" stroke-width="4" d="M 468 -80 V 680"/>
      <path class="pov__streak pov__streak--blue pov__streak--o3" stroke-width="9" d="M 528 -80 V 680"/>
      <path class="pov__streak pov__streak--ink pov__streak--o0" stroke-width="3" d="M 590 -80 V 680"/>
      <path class="pov__streak pov__streak--amber pov__streak--o1" stroke-width="6" d="M 650 -80 V 680"/>
      <path class="pov__streak pov__streak--blue pov__streak--o2" stroke-width="7" d="M 700 -80 V 680"/>
    </g>
  </g>
</svg>

<%-- "home" matches no nav item, so no link shows as current page --%>
<% request.setAttribute("navPage", "home"); %>
<%-- .page-landing .topbar rules and landing-loop.js find bar by its class, so plain header wrapper is enough --%>
<header>
<%@ include file="/WEB-INF/jspf/topbar.jspf" %>
</header>

<main id="main">
  <section class="hero">
    <div class="hero__text">
      <div class="hero__wordmark" aria-label="CourseCraft">Course<span class="hero__accent">Craft</span></div>
      <h1 class="hero__title">Craft Your Academic Path</h1>
      <p class="hero__lede">Your <span class="hero__sjsu">SJSU</span> major, mapped. Skip the registration bottlenecks and graduate on time.</p>
      <a class="hero__cta" href="catalog.jsp">
        Get Started
        <svg class="hero__cta-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M4 12h16M13 5l7 7-7 7"/></svg>
      </a>
      <p class="hero__note">Student course project. Not an official SJSU product and not affiliated with SJSU.</p>
    </div>
  </section>

  <section class="features" aria-label="Features">
    <ul class="features__list">
      <li class="feature">
        <img class="feature__img" src="images/map.png" alt="" width="84" height="84">
        <div class="feature__text">
          <h2 class="feature__title">Major Roadmaps</h2>
          <p class="feature__body">See your full major course sequence and plan your semesters with confidence.</p>
        </div>
      </li>
      <li class="feature">
        <img class="feature__img" src="images/alert.png" alt="" width="84" height="84">
        <div class="feature__text">
          <h2 class="feature__title">Bottleneck Alerts</h2>
          <p class="feature__body">Know which classes are high demand, have limited capacity, or are commonly waitlisted.</p>
        </div>
      </li>
      <li class="feature">
        <img class="feature__img" src="images/calendar.png" alt="" width="84" height="84">
        <div class="feature__text">
          <h2 class="feature__title">Smarter Registration</h2>
          <p class="feature__body">Make informed decisions, avoid waitlists, and get the classes you need.</p>
        </div>
      </li>
      <li class="feature">
        <img class="feature__img" src="images/clock.png" alt="" width="84" height="84">
        <div class="feature__text">
          <h2 class="feature__title">On Time Graduation</h2>
          <p class="feature__body">Stay on track, save time and money, and get to your career sooner.</p>
        </div>
      </li>
    </ul>
  </section>
</main>

<script src="js/landing-loop.js" defer></script>

</body>
</html>

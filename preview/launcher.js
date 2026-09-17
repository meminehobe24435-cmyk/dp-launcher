/*
 * Behaviour of the 1:1 preview: same focus model as the Android launcher, driven by the arrow
 * keys instead of a D-pad, plus a scripted timeline used to record the demo video.
 */
(function () {
  'use strict';

  /*
   * The four featured apps of the reference home row.
   *
   * `color` is the card colour sampled from the reference screenshot (the Android build pins the
   * same values per package in AppColorResolver). `iconSize` is the size the brand mark occupies
   * inside the 140x140 icon box, also measured from the reference: real app icons ship with
   * padding, so the marks are not drawn edge to edge.
   */
  const FEATURED = [
    { id: 'netflix', label: 'NETFLIX', icon: 'assets/netflix.svg', color: '#010005', iconSize: [66, 121] },
    { id: 'youtube', label: 'YouTube', icon: 'assets/youtube.svg', color: '#47426A', iconSize: [136, 95] },
    { id: 'play', label: 'Google Play', icon: 'assets/googleplay.svg', color: '#82DD7E', iconSize: [110, 120] },
    { id: 'chrome', label: 'chrome', icon: 'assets/chrome.svg', color: '#8A48D0', iconSize: [118, 118] },
  ];

  const DOCK = [
    { id: 'keystone', label: 'Keystone', icon: 'assets/dock-keystone.svg' },
    { id: 'miracast', label: 'Miracast', icon: 'assets/dock-miracast.svg' },
    { id: 'signal', label: 'Signal Source', icon: 'assets/dock-signal-source.svg' },
    { id: 'myapps', label: 'My Apps', icon: 'assets/dock-my-apps.svg' },
    { id: 'settings', label: 'Settings', icon: 'assets/dock-settings.svg' },
  ];

  /** Apps that only exist in the preview, so the drawer looks like a real device. */
  const EXTRA_APPS = [
    { label: 'Prime Video', color: '#1F70C1' },
    { label: 'Disney+', color: '#1B2A6B' },
    { label: 'Spotify', color: '#1D8348' },
    { label: 'Kodi', color: '#2E4053' },
    { label: 'VLC', color: '#C0392B' },
    { label: 'AirScreen', color: '#1A5276' },
    { label: 'File Manager', color: '#5D6D7E' },
    { label: 'Media Player', color: '#7D3C98' },
  ];

  const state = {
    row: 'dock',
    cardIndex: 0,
    dockIndex: 4,
    gridIndex: 0,
    drawerOpen: false,
    launching: false,
  };

  const dom = {};

  function placeholderIcon(label, color) {
    const letter = label.trim().charAt(0).toUpperCase();
    const svg =
      '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">' +
      '<rect width="100" height="100" rx="22" fill="' + color + '"/>' +
      '<text x="50" y="50" text-anchor="middle" dominant-baseline="central" ' +
      'font-family="Roboto,Arial,sans-serif" font-size="52" fill="#ffffff">' + letter + '</text>' +
      '</svg>';
    return 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg);
  }

  function buildCard(app, interactive) {
    const card = document.createElement('div');
    card.className = 'card';
    card.dataset.app = app.id;
    card.style.background = app.color;
    if (interactive) {
      card.tabIndex = 0;
    }
    const box = document.createElement('div');
    box.className = 'card-icon-box';
    const icon = document.createElement('img');
    icon.className = 'card-icon';
    icon.src = app.icon;
    icon.alt = app.label;
    if (app.iconSize) {
      icon.style.width = app.iconSize[0] + 'px';
      icon.style.height = app.iconSize[1] + 'px';
    }
    box.appendChild(icon);
    const label = document.createElement('span');
    label.className = 'card-label';
    label.textContent = app.label;
    card.appendChild(box);
    card.appendChild(label);
    return card;
  }

  function buildDockItem(item, index) {
    const node = document.createElement('div');
    node.className = 'dock-item';
    node.dataset.dock = item.id;
    node.tabIndex = 0;
    const box = document.createElement('div');
    box.className = 'dock-icon-box';
    const icon = document.createElement('img');
    icon.className = 'dock-icon';
    icon.src = item.icon;
    icon.alt = item.label;
    box.appendChild(icon);
    const label = document.createElement('span');
    label.className = 'dock-label';
    label.textContent = item.label;
    node.appendChild(box);
    node.appendChild(label);
    node.addEventListener('mouseenter', function () {
      state.row = 'dock';
      state.dockIndex = index;
      render();
    });
    return node;
  }

  function buildGridItem(app, index) {
    const node = document.createElement('div');
    node.className = 'grid-item';
    node.tabIndex = 0;
    const icon = document.createElement('img');
    icon.className = 'grid-icon';
    icon.src = app.icon || placeholderIcon(app.label, app.color);
    icon.alt = app.label;
    const label = document.createElement('span');
    label.className = 'grid-label';
    label.textContent = app.label;
    node.appendChild(icon);
    node.appendChild(label);
    node.addEventListener('mouseenter', function () {
      state.gridIndex = index;
      render();
    });
    return node;
  }

  function build() {
    dom.screen = document.getElementById('screen');
    dom.cardRow = document.getElementById('card-row');
    dom.dockRow = document.getElementById('dock-row');
    dom.drawer = document.getElementById('drawer');
    dom.grid = document.getElementById('drawer-grid');
    dom.count = document.getElementById('drawer-count');
    dom.clock = document.getElementById('clock');
    dom.date = document.getElementById('date');

    FEATURED.forEach(function (app) {
      const slot = document.createElement('div');
      slot.className = 'card-slot';
      const card = buildCard(app, true);
      card.addEventListener('mouseenter', function () {
        state.row = 'cards';
        state.cardIndex = FEATURED.indexOf(app);
        render();
      });
      card.addEventListener('click', function () {
        launch(app.label);
      });
      slot.appendChild(card);

      const reflection = document.createElement('div');
      reflection.className = 'card-reflection';
      reflection.appendChild(buildCard(app, false));
      slot.appendChild(reflection);

      dom.cardRow.appendChild(slot);
    });

    DOCK.forEach(function (item, index) {
      const node = buildDockItem(item, index);
      node.addEventListener('click', function () {
        if (item.id === 'myapps') {
          openDrawer();
        } else {
          launch(item.label);
        }
      });
      dom.dockRow.appendChild(node);
    });

    const gridApps = FEATURED.map(function (app) {
      return { label: app.label, icon: app.icon };
    }).concat(EXTRA_APPS);
    gridApps.forEach(function (app, index) {
      dom.grid.appendChild(buildGridItem(app, index));
    });
    dom.count.textContent = gridApps.length + ' apps';
    dom.grid.addEventListener('click', function (event) {
      const item = event.target.closest('.grid-item');
      if (item) {
        launch(item.querySelector('.grid-label').textContent);
      }
    });
  }

  function render() {
    const cards = dom.cardRow.querySelectorAll('.card-slot > .card');
    cards.forEach(function (card, index) {
      card.classList.toggle('focused', state.row === 'cards' && index === state.cardIndex);
    });
    const dockItems = dom.dockRow.querySelectorAll('.dock-item');
    dockItems.forEach(function (item, index) {
      item.classList.toggle('focused', state.row === 'dock' && index === state.dockIndex);
    });
    const gridItems = dom.grid.querySelectorAll('.grid-item');
    gridItems.forEach(function (item, index) {
      item.classList.toggle('focused', state.drawerOpen && index === state.gridIndex);
    });
    dom.drawer.classList.toggle('open', state.drawerOpen);
    dom.screen.classList.toggle('launching', state.launching);
  }

  function openDrawer() {
    state.drawerOpen = true;
    state.gridIndex = 0;
    render();
  }

  function closeDrawer() {
    state.drawerOpen = false;
    render();
  }

  function launch(label) {
    state.launching = true;
    render();
    window.setTimeout(function () {
      state.launching = false;
      state.drawerOpen = false;
      state.row = 'cards';
      state.cardIndex = 0;
      render();
    }, 900);
  }

  function handleKey(key) {
    if (state.drawerOpen) {
      const columns = 6;
      const total = dom.grid.querySelectorAll('.grid-item').length;
      if (key === 'Escape' || key === 'Backspace') {
        closeDrawer();
        return;
      }
      if (key === 'ArrowUp' && state.gridIndex < columns) {
        closeDrawer();
        return;
      }
      if (key === 'ArrowRight') {
        state.gridIndex = Math.min(total - 1, state.gridIndex + 1);
      } else if (key === 'ArrowLeft') {
        state.gridIndex = Math.max(0, state.gridIndex - 1);
      } else if (key === 'ArrowDown') {
        state.gridIndex = Math.min(total - 1, state.gridIndex + columns);
      } else if (key === 'ArrowUp') {
        state.gridIndex = Math.max(0, state.gridIndex - columns);
      } else if (key === 'Enter') {
        launch(dom.grid.querySelectorAll('.grid-label')[state.gridIndex].textContent);
        return;
      }
      render();
      return;
    }

    switch (key) {
      case 'ArrowRight':
        if (state.row === 'cards') {
          state.cardIndex = Math.min(FEATURED.length - 1, state.cardIndex + 1);
        } else {
          state.dockIndex = Math.min(DOCK.length - 1, state.dockIndex + 1);
        }
        break;
      case 'ArrowLeft':
        if (state.row === 'cards') {
          state.cardIndex = Math.max(0, state.cardIndex - 1);
        } else {
          state.dockIndex = Math.max(0, state.dockIndex - 1);
        }
        break;
      case 'ArrowDown':
        if (state.row === 'cards') {
          state.row = 'dock';
          state.dockIndex = Math.min(state.cardIndex, DOCK.length - 1);
        } else {
          openDrawer();
          return;
        }
        break;
      case 'ArrowUp':
        if (state.row === 'dock') {
          state.row = 'cards';
          state.cardIndex = Math.min(state.dockIndex, FEATURED.length - 1);
        } else {
          openDrawer();
          return;
        }
        break;
      case 'Enter':
        if (state.row === 'cards') {
          launch(FEATURED[state.cardIndex].label);
          return;
        }
        if (DOCK[state.dockIndex].id === 'myapps') {
          openDrawer();
          return;
        }
        launch(DOCK[state.dockIndex].label);
        return;
      case 'm':
      case 'M':
      case 'Menu':
        openDrawer();
        return;
      default:
        return;
    }
    render();
  }

  /** Scripted demo used for the screen recording. Times are milliseconds. */
  const TIMELINE = [
    { t: 900, run: function () { state.row = 'cards'; state.cardIndex = 0; render(); } },
    { t: 1500, key: 'ArrowRight' },
    { t: 2100, key: 'ArrowRight' },
    { t: 2700, key: 'ArrowRight' },
    { t: 3600, key: 'ArrowDown' },
    { t: 4200, key: 'ArrowRight' },
    { t: 5200, key: 'ArrowLeft' },
    { t: 6000, key: 'ArrowRight' },
    { t: 7000, key: 'ArrowDown' },
    { t: 8100, key: 'ArrowRight' },
    { t: 8700, key: 'ArrowRight' },
    { t: 9600, key: 'ArrowDown' },
    { t: 10600, key: 'ArrowUp' },
    { t: 11200, key: 'Escape' },
    { t: 12200, key: 'Enter' },
  ];

  function pad(value) {
    return value < 10 ? '0' + value : String(value);
  }

  function tickClock() {
    const now = new Date();
    const hours24 = now.getHours();
    const hours = hours24 % 12 === 0 ? 12 : hours24 % 12;
    const suffix = hours24 < 12 ? 'AM' : 'PM';
    dom.clock.textContent = hours + ':' + pad(now.getMinutes()) + ' ' + suffix;
    const days = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
    const months = ['January', 'February', 'March', 'April', 'May', 'June', 'July',
      'August', 'September', 'October', 'November', 'December'];
    dom.date.textContent = days[now.getDay()] + ',' + months[now.getMonth()] + ' ' + now.getDate();
  }

  function fitStage() {
    const scale = Math.min(window.innerWidth / 1280, window.innerHeight / 720);
    dom.screen.style.setProperty('--stage-scale', String(scale));
  }

  /**
   * Calibration hook. `tools/calibrate.py` re-renders this page with candidate geometry so the
   * design values can be fitted against the reference screenshot instead of guessed.
   * Example: index.html?cardW=245&cardGap=17
   */
  function applyQueryOverrides() {
    const map = {
      cardW: ['--ds-card-width', 'px'],
      cardH: ['--ds-card-height', 'px'],
      cardGap: ['--ds-card-gap', 'px'],
      cardRowTop: ['--ds-card-row-top', 'px'],
      cardLabelSize: ['--ds-card-label-size', 'px'],
      cardLabelBottom: ['--ds-card-label-bottom-margin', 'px'],
      reflectionHeight: ['--ds-reflection-height', 'px'],
      dockRowTop: ['--ds-dock-row-top', 'px'],
      dockItemW: ['--ds-dock-item-width', 'px'],
      dockItemH: ['--ds-dock-item-height', 'px'],
      dockItemGap: ['--ds-dock-item-gap', 'px'],
      dockFocusScale: ['--ds-dock-focus-scale', ''],
      dockBorder: ['--dock-border', 'color'],
      dockLabel: ['--dock-label-color', 'color'],
      statusCenterY: ['--ds-status-center-y', 'px'],
      statusEndMargin: ['--ds-status-end-margin', 'px'],
      statusTextSize: ['--ds-status-text-size', 'px'],
    };
    const params = new URLSearchParams(window.location.search);
    Object.keys(map).forEach(function (key) {
      const value = params.get(key);
      if (!value) return;
      const target = map[key];
      const suffix = target[1] === 'px' ? 'px' : '';
      const prefix = target[1] === 'color' && value.indexOf('#') !== 0 ? '#' : '';
      document.documentElement.style.setProperty(target[0], prefix + value + suffix);
    });
  }

  function init() {
    applyQueryOverrides();
    build();
    tickClock();
    window.setInterval(tickClock, 1000);
    render();
    fitStage();
    window.addEventListener('resize', fitStage);
    window.addEventListener('keydown', function (event) {
      if (event.key.indexOf('Arrow') === 0 || event.key === 'Enter') {
        event.preventDefault();
      }
      handleKey(event.key);
    });
    if (new URLSearchParams(window.location.search).has('play')) {
      playTimeline();
    }
    document.body.classList.add('ready');
  }

  function playTimeline() {
    TIMELINE.forEach(function (step) {
      window.setTimeout(function () {
        if (step.key) {
          handleKey(step.key);
        } else if (step.run) {
          step.run();
        }
      }, step.t);
    });
  }

  window.Launcher = {
    state: state,
    handleKey: handleKey,
    openDrawer: openDrawer,
    closeDrawer: closeDrawer,
    playTimeline: playTimeline,
    render: render,
    timeline: TIMELINE,
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();

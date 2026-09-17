/*
 * Records the demo video of the 1:1 preview.
 *
 * Chrome's DevTools screencast is used instead of repeated `page.screenshot()` calls, so the
 * frames arrive at the compositor rate (smooth animations) with a timestamp each. The raw frames
 * land in preview/out/frames/ and `tools/make_video.py` turns them into the GIF/WebP shipped in
 * demo/.
 *
 * Usage: node tools/capture_video.mjs [--base-url file:///.../preview/index.html]
 */
import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import puppeteer from 'puppeteer-core';

const CHROME_CANDIDATES = [
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe',
  'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
  '/usr/bin/google-chrome',
  '/usr/bin/chromium',
  '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
];

const ROOT = path.resolve(import.meta.dirname, '..');
const FRAME_DIR = path.join(ROOT, 'preview', 'out', 'frames');
const PREVIEW = path.join(ROOT, 'preview', 'index.html');

function findChrome() {
  for (const candidate of CHROME_CANDIDATES) {
    if (fs.existsSync(candidate)) return candidate;
  }
  throw new Error('no Chrome/Edge binary found');
}

function baseUrl() {
  const flag = process.argv.indexOf('--base-url');
  if (flag >= 0 && process.argv[flag + 1]) return process.argv[flag + 1];
  return 'file:///' + PREVIEW.replace(/\\/g, '/');
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

/*
 * The demo script: the same navigation rules the Android build follows.
 * Each entry waits `hold` ms after the previous key, so the recording shows the focus
 * animation of every move.
 */
const SCRIPT = [
  { hold: 1400 },                                        // home screen, exactly as in the reference
  { keys: ['ArrowUp'], hold: 900 },                      // dock -> app row (Netflix)
  { keys: ['ArrowRight'], hold: 650 },
  { keys: ['ArrowRight'], hold: 650 },
  { keys: ['ArrowRight'], hold: 900 },                   // Chrome card focused
  { keys: ['ArrowDown'], hold: 900 },                    // back to the dock (My Apps column)
  { keys: ['ArrowRight'], hold: 1100 },                  // Settings focused again
  { keys: ['ArrowDown'], hold: 1700, screenshot: 'drawer' }, // jump into the all-apps list
  { keys: ['ArrowRight'], hold: 550 },
  { keys: ['ArrowRight'], hold: 550 },
  { keys: ['ArrowDown'], hold: 800 },
  { keys: ['ArrowUp'], hold: 800 },
  { keys: ['ArrowUp'], hold: 1200, screenshot: 'home' }, // UP from the first row closes the drawer
  { keys: ['ArrowUp'], hold: 600 },                      // dock -> app row
  { keys: ['ArrowLeft'], hold: 500 },
  { keys: ['ArrowLeft'], hold: 500 },
  { keys: ['ArrowLeft'], hold: 900 },                    // Netflix card focused
  { keys: ['Enter'], hold: 2000 },                       // open the app
  { hold: 900, screenshot: 'launch' },
];

async function main() {
  fs.rmSync(FRAME_DIR, { recursive: true, force: true });
  fs.mkdirSync(FRAME_DIR, { recursive: true });
  const stillDir = path.join(ROOT, 'preview', 'out', 'stills');
  fs.mkdirSync(stillDir, { recursive: true });

  const browser = await puppeteer.launch({
    executablePath: findChrome(),
    headless: true,
    args: ['--hide-scrollbars', '--force-device-scale-factor=1', '--disable-gpu'],
    defaultViewport: { width: 1280, height: 720 },
  });

  const page = await browser.newPage();
  await page.goto(baseUrl(), { waitUntil: 'load' });
  await page.evaluate(() => document.fonts.ready);

  const client = await page.createCDPSession();
  const frames = [];
  client.on('Page.screencastFrame', async (event) => {
    const index = frames.length;
    frames.push({ index, timestamp: event.metadata.timestamp, data: event.data });
    await client.send('Page.screencastFrameAck', { sessionId: event.sessionId });
  });
  await client.send('Page.startScreencast', {
    format: 'jpeg',
    quality: 92,
    maxWidth: 1280,
    maxHeight: 720,
    everyNthFrame: 1,
  });

  const started = Date.now();
  for (const step of SCRIPT) {
    for (const key of step.keys ?? []) {
      await page.keyboard.press(key);
      await sleep(140);
    }
    await sleep(step.hold ?? 600);
    if (step.screenshot) {
      await page.screenshot({ path: path.join(stillDir, step.screenshot + '.png') });
    }
  }
  const elapsed = Date.now() - started;
  await sleep(400);
  await client.send('Page.stopScreencast');
  await browser.close();

  for (const frame of frames) {
    const buffer = Buffer.from(frame.data, 'base64');
    const name = String(frame.index).padStart(4, '0') + '.jpg';
    fs.writeFileSync(path.join(FRAME_DIR, name), buffer);
  }
  fs.writeFileSync(
    path.join(FRAME_DIR, 'index.json'),
    JSON.stringify({ elapsedMs: elapsed, frames: frames.map((f) => f.timestamp) }, null, 2),
  );
  console.log(`captured ${frames.length} frames over ${elapsed} ms -> ${FRAME_DIR}`);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});

import sharp from "sharp";
import { mkdir } from "node:fs/promises";

const src = process.argv[2];
const outDir = process.argv[3];
const skipBoxes = process.argv[4] ? JSON.parse(process.argv[4]) : []; // regions to exclude (e.g. AED card)

await mkdir(outDir, { recursive: true });

const { data, info } = await sharp(src).ensureAlpha().raw().toBuffer({ resolveWithObject: true });
const { width, height, channels } = info;

const inSkip = (x, y) => skipBoxes.some(b => x >= b.left && x < b.left + b.width && y >= b.top && y < b.top + b.height);
const isOpaque = (x, y) => data[(y * width + x) * channels + 3] > 10 && !inSkip(x, y);

const labels = new Int32Array(width * height).fill(-1);
const queue = new Int32Array(width * height);
const components = [];

for (let y = 0; y < height; y++) {
  for (let x = 0; x < width; x++) {
    const p = y * width + x;
    if (labels[p] !== -1 || !isOpaque(x, y)) continue;
    const id = components.length;
    let qHead = 0, qTail = 0;
    queue[qTail++] = p; labels[p] = id;
    let minX = x, maxX = x, minY = y, maxY = y, count = 0;
    while (qHead < qTail) {
      const cp = queue[qHead++];
      const cx = cp % width, cy = (cp / width) | 0;
      count++;
      minX = Math.min(minX, cx); maxX = Math.max(maxX, cx);
      minY = Math.min(minY, cy); maxY = Math.max(maxY, cy);
      for (const [nx, ny] of [[cx - 1, cy], [cx + 1, cy], [cx, cy - 1], [cx, cy + 1]]) {
        if (nx < 0 || ny < 0 || nx >= width || ny >= height) continue;
        const np = ny * width + nx;
        if (labels[np] !== -1 || !isOpaque(nx, ny)) continue;
        labels[np] = id; queue[qTail++] = np;
      }
    }
    components.push({ id, minX, minY, maxX, maxY, count });
  }
}

const merges = process.argv[5] ? JSON.parse(process.argv[5]) : []; // array of [name, [ids...]]
const used = new Set();
const groups = [];
for (const [name, ids] of merges) {
  groups.push({ name, ids });
  ids.forEach(i => used.add(i));
}
components
  .filter(c => c.count > 300 && !used.has(c.id))
  .forEach((c, i) => groups.push({ name: `auto${i}`, ids: [c.id] }));

for (const group of groups) {
  const relevant = components.filter(c => group.ids.includes(c.id));
  if (!relevant.length) { console.log(group.name, "NOT FOUND (bad id)"); continue; }
  const minX = Math.min(...relevant.map(c => c.minX));
  const minY = Math.min(...relevant.map(c => c.minY));
  const maxX = Math.max(...relevant.map(c => c.maxX));
  const maxY = Math.max(...relevant.map(c => c.maxY));
  const w = maxX - minX + 1, h = maxY - minY + 1;
  const idSet = new Set(group.ids);
  const outBuf = Buffer.alloc(w * h * 4);
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      const sx = minX + x, sy = minY + y;
      const sp = sy * width + sx;
      const dp = (y * w + x) * 4;
      if (idSet.has(labels[sp])) {
        const sIdx = sp * channels;
        outBuf[dp] = data[sIdx]; outBuf[dp + 1] = data[sIdx + 1]; outBuf[dp + 2] = data[sIdx + 2]; outBuf[dp + 3] = data[sIdx + 3];
      }
    }
  }
  const out = `${outDir}/${group.name}.png`;
  await sharp(outBuf, { raw: { width: w, height: h, channels: 4 } }).png().toFile(out);
  console.log(out, `${w}x${h}`, "ids:", group.ids.join(","));
}

console.log("\nAll components (id, box, pixel count):");
components.forEach(c => console.log(c.id, `left=${c.minX} top=${c.minY} w=${c.maxX - c.minX + 1} h=${c.maxY - c.minY + 1}`, "px=" + c.count));

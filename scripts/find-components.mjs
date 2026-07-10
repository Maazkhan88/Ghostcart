import sharp from "sharp";

const src = process.argv[2];
const { data, info } = await sharp(src).ensureAlpha().raw().toBuffer({ resolveWithObject: true });
const { width, height, channels } = info;

const visited = new Uint8Array(width * height);
const isOpaque = (x, y) => data[(y * width + x) * channels + 3] > 10;

const components = [];
const queue = new Int32Array(width * height);

for (let y = 0; y < height; y++) {
  for (let x = 0; x < width; x++) {
    const p = y * width + x;
    if (visited[p] || !isOpaque(x, y)) continue;
    let qHead = 0, qTail = 0;
    queue[qTail++] = p;
    visited[p] = 1;
    let minX = x, maxX = x, minY = y, maxY = y, count = 0;
    while (qHead < qTail) {
      const cp = queue[qHead++];
      const cx = cp % width, cy = (cp / width) | 0;
      count++;
      minX = Math.min(minX, cx); maxX = Math.max(maxX, cx);
      minY = Math.min(minY, cy); maxY = Math.max(maxY, cy);
      const neighbors = [[cx - 1, cy], [cx + 1, cy], [cx, cy - 1], [cx, cy + 1]];
      for (const [nx, ny] of neighbors) {
        if (nx < 0 || ny < 0 || nx >= width || ny >= height) continue;
        const np = ny * width + nx;
        if (visited[np] || !isOpaque(nx, ny)) continue;
        visited[np] = 1;
        queue[qTail++] = np;
      }
    }
    if (count > 80) components.push({ minX, minY, maxX, maxY, count });
  }
}

// Merge components whose bounding boxes are close (within 12px) — likely one composition.
function boxesClose(a, b, gap = 12) {
  return !(a.maxX + gap < b.minX || b.maxX + gap < a.minX || a.maxY + gap < b.minY || b.maxY + gap < a.minY);
}
let merged = true;
while (merged) {
  merged = false;
  outer: for (let i = 0; i < components.length; i++) {
    for (let j = i + 1; j < components.length; j++) {
      if (boxesClose(components[i], components[j])) {
        const a = components[i], b = components[j];
        components[i] = {
          minX: Math.min(a.minX, b.minX), minY: Math.min(a.minY, b.minY),
          maxX: Math.max(a.maxX, b.maxX), maxY: Math.max(a.maxY, b.maxY),
          count: a.count + b.count,
        };
        components.splice(j, 1);
        merged = true;
        break outer;
      }
    }
  }
}

components.sort((a, b) => a.minY - b.minY || a.minX - b.minX);
console.log(JSON.stringify(components.map(c => ({
  left: c.minX, top: c.minY, width: c.maxX - c.minX + 1, height: c.maxY - c.minY + 1, pixels: c.count,
})), null, 2));

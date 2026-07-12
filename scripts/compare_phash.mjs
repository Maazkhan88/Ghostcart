import sharp from 'sharp';
import fs from 'fs';
import path from 'path';

const mobileUiDir = 'mobileui';
const designDir = 'design/mobile-ui';

async function getPerceptualHash(filePath) {
    try {
        const buffer = await sharp(filePath)
            .resize(16, 16, { fit: 'fill' }) // larger size for better resolution
            .greyscale()
            .raw()
            .toBuffer();
        return buffer;
    } catch (e) {
        return null;
    }
}

function compareHashes(h1, h2) {
    if (!h1 || !h2 || h1.length !== h2.length) return Infinity;
    let diff = 0;
    for (let i = 0; i < h1.length; i++) {
        diff += Math.abs(h1[i] - h2[i]);
    }
    return diff;
}

try {
    const mobileUiFiles = fs.readdirSync(mobileUiDir).filter(f => f.endsWith('.png')).sort();
    const designFiles = fs.readdirSync(designDir).filter(f => f.endsWith('.png')).sort();

    console.log(`Comparing...`);

    const designHashes = [];
    for (const dFile of designFiles) {
        const hash = await getPerceptualHash(path.join(designDir, dFile));
        designHashes.push({ name: dFile, hash });
    }

    for (const mFile of mobileUiFiles) {
        const mHash = await getPerceptualHash(path.join(mobileUiDir, mFile));
        const matches = [];

        for (const dHash of designHashes) {
            const diff = compareHashes(mHash, dHash.hash);
            matches.push({ name: dHash.name, diff });
        }

        matches.sort((a, b) => a.diff - b.diff);
        console.log(`\nmatches for mobileui/${mFile}:`);
        for (let i = 0; i < 3; i++) {
            console.log(`  - ${matches[i].name} (score: ${matches[i].diff})`);
        }
    }
} catch (e) {
    console.error(e);
}

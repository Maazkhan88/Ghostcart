import fs from 'fs';
import path from 'path';
import crypto from 'crypto';

const mobileUiDir = 'mobileui';
const designDir = 'design/mobile-ui';
const randomAssetsDir = 'randomassets';

function getHash(filePath) {
    try {
        const fileBuffer = fs.readFileSync(filePath);
        const hashSum = crypto.createHash('sha256');
        hashSum.update(fileBuffer);
        return hashSum.digest('hex');
    } catch (e) {
        return null;
    }
}

function getFiles(dir) {
    try {
        return fs.readdirSync(dir)
            .filter(f => f.endsWith('.png'))
            .map(f => ({ name: f, path: path.join(dir, f), size: fs.statSync(path.join(dir, f)).size }));
    } catch (e) {
        return [];
    }
}

const mobileUiFiles = getFiles(mobileUiDir);
const designFiles = getFiles(designDir);
const randomAssetsFiles = getFiles(randomAssetsDir);

console.log(`Found ${mobileUiFiles.length} files in mobileui`);
console.log(`Found ${designFiles.length} files in design/mobile-ui`);
console.log(`Found ${randomAssetsFiles.length} files in randomassets`);

for (const mFile of mobileUiFiles) {
    const mHash = getHash(mFile.path);
    let match = null;

    // Check design/mobile-ui
    for (const dFile of designFiles) {
        if (getHash(dFile.path) === mHash) {
            match = { source: 'design/mobile-ui', name: dFile.name };
            break;
        }
    }

    if (!match) {
        // Check randomassets
        for (const rFile of randomAssetsFiles) {
            if (getHash(rFile.path) === mHash) {
                match = { source: 'randomassets', name: rFile.name };
                break;
            }
        }
    }

    if (match) {
        console.log(`mobileui/${mFile.name} (size: ${mFile.size}) EXACT MATCH with ${match.source}/${match.name}`);
    } else {
        console.log(`mobileui/${mFile.name} (size: ${mFile.size}) NO EXACT MATCH`);
    }
}

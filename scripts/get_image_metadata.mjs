import sharp from 'sharp';
import fs from 'fs';
import path from 'path';

const dir = 'mobileui';
try {
    const files = fs.readdirSync(dir).filter(f => f.endsWith('.png'));
    for (const file of files) {
        const filePath = path.join(dir, file);
        const metadata = await sharp(filePath).metadata();
        console.log(`${file}: size=${fs.statSync(filePath).size}, width=${metadata.width}, height=${metadata.height}, format=${metadata.format}`);
    }
} catch (e) {
    console.error(e);
}

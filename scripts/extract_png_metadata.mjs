import fs from 'fs';
import path from 'path';

// A simple PNG parser to extract tEXt/iTXt metadata chunks
function readPngTextChunks(filePath) {
    const buffer = fs.readFileSync(filePath);
    let offset = 8; // skip PNG signature
    const chunks = [];

    while (offset < buffer.length) {
        if (offset + 8 > buffer.length) break;
        const length = buffer.readUInt32BE(offset);
        const type = buffer.toString('ascii', offset + 4, offset + 8);
        
        if (type === 'tEXt') {
            const data = buffer.slice(offset + 8, offset + 8 + length);
            let i = 0;
            while (i < data.length && data[i] !== 0) i++;
            const keyword = data.toString('ascii', 0, i);
            const text = data.toString('utf8', i + 1);
            chunks.push({ type, keyword, text });
        } else if (type === 'iTXt') {
            const data = buffer.slice(offset + 8, offset + 8 + length);
            let i = 0;
            while (i < data.length && data[i] !== 0) i++;
            const keyword = data.toString('utf8', 0, i);
            // iTXt has compression flag, method, language tag, translated keyword before text.
            // For simple extraction we can find the null terminators.
            let j = i + 3; // skip compression flag, method
            while (j < data.length && data[j] !== 0) j++; // skip language tag
            j++;
            while (j < data.length && data[j] !== 0) j++; // skip translated keyword
            j++;
            const text = data.toString('utf8', j);
            chunks.push({ type, keyword, text });
        }
        
        offset += 12 + length; // length (4) + type (4) + data (length) + crc (4)
    }
    return chunks;
}

const dir = 'mobileui';
try {
    const files = fs.readdirSync(dir).filter(f => f.endsWith('.png')).sort();
    for (const file of files) {
        const filePath = path.join(dir, file);
        const chunks = readPngTextChunks(filePath);
        console.log(`=== ${file} ===`);
        if (chunks.length === 0) {
            console.log('  No text chunks found');
        } else {
            for (const chunk of chunks) {
                console.log(`  [${chunk.type}] ${chunk.keyword}: ${chunk.text.substring(0, 500)}${chunk.text.length > 500 ? '...' : ''}`);
            }
        }
    }
} catch (e) {
    console.error(e);
}

import fs from 'fs';
try {
    const stats = fs.statSync('android/app/build/outputs/apk/debug/app-debug.apk');
    console.log(`APK Size: ${stats.size} bytes`);
    console.log(`APK Created: ${stats.birthtime}`);
    console.log(`APK Modified: ${stats.mtime}`);
} catch (e) {
    console.error(e);
}

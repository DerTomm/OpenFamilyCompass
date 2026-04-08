const fs = require('fs');
const path = require('path');

const sourceFile = process.env.GOOGLE_SERVICES_JSON;
const targetFile = path.join(__dirname, '..', 'android', 'app', 'google-services.json');

if (sourceFile && fs.existsSync(sourceFile)) {
    fs.copyFileSync(sourceFile, targetFile);
    console.log(`Copied google-services.json from ${sourceFile} to ${targetFile}`);
} else if (fs.existsSync(targetFile)) {
    console.log('google-services.json already present at target location, skipping copy.');
} else {
    console.warn('WARNING: google-services.json not found. Build may fail.');
}

const appJson = require('./app.json');

module.exports = {
    ...appJson.expo,
    android: {
        ...appJson.expo.android,
        googleServicesFile: process.env.GOOGLE_SERVICES_JSON || './android/app/google-services.json',
    },
};

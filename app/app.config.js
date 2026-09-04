const packageJson = require('./package.json');

module.exports = ({ config }) => ({
  ...config,
  version: packageJson.version,
});

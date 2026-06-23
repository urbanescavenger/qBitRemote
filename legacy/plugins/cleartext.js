// Config plugin: allow HTTP (cleartext) traffic to qBittorrent servers.
// Android: android:usesCleartextTraffic="true" on <application>.
// iOS: NSAppTransportSecurity.NSAllowsArbitraryLoads=true (+ localhost exception).
const { withAndroidManifest, withInfoPlist } = require('@expo/config-plugins');

function withCleartextAndroid(config) {
  return withAndroidManifest(config, (config) => {
    const applications = config.modResults.manifest.application;
    if (Array.isArray(applications)) {
      applications.forEach((app) => {
        app.$['android:usesCleartextTraffic'] = 'true';
      });
    }
    return config;
  });
}

function withCleartextIos(config) {
  return withInfoPlist(config, (config) => {
    config.modResults.NSAppTransportSecurity = {
      NSAllowsArbitraryLoads: true,
      NSExceptionDomains: {
        localhost: { NSExceptionAllowsInsecureHTTPLoads: true },
      },
    };
    return config;
  });
}

module.exports = (config) => withCleartextIos(withCleartextAndroid(config));
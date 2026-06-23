// Config plugin: re-inject a release signingConfig into android/app/build.gradle
// on every prebuild, gated on the `key.store` gradle property so that:
//   - CI tag builds that pass -Pkey.store/-Pkey.alias/-Pkey.store.password/
//     -Pkey.key.password sign the release APK with the repo keystore secret;
//   - everyone else (local `expo run:android`, branch debug CI) falls back to
//     the debug keystore so the build still produces an installable APK.
//
// Appended at the end of the generated build.gradle (Gradle lets you re-open the
// signingConfigs/buildTypes containers; last assignment wins), so it overrides
// whatever the prebuild template set without fragile in-block string surgery.
const { withAppBuildGradle } = require('@expo/config-plugins');

const MARKER = '// === begin injected release signing (plugins/signing.js) ===';

const SIGNING_BLOCK = `
${MARKER}
android {
    if (project.hasProperty('key.store')) {
        signingConfigs {
            release {
                storeFile file(project.properties['key.store'])
                storePassword project.properties['key.store.password']
                keyAlias project.properties['key.alias']
                keyPassword project.properties['key.key.password']
            }
        }
    }
    buildTypes {
        release {
            signingConfig project.hasProperty('key.store')
                ? signingConfigs.release : signingConfigs.debug
        }
    }
}
// === end injected release signing ===
`;

function withSigning(config) {
  return withAppBuildGradle(config, (config) => {
    const contents = config.modResults.contents;
    if (contents.includes(MARKER)) {
      return config; // idempotent across re-prebuilds
    }
    config.modResults.contents = contents + '\n' + SIGNING_BLOCK;
    return config;
  });
}

module.exports = (config) => withSigning(config);
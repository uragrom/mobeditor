# Changelog

All notable changes to this project will be documented in this file.

## [1.0.0] - 2024

### Fixed
- **Fixed duplicate files issue for Modrinth**
  - Configured build to create two files with different names:
    - `mobeditor-1.0.0-dev.jar` - deobfuscated version for development
    - `mobeditor-1.0.0.jar` - reobfuscated version for Modrinth publication
  - This resolves CDN link conflicts when uploading to Modrinth
  - For publication, use only `mobeditor-1.0.0.jar`

- **Fixed resource structure**
  - Removed incorrectly placed file `Postal2.ogg` from `src/main/java/com/mobeditor/resources/`
  - Resources are now only in the correct folder `src/main/resources/`

- **Improved build configuration**
  - Added exclusions for `bin/` folder from JAR file builds
  - Configured proper archive names for dev and production versions

### Technical Details
- Updated `build.gradle` for proper JAR file name configuration
- Added `bin/**` exclusions to `jar` and `reobfJar` tasks
- Configured `sourceSets.main.resources` with `bin/` folder exclusion

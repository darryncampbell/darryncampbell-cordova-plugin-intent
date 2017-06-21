#!/usr/bin/env node

var fs = require('fs');
fs.writeFileSync('./platforms/android/res/xml/file_paths.xml', "<paths xmlns:android='http://schemas.android.com/apk/res/android'><external-path name='files' path='.' /></paths>");

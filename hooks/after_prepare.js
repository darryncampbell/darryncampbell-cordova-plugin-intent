const fs = require("fs");
const replace = require('replace-in-file');

function androidXUpgrade (ctx) {
    if (!ctx.opts.platforms.includes('android'))
        return;

    const enableAndroidX = "android.useAndroidX=true";
    const enableJetifier = "android.enableJetifier=true";
    const gradlePropertiesPath = "./platforms/android/gradle.properties";

    let gradleProperties = fs.readFileSync(gradlePropertiesPath, "utf8");

    if (gradleProperties)
    {
        const isAndroidXEnabled = gradleProperties.includes(enableAndroidX);
        const isJetifierEnabled = gradleProperties.includes(enableJetifier);

        if (isAndroidXEnabled && isJetifierEnabled)
            return;

        if (isAndroidXEnabled === false)
            gradleProperties += "\n" + enableAndroidX;

        if (isJetifierEnabled === false)
            gradleProperties += "\n" + enableJetifier;

        fs.writeFileSync(gradlePropertiesPath, gradleProperties);
    }
}

function androidXReplace (ctx) {
    if (!ctx.opts.platforms.includes('android'))
        return;
        
    replace.sync({
        files: 'platforms/android/**/*',
        from: /android\.support\.annotation\.RequiresApi/g,
        to: 'androidx.annotation.RequiresApi',
    });
}

module.exports = function (ctx) {
    androidXUpgrade(ctx);
    androidXReplace(ctx);
};

cordova.define("com.darryncampbell.plugin.intent.IntentShim", function(require, exports, module) {
//exports.coolMethod = function(arg0, success, error) {
//    exec(success, error, "EnterpriseBarcode", "coolMethod", [arg0]);
//};

var argscheck = require('cordova/argscheck'),
    channel = require('cordova/channel'),
    utils = require('cordova/utils'),
    exec = require('cordova/exec'),
    cordova = require('cordova');


/**
 * This represents the mobile device, and provides properties for inspecting the model, version, UUID of the
 * phone, etc.
 * @constructor
 */
function IntentShim() {
    this.available = false;
    var me = this;
}

    IntentShim.prototype.ACTION_SEND = "android.intent.action.SEND";
    IntentShim.prototype.ACTION_VIEW= "android.intent.action.VIEW";
    IntentShim.prototype.EXTRA_TEXT = "android.intent.extra.TEXT";
    IntentShim.prototype.EXTRA_SUBJECT = "android.intent.extra.SUBJECT";
    IntentShim.prototype.EXTRA_STREAM = "android.intent.extra.STREAM";
    IntentShim.prototype.EXTRA_EMAIL = "android.intent.extra.EMAIL";
    IntentShim.prototype.ACTION_CALL = "android.intent.action.CALL";
    IntentShim.prototype.ACTION_SENDTO = "android.intent.action.SENDTO";
    //  StartActivityForResult
    IntentShim.prototype.ACTION_GET_CONTENT = "android.intent.action.GET_CONTENT";
    IntentShim.prototype.ACTION_PICK = "android.intent.action.PICK";
    IntentShim.prototype.PICK_CONTACT = "content://com.android.contacts/contacts";

/**
 * @param {Function} successCallback The function to call when the heading data is available
 * @param {Function} errorCallback The function to call when there is an error getting the heading data. (OPTIONAL)
 */
IntentShim.prototype.startActivity = function(params, successCallback, errorCallback) {
    argscheck.checkArgs('off', 'IntentShim.startActivity', arguments);
    exec(successCallback, errorCallback, "IntentShim", "startActivity", [params]);
};

IntentShim.prototype.startActivityForResult = function(params, successCallback, errorCallback) {
    argscheck.checkArgs('off', 'IntentShim.startActivity', arguments);
    exec(successCallback, errorCallback, "IntentShim", "startActivityForResult", [params]);
};

IntentShim.prototype.startService = function(params, successCallback, errorCallback) {
    argscheck.checkArgs('off', 'IntentShim.startService', arguments);
    exec(successCallback, errorCallback, "IntentShim", "startService", [params]);
};

IntentShim.prototype.sendBroadcast = function(params, successCallback, errorCallback) {
    argscheck.checkArgs('off', 'IntentShim.sendBroadcast', arguments);
    exec(successCallback, errorCallback, "IntentShim", "sendBroadcast", [params]);
};

IntentShim.prototype.sendBroadcast = function(params, successCallback, errorCallback) {
    argscheck.checkArgs('off', 'IntentShim.sendBroadcast', arguments);
    exec(successCallback, errorCallback, "IntentShim", "sendBroadcast", [params]);
};

IntentShim.prototype.registerBroadcastReceiver = function(params, callback) {
    argscheck.checkArgs('of', 'IntentShim.registerBroadcastReceiver', arguments);
    exec(callback, null, "IntentShim", "registerBroadcastReceiver", [params]);
};

IntentShim.prototype.unregisterBroadcastReceiver = function() {
    argscheck.checkArgs('', 'IntentShim.unregisterBroadcastReceiver', arguments);
    exec(null, null, "IntentShim", "unregisterBroadcastReceiver", []);
};

IntentShim.prototype.onIntent = function(callback) {
    argscheck.checkArgs('f', 'IntentShim.onIntent', arguments);
    exec(callback, null, "IntentShim", "onIntent", [callback]);
};

IntentShim.prototype.onActivityResult = function(callback) {
    argscheck.checkArgs('f', 'IntentShim.onActivityResult', arguments);
    exec(callback, null, "IntentShim", "onActivityResult", [callback]);
};




//module.exports = new IntentShim();
window.intentShim = new IntentShim();
window.plugins = window.plugins || {};
window.plugins.intentShim = window.intentShim;



});

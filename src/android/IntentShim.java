package com.darryncampbell.cordova.plugin.intent;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.text.Html;
import android.util.Log;
import android.webkit.MimeTypeMap;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaActivity;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static android.os.Environment.getExternalStorageDirectory;
import static android.os.Environment.getExternalStorageState;

public class IntentShim extends CordovaPlugin {

    private static final String LOG_TAG = "Cordova Intents Shim";
    private CallbackContext onNewIntentCallbackContext = null;
    private CallbackContext onBroadcastCallbackContext = null;
    private CallbackContext onActivityResultCallbackContext = null;

    public IntentShim() {

    }

    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException
    {
        Log.d(LOG_TAG, "Action: " + action);
        if (action.equals("startActivity") || action.equals("startActivityForResult"))
        {
            //  Credit: https://github.com/chrisekelley/cordova-webintent
            if (args.length() != 1) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                return false;
            }

            final CordovaResourceApi resourceApi = webView.getResourceApi();
            JSONObject obj = args.getJSONObject(0);
            String type = obj.has("type") ? obj.getString("type") : null;
            //Uri uri = obj.has("url") ? resourceApi.remapUri(Uri.parse(obj.getString("url"))) : null;
            Uri uri = null;
            if (obj.has("url"))
            {
                String uriAsString = obj.getString("url");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && uriAsString.startsWith("file://"))
                {
                    uri = remapUriWithFileProvider(uriAsString, callbackContext);
                }
                else
                {
                    uri = resourceApi.remapUri(Uri.parse(obj.getString("url")));
                }
            }
            JSONObject extras = obj.has("extras") ? obj.getJSONObject("extras") : null;
            Map<String, String> extrasMap = new HashMap<String, String>();
            int requestCode = obj.has("requestCode") ? obj.getInt("requestCode") : 1;

            // Populate the extras if any exist
            if (extras != null) {
                JSONArray extraNames = extras.names();
                for (int i = 0; i < extraNames.length(); i++) {
                    String key = extraNames.getString(i);
                    String value = extras.getString(key);
                    extrasMap.put(key, value);
                }
            }

            boolean bExpectResult = false;
            if (action.equals("startActivityForResult"))
            {
                bExpectResult = true;
                this.onActivityResultCallbackContext = callbackContext;
            }
            startActivity(obj.getString("action"), uri, type, extrasMap, bExpectResult, requestCode, callbackContext);

            return true;
        }
        else if (action.equals("sendBroadcast"))
        {
            //  Credit: https://github.com/chrisekelley/cordova-webintent
            if (args.length() != 1) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                return false;
            }

            // Parse the arguments
            JSONObject obj = args.getJSONObject(0);
            JSONObject extras = obj.has("extras") ? obj.getJSONObject("extras") : null;
            Map<String, String> extrasMap = new HashMap<String, String>();

            if (extras != null) {
                JSONArray extraNames = extras.names();
                for (int i = 0; i < extraNames.length(); i++) {
                    String key = extraNames.getString(i);
                    String value = extras.getString(key);
                    extrasMap.put(key, value);
                }
            }

            sendBroadcast(obj.getString("action"), extrasMap);
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
            return true;
        } else if (action.equals("registerBroadcastReceiver")) {
            try
            {
                //  Ensure we only have a single registered broadcast receiver
                ((CordovaActivity)this.cordova.getActivity()).unregisterReceiver(myBroadcastReceiver);
            }
            catch (IllegalArgumentException e) {}

            //  No error callback
            if(args.length() != 1) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                return false;
            }

            //  Expect an array of filterActions
            JSONObject obj = args.getJSONObject(0);
            JSONArray filterActions = obj.has("filterActions") ? obj.getJSONArray("filterActions") : null;
            if (filterActions == null || filterActions.length() == 0)
            {
                //  The arguments are not correct
                Log.w(LOG_TAG, "filterActions argument is not in the expected format");
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                return false;
            }

            this.onBroadcastCallbackContext = callbackContext;

            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);

            IntentFilter filter = new IntentFilter();
            for (int i = 0; i < filterActions.length(); i++) {
                Log.d(LOG_TAG, "Registering broadcast receiver for filter: " + filterActions.getString(i));
                filter.addAction(filterActions.getString(i));
            }

            //  Allow an array of filterCategories
            JSONArray filterCategories = obj.has("filterCategories") ? obj.getJSONArray("filterCategories") : null;
            if (filterCategories != null) {
                for (int i = 0; i < filterCategories.length(); i++) {
                    Log.d(LOG_TAG, "Registering broadcast receiver for category filter: " + filterCategories.getString(i));
                    filter.addCategory(filterCategories.getString(i));
                }
            }
            
            //  Add any specified Data Schemes
            //  https://github.com/darryncampbell/darryncampbell-cordova-plugin-intent/issues/24
            JSONArray filterDataSchemes = obj.has("filterDataSchemes") ? obj.getJSONArray("filterDataSchemes") : null;
            if (filterDataSchemes != null && filterDataSchemes.length() > 0)
            {
                for (int i = 0; i < filterDataSchemes.length(); i++)
                {
                    Log.d(LOG_TAG, "Associating data scheme to filter: " + filterDataSchemes.getString(i));
                    filter.addDataScheme(filterDataSchemes.getString(i));
                }
            }

            ((CordovaActivity)this.cordova.getActivity()).registerReceiver(myBroadcastReceiver, filter);

            callbackContext.sendPluginResult(result);
        }
        else if (action.equals("unregisterBroadcastReceiver"))
        {
			try
			{
				((CordovaActivity)this.cordova.getActivity()).unregisterReceiver(myBroadcastReceiver);
			}
            catch (IllegalArgumentException e) {}
        }
        else if (action.equals("onIntent"))
        {
            //  Credit: https://github.com/napolitano/cordova-plugin-intent
            if(args.length() != 1) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                return false;
            }

            this.onNewIntentCallbackContext = callbackContext;

            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
            return true;
        }
        else if (action.equals("onActivityResult"))
        {
            if(args.length() != 1) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                return false;
            }

            this.onActivityResultCallbackContext = callbackContext;

            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
            return true;
        }
        else if (action.equals("getIntent"))
        {
            //  Credit: https://github.com/napolitano/cordova-plugin-intent
            if(args.length() != 0) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                return false;
            }

            Intent intent = cordova.getActivity().getIntent();
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, getIntentJson(intent)));
            return true;
        }
        else if (action.equals("sendResult"))
        {
            //  Assuming this application was started with startActivityForResult, send the result back
            //  https://github.com/darryncampbell/darryncampbell-cordova-plugin-intent/issues/3
            Intent result = new Intent();
            if (args.length() > 0)
            {
                JSONObject json = args.getJSONObject(0);
                JSONObject extras = (json.has("extras"))?json.getJSONObject("extras"):null;

                // Populate the extras if any exist
                if (extras != null) {
                    JSONArray extraNames = extras.names();
                    for (int i = 0; i < extraNames.length(); i++) {
                        String key = extraNames.getString(i);
                        String value = extras.getString(key);
                        result.putExtra(key, value);
                    }
                }
            }

            //set result
            cordova.getActivity().setResult(Activity.RESULT_OK, result);
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));

            //finish the activity
            cordova.getActivity().finish();

        }

        return true;
    }

    private Uri remapUriWithFileProvider(String uriAsString, final CallbackContext callbackContext)
    {
        //  Create the URI via FileProvider  Special case for N and above when installing apks
        int permissionCheck = ContextCompat.checkSelfPermission(this.cordova.getActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED)
        {
            //  Could do better here - if the app does not already have permission should
            //  only continue when we get the success callback from this.
            ActivityCompat.requestPermissions(this.cordova.getActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            callbackContext.error("Please grant read external storage permission");
            return null;
        }

        try
        {
            String externalStorageState = getExternalStorageState();
            if (externalStorageState.equals(Environment.MEDIA_MOUNTED) || externalStorageState.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
                String fileName = uriAsString.substring(uriAsString.indexOf('/') + 2, uriAsString.length());
                File uriAsFile = new File(fileName);
                boolean fileExists = uriAsFile.exists();
                if (!fileExists)
                {
                    Log.e(LOG_TAG, "File at path " + uriAsFile.getPath() + " with name " + uriAsFile.getName() + "does not exist");
                    callbackContext.error("File not found: " + uriAsFile.toString());
                    return null;
                }
                String PACKAGE_NAME = this.cordova.getActivity().getPackageName() + ".provider";
                Uri uri = FileProvider.getUriForFile(this.cordova.getActivity().getApplicationContext(), PACKAGE_NAME, uriAsFile);
                return uri;
            }
            else
            {
                Log.e(LOG_TAG, "Storage directory is not mounted.  Please ensure the device is not connected via USB for file transfer");
                callbackContext.error("Storage directory is returning not mounted");
                return null;
            }
        }
        catch(StringIndexOutOfBoundsException e)
        {
            Log.e(LOG_TAG, "URL is not well formed");
            callbackContext.error("URL is not well formed");
            return null;
        }
    }

    private void startActivity(String action, Uri uri, String type, Map<String, String> extras, boolean bExpectResult, int requestCode, CallbackContext callbackContext) {
        //  Credit: https://github.com/chrisekelley/cordova-webintent
        Intent i = (uri != null ? new Intent(action, uri) : new Intent(action));

        if (type != null && uri != null) {
            i.setDataAndType(uri, type); //Fix the crash problem with android 2.3.6
        } else {
            if (type != null) {
                i.setType(type);
            }
            if (uri != null)
            {
                i.setData(uri);
            }
        }

        for (String key : extras.keySet()) {
            String value = extras.get(key);
            // If type is text html, the extra text must sent as HTML
            if (key.equals(Intent.EXTRA_TEXT) && type.equals("text/html")) {
                i.putExtra(key, Html.fromHtml(value));
            } else if (key.equals(Intent.EXTRA_STREAM)) {
                // allows sharing of images as attachments.
                // value in this case should be a URI of a file
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && value.startsWith("file://"))
                {
                    Uri uriOfStream = remapUriWithFileProvider(value, callbackContext);
                    if (uriOfStream != null)
                        i.putExtra(key, uriOfStream);
                }
                else
                {
                    final CordovaResourceApi resourceApi = webView.getResourceApi();
                    i.putExtra(key, resourceApi.remapUri(Uri.parse(value)));
                }
            } else if (key.equals(Intent.EXTRA_EMAIL)) {
                // allows to add the email address of the receiver
                i.putExtra(Intent.EXTRA_EMAIL, new String[] { value });
            } else {
                i.putExtra(key, value);
            }
        }

        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (i.resolveActivityInfo(this.cordova.getActivity().getPackageManager(), 0) != null)
        {
        if (bExpectResult)
        {
            cordova.setActivityResultCallback(this);
            ((CordovaActivity) this.cordova.getActivity()).startActivityForResult(i, requestCode);
        }
        else
            {
            ((CordovaActivity)this.cordova.getActivity()).startActivity(i);
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
            }
        }
        else
        {
            //  Return an error as there is no app to handle this intent
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR));
        }
    }

    private void sendBroadcast(String action, Map<String, String> extras) {
        //  Credit: https://github.com/chrisekelley/cordova-webintent
        Intent intent = new Intent();
        intent.setAction(action);
        for (String key : extras.keySet()) {
            String value = extras.get(key);
            intent.putExtra(key, value);
        }

        ((CordovaActivity)this.cordova.getActivity()).sendBroadcast(intent);
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (this.onNewIntentCallbackContext != null) {

            PluginResult result = new PluginResult(PluginResult.Status.OK, getIntentJson(intent));
            result.setKeepCallback(true);
            this.onNewIntentCallbackContext.sendPluginResult(result);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        super.onActivityResult(requestCode, resultCode, intent);
        if (onActivityResultCallbackContext != null && intent != null)
        {
            intent.putExtra("requestCode", requestCode);
            intent.putExtra("resultCode", resultCode);
            PluginResult result = new PluginResult(PluginResult.Status.OK, getIntentJson(intent));
            result.setKeepCallback(true);
            onActivityResultCallbackContext.sendPluginResult(result);
        }
        else if (onActivityResultCallbackContext != null)
        {
            Intent canceledIntent = new Intent();
            canceledIntent.putExtra("requestCode", requestCode);
            canceledIntent.putExtra("resultCode", resultCode);
            PluginResult canceledResult = new PluginResult(PluginResult.Status.OK, getIntentJson(canceledIntent));
            canceledResult.setKeepCallback(true);
            onActivityResultCallbackContext.sendPluginResult(canceledResult);
        }

    }

    private BroadcastReceiver myBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (onBroadcastCallbackContext != null)
            {
                PluginResult result = new PluginResult(PluginResult.Status.OK, getIntentJson(intent));
                result.setKeepCallback(true);
                onBroadcastCallbackContext.sendPluginResult(result);
            }
        }
    };

    /**
     * Return JSON representation of intent attributes
     *
     * @param intent
     * Credit: https://github.com/napolitano/cordova-plugin-intent
     */
    private JSONObject getIntentJson(Intent intent) {
        JSONObject intentJSON = null;
        ClipData clipData = null;
        JSONObject[] items = null;
        ContentResolver cR = this.cordova.getActivity().getApplicationContext().getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            clipData = intent.getClipData();
            if(clipData != null) {
                int clipItemCount = clipData.getItemCount();
                items = new JSONObject[clipItemCount];

                for (int i = 0; i < clipItemCount; i++) {

                    ClipData.Item item = clipData.getItemAt(i);

                    try {
                        items[i] = new JSONObject();
                        items[i].put("htmlText", item.getHtmlText());
                        items[i].put("intent", item.getIntent());
                        items[i].put("text", item.getText());
                        items[i].put("uri", item.getUri());

                        if (item.getUri() != null) {
                            String type = cR.getType(item.getUri());
                            String extension = mime.getExtensionFromMimeType(cR.getType(item.getUri()));

                            items[i].put("type", type);
                            items[i].put("extension", extension);
                        }

                    } catch (JSONException e) {
                        Log.d(LOG_TAG, " Error thrown during intent > JSON conversion");
                        Log.d(LOG_TAG, e.getMessage());
                        Log.d(LOG_TAG, Arrays.toString(e.getStackTrace()));
                    }

                }
            }
        }

        try {
            intentJSON = new JSONObject();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if(items != null) {
                    intentJSON.put("clipItems", new JSONArray(items));
                }
            }

            intentJSON.put("type", intent.getType());
            intentJSON.put("extras", toJsonObject(intent.getExtras()));
            intentJSON.put("action", intent.getAction());
            intentJSON.put("categories", intent.getCategories());
            intentJSON.put("flags", intent.getFlags());
            intentJSON.put("component", intent.getComponent());
            intentJSON.put("data", intent.getData());
            intentJSON.put("package", intent.getPackage());

            return intentJSON;
        } catch (JSONException e) {
            Log.d(LOG_TAG, " Error thrown during intent > JSON conversion");
            Log.d(LOG_TAG, e.getMessage());
            Log.d(LOG_TAG, Arrays.toString(e.getStackTrace()));

            return null;
        }
    }

    private static JSONObject toJsonObject(Bundle bundle) {
        //  Credit: https://github.com/napolitano/cordova-plugin-intent
        try {
            return (JSONObject) toJsonValue(bundle);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Cannot convert bundle to JSON: " + e.getMessage(), e);
        }
    }

    private static Object toJsonValue(final Object value) throws JSONException {
        //  Credit: https://github.com/napolitano/cordova-plugin-intent
        if (value == null) {
            return null;
        } else if (value instanceof Bundle) {
            final Bundle bundle = (Bundle) value;
            final JSONObject result = new JSONObject();
            for (final String key : bundle.keySet()) {
                result.put(key, toJsonValue(bundle.get(key)));
            }
            return result;
        } else if (value.getClass().isArray()) {
            final JSONArray result = new JSONArray();
            int length = Array.getLength(value);
            for (int i = 0; i < length; ++i) {
                result.put(i, toJsonValue(Array.get(value, i)));
            }
            return result;
        } else if (
                value instanceof String
                        || value instanceof Boolean
                        || value instanceof Integer
                        || value instanceof Long
                        || value instanceof Double) {
            return value;
        } else {
            return String.valueOf(value);
        }
    }
}



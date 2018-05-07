package com.kelth.myfacebook;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.util.Base64;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends Activity {

    private static final String EMAIL = "email";
    private static final String TAG = MainActivity.class.getSimpleName();

    CallbackManager callbackManager;
    LoginButton loginButton;

    AccessToken accessToken;
    AccessTokenTracker accessTokenTracker;

    Profile profile;
    ProfileTracker profileTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // KEL
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);

        callbackManager = CallbackManager.Factory.create();

        loginButton = (LoginButton) findViewById(R.id.login_button);
        loginButton.setReadPermissions(Arrays.asList(EMAIL));
        // If you are using in a fragment, call loginButton.setFragment(this);

        // Callback registration
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "registerCallback: " + loginResult);
            }

            @Override
            public void onCancel() {
                // App code
            }

            @Override
            public void onError(FacebookException exception) {
                Log.d(TAG, "registerCallback: " + exception.getLocalizedMessage());
            }
        });

        // Check facebook token still valid
        boolean loggedIn = AccessToken.getCurrentAccessToken() == null ? false : true;
        Log.d(TAG, "FB loggedin: " + loggedIn);

        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    "com.facebook.samples.loginhowto",
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG,"NameNotFoundException: " + e.getLocalizedMessage());
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "NoSuchAlgorithmException" + e.getLocalizedMessage());
        }

        // Track access token
        accessTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken, AccessToken currentAccessToken) {
                Log.d(TAG, "onCurrentAccessTokenChanged old: " + oldAccessToken + ", new: " + currentAccessToken);
                accessToken = currentAccessToken;
            }
        };
        accessToken = AccessToken.getCurrentAccessToken();
        Log.d(TAG, "accessToken: " + accessToken.getToken());
        Log.d(TAG, "accessToken UserId: " + accessToken.getUserId());
        Log.d(TAG, "accessToken AppsId: " + accessToken.getApplicationId());

        // Track current profile
        profileTracker = new ProfileTracker() {
            @Override
            protected void onCurrentProfileChanged(Profile oldProfile, Profile currentProfile) {
                Log.d(TAG, "onCurrentProfileChanged old: " + oldProfile + ", new: " + currentProfile);
                profile = currentProfile;
            }
        };
        profile = Profile.getCurrentProfile();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        loginButton.unregisterCallback(callbackManager);
        accessTokenTracker.stopTracking();
        profileTracker.stopTracking();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void onBtnFacebookClick(View view) {
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile"));
    }

    public void onBtnGetClick(View view) {
        GraphRequest request = GraphRequest.newMeRequest(
                accessToken,
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {

                        String userName = null;
                        try {
                            userName = object.getString("name");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        String id = null;
                        try {
                            id = object.getString("id");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        Log.d(TAG, "Username: " + userName + ", Id: " + id);
                    }
                });

        Bundle parameters = new Bundle();
        parameters.putString("fields", "id, name, link");
        request.setParameters(parameters);
        request.executeAsync();
    }
}

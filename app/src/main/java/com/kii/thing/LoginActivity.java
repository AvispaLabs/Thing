package com.kii.thing;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.kii.thing.helpers.Constants;
import com.kii.thing.helpers.Preferences;
import com.kii.cloud.storage.Kii;
import com.kii.cloud.storage.KiiUser;


/**
 * A login screen that offers login via 4 digit pin.

 */
public class LoginActivity extends Activity {

    private static final String TAG = LoginActivity.class.getName();

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserSignInTask mSignInTask = null;
    private UserRegistrationTask mRegisterTask = null;
    private UserTokenSignInTask mTokenSignInTask = null;

    // UI references.
    private EditText mUsernameView;
    private EditText mPasswordView;
    private CheckBox mRememberCheckbox;
    private boolean rememberMe = false;
    private View mProgressView;
    private View mAuthFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        /**
         * Initialize Kii SDK
         * Please change APP_ID/APP_KEY to your application on class Constants
         * This method does not talk to the server so it can be done on a UI thread
         * This method does not fail so it won't return an error
         * Consider moving to Interactor or Helper to free the MainActivity?
         */
        Kii.initialize(
                Constants.APP_ID,  // Put your App ID
                Constants.APP_KEY, // Put your App Key
                Constants.APP_SITE // Put your site as you've specified upon creating the app on the dev portal
        );

        // Set up the auth form.
        mUsernameView = (EditText) findViewById(R.id.username);
        mPasswordView = (EditText) findViewById(R.id.passw);
        mRememberCheckbox = (CheckBox) findViewById(R.id.rememberBox);

        Button mSignInButton = (Button) findViewById(R.id.sign_in_button);
        Button mRegisterButton = (Button) findViewById(R.id.register_button);

        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptAuth(false);
            }
        });
        mRegisterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptAuth(true);
            }
        });
        mRememberCheckbox.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                rememberMe = ((CheckBox) v).isChecked();
            }
        });

        mAuthFormView = findViewById(R.id.auth_form);
        mProgressView = findViewById(R.id.progressBar);

        // Now that the UI is set up we try to login via previous user access token
        tryLoginWithToken();
    }

    // Login with existing token
    // Consider converting to Interactor since this callback talks to the server and can succeed or fail
    private void tryLoginWithToken() {
        Log.d(TAG, "Trying to retrieve access token...");
        String token = Preferences.getStoredAccessToken(this);
        if (token == null || token.length() == 0) {
            Log.d(TAG, "Found no access token");
        } else {
            Log.d(TAG, "Token: " + token);
            showProgress(true);
            mTokenSignInTask = new UserTokenSignInTask(this, token);
            mTokenSignInTask.execute((Void) null);
        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptAuth(boolean isRegistration) {
        if(!isRegistration) {
            if (mSignInTask != null) {
                return;
            }
        } else {
            if (mRegisterTask != null) {
                return;
            }
        }

        // Reset errors.
        mUsernameView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the auth attempt.
        String username = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;


        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password) || password.length() < 4) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        } else

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            if(!isRegistration) {
                mSignInTask = new UserSignInTask(getApplicationContext(), username, password, rememberMe);
                mSignInTask.execute((Void) null);
            }
            else {
                mRegisterTask = new UserRegistrationTask(this, username, password, rememberMe);
                mRegisterTask.execute((Void) null);
            }
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
            mAuthFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mAuthFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mAuthFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mAuthFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login task used to authenticate
     * the user.
     */
    public class UserSignInTask extends AsyncTask<Void, Void, Boolean> {

        private final Context mContext;
        private final String mUsername;
        private final String mPassword;
        private final boolean mRememberMe;

        UserSignInTask(Context context, String username, String password, boolean rememberMe) {
            mContext = context;
            mUsername = username;
            mPassword = password;
            mRememberMe = rememberMe;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // attempt sign in against Kii Cloud
            try {
                //String id = Settings.id(mContext);
                Log.d(TAG, "Attempting sign in with username: " + mUsername);
                KiiUser.logIn(mUsername, mPassword);
                if(mRememberMe) {
                    Log.d(TAG, "Storing access token...");
                    KiiUser user = KiiUser.getCurrentUser();
                    String token = user.getAccessToken();
                    Preferences.setStoredAccessToken(mContext, token);
                }
            } catch (Exception e) {
                return false;
            }
            Log.d(TAG, "Sign in successful");
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mSignInTask = null;
            showProgress(false);

            if (success) {
                Intent intent = new Intent(mContext, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mSignInTask = null;
            showProgress(false);
        }
    }

    /**
     * Represents an asynchronous login task used to authenticate
     * the user via an access token.
     */
    public class UserTokenSignInTask extends AsyncTask<Void, Void, Boolean> {

        private final Context mContext;
        private final String mToken;

        UserTokenSignInTask(Context context, String token) {
            mContext = context;
            mToken = token;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // attempt sign in against Kii Cloud using an access token
            try {
                Log.d(TAG, "Attempting sign in with access token");
                KiiUser.loginWithToken(mToken);
                Log.d(TAG, "Sign in with token successful");
            } catch (Exception e) {
                Log.e(TAG, "Error signing in with token: " + e.getMessage());
                //Preferences.clearStoredAccessToken(mContext);
                Log.e(TAG, e.toString());
                return false;
            }
            Log.d(TAG, "User id: " + KiiUser.getCurrentUser().getUsername());
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mTokenSignInTask = null;
            showProgress(false);
            if (success) {
                Intent intent = new Intent(mContext, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                Log.e(TAG, "Error signing in with token");
            }
        }

        @Override
        protected void onCancelled() {
            mTokenSignInTask = null;
            showProgress(false);
        }
    }

    /**
     * Represents an asynchronous registration task used to authenticate
     * the user.
     */
    public class UserRegistrationTask extends AsyncTask<Void, Void, Boolean> {

        private final Context mContext;
        private final String mUsername;
        private final String mPassword;
        private final boolean mRememberMe;

        UserRegistrationTask(Context context, String username, String password, boolean rememberMe) {
            mContext = context;
            mUsername = username;
            mPassword = password;
            mRememberMe = rememberMe;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // attempt registration against Kii Cloud
            try {
                Log.d(TAG, "Attempting registration with username: " + mUsername);
                KiiUser.Builder builder = KiiUser.builderWithName(mUsername);
                KiiUser user = builder.build();
                user.register(mPassword);
                if(mRememberMe) {
                    Log.d(TAG, "Storing access token...");
                    KiiUser user2 = KiiUser.getCurrentUser();
                    String token = user2.getAccessToken();
                    Preferences.setStoredAccessToken(mContext, token);
                    Log.d(TAG, "Access token saved");
                }
            }
            catch (Exception e) {
                Log.e(TAG, "Error storing access token: " + e.getMessage());
                return false;
            }
            Log.d(TAG, "Registration successful");
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mRegisterTask = null;
            showProgress(false);

            if (success) {
                Intent intent = new Intent(mContext, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mRegisterTask = null;
            showProgress(false);
        }
    }
}
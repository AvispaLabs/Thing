package com.kii.thing;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.kii.cloud.storage.KiiBucket;
import com.kii.cloud.storage.KiiCallback;
import com.kii.cloud.storage.KiiThing;
import com.kii.cloud.storage.KiiThingOwner;
import com.kii.cloud.storage.KiiUser;
import com.kii.cloud.storage.callback.KiiPushCallBack;
import com.kii.thing.helpers.Constants;
import com.kii.thing.helpers.GCMPreference;
import com.kii.thing.helpers.Preferences;

import org.json.JSONArray;
import org.json.JSONObject;


public class MainActivity extends ActionBarActivity {

    private static final String TAG = "MainActivity";
    private GoogleCloudMessaging gcm;
    public static MainActivity mainActivity = null;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        registerGCM();

    }

    @Override
    protected void onResume() {
        super.onResume();
        mainActivity = this;
    }
    @Override
    protected void onPause() {
        super.onPause();
        mainActivity = null;
    }

    private void registerGCM() {
        // GCM setup
        final GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(getApplicationContext());
        String regId = GCMPreference.getRegistrationId(getApplicationContext());
        if (regId.isEmpty()) {
            Log.d(TAG, "Previous GCM Reg Id not found");
            new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... params) {
                    try {
                        Log.d(TAG, "Registering GCM...");
                        // call register
                        String regId = gcm.register(Constants.GCM_PROJECT_NUMBER);
                        // install user device (assumes user is already logged in)
                        KiiUser.pushInstallation().install(regId);
                        // if all succeeded, save registration ID to preference.
                        GCMPreference.setRegistrationId(MainActivity.this.getApplicationContext(), regId);
                        Log.d(TAG, "Registered GCM");
                        return regId;
                    } catch (Exception e) {
                        Log.e(TAG, "Error registering GCM push: " +  e.toString());
                        return null;
                    }
                }
            }.execute();
        } else
            Log.d(TAG, "Previous GCM Reg Id found, no need to register");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            this.startActivity(intent);
            return true;
        }

        if (id == R.id.action_logout) {
            if(KiiUser.getCurrentUser() != null) {
                KiiUser.logOut();
                Preferences.clearStoredAccessToken(this);
            }
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public String loadJSONFromAsset(String filename) throws java.io.IOException {
        String json = null;
        InputStream is = getAssets().open(filename);
        int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();
        json = new String(buffer, "UTF-8");
        return json;
    }

    public void processJSON() throws Exception {
        JSONObject obj = new JSONObject(loadJSONFromAsset("DEVICE.JSN"));
        JSONArray m_jArry = obj.getJSONArray("formules");
        ArrayList<HashMap<String, String>> formList= new ArrayList<HashMap<String, String>>();
        HashMap<String, String> m_li;

        for (int i = 0; i < m_jArry.length(); i++)
        {
            JSONObject jo_inside = m_jArry.getJSONObject(i);
            Log.d("Details-->", jo_inside.getString("formule"));
            String formula_value = jo_inside.getString("formule");
            String url_value = jo_inside.getString("url");

            //Add your values in your `ArrayList` as below:

            m_li=new HashMap<String, String>();
            m_li.put("formule", formula_value );
            m_li.put("url", url_value );

            formList.add(m_li);
            //Same way for other value...
        }
    }


    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
                case 2:
                    return getString(R.string.title_section3).toUpperCase(l);
            }
            return null;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }

    public void scanQr(View view){
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        //integrator.setPrompt(String.valueOf(R.string.txt_scan_qr_code));
        integrator.setResultDisplayDuration(0);
        integrator.setCameraId(0);  // Use a specific camera of the device
        integrator.initiateScan();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        //retrieve scan result
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);

        if (scanningResult != null) {
            //we have a result
            String scanContent = scanningResult.getContents();
            String scanFormat = scanningResult.getFormatName();

            // display it on screen
            //formatTxt.setText("FORMAT: " + scanFormat);
            //contentTxt.setText("CONTENT: " + scanContent);

            // Parse thing id and token from QR
            String[] splitted = scanContent.split(",");
            String thingId = splitted[0];
            String thingToken = splitted[1];

            grabThingOwnership(thingId, thingToken);
        } else {
            Toast toast = Toast.makeText(getApplicationContext(), "No scan data received!", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    private void grabThingOwnership(String thingId, final String thingToken) {
        // Assume user is logged in
        final KiiUser user = KiiUser.getCurrentUser();
        if(user == null) {
            Toast.makeText(getApplicationContext(), "Not logged in!",
                    Toast.LENGTH_LONG).show();
            Log.e(TAG, "Not logged in!");
            return;
        }
        KiiThing.loadWithThingID(thingId, thingToken, new KiiCallback<KiiThing>() {
            @Override
            public void onComplete(final KiiThing result, Exception e) {
                if (e != null) {
                    // Error handling
                    Toast.makeText(getApplicationContext(), "Thing retrieval error",
                            Toast.LENGTH_LONG).show();
                    Log.e(TAG, e.toString());
                    return;
                }
                result.isOwner(user, thingToken, new KiiCallback<Boolean>() {
                    @Override
                    public void onComplete(Boolean isOwner, Exception e) {
                        if (e != null) {
                            Toast.makeText(getApplicationContext(), "Thing ownership retrieval error",
                                    Toast.LENGTH_LONG).show();
                            Log.e(TAG, e.toString());
                            return;
                        }
                        if (!isOwner) {
                            // Current user is not owner of thing, let's transfer ownership to the user
                            result.registerOwner(user, user.getAccessToken(), new KiiCallback<KiiThingOwner>() {
                                @Override
                                public void onComplete(KiiThingOwner result2, Exception e) {
                                    if (e != null) {
                                        // Error handling
                                        Toast.makeText(getApplicationContext(), "Thing owner registration error",
                                                Toast.LENGTH_LONG).show();
                                        Log.e(TAG, e.toString());
                                        return;
                                    } else {
                                        Toast.makeText(getApplicationContext(), "User registered as Thing owner",
                                                Toast.LENGTH_LONG).show();
                                        Log.i(TAG, "User registered as Thing owner");
                                        // Subscribing push o bucket
                                        KiiBucket thingBucket = result.bucket(Constants.THING_BUCKET);
                                        user.pushSubscription().subscribe(thingBucket, new KiiPushCallBack() {
                                            @Override
                                            public void onInstallCompleted(int taskId, Exception e) {
                                                if (e != null) {
                                                    Toast.makeText(getApplicationContext(), "Push subscription error",
                                                            Toast.LENGTH_LONG).show();
                                                    Log.e(TAG, e.toString());
                                                    return;
                                                }
                                                Toast.makeText(getApplicationContext(), "Push subscription success",
                                                        Toast.LENGTH_LONG).show();
                                                Log.d(TAG, e.toString());
                                            }
                                        });
                                    }
                                }
                            });
                        } else {
                            // Current user is owner of thing, let's remove ownership from the user
                            result.unregisterOwner(user, user.getAccessToken(), new KiiCallback<KiiThingOwner>() {
                                @Override
                                public void onComplete(KiiThingOwner result3, Exception e) {
                                    if (e != null) {
                                        // Error handling
                                        Toast.makeText(getApplicationContext(), "Thing owner unregister error",
                                                Toast.LENGTH_LONG).show();
                                        Log.e(TAG, e.toString());
                                        return;
                                    } else {
                                        Toast.makeText(getApplicationContext(), "User unregistered as Thing owner",
                                                Toast.LENGTH_LONG).show();
                                        Log.i(TAG, "User unregistered as Thing owner");
                                        // Unsubscribing push o bucket
                                        KiiBucket thingBucket = result.bucket(Constants.THING_BUCKET);
                                        user.pushSubscription().unsubscribe(thingBucket, new KiiPushCallBack() {
                                            @Override
                                            public void onInstallCompleted(int taskId, Exception e) {
                                                if (e != null) {
                                                    Toast.makeText(getApplicationContext(), "Push unsubscribe error",
                                                            Toast.LENGTH_LONG).show();
                                                    Log.e(TAG, e.toString());
                                                    return;
                                                }
                                                Toast.makeText(getApplicationContext(), "Push unsubscribe success",
                                                        Toast.LENGTH_LONG).show();
                                                Log.d(TAG, e.toString());
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    }
                });
            }
        });

    }


}

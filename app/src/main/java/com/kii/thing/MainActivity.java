package com.kii.thing;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
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

public class MainActivity extends ActionBarActivity {

    private static final String TAG = "MainActivity";
    private GoogleCloudMessaging gcm;
    private LineChart mChart;
    public static MainActivity instance = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;
        mChart = (LineChart) findViewById(R.id.chart);
        setupChart(mChart);
        registerGCM();
    }

    private void setupChart(LineChart lineChart){
        lineChart.setDescription("");
        lineChart.setNoDataTextDescription("No data");
        lineChart.setHighlightEnabled(true);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setDrawGridBackground(false);
        lineChart.setPinchZoom(true);
        lineChart.setBackgroundColor(Color.LTGRAY);
        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);
        lineChart.setData(data);
        Legend l = lineChart.getLegend();
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.WHITE);
        XAxis xl = lineChart.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        YAxis yl = lineChart.getAxisLeft();
        yl.setTextColor(Color.WHITE);
        yl.setAxisMaxValue(120f);
        yl.setDrawGridLines(true);
        YAxis yl2 = lineChart.getAxisRight();
        yl2.setEnabled(false);
    }

    private void addRandomChartEntry(){
        if(mChart == null)
            return;
        LineData data = mChart.getData();
        if(data != null) {
            LineDataSet dataSet = data.getDataSetByIndex(0);
            if(dataSet == null) {
                dataSet = createEmptyDataSet();
                data.addDataSet(dataSet);
            }
            data.addXValue("");
            data.addEntry(new Entry((float) (Math.random() * 120) + 5f, dataSet.getEntryCount()), 0);
            mChart.notifyDataSetChanged();
            mChart.setVisibleXRangeMaximum(6f);
            mChart.moveViewToX(data.getXValCount() - 7);
        }
    }

    public void addChartEntry(float value){
        if(mChart == null)
            return;
        LineData data = mChart.getData();
        if(data != null) {
            LineDataSet dataSet = data.getDataSetByIndex(0);
            if(dataSet == null) {
                dataSet = createEmptyDataSet();
                data.addDataSet(dataSet);
            }
            data.addXValue("");
            data.addEntry(new Entry(value, dataSet.getEntryCount()), 0);
            mChart.notifyDataSetChanged();
            mChart.setVisibleXRangeMaximum(6f);
            mChart.moveViewToX(data.getXValCount() - 7);
        }
    }

    private LineDataSet createEmptyDataSet() {
        LineDataSet dataSet = new LineDataSet(null, "Temperature");
        dataSet.setDrawCubic(true);
        dataSet.setCubicIntensity(0.2f);
        dataSet.setColor(ColorTemplate.getHoloBlue());
        dataSet.setCircleColor(ColorTemplate.getHoloBlue());
        dataSet.setLineWidth(2f);
        dataSet.setCircleSize(4f);
        dataSet.setFillAlpha(65);
        dataSet.setFillColor(ColorTemplate.getHoloBlue());
        dataSet.setHighLightColor(Color.rgb(244, 117, 177));
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);
        return dataSet;
    }
/*
    @Override
    public void onResume() {
        super.onResume();
        new Thread(new Runnable() {
            @Override
            public void run() {
                for(int i = 0; i < 100; i++) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            addRandomChartEntry();
                        }
                    });
                    try {
                        Thread.sleep(600);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }).start();
    }
*/

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

        if (id == R.id.action_scanqr) {
            scanQr(findViewById(R.id.mainLayout));
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

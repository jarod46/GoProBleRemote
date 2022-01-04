package com.lazyjarod.goproremote;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class SettingsActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        SharedPreferences sharedPref = MainActivity.getMainActivity().getPreferences(MODE_PRIVATE);
        //String currentConnectIQDevice = sharedPref.getString("ConnectIQDevice", "empty");

        ArrayAdapter<String> buttonPressAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        buttonPressAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        buttonPressAdapter.addAll(getResources().getStringArray(R.array.button_reaction));

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String name = "";
                switch (adapterView.getId()) {
                    case R.id.simplePress:
                        name = "simplePress";
                        break;
                    case R.id.doublePress:
                        name = "doublePress";
                        break;
                    case R.id.longPress:
                        name = "longPress";
                        break;
                }
                if (name.length() > 0) {
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putInt(name, i);
                    editor.apply();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        };

        Spinner simplePress = findViewById(R.id.simplePress);
        simplePress.setAdapter(buttonPressAdapter);
        simplePress.setSelection(sharedPref.getInt("simplePress", 2));
        simplePress.setOnItemSelectedListener(listener);
        Spinner doublePress = findViewById(R.id.doublePress);
        doublePress.setAdapter(buttonPressAdapter);
        doublePress.setSelection(sharedPref.getInt("doublePress", 3));
        doublePress.setOnItemSelectedListener(listener);
        Spinner longPress = findViewById(R.id.longPress);
        longPress.setAdapter(buttonPressAdapter);
        longPress.setSelection(sharedPref.getInt("longPress", 4));
        longPress.setOnItemSelectedListener(listener);

        Switch alertOnButton = findViewById(R.id.alertOnButton);
        alertOnButton.setChecked(sharedPref.getBoolean("alertOnButton", true));
        alertOnButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean("alertOnButton", b);
                editor.apply();
            }
        });

        Switch alertOnCamConnec = findViewById(R.id.alertOnCamConnec);
        alertOnCamConnec.setChecked(sharedPref.getBoolean("alertOnCamConnec", false));
        alertOnCamConnec.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean("alertOnCamConnec", b);
                editor.apply();
            }
        });

        Switch alertOnMonDisabled = findViewById(R.id.alertOnMonDisabled);
        alertOnMonDisabled.setChecked(sharedPref.getBoolean("alertOnMonDisabled", true));
        alertOnMonDisabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean("alertOnMonDisabled", b);
                editor.apply();
            }
        });

        Switch alertOnMonEnabled = findViewById(R.id.alertOnMonEnabled);
        alertOnMonEnabled.setChecked(sharedPref.getBoolean("alertOnMonEnabled", true));
        alertOnMonEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean("alertOnMonEnabled", b);
                editor.apply();
            }
        });

        Button bClose = findViewById(R.id.bClose);
        bClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }
    }
}
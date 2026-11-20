package com.rickgaming.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class MainActivity extends Activity {

    private static final String TARGET_URL = "https://rick-biswas.github.io/RICK-GAMING/";
    private static final String CHROME_PACKAGE = "com.android.chrome";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri uri = Uri.parse(TARGET_URL);

        try {
            Intent chromeIntent = new Intent(Intent.ACTION_VIEW, uri);
            chromeIntent.setPackage(CHROME_PACKAGE);
            chromeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(chromeIntent);
        } catch (ActivityNotFoundException e) {
            Intent defaultBrowserIntent = new Intent(Intent.ACTION_VIEW, uri);
            defaultBrowserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(defaultBrowserIntent);
        }

        finish();
    }
}

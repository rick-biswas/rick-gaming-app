package com.rickgaming.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class MainActivity extends Activity {

    private static final String SITE_URL = "https://rick-biswas.github.io/RICK-GAMING/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        openSiteInChrome();
        finish();
    }

    private void openSiteInChrome() {
        Uri uri = Uri.parse(SITE_URL);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setPackage("com.android.chrome");

        try {
            startActivity(intent);
        } catch (Exception e) {
            Intent fallbackIntent = new Intent(Intent.ACTION_VIEW, uri);
            fallbackIntent.addCategory(Intent.CATEGORY_BROWSABLE);
            try {
                startActivity(fallbackIntent);
            } catch (Exception ignored) {
            }
        }
    }
}

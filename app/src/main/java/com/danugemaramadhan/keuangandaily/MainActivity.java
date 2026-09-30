package com.danugemaramadhan.keuangandaily;

import android.app.Activity;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import org.json.JSONObject;

import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private WebView web;
    private CredentialManager credentialManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(
                android.graphics.Color.rgb(6, 18, 15)
        );

        getWindow().setNavigationBarColor(
                android.graphics.Color.rgb(4, 17, 14)
        );

        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMixedContentMode(
                WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        );

        credentialManager = CredentialManager.create(this);

        web.addJavascriptInterface(new GoogleBridge(), "Android");

        web.setWebViewClient(new WebViewClient());

        web.loadUrl("file:///android_asset/index.html");
    }

    public class GoogleBridge {

        @JavascriptInterface
        public void signInWithGoogle() {

            GetGoogleIdOption googleIdOption =
                    new GetGoogleIdOption.Builder()
                            .setServerClientId(
                                    getString(
                                            com.danugemaramadhan.keuangandaily.R.string.default_web_client_id
                                    )
                            )
                            .setFilterByAuthorizedAccounts(false)
                            .build();

            GetCredentialRequest request =
                    new GetCredentialRequest.Builder()
                            .addCredentialOption(googleIdOption)
                            .build();

            credentialManager.getCredentialAsync(
                    MainActivity.this,
                    request,
                    new CancellationSignal(),
                    Executors.newSingleThreadExecutor(),
                    new CredentialManagerCallback<GetCredentialResponse, Exception>() {

                        @Override
                        public void onResult(GetCredentialResponse result) {

                            Credential credential =
                                    result.getCredential();

                            if (credential instanceof CustomCredential) {

                                CustomCredential customCredential =
                                        (CustomCredential) credential;

                                if (GoogleIdTokenCredential
                                        .TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                        .equals(customCredential.getType())) {

                                    try {

                                        GoogleIdTokenCredential googleCredential =
                                                GoogleIdTokenCredential.createFrom(
                                                        customCredential.getData()
                                                );

                                        String idToken =
                                                googleCredential.getIdToken();

                                        web.post(() -> {

                                            String js =
                                                    "window.onNativeGoogleSignIn("
                                                            + JSONObject.quote(idToken)
                                                            + ");";

                                            web.evaluateJavascript(
                                                    js,
                                                    null
                                            );
                                        });

                                    } catch (Exception e) {
                                        sendError(e.getMessage());
                                    }

                                } else {
                                    sendError("Credential Google tidak valid");
                                }

                            } else {
                                sendError("Credential Google tidak ditemukan");
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            sendError(
                                    e.getMessage() != null
                                            ? e.getMessage()
                                            : "Login Google dibatalkan"
                            );
                        }
                    }
            );
        }

        private void sendError(String message) {

            web.post(() -> {

                String js =
                        "window.onNativeGoogleSignInError("
                                + JSONObject.quote(
                                        message != null
                                                ? message
                                                : "Unknown error"
                                )
                                + ");";

                web.evaluateJavascript(js, null);
            });
        }
    }

    @Override
    public void onBackPressed() {

        if (web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
Add native Google Sign-In bridge

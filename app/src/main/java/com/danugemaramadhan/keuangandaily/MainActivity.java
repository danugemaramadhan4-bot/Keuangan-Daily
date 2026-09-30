package com.danugemaramadhan.keuangandaily;

import android.app.Activity;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.net.Uri;
import android.view.Window;

import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

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

        Window w = getWindow();
        w.setStatusBarColor(android.graphics.Color.rgb(6, 18, 15));
        w.setNavigationBarColor(android.graphics.Color.rgb(4, 17, 14));

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

        web.addJavascriptInterface(this, "Android");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest req
            ) {
                Uri u = req.getUrl();

                if ("http".equals(u.getScheme())
                        || "https".equals(u.getScheme())) {
                    return false;
                }

                return true;
            }
        });

        web.loadUrl("file:///android_asset/index.html");
    }

    @JavascriptInterface
    public void signInWithGoogle() {

        runOnUiThread(() -> {

            try {

                GetGoogleIdOption googleIdOption =
                        new GetGoogleIdOption.Builder()
                                .setFilterByAuthorizedAccounts(false)
                                .setServerClientId(
                                        getString(
                                                R.string.default_web_client_id
                                        )
                                )
                                .build();

                GetCredentialRequest request =
                        new GetCredentialRequest.Builder()
                                .addCredentialOption(googleIdOption)
                                .build();

                credentialManager.getCredentialAsync(
                        this,
                        request,
                        new CancellationSignal(),
                        Executors.newSingleThreadExecutor(),
                        new CredentialManagerCallback<
                                GetCredentialResponse,
                                GetCredentialException
                                >() {

                            @Override
                            public void onResult(
                                    GetCredentialResponse result
                            ) {

                                Credential credential =
                                        result.getCredential();

                                if (credential instanceof CustomCredential) {

                                    CustomCredential customCredential =
                                            (CustomCredential) credential;

                                    if (GoogleIdTokenCredential
                                            .TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                            .equals(
                                                    customCredential.getType()
                                            )) {

                                        try {

                                            GoogleIdTokenCredential
                                                    googleCredential =
                                                    GoogleIdTokenCredential
                                                            .createFrom(
                                                                    customCredential
                                                                            .getData()
                                                            );

                                            sendToken(
                                                    googleCredential
                                                            .getIdToken()
                                            );

                                        } catch (Exception e) {

                                            sendError(
                                                    e.getMessage()
                                            );
                                        }

                                    } else {

                                        sendError(
                                                "Credential Google tidak valid"
                                        );
                                    }

                                } else {

                                    sendError(
                                            "Credential Google tidak ditemukan"
                                    );
                                }
                            }

                            @Override
                            public void onError(
                                    GetCredentialException e
                            ) {

                                sendError(
                                        e.getMessage() != null
                                                ? e.getMessage()
                                                : "Login Google dibatalkan"
                                );
                            }
                        }
                );

            } catch (Exception e) {

                sendError(e.getMessage());
            }
        });
    }

    private void sendToken(String idToken) {

        web.post(() -> {

            try {

                String js =
                        "window.onNativeGoogleSignIn("
                                + JSONObject.quote(idToken)
                                + ");";

                web.evaluateJavascript(js, null);

            } catch (Exception e) {

                sendError(e.getMessage());
            }
        });
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

    @Override
    public void onBackPressed() {

        if (web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
    
        
                                    

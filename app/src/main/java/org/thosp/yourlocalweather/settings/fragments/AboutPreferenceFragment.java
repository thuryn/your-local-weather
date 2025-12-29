package org.thosp.yourlocalweather.settings.fragments;

import android.app.Dialog;
import androidx.fragment.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.utils.Constants;

public class AboutPreferenceFragment extends PreferenceFragmentCompat {

    private static final String TAG = "AboutPreferenceFragment";
    PackageManager mPackageManager;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_about, rootKey);

        mPackageManager = getActivity().getPackageManager();
        findPreference(Constants.KEY_PREF_ABOUT_VERSION).setSummary(getVersionName());
        findPreference(Constants.KEY_PREF_ABOUT_F_DROID).setIntent(fDroidIntent());
        findPreference(Constants.KEY_PREF_ABOUT_GOOGLE_PLAY).setIntent(googlePlayIntent());

        Preference licensePref = findPreference(Constants.KEY_PREF_ABOUT_OPEN_SOURCE_LICENSES);

        if (licensePref != null) {
            licensePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    LicensesDialogFragment licensesDialog = LicensesDialogFragment.newInstance();
                    licensesDialog.show(getParentFragmentManager(), "LicensesDialog");
                    return true;
                }
            });
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        int horizontalMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
        int verticalMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
        int topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 56, getResources().getDisplayMetrics());

        if (view != null) {
            view.setPadding(horizontalMargin, topMargin, horizontalMargin, verticalMargin);
        }
        return view;
    }

    private String getVersionName() {
        String versionName;
        try {
            versionName = mPackageManager.getPackageInfo(getActivity().getPackageName(),
                    0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Get version name error", e);
            versionName = "666";
        }
        return versionName;
    }

    private Intent fDroidIntent() {
        String ACTION_VIEW = Intent.ACTION_VIEW;
        String fDroidWebUri = String.format(Constants.F_DROID_WEB_URI,
                getActivity().getPackageName());

        return new Intent(ACTION_VIEW, Uri.parse(fDroidWebUri));
    }

    private Intent googlePlayIntent() {
        String ACTION_VIEW = Intent.ACTION_VIEW;
        String googlePlayAppUri = String.format(Constants.GOOGLE_PLAY_APP_URI,
                getActivity().getPackageName());
        String googlePlayWebUri = String.format(Constants.GOOGLE_PLAY_WEB_URI,
                getActivity().getPackageName());

        Intent intent = new Intent(ACTION_VIEW, Uri.parse(googlePlayAppUri));
        if (mPackageManager.resolveActivity(intent, 0) == null) {
            intent = new Intent(ACTION_VIEW, Uri.parse(googlePlayWebUri));
        }

        return intent;
    }

    public static class LicensesDialogFragment extends DialogFragment {

        static LicensesDialogFragment newInstance() {
            return new LicensesDialogFragment();
        }

        /**
         * Opens link in simple dialog with WebView
         *
         * @param context
         * @param link
         */
        private static void openLinkInWebView(Context context, String link) {
            WebView webView = new WebView(context);
            webView.loadUrl(link);
            new AlertDialog.Builder(context)
                    .setView(webView)
                    .setPositiveButton(R.string.ok, null)
                    .show();
        }

        /**
         * Opens link in external browser
         *
         * @param context
         * @param link
         */
        private static void openLinkInBrowser(Context context, String link) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
            context.startActivity(browserIntent);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final TextView textView = new TextView(getActivity());
            int padding = (int) getResources().getDimension(R.dimen.activity_horizontal_margin);
            textView.setPadding(padding, padding, padding, padding);
            textView.setLineSpacing(0, 1.2f);
            textView.setLinkTextColor(ContextCompat.getColor(getActivity(), R.color.link_color));
            setTextViewHTML(getActivity(), textView, getString(R.string.licenses));
            return new AlertDialog.Builder(getActivity())
                    .setTitle(getString(R.string.title_open_source_licenses))
                    .setView(textView)
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
        }

        /**
         * Since Android 24, apps banned from using file:// scheme
         * (https://stackoverflow.com/q/38200282)
         * <p>
         * This method sets licenses text as html with clickable links
         * </p><p>
         * Link clicks processed in makeLinkClickable() method
         * </p>
         *
         * @param context
         * @param text
         * @param html
         */
        protected void setTextViewHTML(Context context, TextView text, String html) {
            CharSequence sequence = Html.fromHtml(html);
            SpannableStringBuilder strBuilder = new SpannableStringBuilder(sequence);
            URLSpan[] urls = strBuilder.getSpans(0, sequence.length(), URLSpan.class);
            for (URLSpan span : urls) {
                makeLinkClickable(context, strBuilder, span);
            }
            text.setText(strBuilder);
            text.setMovementMethod(LinkMovementMethod.getInstance());
        }

        /**
         * Opens file:/// urls in built-in WebView and launches browser for any other url type
         *
         * @param context
         * @param strBuilder
         * @param span
         */
        protected void makeLinkClickable(final Context context, SpannableStringBuilder strBuilder, final URLSpan span) {
            int start = strBuilder.getSpanStart(span);
            int end = strBuilder.getSpanEnd(span);
            int flags = strBuilder.getSpanFlags(span);
            ClickableSpan clickable = new ClickableSpan() {
                public void onClick(View view) {
                    if (span.getURL().startsWith("file:///"))
                        openLinkInWebView(context, span.getURL());
                    else
                        openLinkInBrowser(context, span.getURL());
                }
            };
            strBuilder.setSpan(clickable, start, end, flags);
            strBuilder.removeSpan(span);
        }
    }
}

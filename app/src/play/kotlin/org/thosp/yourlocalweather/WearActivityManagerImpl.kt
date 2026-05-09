package org.thosp.yourlocalweather

import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.preference.PreferenceManager
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableStatusCodes
import org.thosp.yourlocalweather.utils.LogToFile.appendLog


class WearActivityManagerImpl(private val activity: Activity) : WearActivityManager {

    private var TAG = "WearActivityManagerImpl"

    override fun checkWearables() {
        Wearable.getNodeClient(activity).getConnectedNodes()
            .addOnSuccessListener(OnSuccessListener { nodes: MutableList<Node>? ->
                val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
                val promoShown = prefs.getBoolean("wearos_promo_shown", false)
                // Načteme všechna ID zařízení, o kterých už víme
                val knownNodes: MutableSet<String?> =
                    HashSet<String?>(prefs.getStringSet("wearos_known_nodes", HashSet<String?>()))

                // PŘÍPAD 1: Žádné připojené zařízení
                if (nodes!!.isEmpty()) {
                    storeWearosPromo(activity, false, true, null)
                } else {
                    Wearable.getCapabilityClient(activity)
                        .getCapability("your_local_weather_companion", CapabilityClient.FILTER_ALL)
                        .addOnSuccessListener(OnSuccessListener { capabilityInfo: CapabilityInfo? ->
                            val installedNodes = capabilityInfo!!.getNodes()
                            var shouldShowInstallBanner = false
                            var prefsChanged = false

                            for (node in nodes) {
                                if (installedNodes.contains(node)) {
                                    // Uživatel appku na hodinkách MÁ.
                                    // Potichu si hodinky uložíme jako "známé", ať ho neotravujeme, když ji smaže.
                                    if (!knownNodes.contains(node.getId())) {
                                        knownNodes.add(node.getId())
                                        prefsChanged = true
                                    }
                                } else {
                                    // Uživatel appku na hodinkách NEMÁ.
                                    // Ptali jsme se už na tyto konkrétní hodinky?
                                    if (!knownNodes.contains(node.getId())) {
                                        shouldShowInstallBanner = true
                                        knownNodes.add(node.getId())
                                        prefsChanged = true
                                    }
                                }
                            }

                            // Pokud jsme objevili nové hodinky bez aplikace, ukážeme banner
                            if (shouldShowInstallBanner) {
                                showWearOsBanner(
                                    activity,
                                    activity.getString(R.string.activity_main_wearos_without_app_title),
                                    activity.getString(R.string.activity_main_wearos_without_app_message),
                                    promoShown,
                                    prefsChanged,
                                    knownNodes
                                )
                            }
                        })
                }
            }).addOnFailureListener(OnFailureListener { exception: java.lang.Exception? ->
                if (exception is ApiException) {
                    val apiException = exception as ApiException
                    if (apiException.getStatusCode() == WearableStatusCodes.API_NOT_CONNECTED) {
                        appendLog(
                            activity,
                            TAG,
                            "Wearable API is not availablle on this device."
                        )
                    } else {
                        appendLog(
                            activity,
                            TAG,
                            "Error looking for wearables",
                            exception
                        )
                    }
                } else {
                    appendLog(
                        activity,
                        TAG,
                        "Error looking for wearables",
                        exception
                    )
                }
            }
            )
    }

    // Metoda, která zviditelní náš nový neintruzivní Banner
    private fun showWearOsBanner(
        activity: Activity,
        title: String?,
        message: String?,
        promoShown: Boolean,
        prefsChanged: Boolean,
        knownNodes: MutableSet<String?>?
    ) {
        activity.runOnUiThread(Runnable {
            val bannerLayout: View? = activity.findViewById<View?>(R.id.wearos_banner_layout)
            if (bannerLayout != null) {
                val titleView = bannerLayout.findViewById<TextView?>(R.id.wearos_banner_title)
                val messageView = bannerLayout.findViewById<TextView?>(R.id.wearos_banner_message)
                val actionButton = bannerLayout.findViewById<Button?>(R.id.wearos_banner_action)
                val closeButton = bannerLayout.findViewById<ImageButton?>(R.id.wearos_banner_close)
                titleView.setText(title)
                messageView.setText(message)
                actionButton.setOnClickListener(View.OnClickListener { v: View? ->
                    storeWearosPromo(activity, promoShown, prefsChanged, knownNodes)
                    val wearIntent = Intent()
                    wearIntent.setClassName(
                        activity,
                        "org.thosp.yourlocalweather.WearosActivity"
                    )
                    activity.startActivity(wearIntent)
                    bannerLayout.setVisibility(View.GONE)
                })
                closeButton.setOnClickListener(View.OnClickListener { v: View? ->
                    storeWearosPromo(activity, promoShown, prefsChanged, knownNodes)
                    bannerLayout.setVisibility(View.GONE)
                })
                bannerLayout.setVisibility(View.VISIBLE)
            }
        })
    }

    private fun storeWearosPromo(
        activity: Activity,
        promoShown: Boolean,
        prefsChanged: Boolean,
        knownNodes: MutableSet<String?>?
    ) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        val editor = prefs.edit()
        if (prefsChanged) {
            editor.putStringSet("wearos_known_nodes", knownNodes)
        }
        // Pokud zjistíme, že má hodinky, už nikdy mu neukážeme obecné promo
        if (!promoShown) {
            editor.putBoolean("wearos_promo_shown", true)
        }
        editor.apply()
    }
}
package org.thosp.yourlocalweather

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import java.util.concurrent.Executors

class WearosActivity : BaseActivity() {

    private lateinit var devicesContainer: LinearLayout
    private lateinit var loadingText: TextView
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wearos)
        setTitle(R.string.activity_wearos_title)

        devicesContainer = findViewById(R.id.wearos_devices_container)
        loadingText = findViewById(R.id.wearos_loading_text)

        loadWearOsDevices()
    }

    override fun updateUI() {
    }

    private fun loadWearOsDevices() {
        val nodeClient = Wearable.getNodeClient(this)
        val capabilityClient = Wearable.getCapabilityClient(this)

        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            if (nodes.isEmpty()) {
                loadingText.text = this@WearosActivity.getString(R.string.activity_wearos_wearos_device_not_found)
                return@addOnSuccessListener
            }

            capabilityClient.getCapability("your_local_weather_companion", CapabilityClient.FILTER_ALL)
                .addOnSuccessListener { capabilityInfo ->
                    val installedNodes = capabilityInfo.nodes
                    renderDevicesTable(nodes, installedNodes)
                }
                .addOnFailureListener {
                    loadingText.text = this@WearosActivity.getString(R.string.activity_wearos_error_getting_device_status)
                }
        }.addOnFailureListener {
            loadingText.text = this@WearosActivity.getString(R.string.activity_wearos_error_on_device_finding)
        }
    }

    private fun renderDevicesTable(allNodes: List<Node>, installedNodes: Set<Node>) {
        loadingText.visibility = View.GONE
        devicesContainer.removeAllViews()

        for (node in allNodes) {
            val isInstalled = installedNodes.contains(node)

            // Vytvoříme jednoduchý View pro každý řádek (můžeš si na to udělat i extra XML layout a použít LayoutInflater)
            val rowView = layoutInflater.inflate(R.layout.item_wearos_device, devicesContainer, false)

            val deviceNameText = rowView.findViewById<TextView>(R.id.device_name)
            val actionButton = rowView.findViewById<Button>(R.id.device_action_button)

            deviceNameText.text = node.displayName

            if (isInstalled) {
                actionButton.text = this@WearosActivity.getString(R.string.activity_wearos_app_installed)
                actionButton.isEnabled = false // Tlačítko už nic nedělá
                // Tady bys mohl nastavit i nějakou zelenou barvu přes backgroundTint
            } else {
                actionButton.text = this@WearosActivity.getString(R.string.activity_wearos_app_install_from_play)
                actionButton.setOnClickListener {
                    installOnWearable(node.id)
                }
            }

            devicesContainer.addView(rowView)
        }
    }

    private fun installOnWearable(nodeId: String) {
        val intent = Intent(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(Uri.parse("market://details?id=org.thosp.yourlocalweather"))

        val remoteActivityHelper = RemoteActivityHelper(this, executor)
        remoteActivityHelper.startRemoteActivity(intent, nodeId)
    }
}
package no.talgoe.scanwedge.scanwedge

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import com.sunmi.scanner.IScanInterface

// Hardware plugin for Sunmi devices that extends the IHardwarePlugin interface.
// Uses AIDL service binding for scanner control and BroadcastReceiver for scan results.
class SunmiPlugin(private val scanW: ScanwedgePlugin, private val log: Logger?) : IHardwarePlugin {
    companion object {
        private const val TAG = "SunmiPlugin"
        private const val SCAN_ACTION = "com.sunmi.scanner.ACTION_DATA_CODE_RECEIVED"
        private const val SERVICE_PACKAGE = "com.sunmi.scanner"
        private const val SERVICE_ACTION = "com.sunmi.scanner.ACTION_BIND"
    }

    private var scanInterface: IScanInterface? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            scanInterface = IScanInterface.Stub.asInterface(service)
            serviceBound = true
            log?.i(TAG, "Sunmi scanner service connected, model: ${runCatching { scanInterface?.scannerModel }.getOrNull()}")
        }

        override fun onServiceDisconnected(name: ComponentName) {
            scanInterface = null
            serviceBound = false
            log?.w(TAG, "Sunmi scanner service disconnected")
        }
    }

    private val barcodeDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                if (intent.action != SCAN_ACTION) return
                val barcode = intent.getStringExtra("data") ?: return
                val codeId = intent.getStringExtra("type") ?: ""
                log?.i(TAG, "Barcode: $barcode, Type: $codeId")
                scanW.sendScanResult(ScanResult(barcode, BarcodeTypes.fromSunmiCode(codeId), codeId))
            } catch (e: Exception) {
                log?.e(TAG, "Error in barcodeDataReceiver: ${e.message}")
            }
        }
    }

    override val apiVersion: String get() = "SUNMI"

    override fun initialize(context: Context?): Boolean {
        if (context == null) return false
        try {
            // Register broadcast receiver for scan results.
            // RECEIVER_EXPORTED is required because the broadcast originates from
            // the external Sunmi scanner service (com.sunmi.scanner).
            val filter = IntentFilter(SCAN_ACTION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                context.registerReceiver(barcodeDataReceiver, filter, Context.RECEIVER_EXPORTED)
            else
                context.registerReceiver(barcodeDataReceiver, filter)

            // Bind to the Sunmi AIDL scanner service for programmatic control.
            val serviceIntent = Intent(SERVICE_ACTION).apply { setPackage(SERVICE_PACKAGE) }
            context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
            return true
        } catch (e: Exception) {
            log?.e(TAG, "Error initializing SunmiPlugin: ${e.message}")
            return false
        }
    }

    override fun dispose(context: Context?) {
        try {
            context?.unregisterReceiver(barcodeDataReceiver)
        } catch (e: Exception) {
            log?.e(TAG, "Error unregistering receiver: ${e.message}")
        }
        try {
            if (serviceBound) {
                context?.unbindService(serviceConnection)
                serviceBound = false
            }
        } catch (e: Exception) {
            log?.e(TAG, "Error unbinding service: ${e.message}")
        }
        scanInterface = null
    }

    override fun toggleScanning(): Boolean {
        return try {
            scanInterface?.scan()
            scanInterface != null
        } catch (e: Exception) {
            log?.e(TAG, "Error toggling scan: ${e.message}")
            false
        }
    }

    override fun enableScanner(): Boolean {
        return try {
            scanInterface?.scan()
            scanInterface != null
        } catch (e: Exception) {
            log?.e(TAG, "Error enabling scanner: ${e.message}")
            false
        }
    }

    override fun disableScanner(): Boolean {
        return try {
            scanInterface?.stop()
            scanInterface != null
        } catch (e: Exception) {
            log?.e(TAG, "Error disabling scanner: ${e.message}")
            false
        }
    }

    override fun createProfile(
        name: String,
        enabledBarcodes: List<BarcodePlugin>?,
        hwConfig: HashMap<String, Any>?,
        keepDefaults: Boolean
    ): Boolean {
        // Sunmi does not support profile creation via intent API.
        log?.w(TAG, "createProfile not supported for Sunmi devices")
        return false
    }
}

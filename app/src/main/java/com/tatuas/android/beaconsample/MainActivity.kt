package com.tatuas.android.beaconsample

import android.content.ServiceConnection
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import org.altbeacon.beacon.* // AS 3.0 で Format をかけるとこうなる…

// TODO: 位置情報、OSバージョン、Bluetooth設定
class MainActivity : AppCompatActivity(), BeaconConsumer {

    companion object {
        private const val TAG = "beacon_log"
        private const val BEACON_LAYOUT_IBEACON = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"
    }

    private val beaconManager by lazy {
        BeaconManager.getInstanceForApplication(this).apply {
            beaconParsers.add(BeaconParser().setBeaconLayout(BEACON_LAYOUT_IBEACON))
            foregroundBetweenScanPeriod = 1024L // おおよそ1秒毎
        }
    }

    // range: 取得可能な iBeacon 信号を受信し続ける

    private val rangeRegion = Region("", null, null, null)

    private val rangeNotifier = RangeNotifier { beacons, region ->
        val regionString = region.toString()

        // 検出したすべてのビーコン情報を出力する
        val beaconsString = TextUtils.join(", ", beacons.map { beacon ->
            val beaconString = TextUtils.join(", ", mapOf(
                    "UUID" to beacon.id1,
                    "major" to beacon.id2,
                    "minor" to beacon.id3,
                    "distance" to beacon.distance,
                    "RSSI" to beacon.rssi,
                    "bluetoothName" to beacon.bluetoothName,
                    "bluetoothAddress" to beacon.bluetoothAddress)
                    .map { it.key + " : " + it.value })

            "{ $beaconString }"
        })

        log("rangeNotifier: region: { $regionString }, beacons: [ $beaconsString ]")
    }

    // monitor: 特定の iBeacon の動きをモニタリングする

    private val monitorRegion: Region
        get() {
            val uniqueId = ""
            val uuid = Identifier.parse("")
            val major = Identifier.parse("")
            val minor = Identifier.parse("")

            return Region(uniqueId, uuid, major, minor)
        }

    private val monitorNotifier = object : MonitorNotifier {
        override fun didDetermineStateForRegion(state: Int, region: Region?) {
            // 領域への入退場のステータス変化を検知
            val stateString = when (state) {
                MonitorNotifier.INSIDE -> "inside"
                MonitorNotifier.OUTSIDE -> "outside"
                else -> "unknown"
            }

            log("monitorNotifier:determine: region:" + region?.toString() + ", state:" + stateString)
        }

        override fun didEnterRegion(region: Region?) {
            // 領域への入場を検知
            log("monitorNotifier:enter: region:" + region?.toString())
        }

        override fun didExitRegion(region: Region?) {
            // 領域からの退場を検知
            log("monitorNotifier:exit: region:" + region?.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        beaconManager.bind(this)
    }

    override fun onDestroy() {
        beaconManager.unbind(this)
        super.onDestroy()
    }

    override fun onBeaconServiceConnect() {
        beaconManager.addRangeNotifier(rangeNotifier)
        beaconManager.startRangingBeaconsInRegion(rangeRegion)

        beaconManager.addMonitorNotifier(monitorNotifier)
        beaconManager.startMonitoringBeaconsInRegion(monitorRegion)
    }

    override fun unbindService(serviceConnection: ServiceConnection?) {
        beaconManager.stopRangingBeaconsInRegion(rangeRegion)
        beaconManager.removeAllRangeNotifiers()

        beaconManager.stopMonitoringBeaconsInRegion(monitorRegion)
        beaconManager.removeAllMonitorNotifiers()
    }

    private fun log(message: String) {
        Log.d(TAG, message)
    }
}

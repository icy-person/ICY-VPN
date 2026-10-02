package com.icyvpn.vpn
import android.app.*
import android.content.Intent
import android.net.VpnService
import android.os.IBinder
import android.os.ParcelFileDescriptor
import com.icyvpn.core.ConfigWatcher
import java.util.concurrent.Executors

class IcyVpnService: VpnService() {
    companion object { const val START="START"; const val STOP="STOP"; private const val CHANNEL="icy-vpn" }
    private var tun: ParcelFileDescriptor?=null
    private val executor=Executors.newSingleThreadExecutor()
    private var watcher: ConfigWatcher?=null

    override fun onStartCommand(i:Intent?, flags:Int, startId:Int):Int {
        if(i?.action==START) startVpn() else if(i?.action==STOP) stopVpn()
        return START_STICKY
    }
    private fun startVpn() {
        val nm=getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL,"ICY VPN",NotificationManager.IMPORTANCE_LOW))
        startForeground(100,Notification.Builder(this,CHANNEL).setContentTitle("ICY VPN").setContentText("VPN running").setSmallIcon(android.R.drawable.stat_sys_warning).build())
        if(tun!=null)return
        tun=Builder().setSession("ICY VPN").setMtu(1500).addAddress("10.10.0.2",32)
            .addRoute("0.0.0.0",0).addRoute("::",0).addDnsServer("8.8.8.8").addDnsServer("8.8.4.4").establish()
        val fd=tun?.fd ?: return
        watcher=ConfigWatcher(this,{json->NativeBridge.start(json,fd)},{NativeBridge.stop()})
        executor.execute{watcher?.run()}
    }
    private fun stopVpn(){watcher?.close();watcher=null;NativeBridge.stop();tun?.close();tun=null;stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()}
    override fun onDestroy(){stopVpn();executor.shutdownNow();super.onDestroy()}
    override fun onBind(intent:Intent):IBinder?=super.onBind(intent)
}
object NativeBridge {
    init { try { System.loadLibrary("icyvpn") } catch(_:Throwable){} }
    external fun start(config:String,tunFd:Int)
    external fun stop()
}

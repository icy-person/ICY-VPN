package com.icyvpn.vpn
import android.app.*
import android.content.Intent
import android.net.VpnService
import android.os.IBinder
import android.os.ParcelFileDescriptor
import com.icyvpn.core.AppPrefs
import com.icyvpn.core.ConfigWatcher
import com.icyvpn.core.VpnStateStore
import com.icyvpn.xray.XrayEngine
import java.util.concurrent.Executors
class IcyVpnService:VpnService(){
 companion object{const val START="com.icyvpn.START";const val STOP="com.icyvpn.STOP";private const val CHANNEL="icy-vpn"}
 private val executor=Executors.newSingleThreadExecutor();private var vpnInterface:ParcelFileDescriptor?=null;private var watcher:ConfigWatcher?=null;private lateinit var engine:XrayEngine
 override fun onCreate(){super.onCreate();engine=XrayEngine(this);getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL,"ICY VPN",NotificationManager.IMPORTANCE_LOW))}
 override fun onStartCommand(i:Intent?,flags:Int,startId:Int):Int{when(i?.action){START->startVpn();STOP->stopVpn()};return START_STICKY}
 private fun startVpn(){
  if(vpnInterface!=null)return
  startForeground(100,Notification.Builder(this,CHANNEL).setContentTitle("ICY VPN").setContentText("Starting…").setOngoing(true).setSmallIcon(com.icyvpn.R.drawable.ic_stat_vpn).build())
  val dns=AppPrefs.dnsServers(this)
  vpnInterface=Builder().setSession("ICY VPN").setMtu(1500).addAddress("10.10.0.2",32).addAddress("fd00:1::2",128).addRoute("0.0.0.0",0).addRoute("::",0).addDnsServer(dns.getOrElse(0){"8.8.8.8"}).addDnsServer(dns.getOrElse(1){"8.8.4.4"}).setMetered(false).establish()
  if(vpnInterface==null){VpnStateStore.update{it.copy(status="TUN creation failed",error="Android did not grant a VPN interface")};stopVpn();return}
  VpnStateStore.update{it.copy(status="Loading node…",error=null)}
  AppPrefs.cachedConfig(this)?.let { cached ->
   engine.start(cached, vpnInterface?.fd ?: -1, dns)
  }
  watcher=ConfigWatcher(this,{config,_,_->val fd=vpnInterface?.fd?:return@ConfigWatcher false;engine.replace(config,fd,AppPrefs.dnsServers(this))},{message->val cached=AppPrefs.cachedConfig(this);VpnStateStore.update{it.copy(status=if(cached!=null)"Using cached node" else "Waiting for node",error=message)}})
  executor.execute{watcher?.start()}
 }
 private fun stopVpn(){watcher?.close();watcher=null;engine.stop();vpnInterface?.close();vpnInterface=null;VpnStateStore.update{it.copy(running=false,status="Disconnected",busy=false)};stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()}
 override fun onDestroy(){stopVpn();executor.shutdownNow();super.onDestroy()}
 override fun onBind(i:Intent):IBinder?=super.onBind(i)
}
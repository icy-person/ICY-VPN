package com.icyvpn.core
import android.content.Context
object AppPrefs {
 private const val NAME="icy_vpn"; private const val AUTO="auto_update"; private const val INTERVAL="interval_ms"; private const val DNS1="dns1"; private const val DNS2="dns2"; private const val HASH="source_hash"; private const val ETAG="etag"; private const val CONFIG="last_config"
 private fun p(c:Context)=c.getSharedPreferences(NAME,Context.MODE_PRIVATE)
 fun autoUpdate(c:Context)=p(c).getBoolean(AUTO,true)
 fun intervalMs(c:Context)=p(c).getLong(INTERVAL,10000L).coerceIn(5000L,300000L)
 fun dnsServers(c:Context)=listOf(p(c).getString(DNS1,"8.8.8.8")?:"8.8.8.8",p(c).getString(DNS2,"8.8.4.4")?:"8.8.4.4").filter(String::isNotBlank)
 fun setAutoUpdate(c:Context,v:Boolean)=p(c).edit().putBoolean(AUTO,v).apply()
 fun setInterval(c:Context,v:Long)=p(c).edit().putLong(INTERVAL,v.coerceIn(5000L,300000L)).apply()
 fun setDns(c:Context,a:String,b:String)=p(c).edit().putString(DNS1,a.trim()).putString(DNS2,b.trim()).apply()
 fun sourceHash(c:Context)=p(c).getString(HASH,null)
 fun saveSourceHash(c:Context,v:String)=p(c).edit().putString(HASH,v).apply()
 fun etag(c:Context)=p(c).getString(ETAG,null)
 fun saveEtag(c:Context,v:String?){val e=p(c).edit();if(v.isNullOrBlank())e.remove(ETAG)else e.putString(ETAG,v);e.apply()}
 fun cachedConfig(c:Context)=p(c).getString(CONFIG,null)
 fun saveCachedConfig(c:Context,v:String)=p(c).edit().putString(CONFIG,v).apply()
}
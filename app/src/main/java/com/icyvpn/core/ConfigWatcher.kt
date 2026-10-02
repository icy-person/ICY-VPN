package com.icyvpn.core
import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
class ConfigWatcher(private val context:Context,private val onCandidate:(String,String,String)->Boolean,private val onFailure:(String)->Unit):AutoCloseable{
 companion object{const val SOURCE="https://icy-person.github.io/ir/vless.txt"}
 private val running=AtomicBoolean(true)
 fun start()=runBlocking{while(running.get()){if(AppPrefs.autoUpdate(context))checkOnce();if(running.get())delay(AppPrefs.intervalMs(context))}}
 private fun checkOnce(){
  VpnStateStore.update{it.copy(lastCheck=System.currentTimeMillis())}
  try{
   val r=NetworkFetcher.fetch(context,SOURCE,AppPrefs.etag(context));AppPrefs.saveEtag(context,r.etag);if(r.notModified)return
   val link=r.body?.lineSequence()?.map(String::trim)?.firstOrNull{it.startsWith("vless://",true)}?:throw IllegalStateException("No VLESS node in subscription")
   val hash=sha256(link);if(hash==AppPrefs.sourceHash(context)&&AppPrefs.cachedConfig(context)!=null)return
   val dns=AppPrefs.dnsServers(context);val d1=dns.firstOrNull()?:"8.8.8.8";val d2=dns.getOrNull(1)?:"8.8.4.4"
   val config=RustConfig.vlessToXray(link,d1,d2);if(config.startsWith("ERROR:"))throw IllegalStateException(config.removePrefix("ERROR:").trim())
   if(onCandidate(config,link,hash)){AppPrefs.saveSourceHash(context,hash);AppPrefs.saveCachedConfig(context,config);VpnStateStore.update{it.copy(lastChange=System.currentTimeMillis(),nodeName=nodeName(config),error=null)}}
  }catch(t:Throwable){onFailure(t.message?:"Configuration update failed")}
 }
 private fun nodeName(c:String)=runCatching{JSONObject(c).getJSONArray("outbounds").getJSONObject(0).optString("name","ICY-VPN")}.getOrDefault("ICY-VPN")
 private fun sha256(s:String)=MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString(""){"%02x".format(it)}
 override fun close(){running.set(false)}
}
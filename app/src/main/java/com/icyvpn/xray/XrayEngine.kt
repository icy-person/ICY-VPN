package com.icyvpn.xray
import android.net.VpnService
import com.icyvpn.core.VpnStateStore
import org.json.JSONObject
import libXray.DialerController
import libXray.LibXray
class XrayEngine(private val service:VpnService){
 private val controller=object:DialerController{override fun protectFd(fd:Int)=service.protect(fd)}
 private var active=false;private var activeConfig:String?=null
 fun start(config:String,tunFd:Int,dns:List<String>):Boolean{
  stop()
  val testConfig=JSONObject(config).apply{put("inbounds",org.json.JSONArray())}.toString()
  val test=invoke("testXray",JSONObject().put("xrayJson",testConfig));if(!test.first){VpnStateStore.setError(test.second);return false}
  return try{
   LibXray.registerDialerController(controller)
   val primary=dns.firstOrNull()?:"8.8.8.8";LibXray.setDNS(controller,primary+":53")
   val runtime=JSONObject(config);val env=JSONObject(runtime.optJSONObject("env")?.toString()?:"{}");env.put("xray.tun.fd",tunFd.toString());runtime.put("env",env)
   val run=invoke("runXray",JSONObject().put("xrayJson",runtime.toString()))
   if(!run.first){runCatching{LibXray.resetDNS()};VpnStateStore.setError(run.second);return false}
   active=true;activeConfig=config;VpnStateStore.update{it.copy(running=true,status="Connected",busy=false,error=null)};true
  }catch(t:Throwable){runCatching{LibXray.resetDNS()};VpnStateStore.setError(t.message?:"Xray startup failed");false}
 }
 fun replace(config:String,tunFd:Int,dns:List<String>):Boolean{
  val old=activeConfig;VpnStateStore.update{it.copy(busy=true,status="Updating node…")};stop()
  if(start(config,tunFd,dns))return true
  if(old!=null)start(old,tunFd,dns)
  return false
 }
 fun stop(){if(!active)return;runCatching{invoke("stopXray",JSONObject())};runCatching{LibXray.resetDNS()};active=false;activeConfig=null;VpnStateStore.update{it.copy(running=false,status="Disconnected",busy=false)}}
 private fun invoke(method:String,payload:JSONObject):Pair<Boolean,String>{
  val req=JSONObject().put("apiVersion",3).put("method",method).put("payload",payload).toString();val res=JSONObject(LibXray.invoke(req));return res.optBoolean("success",false) to res.optString("error","Xray request failed")
 }
}
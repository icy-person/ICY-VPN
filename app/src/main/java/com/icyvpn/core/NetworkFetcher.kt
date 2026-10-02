package com.icyvpn.core
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.HttpURLConnection
import java.net.URL
data class FetchResult(val body:String?,val etag:String?,val notModified:Boolean)
object NetworkFetcher{
 fun fetch(context:Context,urlText:String,previousEtag:String?):FetchResult{
  val cm=context.getSystemService(ConnectivityManager::class.java)
  val physical=cm.allNetworks.firstOrNull{n->val caps=cm.getNetworkCapabilities(n)?:return@firstOrNull false;caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)&&caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)}
  val c=((physical?.openConnection(URL(urlText))?:URL(urlText).openConnection()) as HttpURLConnection)
  c.connectTimeout=5000;c.readTimeout=5000;c.instanceFollowRedirects=true;c.setRequestProperty("Cache-Control","no-cache")
  if(!previousEtag.isNullOrBlank())c.setRequestProperty("If-None-Match",previousEtag)
  return try{val code=c.responseCode;val et=c.getHeaderField("ETag");if(code==HttpURLConnection.HTTP_NOT_MODIFIED)FetchResult(null,et?:previousEtag,true)else if(code in 200..299)FetchResult(c.inputStream.bufferedReader().use{it.readText()},et,false)else throw IllegalStateException("HTTP $code")}finally{c.disconnect()}
 }
}
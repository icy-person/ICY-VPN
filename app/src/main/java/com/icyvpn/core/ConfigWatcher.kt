package com.icyvpn.core
import android.content.Context
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean

class ConfigWatcher(private val context:Context,private val onChange:(String)->Unit,private val onStop:()->Unit):AutoCloseable{
    companion object { const val SOURCE="https://icy-person.github.io/ir/vless.txt" }
    private val running=AtomicBoolean(true)
    private var hash:String?=null
    fun run(){
        while(running.get()){
            try{
                val text=URL(SOURCE).openStream().bufferedReader().use{it.readText()}
                val link=text.lineSequence().map{it.trim()}.firstOrNull{it.startsWith("vless://")}
                if(link!=null){
                    val h=sha(link)
                    if(h!=hash){onChange(RustConfig.vlessToXray(link));hash=h}
                }
            }catch(_:Throwable){}
            try{Thread.sleep(10_000)}catch(_:InterruptedException){break}
        }
        onStop()
    }
    private fun sha(s:String)=MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString(""){"%02x".format(it)}
    override fun close(){running.set(false)}
}
object RustConfig{
    init{try{System.loadLibrary("icyvpn")}catch(_:Throwable){}}
    external fun vlessToXray(link:String):String
}

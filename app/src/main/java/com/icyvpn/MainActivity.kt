package com.icyvpn
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.icyvpn.vpn.IcyVpnService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var connected by remember { mutableStateOf(false) }
                Scaffold(topBar={ TopAppBar(title={Text("ICY VPN")}) }) { p ->
                    Column(Modifier.padding(p).padding(24.dp), verticalArrangement=Arrangement.spacedBy(18.dp)) {
                        Text(if (connected) "Connected" else "Disconnected", style=MaterialTheme.typography.headlineMedium)
                        Text("Auto-update: 10 seconds")
                        Text("DNS: 8.8.8.8 • 8.8.4.4")
                        Button(onClick={
                            if (connected) startService(Intent(this@MainActivity,IcyVpnService::class.java).setAction(IcyVpnService.STOP))
                            else {
                                val prepare=VpnService.prepare(this@MainActivity)
                                if (prepare != null) startActivityForResult(prepare,1001)
                                else startService(Intent(this@MainActivity,IcyVpnService::class.java).setAction(IcyVpnService.START))
                            }
                            connected=!connected
                        }, modifier=Modifier.fillMaxWidth()) { Text(if(connected) "Disconnect" else "Connect") }
                    }
                }
            }
        }
    }
}

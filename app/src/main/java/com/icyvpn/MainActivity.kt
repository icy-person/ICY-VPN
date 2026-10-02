package com.icyvpn
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.icyvpn.core.*
import com.icyvpn.vpn.IcyVpnService
class MainActivity:ComponentActivity(){
 private val permission=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){if(it.resultCode==RESULT_OK)startVpn()}
 override fun onCreate(b:Bundle?){super.onCreate(b);setContent{IcyTheme{val state by VpnStateStore.state.collectAsState();IcyApp(state,{connect()},{disconnect()})}}}
 private fun connect(){val p=VpnService.prepare(this);if(p!=null)permission.launch(p)else startVpn()}
 private fun startVpn(){ContextCompat.startForegroundService(this,Intent(this,IcyVpnService::class.java).setAction(IcyVpnService.START))}
 private fun disconnect(){startService(Intent(this,IcyVpnService::class.java).setAction(IcyVpnService.STOP))}
}
@Composable private fun IcyTheme(content: @Composable () -> Unit)=MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFF9B87F5),secondary=Color(0xFF6D9CFF),surface=Color(0xFF151820),background=Color(0xFF0B0D12)),content=content)
@Composable private fun IcyApp(state:VpnUiState,onConnect:()->Unit,onDisconnect:()->Unit){
 var settings by remember{mutableStateOf(false)}
 Scaffold(topBar={TopAppBar(title={Text(if(settings)"Settings" else "ICY VPN")})}){pad->
  if(settings)SettingsScreen{settings=false}else Column(Modifier.fillMaxSize().padding(pad).padding(22.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
   Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=Color(0xFF151820))){Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(if(state.running)"SECURE TUNNEL"else"READY",color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelLarge);Text(state.status,style=MaterialTheme.typography.headlineMedium);Text(state.nodeName,style=MaterialTheme.typography.titleMedium);state.error?.let{Text(it,color=Color(0xFFFF9C9C),style=MaterialTheme.typography.bodySmall)}}}
   Button(onClick={if(state.running)onDisconnect()else onConnect()},enabled=!state.busy,modifier=Modifier.fillMaxWidth().height(54.dp)){Text(if(state.running)"Disconnect"else"Connect")}
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){val c=androidx.compose.ui.platform.LocalContext.current;InfoCard("DNS",AppPrefs.dnsServers(c).joinToString(" / "),Modifier.weight(1f));InfoCard("Update",(AppPrefs.intervalMs(c)/1000L).toString()+"s",Modifier.weight(1f))}
   Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=Color(0xFF151820))){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Configuration source",style=MaterialTheme.typography.labelLarge);Text(ConfigWatcher.SOURCE,style=MaterialTheme.typography.bodySmall);Text("Rust parse → Xray validate → safe node switch",style=MaterialTheme.typography.bodySmall)}}
   OutlinedButton(onClick={settings=true},modifier=Modifier.fillMaxWidth()){Text("Settings & diagnostics")}
  }}}
@Composable private fun InfoCard(title:String,value:String,modifier:Modifier)=Card(modifier,colors=CardDefaults.cardColors(containerColor=Color(0xFF151820))){Column(Modifier.padding(16.dp)){Text(title,color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelMedium);Text(value)}}
@Composable private fun SettingsScreen(onBack:()->Unit){
 val c=androidx.compose.ui.platform.LocalContext.current
 var auto by remember{mutableStateOf(AppPrefs.autoUpdate(c))};var interval by remember{mutableStateOf(AppPrefs.intervalMs(c)/1000f)};var d1 by remember{mutableStateOf(AppPrefs.dnsServers(c).getOrElse(0){"8.8.8.8"})};var d2 by remember{mutableStateOf(AppPrefs.dnsServers(c).getOrElse(1){"8.8.4.4"})}
 Column(Modifier.fillMaxSize().padding(22.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("Automatic node updates");Switch(checked=auto,onCheckedChange={auto=it;AppPrefs.setAutoUpdate(c,it)})}
  HorizontalDivider();Text("Polling interval: "+interval.toInt()+" seconds");Slider(value=interval,onValueChange={interval=it},valueRange=5f..60f,steps=10,onValueChangeFinished={AppPrefs.setInterval(c,(interval*1000).toLong())})
  Text("Default is 10 seconds. Polling runs in the foreground VPN service.",style=MaterialTheme.typography.bodySmall)
  Text("DNS",style=MaterialTheme.typography.titleLarge);OutlinedTextField(value=d1,onValueChange={d1=it},modifier=Modifier.fillMaxWidth(),label={Text("Primary DNS")});OutlinedTextField(value=d2,onValueChange={d2=it},modifier=Modifier.fillMaxWidth(),label={Text("Secondary DNS")})
  Button(onClick={AppPrefs.setDns(c,d1,d2)},modifier=Modifier.fillMaxWidth()){Text("Save DNS")}
  Text("Source",style=MaterialTheme.typography.titleLarge);Text(ConfigWatcher.SOURCE,style=MaterialTheme.typography.bodySmall);Spacer(Modifier.weight(1f));TextButton(onClick=onBack,modifier=Modifier.fillMaxWidth()){Text("Back")}
 }}

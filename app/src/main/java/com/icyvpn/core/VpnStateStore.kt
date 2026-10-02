package com.icyvpn.core
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
data class VpnUiState(val running:Boolean=false,val busy:Boolean=false,val status:String="Disconnected",val nodeName:String="No node loaded",val source:String=ConfigWatcher.SOURCE,val lastCheck:Long?=null,val lastChange:Long?=null,val error:String?=null)
object VpnStateStore{private val _state=MutableStateFlow(VpnUiState());val state=_state.asStateFlow();fun update(f:(VpnUiState)->VpnUiState){_state.value=f(_state.value)}fun setError(s:String?){update{it.copy(error=s)}}}
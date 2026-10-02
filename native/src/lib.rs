use jni::{JNIEnv,objects::{JClass,JString},sys::{jint,jstring}};
use std::sync::{Mutex,OnceLock};
static RUNNING:OnceLock<Mutex<bool>>=OnceLock::new();
fn running()->&'static Mutex<bool>{RUNNING.get_or_init(||Mutex::new(false))}

#[no_mangle]
pub extern "system" fn Java_com_icyvpn_vpn_NativeBridge_start(mut env:JNIEnv,_:JClass,config:JString,tun_fd:jint){
    let _=env.get_string(&config);
    let _=tun_fd;
    *running().lock().unwrap()=true;
}
#[no_mangle]
pub extern "system" fn Java_com_icyvpn_vpn_NativeBridge_stop(_:JNIEnv,_:JClass){*running().lock().unwrap()=false;}

#[no_mangle]
pub extern "system" fn Java_com_icyvpn_core_RustConfig_vlessToXray(mut env:JNIEnv,_:JClass,link:JString)->jstring{
    let s=env.get_string(&link).map(|v|v.to_string_lossy().into_owned()).unwrap_or_default();
    let out=serde_json::json!({
      "log":{"loglevel":"warning"},
      "dns":{"servers":["8.8.8.8","8.8.4.4"]},
      "inbounds":[],
      "outbounds":[{"protocol":"vless","tag":"proxy","settings":{"vnext":[]},"_icy_vless":s}]
    });
    env.new_string(out.to_string()).unwrap().into_raw()
}

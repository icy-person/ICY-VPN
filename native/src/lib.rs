use jni::{objects::{JClass,JString},sys::jstring,JNIEnv};
use serde_json::{json,Map,Value};
use std::collections::HashMap;
use url::Url;
use uuid::Uuid;
fn qmap(u:&Url)->HashMap<String,String>{u.query_pairs().into_owned().map(|(k,v)|(k.to_ascii_lowercase(),v)).collect()}
fn csv(v:Option<&String>)->Vec<String>{v.map(|s|s.split(',').map(str::trim).filter(|x|!x.is_empty()).map(str::to_owned).collect()).unwrap_or_default()}
fn boolish(v:Option<&String>)->bool{matches!(v.map(String::as_str),Some("1")|Some("true")|Some("yes"))}
fn build_config(link:&str,dns1:&str,dns2:&str)->Result<String,String>{
 let u=Url::parse(link).map_err(|e|format!("invalid VLESS URI: {e}"))?;if !u.scheme().eq_ignore_ascii_case("vless"){return Err("URI scheme must be vless".into())}
 let id=u.username();Uuid::parse_str(id).map_err(|_|"VLESS UUID is invalid".to_string())?;let host=u.host_str().ok_or("VLESS server address is missing")?.to_owned();let port=u.port().unwrap_or(443);let q=qmap(&u);
 let network=q.get("type").or_else(||q.get("network")).map(String::as_str).unwrap_or("tcp").to_ascii_lowercase();let security=q.get("security").map(String::as_str).unwrap_or("none").to_ascii_lowercase();
 let mut user=Map::new();user.insert("id".into(),json!(id));user.insert("encryption".into(),json!("none"));if let Some(flow)=q.get("flow").filter(|x|!x.is_empty()){user.insert("flow".into(),json!(flow));}
 let vnext=json!({"address":host,"port":port,"users":[Value::Object(user)]});let mut stream=Map::new();stream.insert("network".into(),json!(network));
 match security.as_str(){
  "none"|""=>{},
  "tls"=>{let mut t=Map::new();t.insert("serverName".into(),json!(q.get("sni").cloned().unwrap_or_else(||u.host_str().unwrap_or_default().to_owned())));if let Some(fp)=q.get("fp").filter(|x|!x.is_empty()){t.insert("fingerprint".into(),json!(fp));}let alpn=csv(q.get("alpn"));if !alpn.is_empty(){t.insert("alpn".into(),json!(alpn));}if boolish(q.get("allowinsecure").or_else(||q.get("insecure"))){t.insert("allowInsecure".into(),json!(true));}stream.insert("security".into(),json!("tls"));stream.insert("tlsSettings".into(),Value::Object(t));},
  "reality"=>{let mut r=Map::new();r.insert("serverName".into(),json!(q.get("sni").cloned().unwrap_or_else(||u.host_str().unwrap_or_default().to_owned())));if let Some(fp)=q.get("fp").filter(|x|!x.is_empty()){r.insert("fingerprint".into(),json!(fp));}if let Some(p)=q.get("pbk").or_else(||q.get("publickey")).filter(|x|!x.is_empty()){r.insert("publicKey".into(),json!(p));}if let Some(s)=q.get("sid").or_else(||q.get("shortid")).filter(|x|!x.is_empty()){r.insert("shortId".into(),json!(s));}if let Some(s)=q.get("spx").or_else(||q.get("spiderx")).filter(|x|!x.is_empty()){r.insert("spiderX".into(),json!(s));}stream.insert("security".into(),json!("reality"));stream.insert("realitySettings".into(),Value::Object(r));},
  x=>return Err(format!("unsupported VLESS security: {x}"))
 }
 match network.as_str(){
  "tcp"|"raw"=>{if let Some(h)=q.get("headertype").filter(|x|!x.eq_ignore_ascii_case("none")){stream.insert("tcpSettings".into(),json!({"header":{"type":h}}));}},
  "ws"=>{let path=q.get("path").cloned().unwrap_or_else(||"/".into());let hh=q.get("host").cloned().unwrap_or_else(||u.host_str().unwrap_or_default().to_owned());stream.insert("wsSettings".into(),json!({"path":path,"headers":{"Host":hh}}));},
  "grpc"=>{let mut g=Map::new();g.insert("serviceName".into(),json!(q.get("servicename").cloned().unwrap_or_default()));if let Some(a)=q.get("authority").filter(|x|!x.is_empty()){g.insert("authority".into(),json!(a));}if q.get("mode").map(|m|m.eq_ignore_ascii_case("multi")).unwrap_or(false){g.insert("multiMode".into(),json!(true));}stream.insert("grpcSettings".into(),Value::Object(g));},
  "http"|"h2"=>{let mut h=Map::new();h.insert("path".into(),json!(q.get("path").cloned().unwrap_or_else(||"/".into())));let hosts=csv(q.get("host"));if !hosts.is_empty(){h.insert("host".into(),json!(hosts));}stream.insert("httpSettings".into(),Value::Object(h));},
  "xhttp"|"splithttp"=>{let mut x=Map::new();x.insert("path".into(),json!(q.get("path").cloned().unwrap_or_else(||"/".into())));if let Some(h)=q.get("host").filter(|x|!x.is_empty()){x.insert("host".into(),json!(h));}if let Some(m)=q.get("mode").filter(|x|!x.is_empty()){x.insert("mode".into(),json!(m));}stream.insert("xhttpSettings".into(),Value::Object(x));},
  x=>return Err(format!("unsupported VLESS transport: {x}"))
 }
 let name=u.fragment().filter(|x|!x.is_empty()).unwrap_or("ICY-VPN");
 let config=json!({"log":{"loglevel":"warning"},"dns":{"servers":[dns1,dns2]},"inbounds":[{"tag":"tun","port":0,"protocol":"tun","settings":{"name":"icy0","desc":"ICY VPN","mtu":1500},"sniffing":{"enabled":true,"destOverride":["http","tls","quic"]}}],"outbounds":[{"tag":"proxy","protocol":"vless","settings":{"vnext":[vnext]},"streamSettings":stream,"name":name},{"tag":"dns-out","protocol":"dns","settings":{"address":dns1,"port":53,"network":"udp"}},{"tag":"block","protocol":"blackhole"}],"routing":{"domainStrategy":"IPIfNonMatch","rules":[{"type":"field","inboundTag":["tun"],"network":"udp","port":53,"outboundTag":"dns-out"}]}});
 serde_json::to_string_pretty(&config).map_err(|e|e.to_string())
}
#[no_mangle]pub extern "system" fn Java_com_icyvpn_core_RustConfig_vlessToXray(mut env:JNIEnv,_:JClass,link:JString,dns1:JString,dns2:JString)->jstring{
 let l=env.get_string(&link).map(|v|v.to_string_lossy().into_owned()).unwrap_or_default();let d1=env.get_string(&dns1).map(|v|v.to_string_lossy().into_owned()).unwrap_or_else(|_|"8.8.8.8".into());let d2=env.get_string(&dns2).map(|v|v.to_string_lossy().into_owned()).unwrap_or_else(|_|"8.8.4.4".into());let r=build_config(&l,&d1,&d2).unwrap_or_else(|e|format!("ERROR: {e}"));env.new_string(r).unwrap().into_raw()
}
#[cfg(test)]mod tests{use super::build_config;#[test]fn parses_reality_ws(){let u="vless://123e4567-e89b-12d3-a456-426614174000@example.com:443?security=reality&type=ws&sni=example.com&fp=chrome&pbk=PUB&sid=01&path=%2Fws#demo";let v:serde_json::Value=serde_json::from_str(&build_config(u,"8.8.8.8","8.8.4.4").unwrap()).unwrap();assert_eq!(v["outbounds"][0]["protocol"],"vless");assert_eq!(v["outbounds"][0]["streamSettings"]["security"],"reality");}}

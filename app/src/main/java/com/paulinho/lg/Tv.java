package com.paulinho.lg;
import org.json.*;
class Tv {
 String id, name, host, key, cert, mac;
 Tv(String id,String name,String host) {this.id=id;this.name=name;this.host=host;key="";cert="";mac="";}
 Tv(JSONObject j){id=j.optString("id");name=j.optString("name");host=j.optString("host");key=j.optString("key");cert=j.optString("cert");mac=j.optString("mac");}
 JSONObject json(){JSONObject j=new JSONObject();try{j.put("id",id).put("name",name).put("host",host).put("key",key).put("cert",cert).put("mac",mac);}catch(Exception ignored){}return j;}
}

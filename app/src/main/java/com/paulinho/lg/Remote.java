package com.paulinho.lg;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.*;
import java.net.*;
import java.security.*;
import java.security.cert.*;
import javax.net.ssl.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
class Remote {
 interface Listener {void status(String status);void paired(Tv tv);void data(String kind,JSONObject payload);}
 final Listener listener;final AtomicInteger ids=new AtomicInteger();
 final ConcurrentHashMap<String,String> pending=new ConcurrentHashMap<>();
 WebSocketClient main,pointer;volatile boolean ready=false;volatile int generation=0;Tv tv;String observedCert="";
 Remote(Listener l){listener=l;}
 static JSONObject obj(Object... values){JSONObject j=new JSONObject();try{for(int i=0;i<values.length;i+=2)j.put((String)values[i],values[i+1]);}catch(Exception ignored){}return j;}
 void connect(Tv t){disconnect();tv=t;observedCert="";int g=generation;listener.status("Conectando a "+t.name+"…");open(t,g,true);}
 void open(Tv target,int g,boolean secure){
 try{
 WebSocketClient client=new WebSocketClient(new URI((secure?"wss":"ws")+"://"+target.host+":"+(secure?3001:3000)),new org.java_websocket.drafts.Draft_6455(),null,6000){
 protected void onSetSSLParameters(SSLParameters parameters){parameters.setEndpointIdentificationAlgorithm(null);}
 boolean opened=false;
 public void onOpen(ServerHandshake h){if(g!=generation){close();return;}opened=true;listener.status("Aceite a conexão na tela da TV, se aparecer.");
 JSONArray permissions=new JSONArray();for(String p:new String[]{"CONTROL_AUDIO","CONTROL_INPUT_TV","CONTROL_POWER","CONTROL_INPUT_TEXT","CONTROL_MOUSE_AND_KEYBOARD","CONTROL_INPUT_MEDIA_PLAYBACK","LAUNCH","READ_INSTALLED_APPS","READ_CURRENT_CHANNEL","READ_INPUT_DEVICE_LIST","READ_TV_CHANNEL_LIST","READ_RUNNING_APPS"})permissions.put(p);
 JSONObject payload=obj("pairingType","PROMPT","manifest",obj("manifestVersion",1,"appVersion","1.0","permissions",permissions));if(!target.key.isEmpty())try{payload.put("client-key",target.key);}catch(Exception ignored){}
 send(obj("id","register","type","register","payload",payload).toString());
 }
 public void onMessage(String message){if(g!=generation)return;try{
 JSONObject m=new JSONObject(message),p=m.optJSONObject("payload");if(p==null)p=new JSONObject();String type=m.optString("type"),id=m.optString("id");
 if(type.equals("registered")){ready=true;target.key=p.optString("client-key",target.key);if(!observedCert.isEmpty())target.cert=observedCert;listener.paired(target);listener.status("Conectado • "+target.name);request("ssap://com.webos.service.networkinput/getPointerInputSocket",new JSONObject(),"pointer");subscribe("ssap://com.webos.service.ime/registerRemoteKeyboard","keyboard");subscribe("ssap://audio/getVolume","volume");}
 else if(type.equals("error")){String kind=pending.remove(id);listener.status("A TV recusou "+(kind==null?"o pedido":kind)+": "+m.optString("error","comando indisponível"));if(id.equals("register")){ready=false;close();}}
 else {String kind=pending.get(id);if(kind!=null){if(!kind.equals("volume")&&!kind.equals("keyboard"))pending.remove(id);if(kind.equals("pointer"))openPointer(p.optString("socketPath"),target,g);else listener.data(kind,p);}}
 }catch(Exception e){listener.status("Resposta inválida da TV.");}}
 public void onClose(int code,String reason,boolean remote){if(g!=generation)return;ready=false;if(pointer!=null)pointer.close();if(!opened&&secure&&target.cert.isEmpty()){open(target,g,false);return;}listener.status("Desconectado. Selecione a TV para tentar novamente.");}
 public void onError(Exception e){if(g==generation&&opened)listener.status("Falha na conexão: "+e.getMessage());}
 };
 if(secure)client.setSocketFactory(tls(target).getSocketFactory());main=client;client.setConnectionLostTimeout(15);client.connect();
 Thread timeout=new Thread(()->{try{Thread.sleep(35000);if(g==generation&&!ready&&main==client){listener.status("A conexão expirou. Confira a TV e tente novamente.");client.close();}}catch(InterruptedException ignored){}},"pair-timeout");timeout.setDaemon(true);timeout.start();
 }catch(Exception e){if(secure&&target.cert.isEmpty())open(target,g,false);else listener.status("Falha ao conectar: "+e.getMessage());}
 }
 SSLContext tls(Tv target)throws Exception{
 SSLContext context=SSLContext.getInstance("TLS");context.init(null,new TrustManager[]{new X509TrustManager(){
 public X509Certificate[] getAcceptedIssuers(){return new X509Certificate[0];}public void checkClientTrusted(X509Certificate[] c,String a){}
 public void checkServerTrusted(X509Certificate[] chain,String auth)throws CertificateException{try{
 byte[] digest=MessageDigest.getInstance("SHA-256").digest(chain[0].getEncoded());StringBuilder b=new StringBuilder();for(byte x:digest)b.append(String.format("%02x",x&255));String hash=b.toString();
 if(!target.cert.isEmpty()&&!target.cert.equals(hash))throw new CertificateException("O certificado da TV mudou. Remova a TV salva e pareie novamente.");observedCert=hash;
 }catch(CertificateException e){throw e;}catch(Exception e){throw new CertificateException(e);}}
 }},new SecureRandom());return context;
 }
 void openPointer(String path,Tv target,int g){try{URI uri=new URI(path);if(!target.host.equals(uri.getHost())||!(uri.getScheme().equals("ws")||uri.getScheme().equals("wss")))throw new Exception("endereço inválido");
 pointer=new WebSocketClient(uri){protected void onSetSSLParameters(SSLParameters parameters){parameters.setEndpointIdentificationAlgorithm(null);}public void onOpen(ServerHandshake h){if(g!=generation)close();}public void onMessage(String s){}public void onClose(int c,String r,boolean x){}public void onError(Exception e){if(g==generation)listener.status("Mouse indisponível. Reconecte a TV.");}};
 if(uri.getScheme().equals("wss"))pointer.setSocketFactory(tls(target).getSocketFactory());pointer.setConnectionLostTimeout(15);pointer.connect();
 }catch(Exception e){listener.status("Não foi possível ativar o mouse: "+e.getMessage());}}
 void subscribe(String uri,String kind){sendRequest(uri,new JSONObject(),kind,"subscribe");}
 void request(String uri,JSONObject payload,String kind){sendRequest(uri,payload,kind,"request");}
 void sendRequest(String uri,JSONObject payload,String kind,String type){if(!ready||main==null||!main.isOpen()){listener.status("Selecione e conecte uma TV primeiro.");return;}String id="r"+ids.incrementAndGet();pending.put(id,kind);try{main.send(obj("id",id,"type",type,"uri",uri,"payload",payload).toString());}catch(Exception e){pending.remove(id);listener.status("Não foi possível enviar o comando.");}}
 void command(String uri){request("ssap://"+uri,new JSONObject(),"comando");}
 void pointerSend(String data){if(!ready||pointer==null||!pointer.isOpen()){listener.status("O ponteiro ainda não está conectado. Tente reconectar.");return;}try{pointer.send(data);}catch(Exception e){listener.status("Falha ao enviar para o ponteiro.");}}
 void button(String name){pointerSend("type:button\nname:"+name+"\n\n");}
 void click(){pointerSend("type:click\n\n");}
 void move(float x,float y){pointerSend("type:move\ndx:"+x+"\ndy:"+y+"\ndown:0\n\n");}
 void scroll(int y){pointerSend("type:scroll\ndx:0\ndy:"+y+"\n\n");}
 void disconnect(){generation++;ready=false;pending.clear();if(main!=null){main.close();main=null;}if(pointer!=null){pointer.close();pointer=null;}}
}

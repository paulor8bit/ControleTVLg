package com.paulinho.lg;
import android.content.Context;
import android.net.wifi.WifiManager;
import java.net.*;
import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
class Discovery {
 interface Listener {void found(Tv tv);void done(String error);}
 static String text(Document d,String tag){NodeList n=d.getElementsByTagName(tag);return n.getLength()>0?n.item(0).getTextContent():"";}
 static void search(Context context,Listener listener){new Thread(()->{
 WifiManager.MulticastLock lock=null;String error=null;
 try {
 WifiManager wifi=(WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
 lock=wifi.createMulticastLock("lg-discovery");lock.setReferenceCounted(false);lock.acquire();
 try(DatagramSocket socket=new DatagramSocket()) {
 socket.setSoTimeout(1000);Set<String> seen=new HashSet<>();
 for(String st:new String[]{"urn:lge-com:service:webos-second-screen:1","ssdp:all"}){
 String request="M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\nST: "+st+"\r\n\r\n";
 byte[] bytes=request.getBytes("UTF-8");socket.send(new DatagramPacket(bytes,bytes.length,InetAddress.getByName("239.255.255.250"),1900));}
 long end=System.currentTimeMillis()+7000;
 while(System.currentTimeMillis()<end){byte[] bytes=new byte[8192];DatagramPacket packet=new DatagramPacket(bytes,bytes.length);
 try{socket.receive(packet);}catch(SocketTimeoutException e){continue;}
 String response=new String(bytes,0,packet.getLength(),"UTF-8");String location="";
 for(String line:response.split("\r?\n"))if(line.toLowerCase(Locale.ROOT).startsWith("location:"))location=line.substring(9).trim();
 if(location.isEmpty()||!seen.add(location))continue;
 try {
 URL url=new URL(location);if(!url.getProtocol().equals("http")||!InetAddress.getByName(url.getHost()).equals(packet.getAddress()))continue;
 HttpURLConnection connection=(HttpURLConnection)url.openConnection();connection.setConnectTimeout(1200);connection.setReadTimeout(1200);connection.setInstanceFollowRedirects(false);
 try(InputStream input=connection.getInputStream()){
 org.xmlpull.v1.XmlPullParser parser=android.util.Xml.newPullParser();parser.setInput(input,null);String id="",name="",services="";int event;int events=0;
 while((event=parser.next())!=org.xmlpull.v1.XmlPullParser.END_DOCUMENT){if(++events>10000)throw new IOException("XML demasiado grande");if(event==org.xmlpull.v1.XmlPullParser.START_TAG){String tag=parser.getName();if(tag.equals("UDN"))id=parser.nextText();else if(tag.equals("friendlyName"))name=parser.nextText();else if(tag.equals("serviceType"))services+=" "+parser.nextText();}}
 if(!services.toLowerCase(Locale.ROOT).contains("webos-second-screen")&&!response.toLowerCase(Locale.ROOT).contains("webos-second-screen"))continue;
 listener.found(new Tv(id.isEmpty()?packet.getAddress().getHostAddress():id,name.isEmpty()?"LG webOS":name,packet.getAddress().getHostAddress()));
 }finally{connection.disconnect();}
 }catch(Exception ignored){}
 }
 }
 }catch(Exception e){error="Não foi possível buscar: "+e.getMessage();}finally{if(lock!=null&&lock.isHeld())lock.release();listener.done(error);}
 },"lg-discovery").start();}
}

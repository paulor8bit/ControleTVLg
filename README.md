# Controle LG para Paulinho
App Android nativo (Android 6+) para LG webOS na rede local. Sem anúncios ou serviços externos em execução.

## Instalação e uso
Instale `Controle-LG-Paulinho.apk` da pasta Download. Mantenha o celular e a TV na mesma rede, busque a TV, selecione e aceite o pareamento na tela da TV. A lista e a chave são guardadas no armazenamento privado do app, sem backup. TVs salvas reaparecem ao abrir; a descoberta atualiza o IP quando a TV é reconhecida pelo UUID. Há adição por IP e remoção por toque longo.

Abas: TVs, Controle, Mouse e Teclado. Controle inclui navegação, volume, mudo, canais, mídia, entradas, aplicativos, números, teclas coloridas e desligamento. Ligar usa Wake-on-LAN com MAC informado e depende da opção de ligar pela rede e do suporte do modelo. Texto remoto requer campo de entrada ativo e suporte do app da TV. Não é um produto oficial da LG.

## Rede e pareamento
SSDP/UPnP busca `urn:lge-com:service:webos-second-screen:1`. WebSocket SSAP tenta WSS 3001 e, sem certificado salvo, WS 3000. TVs possuem certificados locais; o primeiro certificado é guardado após pareamento e conferido nas conexões seguintes. O ponteiro só aceita o host da TV selecionada. Não aceitar mudanças de certificado exige remover o pareamento local e parear novamente.

## Validação
Compilação Android e assinatura verificadas. Teste de integração com servidor WebSocket simulado cobre registro, reutilização de chave após serialização, volume, texto UTF-8, seta, movimento e clique. Ainda requer teste numa TV LG real; não há garantia universal para teclas específicas/modelos/apps. Descoberta depende de multicast permitido pela rede.

Fontes primárias do protocolo:
- https://github.com/ConnectSDK/Connect-SDK-Android-Core/blob/master/src/com/connectsdk/service/WebOSTVService.java
- https://github.com/ConnectSDK/Connect-SDK-Android-Core/blob/master/src/com/connectsdk/service/webos/WebOSTVServiceSocketClient.java
- https://github.com/ConnectSDK/Connect-SDK-Android-Core/blob/master/src/com/connectsdk/service/webos/WebOSTVMouseSocketConnection.java
- https://github.com/ConnectSDK/Connect-SDK-Android-Core/blob/master/src/com/connectsdk/service/webos/WebOSTVKeyboardInput.java

Build: `ANDROID_HOME=$HOME/android-sdk /data/data/com.termux/files/usr/share/gradle-8.7/bin/gradle --offline :app:assembleDebug`
Teste: compile `Remote.java`, `Tv.java`, `tests/ProtocolTest.java` com classpath `app/libs/*:tests/lib/*` e execute `com.paulinho.lg.ProtocolTest`.
Bibliotecas: Java-WebSocket 1.5.6 (MIT), SLF4J 2.0.6 (MIT); org.json é usado apenas no teste de desktop.

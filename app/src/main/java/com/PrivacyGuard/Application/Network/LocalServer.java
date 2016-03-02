package com.PrivacyGuard.Application.Network;

import android.util.Log;

import com.PrivacyGuard.Application.Logger;
import com.PrivacyGuard.Application.MyVpnService;
import com.PrivacyGuard.Application.Network.Forwader.MySocketForwarder;
import com.PrivacyGuard.Application.Network.SSL.SSLSocketBuilder;

import org.sandrop.webscarab.model.ConnectionDescriptor;
import org.sandrop.webscarab.plugin.proxy.SiteData;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Created by frank on 2014-06-03.
 */
public class LocalServer extends Thread {
  public static final int SSLPort = 443;
  private static final boolean DEBUG = true;
  private static final String TAG = LocalServer.class.getSimpleName();
  public static int port = 12345;
  private ServerSocketChannel serverSocketChannel;
  private MyVpnService vpnService;
  private Set<String> sslPinning = new HashSet<String>();
  public LocalServer(MyVpnService vpnService) {
    if(serverSocketChannel == null || !serverSocketChannel.isOpen())
      try {
        listen();
      } catch (IOException e) {
        if(DEBUG) Log.d(TAG, "Listen error");
        e.printStackTrace();
      }
    this.vpnService = vpnService;
  }

  private void listen() throws IOException {
    serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.socket().setReuseAddress(true);
    serverSocketChannel.socket().bind(null);
    port = serverSocketChannel.socket().getLocalPort();
  }

  @Override
  public void run() {
    while (!isInterrupted()) {
      try {
        Logger.d(TAG, "Accepting");
        SocketChannel socketChannel = serverSocketChannel.accept();
        Socket socket = socketChannel.socket();
        Logger.d(TAG, "Receiving : " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
        new Thread(new ForwarderHandler(socket)).start();
        Logger.d(TAG, "Not blocked");
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    Log.d(TAG, "Stop Listening");
  }

  private class ForwarderHandler implements Runnable {
    private final String TAG = ForwarderHandler.class.getSimpleName();
    private Socket client;
    public ForwarderHandler(Socket client) {
      this.client = client;
    }
    @Override
    public void run() {
      try {
        ConnectionDescriptor descriptor = vpnService.getClientAppResolver().getClientDescriptorByPort(client.getPort());
        SocketChannel targetChannel = SocketChannel.open();
        Socket target = targetChannel.socket();
        vpnService.protect(target);
        targetChannel.connect(new InetSocketAddress(descriptor.getRemoteAddress(), descriptor.getRemotePort()));
        if(descriptor != null && descriptor.getRemotePort() == SSLPort && !sslPinning.contains(descriptor.getRemoteAddress())) {
          SiteData remoteData = vpnService.getHostNameResolver().getSecureHost(client, descriptor, true);
          Logger.d(TAG, "Begin Handshake : " + remoteData.tcpAddress + " " + remoteData.name);
          SSLSocket ssl_client = SSLSocketBuilder.negotiateSSL(client, remoteData, false, vpnService.getSSlSocketFactoryFactory());
          SSLSession session = ssl_client.getSession();
          Logger.d(TAG, "After Handshake : " + remoteData.tcpAddress + " " + remoteData.name + " " + session + " is valid : " + session.isValid());
          if(session.isValid()) {
            client = ssl_client;
            target = ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(target, descriptor.getRemoteAddress(), descriptor.getRemotePort(), true);
            SSLSession tmp_session = ((SSLSocket) target).getSession();
            Logger.d(TAG, "Remote Handshake : " + tmp_session + " is valid : " + tmp_session.isValid());
          } else {
            sslPinning.add(descriptor.getRemoteAddress());
            ssl_client.close();
            /*
            client.close();
            target.close();
            return;
            */
          }
        }
        MySocketForwarder.connect(client, target, vpnService);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}

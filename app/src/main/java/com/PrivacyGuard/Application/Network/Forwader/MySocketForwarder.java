/*
 * Modify the SocketForwarder of SandroproxyLib
 * Copyright (C) 2014  Yihang Song

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.PrivacyGuard.Application.Network.Forwader;


import com.PrivacyGuard.Application.Logger;
import com.PrivacyGuard.Application.MyVpnService;
import com.PrivacyGuard.Application.PrivacyGuard;
import com.PrivacyGuard.Plugin.IPlugin;
import com.PrivacyGuard.Plugin.LocationDetection;
import com.PrivacyGuard.Utilities.ByteArray;
import com.PrivacyGuard.Utilities.ByteArrayPool;

import org.sandrop.webscarab.model.ConnectionDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;


public class MySocketForwarder extends Thread {

    private static final String TIME_STAMP_FORMAT = "MM-dd HH:mm:ss.SSS";
    private static final String UNKNOWN = "unknown";
    private static String TAG = MySocketForwarder.class.getSimpleName();
    private static boolean EVALUATE = false;
    private static boolean DEBUG = false;
    private static boolean PROTECT = true;
    private static int LIMIT = 1368;
    private static ByteArrayPool byteArrayPool = new ByteArrayPool(10, LIMIT);
    private boolean outgoing = false;
    private ArrayList<IPlugin> plugins;
    private MyVpnService vpnService;
    private String appName = null;
    private String packageName = null;
    private Socket inSocket;
    private InputStream in;
    private OutputStream out;
    private String destIP;
    private SimpleDateFormat df = new SimpleDateFormat(TIME_STAMP_FORMAT, Locale.CANADA);
    private ConcurrentLinkedQueue<ByteArray> toFilter = new ConcurrentLinkedQueue<ByteArray>();
    private SocketChannel inChannel, outChannel;

    public MySocketForwarder(Socket inSocket, Socket outSocket, boolean isOutgoing, MyVpnService vpnService) {
        this.inSocket = inSocket;
        try {
            this.in = inSocket.getInputStream();
            this.out = outSocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.outgoing = isOutgoing;
        this.destIP = outSocket.getInetAddress().getHostAddress();
        if (outSocket.getPort() == 443) destIP += " (SSL)";
        this.vpnService = vpnService;
        this.plugins = vpnService.getNewPlugins();
        setDaemon(true);
    }

    public MySocketForwarder(SocketChannel in, SocketChannel out, boolean isOutgoing, MyVpnService vpnService) {
        this.inChannel = in;
        this.outChannel = out;
        this.outgoing = isOutgoing;
        this.destIP = out.socket().getInetAddress().getHostAddress();
        if (out.socket().getPort() == 443) destIP += " (SSL)";
        this.vpnService = vpnService;
        this.plugins = vpnService.getNewPlugins();
        setDaemon(true);
    }

    public static void connect(Socket clientSocket, Socket serverSocket, MyVpnService vpnService) throws Exception {
        if (clientSocket != null && serverSocket != null && clientSocket.isConnected() && serverSocket.isConnected()) {
            clientSocket.setSoTimeout(0);
            serverSocket.setSoTimeout(0);
            MySocketForwarder clientServer = new MySocketForwarder(clientSocket, serverSocket, true, vpnService);
            MySocketForwarder serverClient = new MySocketForwarder(serverSocket, clientSocket, false, vpnService);
            clientServer.start();
            serverClient.start();

            Logger.d(TAG, "Start forwarding");
            while (clientServer.isAlive())
                clientServer.join();
            while (serverClient.isAlive())
                serverClient.join();
            clientSocket.close();
            serverSocket.close();
            Logger.d(TAG, "Stop forwarding");
        } else {
            Logger.d(TAG, "skipping socket forwarding because of invalid sockets");
            if (clientSocket != null && clientSocket.isConnected()) {
                clientSocket.close();
            }
            if (serverSocket != null && serverSocket.isConnected()) {
                serverSocket.close();
            }
        }
    }

    public void run2() {
        ByteBuffer msg = ByteBuffer.allocate(LIMIT);
        int len, len2 = 0, total = 0;
        StringBuilder stringBuilder = new StringBuilder(LIMIT);
        while (true) {
            try {
                msg.clear();
                len = inChannel.read(msg);
                msg.flip();
                //if (len != msg.remaining()) Logger.d(TAG, "WTFWTF Read " + len + " : but " + msg.remaining());
                //Logger.d(TAG, "" + outChannel.socket().getLocalPort() + ":" + outChannel.socket().getPort() + " " + inChannel.socket().getLocalPort() + ":" + inChannel.socket().getPort() + " : " + total + " : " + len);
                total += len;
                //Logger.d(TAG, "" + outChannel.socket().getLocalPort() + ":" + outChannel.socket().getPort() + " " + inChannel.socket().getLocalPort() + ":" + inChannel.socket().getPort() + " : " + total + " : " + len);
                if (len < 0) return;
                // Build string from byte array
                //stringBuilder.insert(0, msg.array(), 0, len);
                //if(MainActivity.doFilter) filter(new String(msg.array(), 0, len));
                while (msg.hasRemaining()) {
                    len2 = outChannel.write(msg);
                    //Logger.d(TAG, "Remaining" + msg.hasRemaining());
                }
        /*
        if (len != len2) {
          Logger.d(TAG, "WTFWTFWTF" + len + " : " + len2 + " " + outgoing);
        }
        */
            } catch (SocketException e) {
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void run_select() {
        Selector selector = null;
        try {
            selector = Selector.open();
            inChannel.register(selector, SelectionKey.OP_READ);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        ByteBuffer msg = ByteBuffer.allocate(LIMIT);
        while (selector.isOpen()) {
            try {
                selector.select();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                if (!key.isValid()) continue;
                else if (key.isReadable()) {
                    try {
                        msg.clear();
                        int length = inChannel.read(msg);
                        Logger.d(TAG, "Read from channel " + length + " " + outgoing + " " + inChannel.socket().getPort());
                        if (length < 0) return;
                        msg.flip();
                        //filter(new String(msg.array(), 0, length));
                        while (msg.hasRemaining()) {
                            outChannel.write(msg);
                        }
                    } catch (SocketException e) {
                        Logger.d(TAG, "WAIT here");
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Logger.d(TAG, TAG + " End" + " : " + outgoing);
    }

    public void run() {
        FilterThread filterThread = new FilterThread();
        if (PrivacyGuard.doFilter && PrivacyGuard.asynchronous) filterThread.start();
        try {
            byte[] buff = new byte[LIMIT];
            int got;
            while ((got = in.read(buff)) > -1) {
                if (PrivacyGuard.doFilter) {
                    if (PrivacyGuard.asynchronous) {
                        if (!filterThread.isAlive()) filterThread.start();
                        toFilter.offer(byteArrayPool.getByteArray(buff, got));
                    } else {
                        if (filterThread.isAlive()) filterThread.interrupt();
                        filterThread.filter(new String(buff, 0, got));
                    }
                }
                out.write(buff, 0, got);
                out.flush();
            }
        } catch (Exception ignore) {
            ignore.printStackTrace();
            Logger.d(TAG, "outgoing : " + outgoing);
        }
        if (PrivacyGuard.asynchronous && filterThread.isAlive()) filterThread.interrupt();
    }

    public class FilterThread extends Thread {
        public void filter(String msg) {

            if (PrivacyGuard.doFilter) {
                if (EVALUATE) {
                    if (outgoing) {
                        if (appName == null) {
                            ConnectionDescriptor des = vpnService.getClientAppResolver().getClientDescriptorBySocket(inSocket);
                            if (des != null) appName = des.getNamespace();
                            Logger.logTraffic(TAG, appName, "IP : " + destIP + "\nRequest : " + msg, ((LocationDetection) plugins.get(0)).getLocations());
                        }
                    }
                } else {
                    //Log.d("TAG", msg);
                    for (IPlugin plugin : plugins) {
                        String ret = outgoing ? plugin.handleRequest(msg) : plugin.handleResponse(msg);
                        if (outgoing) {
                            if (ret != null) {
                                if (appName == null) {
                                    ConnectionDescriptor des = vpnService.getClientAppResolver().getClientDescriptorBySocket(inSocket);
                                    if (des != null) {
                                        appName = des.getName();
                                        packageName = des.getNamespace();
                                    } else {
                                        appName = UNKNOWN;
                                        packageName = UNKNOWN;
                                    }
                                }
                                vpnService.notify(appName, ret);
                            }
                            if (ret == null) ret = UNKNOWN;
                            Logger.logTraffic(TAG, appName + " " + packageName, "IP : " + destIP + "\nRequest : " + msg + "\nType : " + ret);
                        }
                    }
                }
            }
        }

        public void run() {
            ByteArray temp;
            while (true) {
                while ((temp = toFilter.poll()) == null) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                //Log.d("toFilter", "" + toFilter.size());
                String msg = new String(temp.data(), 0, temp.length());
                byteArrayPool.release(temp);
                filter(msg);
            }
        }
    }
}

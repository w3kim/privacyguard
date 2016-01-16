package com.PrivacyGuard.Network;

import com.PrivacyGuard.Forwader.AbsForwarder;
import com.PrivacyGuard.Network.IP.IPDatagram;
import com.PrivacyGuard.Network.IP.IPHeader;

import java.net.InetAddress;

/**
 * Created by y59song on 16/05/14.
 */
public class ConnectionInfo {
  protected InetAddress clientAddress, serverAddress;
  protected int clientPort, serverPort;
  protected int protocol;

  protected IPHeader responseIPHeader;
  protected TransportHeader responseTransHeader;

  public ConnectionInfo(IPDatagram ipDatagram) {
    reset(ipDatagram);
  }

  public IPHeader getIPHeader() {
    return responseIPHeader;
  }

  public TransportHeader getTransHeader() {
    return responseTransHeader;
  }

  public void reset(IPDatagram ipDatagram) {
    this.clientAddress = ipDatagram.header().getSrcAddress();
    this.serverAddress = ipDatagram.header().getDstAddress();
    this.clientPort = ipDatagram.payLoad().getSrcPort();
    this.serverPort = ipDatagram.payLoad().getDstPort();
    this.protocol = ipDatagram.header().protocol();
    this.responseIPHeader = ipDatagram.header().reverse();
    this.responseTransHeader = ipDatagram.payLoad().header().reverse();
  }

  public void setup(AbsForwarder forwarder) {
    forwarder.setup(clientAddress, clientPort, serverAddress, serverPort);
  }
}

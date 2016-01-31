package com.PrivacyGuard.Application.Network.IP;

import com.PrivacyGuard.Application.Network.TCP.TCPDatagram;
import com.PrivacyGuard.Application.Network.UDP.UDPDatagram;
import com.PrivacyGuard.Utilities.ByteOperations;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by frank on 2014-03-26.
 */

public class IPDatagram {
  public final static String TAG = "IPDatagram";
  IPHeader header;
  IPPayLoad data;

  public static final int TCP = 6, UDP = 17;
  public static IPDatagram create(ByteBuffer packet) {
    IPHeader header = IPHeader.create(packet.array());
    IPPayLoad payLoad;
    if(header.protocol() == TCP) {
      payLoad = TCPDatagram.create(Arrays.copyOfRange(packet.array(), header.headerLength(), packet.limit()));
    } else if(header.protocol() == UDP) {
      payLoad = UDPDatagram.create(Arrays.copyOfRange(packet.array(), header.headerLength(), packet.limit()));
    }
    else return null;
    return new IPDatagram(header, payLoad);
  }

  public IPDatagram(IPHeader header, IPPayLoad data) {
    this.header = header;
    this.data = data;
    int totalLength = header.headerLength() + data.length();
    if(this.header.length() != totalLength) {
      this.header.setLength(totalLength);
      this.header.setCheckSum(new byte[] {0, 0});
      byte[] toComputeCheckSum = this.header.toByteArray();
      this.header.setCheckSum(ByteOperations.computeCheckSum(toComputeCheckSum));
    }
  }

  public IPHeader header() {
    return header;
  }

  public IPPayLoad payLoad() {
    return data;
  }

  public byte[] toByteArray() {
    return ByteOperations.concatenate(header.toByteArray(), data.toByteArray());
  }
}

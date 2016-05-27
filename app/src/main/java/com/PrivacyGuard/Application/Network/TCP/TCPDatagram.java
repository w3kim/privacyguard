package com.PrivacyGuard.Application.Network.TCP;

import com.PrivacyGuard.Application.Logger;
import com.PrivacyGuard.Application.Network.IP.IPPayLoad;

import java.net.InetAddress;
import java.util.Arrays;

/**
 * Created by frank on 2014-03-26.
 */
public class TCPDatagram extends IPPayLoad {
    private static final String TAG = "TCPDatagram";
    private static final boolean DEBUG = false;

    public TCPDatagram(TCPHeader header, byte[] data, InetAddress dst) {
        this.header = header;
        this.data = data;
        debugInfo(dst);
    }

    public TCPDatagram(TCPHeader header, byte[] data, int start, int end, InetAddress dst) {
        this.header = header;
        this.data = Arrays.copyOfRange(data, start, end);
        debugInfo(dst);
    }

    public static TCPDatagram create(byte[] data, InetAddress dst) {
        TCPHeader header = new TCPHeader(data);
        return new TCPDatagram(header, Arrays.copyOfRange(data, header.offset(), data.length), dst);
    }

    public static TCPDatagram create(byte[] data, int offset, int len, InetAddress dst) {
        TCPHeader header = new TCPHeader(data, offset);
        return new TCPDatagram(header, Arrays.copyOfRange(data, header.offset() + offset, len), dst);
    }

    public void debugInfo(InetAddress dstAddress) {
        //if(header.getDstPort() == 80 || header.getSrcPort() == 80)
        byte flag = ((TCPHeader)header).getFlag();
        StringBuffer flags = new StringBuffer();
        if ((flag & TCPHeader.SYN) != 0) flags.append("SYN|");
        if ((flag & TCPHeader.FIN) != 0) flags.append("FIN|");
        if ((flag & TCPHeader.ACK) != 0) flags.append("ACK|");
        if ((flag & TCPHeader.PSH) != 0) flags.append("PSH|");
        if ((flag & TCPHeader.RST) != 0) flags.append("RST|");
        Logger.d(TAG, "Flags=" + flags.toString()  +
                " DstAddr= " + dstAddress.getHostName() +
                " SrcPort=" + header.getSrcPort() + " DstPort=" + header.getDstPort() + " Seq=" + Long.toString(((TCPHeader) header).getSeq_num() & 0xFFFFFFFFL) +
                " Ack=" + Long.toString(((TCPHeader) header).getAck_num() & 0xFFFFFFFFL) +
                " Data Length=" + dataLength());
    }

    @Override
    public int virtualLength() {
        byte flag = ((TCPHeader)header).getFlag();
        if((flag & (TCPHeader.SYN | TCPHeader.FIN)) != 0) return 1;
        else return this.dataLength();
    }
}

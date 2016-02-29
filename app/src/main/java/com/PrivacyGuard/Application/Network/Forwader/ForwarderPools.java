/*
 * Pool for all forwarders
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

import android.util.Pair;

import com.PrivacyGuard.Application.MyVpnService;
import com.PrivacyGuard.Application.Network.IP.IPDatagram;

import java.util.Collections;
import java.util.HashMap;

/**
 * Created by frank on 2014-04-01.
 */
public class ForwarderPools {
    private HashMap<Pair<Integer, Byte>, AbsForwarder> portToForwarder;
    private MyVpnService vpnService;

    public ForwarderPools(MyVpnService vpnService) {
        this.vpnService = vpnService;
        portToForwarder = new HashMap<>();
    }

    public AbsForwarder get(int port, byte protocol) {
        Pair<Integer, Byte> key = new Pair<>(port, protocol);
        if (portToForwarder.containsKey(key) && !portToForwarder.get(key).isClosed()) {
            return portToForwarder.get(key);
        } else {
            AbsForwarder temp = getByProtocol(protocol);
            if (temp != null) {
                temp.open();
                portToForwarder.put(key, temp);
            }
            return temp;
        }
    }

    private AbsForwarder getByProtocol(byte protocol) {
        switch (protocol) {
            case IPDatagram.TCP:
                return new TCPForwarder(vpnService);
            case IPDatagram.UDP:
                return new UDPForwarder(vpnService);
            default:
                return null;
        }
    }

    public void release(UDPForwarder udpForwarder) {
        portToForwarder.values().removeAll(Collections.singleton(udpForwarder));
    }

    public void release(TCPForwarder tcpForwarder) {
        portToForwarder.values().removeAll(Collections.singleton(tcpForwarder));
    }
}

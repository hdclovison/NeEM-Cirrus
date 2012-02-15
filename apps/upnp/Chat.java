/*
 * NeEM - Network-friendly Epidemic Multicast
 * Copyright (c) 2005-2007, University of Minho
 * All rights reserved.
 *
 * Contributors:
 *  - Pedro Santos <psantos@gmail.com>
 *  - Jose Orlando Pereira <jop@di.uminho.pt>
 * 
 * Partially funded by FCT, project P-SON (POSC/EIA/60941/2004).
 * See http://pson.lsd.di.uminho.pt/ for more information.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *  - Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 * 
 *  - Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 * 
 *  - Neither the name of the University of Minho nor the names of its
 *  contributors may be used to endorse or promote products derived from
 *  this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package apps.upnp;

import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import net.sbbi.upnp.impls.InternetGatewayDevice;
import net.sf.neem.MulticastChannel;
import net.sf.neem.ProtocolMBean;
import apps.Addresses;


/**
 * Simple chat application.
 */
public class Chat {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: apps.upnp.Chat local public peer1 ... peerN");
            System.exit(1);
        }

        try {
        	boolean mapped=false;
        	
        	InetSocketAddress local=Addresses.parse(args[0], true);
        	InetSocketAddress publc=Addresses.parse(args[0], true);
        	
        	System.err.println("Searching IGD...");
        	InternetGatewayDevice[] igds = InternetGatewayDevice.getDevices(5000);
            if (igds!=null) {
            	publc=new InetSocketAddress(igds[0].getExternalIPAddress(), publc.getPort());
            	System.err.println("Addding mapping in "+igds[0].getIGDRootDevice().getModelName()+" from "+publc+" to "+local);
            	mapped=igds[0].addPortMapping("NeEM sample apps.upnp.Chat", 
                        null, local.getPort(), publc.getPort(),
                        local.getAddress().getHostAddress(), 0, "TCP");
            } else {
            	System.err.println("Not found.");
            }
            
            if (!mapped) {
            	System.err.println("Not mapped. Continuing with public address set to "+local);
            	publc=local;
            }
            
            MulticastChannel neem = new MulticastChannel(local, publc, null);

            System.out.println("Started: " + neem.getLocalSocketAddress());
			            
            // Export JMX management bean when run with:
            //  java -Dcom.sun.management.jmxremote apps.Chat ...
            // or remote with:
            //  java -Dcom.sun.management.jmxremote.port=<some-port> \
            // 	 -Dcom.sun.management.jmxremote.authenticate=false \
            //   -Dcom.sun.management.jmxremote.ssl=false apps.Chat ...
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ProtocolMBean mbean = neem.getProtocolMBean();
            ObjectName name = new ObjectName("net.sf.neem:type=Protocol,id="+mbean.getLocalId());
            mbs.registerMBean(mbean, name);
            
            apps.Chat chat = new apps.Chat(neem);

            for (int i = 2; i < args.length; i++) 
            	neem.connect(Addresses.parse(args[i], false));

            chat.inputLoop();
            
            neem.close();
            
            if (mapped) {
            	System.err.println("Removing mapping in "+igds[0].getIGDRootDevice().getModelName());
            	igds[0].deletePortMapping(null, publc.getPort(), "TCP");
            	System.err.println("Unmapped "+publc);
            }

        } catch (Exception e) {
        	e.printStackTrace();
        }
    }
}


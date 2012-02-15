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

package apps.perf;

import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import apps.Addresses;

import net.sf.neem.MulticastChannel;
import net.sf.neem.ProtocolMBean;

public class Source {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: apps.perf.Source local iarrival size peer1 ... peerN");
            System.exit(1);
        }
        
        try {
            MulticastChannel neem = new MulticastChannel(Addresses.parse(args[0], true));
            Thread.sleep(1000);
            for (int i = 3; i < args.length; i++)
                neem.connect(Addresses.parse(args[i], false));

            int iarrival=Integer.parseInt(args[1]);
            int size=Integer.parseInt(args[2]);
            
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ProtocolMBean mbean = neem.getProtocolMBean();
            ObjectName name = new ObjectName("net.sf.neem:type=Protocol,id="+mbean.getLocalId());
            mbs.registerMBean(mbean, name);

            int seq=0;
            
            String id=mbean.getLocalId().toString();
            ByteBuffer bb = ByteBuffer.allocate(size);
            byte[] buf="junk".getBytes();
            while(bb.remaining()>buf.length)
            	bb.put(buf);
            bb.put(buf,0,bb.remaining());
            bb.rewind();
            
            Thread.sleep(1000);
            
            while (true) {
            	String msg=id+" "+(seq++)+" "+(System.nanoTime()/1000)+" ";
                buf = msg.getBytes();
                bb.put(buf);
                bb.rewind();
                neem.write(bb);
                bb.clear();
                System.out.println(msg);
                Thread.sleep(iarrival);
            }
        } catch (Exception e) {
        	e.printStackTrace();
        }
    }
}


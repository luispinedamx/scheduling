/*
 * ################################################################
 *
 * ProActive: The Java(TM) library for Parallel, Distributed,
 *            Concurrent computing with Security and Mobility
 *
 * Copyright (C) 1997-2009 INRIA/University of
 * 						   Nice-Sophia Antipolis/ActiveEon
 * Contact: proactive@ow2.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 3 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 * If needed, contact us to obtain a release under GPL Version 2.
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s):
 *
 * ################################################################
 * $$PROACTIVE_INITIAL_DEV$$
 */
package org.ow2.proactive.resourcemanager.frontend.topology;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;

import org.objectweb.proactive.core.node.NodeException;
import org.objectweb.proactive.core.node.NodeFactory;


public class NodePinger {

    public HashMap<InetAddress, Long> ping(List<InetAddress> hosts) {
        HashMap<InetAddress, Long> results = new HashMap<InetAddress, Long>();
        for (InetAddress host : hosts) {
            try {
                if (NodeFactory.getDefaultNode().getVMInformation().getInetAddress().equals(host)) {
                    // nodes on the same host
                    results.put(host, new Long(0));
                } else {
                    results.put(host, pingNode(host));
                }
            } catch (NodeException e) {
            }
        }
        return results;
    }

    private Long pingNode(InetAddress host) {

        final int ATTEMPS = 10;
        long minPing = Long.MAX_VALUE;
        for (int i = 0; i < ATTEMPS; i++) {
            long start = System.nanoTime();
            try {
                host.isReachable(60 * 1000);
            } catch (IOException e) {
                // cannot reach the node
                return (long) -1;
            }

            // microseconds
            long ping = (System.nanoTime() - start) / 1000;
            if (ping < minPing)
                minPing = ping;
        }

        return minPing;
    }
    /*
    private Long pingNode(InetAddress host) {

        final int ATTEMPS = 10;
        long minPing = Long.MAX_VALUE;
        for (int i = 0; i < ATTEMPS; i++) {
            long start = System.nanoTime();
            host.isReachable(60 * 1000);
            try {
                node.getNumberOfActiveObjects();
            } catch (Exception e) {
                // cannot reach the node
                return (long) -1;
            }

            // microseconds
            long ping = (System.nanoTime() - start) / 1000;
            if (ping < minPing)
                minPing = ping;
        }

        return minPing;
    }*/
}

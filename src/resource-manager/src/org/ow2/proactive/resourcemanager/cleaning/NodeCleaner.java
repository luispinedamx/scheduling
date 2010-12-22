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
package org.ow2.proactive.resourcemanager.cleaning;

import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.objectweb.proactive.core.util.log.ProActiveLogger;
import org.ow2.proactive.resourcemanager.rmnode.RMNode;
import org.ow2.proactive.resourcemanager.utils.RMLoggers;


/**
 * Cleans a single node by killing all active objects on the node.
 *
 */
public class NodeCleaner implements Callable<Boolean> {

    private final static Logger logger = ProActiveLogger.getLogger(RMLoggers.CLEANER);
    private RMNode rmnode;

    /**
     * Construct the new instance of the class
     *
     * @param rmnode node to be cleaned
     */
    public NodeCleaner(RMNode rmnode) {
        this.rmnode = rmnode;
    }

    /**
     * Performs the cleaning (for now just killing of all active objects).
     */
    public Boolean call() throws Exception {
        // killing all active objects on the node
        try {
            rmnode.clean();
        } catch (Throwable t) {
            logger.warn("Exception while cleaning the node " + rmnode.getNodeURL() + ": " + t.getMessage());
            logger.warn("Checking if the node " + rmnode.getNodeURL() + " is alive");
            rmnode.getNodeSource().pingNode(rmnode.getNode());
            return false;
        }

        return true;
    }
}
/*
 * ################################################################
 *
 * ProActive: The Java(TM) library for Parallel, Distributed,
 *            Concurrent computing with Security and Mobility
 *
 * Copyright (C) 1997-2007 INRIA/University of Nice-Sophia Antipolis
 * Contact: proactive@objectweb.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version
 * 2 of the License, or any later version.
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
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s):
 *
 * ################################################################
 */
package org.objectweb.proactive.extra.gcmdeployment.GCMApplication;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.Notification;
import javax.management.NotificationListener;

import org.objectweb.proactive.core.jmx.notification.GCMRuntimeRegistrationNotificationData;
import org.objectweb.proactive.core.jmx.notification.NotificationType;
import org.objectweb.proactive.core.jmx.util.JMXNotificationManager;
import org.objectweb.proactive.core.node.Node;
import org.objectweb.proactive.core.runtime.ProActiveRuntimeImpl;
import org.objectweb.proactive.extra.gcmdeployment.GCMDeployment.GCMDeploymentDescriptor;
import static org.objectweb.proactive.extra.gcmdeployment.GCMDeploymentLoggers.GCM_NODEALLOC_LOGGER;
import org.objectweb.proactive.extra.gcmdeployment.core.VirtualNodeInternal;


public class NodeAllocator implements NotificationListener {

    /** The GCM Application Descriptor associated to this Node Allocator*/
    private GCMApplicationDescriptorImpl gcma;

    /** All Virtual Nodes*/
    private List<VirtualNodeInternal> virtualNodes;

    /** Nodes waiting in Stage 2 */
    private Map<Node, GCMDeploymentDescriptor> stage2Pool;

    /** Nodes waiting in Stage 3 */
    private Map<Node, GCMDeploymentDescriptor> stage3Pool;

    /** A Semaphore to activate stage 2/3 node dispatching on node arrival */
    private Object semaphore;

    public NodeAllocator(GCMApplicationDescriptorImpl gcma,
        Collection<VirtualNodeInternal> virtualNodes) {
        this.gcma = gcma;

        this.virtualNodes = new LinkedList<VirtualNodeInternal>();
        this.virtualNodes.addAll(virtualNodes);

        this.semaphore = new Object();

        this.stage2Pool = new ConcurrentHashMap<Node, GCMDeploymentDescriptor>();
        this.stage3Pool = new ConcurrentHashMap<Node, GCMDeploymentDescriptor>();
        subscribeJMXRuntimeEvent();
        startStage23Thread();
    }

    public void subscribeJMXRuntimeEvent() {
        JMXNotificationManager.getInstance()
                              .subscribe(ProActiveRuntimeImpl.getProActiveRuntime()
                                                             .getMBean()
                                                             .getObjectName(),
            this);
    }

    synchronized public void handleNotification(Notification notification,
        Object handback) {
        try {
            String type = notification.getType();

            if (NotificationType.GCMRuntimeRegistered.equals(type)) {
                GCMRuntimeRegistrationNotificationData data = (GCMRuntimeRegistrationNotificationData) notification.getUserData();
                GCMDeploymentDescriptor nodeProvider = gcma.getGCMDeploymentDescriptorId(data.getDeploymentId());

                for (Node node : data.getNodes()) {
                    if (!dispatchS1(node, nodeProvider)) {
                        stage2Pool.put(node, nodeProvider);
                    }
                }

                synchronized (semaphore) {
                    semaphore.notify();
                }
            }
        } catch (Exception e) {
            // If not handled by us, JMX eats the Exception !
            GCM_NODEALLOC_LOGGER.warn(e);
        }
    }

    /**
     * Try to give the node to a Virtual Node to fulfill a NodeProviderContract
     *
     * @param node The node who registered to the local runtime
     * @param nodeProvider The {@link GCMDeploymentDescriptor} who created the node
     * @return returns true if a VirtualNode took the Node, false otherwise
     */
    private boolean dispatchS1(Node node, GCMDeploymentDescriptor nodeProvider) {
        GCM_NODEALLOC_LOGGER.trace("Stage1: " +
            node.getNodeInformation().getURL() + " from " +
            nodeProvider.getDescriptorFilePath());

        for (VirtualNodeInternal virtualNode : virtualNodes) {
            if (virtualNode.doesNodeProviderNeed(node, nodeProvider)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Offers node to each VirtualNode to fulfill VirtualNode Capacity requirement.
     *
     * @param node The node who registered to the local runtime
     * @param nodeProvider The {@link GCMDeploymentDescriptor} who created the node
     * @return returns true if a VirtualNode took the Node, false otherwise
     */
    private boolean dispatchS2(Node node, GCMDeploymentDescriptor nodeProvider) {
        GCM_NODEALLOC_LOGGER.trace("Stage2: " +
            node.getNodeInformation().getURL() + " from " +
            nodeProvider.getDescriptorFilePath());

        // Check this Node can be dispatched 
        for (VirtualNodeInternal virtualNode : virtualNodes) {
            if (virtualNode.hasContractWith(nodeProvider) &&
                    virtualNode.hasUnsatisfiedContract()) {
                return false;
            }
        }

        for (VirtualNodeInternal virtualNode : virtualNodes) {
            if (virtualNode.doYouNeed(node, nodeProvider)) {
                stage2Pool.remove(node);
                return true;
            }
        }

        stage2Pool.remove(node);
        stage3Pool.put(node, nodeProvider);
        return false;
    }

    /**
     *
     * @param node The node who registered to the local runtime
     * @param nodeProvider The {@link GCMDeploymentDescriptor} who created the node
     * @return
     */
    private boolean dispatchS3(Node node, GCMDeploymentDescriptor nodeProvider) {
        GCM_NODEALLOC_LOGGER.trace("Stage3: " +
            node.getNodeInformation().getURL() + " from " +
            nodeProvider.getDescriptorFilePath());

        for (VirtualNodeInternal virtualNode : virtualNodes) {
            if (virtualNode.doYouWant(node, nodeProvider)) {
                stage3Pool.remove(node);
                virtualNodes.add(virtualNodes.remove(0));
                return true;
            }
        }

        return false;
    }

    private void startStage23Thread() {
        Thread t = new Stage23Dispatcher();
        t.setDaemon(true);
        t.start();
    }

    private class Stage23Dispatcher extends Thread {
        @Override
        public void run() {
            while (true) {
                // Wait for next handleNotification invocation
                try {
                    synchronized (semaphore) {
                        semaphore.wait();
                    }
                } catch (InterruptedException e) {
                    GCM_NODEALLOC_LOGGER.info(e);
                }

                for (Node node : stage2Pool.keySet())
                    dispatchS2(node, stage2Pool.get(node));

                for (Node node : stage3Pool.keySet())
                    dispatchS3(node, stage3Pool.get(node));
            }
        }
    }
}

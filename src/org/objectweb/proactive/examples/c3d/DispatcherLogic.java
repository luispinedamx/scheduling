/*
 * ################################################################
 *
 * ProActive: The Java(TM) library for Parallel, Distributed,
 *            Concurrent computing with Security and Mobility
 *
 * Copyright (C) 1997-2002 INRIA/University of Nice-Sophia Antipolis
 * Contact: proactive-support@inria.fr
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://www.inria.fr/oasis/ProActive/contacts.html
 *  Contributor(s):
 *
 * ################################################################
 */
package org.objectweb.proactive.examples.c3d;

import org.objectweb.proactive.Body;


/**
 * This interface describes methods which are accessible by classes
 * internal to the Dispatcher functionality, for example DispatcherGUI
 */
public interface DispatcherLogic {

    /** Sends a [log] message to given user */
    public void userLog(int i_user, String s_message);

    /** Ask users & dispatcher to log s_message, except one  */
    public void allLogExcept(int i_user, String s_message);

    /** Ask all users & dispatcher to log s_message */
    public void allLog(String s_message);

    /** Shut down everything, send warning messages to users */
    public void exit();

    /** See how well the simulation improves with more renderers */
    public void doBenchmarks();

    /** Makes the engine participate in the computation of images */
    public void turnOnEngine(String engineName);

    /** Stops the engine from participating in the computation of images*/
    public void turnOffEngine(String engineName);

    /** Tells what are the operations to perform before starting the activity of the AO.
     * Here, we state that if migration asked, procedure  is : leaveHost, migrate */
    public void initActivity(Body body);

    /** ProActive queue handling */
    public void runActivity(Body body);
}

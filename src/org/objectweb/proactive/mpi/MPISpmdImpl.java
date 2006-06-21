/*
 * ################################################################
 *
 * ProActive: The Java(TM) library for Parallel, Distributed,
 *            Concurrent computing with Security and Mobility
 *
 * Copyright (C) 1997-2005 INRIA/University of Nice-Sophia Antipolis
 * Contact: proactive@objectweb.org
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
package org.objectweb.proactive.mpi;

import org.apache.log4j.Logger;

import org.objectweb.proactive.core.descriptor.data.VirtualNode;
import org.objectweb.proactive.core.node.NodeException;
import org.objectweb.proactive.core.process.AbstractExternalProcess;
import org.objectweb.proactive.core.process.AbstractExternalProcessDecorator;
import org.objectweb.proactive.core.process.ExternalProcess;
import org.objectweb.proactive.core.process.mpi.MPIProcess;
import org.objectweb.proactive.core.util.log.Loggers;
import org.objectweb.proactive.core.util.log.ProActiveLogger;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Hashtable;


public class MPISpmdImpl implements MPISpmd, java.io.Serializable {
    private final static Logger MPI_IMPL_LOGGER = ProActiveLogger.getLogger(Loggers.MPI);

    /**  name of the MPISpmd object */
    private String name;

    /**  MPI Process that the Virtual Node references */
    private ExternalProcess mpiProcess = null;

    /**  Virtual Node containing resources */
    private VirtualNode vn;

    /** user SPMD classes name */
    private ArrayList spmdClasses = null;

    /** user SPMD classes params */
    private Hashtable spmdClassesParams;

    /** user classes name */
    private ArrayList classes = null;

    /** user classes params */
    private Hashtable classesParams;
    private Object[] classesParamsByRank;

    // empty no-args constructor 
    public MPISpmdImpl() {
    }

    /**
     * API method for creating a new MPISPMD object from an existing Virtual Node
     * @throws NodeException
     */
    public MPISpmdImpl(VirtualNode vn) throws RuntimeException, NodeException {
        MPI_IMPL_LOGGER.debug(
            "[MPISpmd object] creating MPI SPMD active object: " +
            vn.getName());
        //  active
        if (!(vn.isActivated())) {
            vn.activate();
        }
        if (vn.hasMPIProcess()) {
            this.spmdClasses = new ArrayList();
            this.classes = new ArrayList();
            this.spmdClassesParams = new Hashtable();
            this.classesParams = new Hashtable();
            this.classesParamsByRank = new Object[vn.getNodes().length];
            this.mpiProcess = vn.getMPIProcess();
            this.name = vn.getName();
            this.vn = vn;
        } else {
            throw new RuntimeException(
                "!!! ERROR: Cannot create MPISpmd object Cause: No MPI process attached with the virtual node " +
                vn.getName());
        }
    }

    /**
     * API method for starting the MPI program
     * @return MPIResult
     */
    public MPIResult startMPI() {
        MPI_IMPL_LOGGER.debug("[MPISpmd Object] Start MPI Process ");
        MPIResult result = new MPIResult();
        try {
            mpiProcess.startProcess();
            mpiProcess.waitFor();
            result.setReturnValue(mpiProcess.exitValue());
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            MPI_IMPL_LOGGER.error(
                "!!! ERROR startMPI: cannot start MPI process " + this.name +
                " with command " + mpiProcess.getCommand());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * API method for reStarting the MPI program - run a new computation independently
     * if the first one is currently running
     * @return MPIResult
     */
    public MPIResult reStartMPI() {
        MPI_IMPL_LOGGER.debug("[MPISpmd Object] reStart MPI Process ");
        reinitProcess();
        return this.startMPI();
    }

    /**
     * API method for killing MPI program -
     * Kills the MPI program. The MPI program represented by this MPISpmd object is forcibly terminated.
     * Only the computation is killed, the MPISpmd object is still alive (the computation can be reStarted).
     */
    public boolean killMPI() {
        MPI_IMPL_LOGGER.debug("[MPISpmd Object] Kill MPI Process ");
        // as killMPI is an immediate service method it's possible that
        // stop method is called before start method on process thus interrupted exceptionis launched. 
        try {
            mpiProcess.stopProcess();

            //the sleep might be needed for processes killed
            Thread.sleep(200);
            return true;
        } catch (IllegalStateException e) {
            MPI_IMPL_LOGGER.error(
                "Exception caught, waiting process to start to kill it.");
            while (!mpiProcess.isStarted()) {
            }
            mpiProcess.stopProcess();
        } catch (InterruptedException e) {
            e.printStackTrace();
            MPI_IMPL_LOGGER.error("!!! ERROR killMPI: cannot kill MPI process " +
                this.name);
        }
        return false;
    }

    /**
     * API method for setting MPI program command arguments
     * @param arguments - the arguments of the MPI program
     */
    public void setMPICommandArguments(String arguments) {
        MPI_IMPL_LOGGER.debug(((AbstractExternalProcess) this.mpiProcess).getCommand());
        // check for the position of the MPIProcess in the mpiProcess
        int rank = getMPIProcessRank(this.mpiProcess);
        ExternalProcess tempProc = this.mpiProcess;
        while (rank != 0) {
            tempProc = ((AbstractExternalProcessDecorator) tempProc).getTargetProcess();
            rank--;
        }
        ((MPIProcess) tempProc).setMpiCommandOptions(arguments);
        MPI_IMPL_LOGGER.debug(((AbstractExternalProcess) this.mpiProcess).getCommand());
    }

    public String getName() {
        return this.name;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("\n Class: ");
        sb.append(this.getClass().getName());
        sb.append("\n Name: ");
        sb.append(this.name);
        sb.append("\n Command: ");
        sb.append(getMPIProcess(this.mpiProcess).getCommand());
        sb.append("\n Processes number: ");
        sb.append(getMPIProcess(this.mpiProcess).getHostsNumber());
        return sb.toString();
    }

    public void reinitProcess() {
        mpiProcess.setStarted(false);
        mpiProcess.setFinished(false);
    }

    public String getStatus() {
        return null;
    }

    //  returns MPI process 
    private MPIProcess getMPIProcess(ExternalProcess process) {
        while (!(process instanceof MPIProcess)) {
            process = ((ExternalProcess) ((AbstractExternalProcessDecorator) process).getTargetProcess());
        }
        return (MPIProcess) process;
    }

    // returns the rank of MPI process in the processes hierarchie
    private int getMPIProcessRank(ExternalProcess process) {
        int res = 0;
        while (!(process instanceof MPIProcess)) {
            res++;
            process = ((ExternalProcess) ((AbstractExternalProcessDecorator) process).getTargetProcess());
        }
        return res;
    }

    public boolean isFinished() {
        return mpiProcess.isFinished();
    }

    public VirtualNode getVn() {
        return vn;
    }

    //  ----+----+----+----+----+----+----+----+----+----+----+-------+----+----
    //  --+----+---- methods for the future wrapping with control ----+----+----
    //  ----+----+----+----+----+----+----+----+----+----+----+-------+----+----
    public void newActiveSpmd(String cl) {
        if (spmdClasses.contains(cl) || classes.contains(cl)) {
            MPI_IMPL_LOGGER.info("!!! ERROR newActiveSpmd: " + cl +
                " class has already been added to the list of user classes to instanciate ");
        } else {
            this.spmdClasses.add(cl);
            ArrayList parameters = new ArrayList(2);
            parameters.add(0, null);
            parameters.add(1, null);
            this.spmdClassesParams.put(cl, parameters);
        }
    }

    public void newActiveSpmd(String cl, Object[] params) {
        if (spmdClasses.contains(cl) || classes.contains(cl)) {
            MPI_IMPL_LOGGER.info("!!! ERROR newActiveSpmd: " + cl +
                " class has already been added to the list of user classes to instanciate ");
        } else {
            this.spmdClasses.add(cl);

            ArrayList parameters = new ArrayList(2);

            // index=0 => Object[] type
            // index=1 => Object[][] type
            parameters.add(0, params);
            parameters.add(1, null);
            this.spmdClassesParams.put(cl, parameters);
        }
    }

    public void newActiveSpmd(String cl, Object[][] params) {
        if (spmdClasses.contains(cl) || classes.contains(cl)) {
            MPI_IMPL_LOGGER.info("!!! ERROR newActiveSpmd: " + cl +
                " class has already been added to the list of user classes to instanciate ");
        } else {
            this.spmdClasses.add(cl);
            ArrayList parameters = new ArrayList(2);

            // index=0 => Object[] type
            // index=1 => Object[][] type
            parameters.add(0, null);
            parameters.add(1, params);
            this.spmdClassesParams.put(cl, parameters);
        }
    }

    public void newActive(String cl, Object[] params, int rank)
        throws ArrayIndexOutOfBoundsException {
        if (spmdClasses.contains(cl) ||
                (classes.contains(cl) &&
                (rank < this.classesParamsByRank.length) &&
                (this.classesParamsByRank[rank] != null))) {
            MPI_IMPL_LOGGER.info("!!! ERROR newActive: " + cl +
                " class has already been added to the list of user classes to instanciate ");
        } else if (rank < this.classesParamsByRank.length) {
            if (!classes.contains(cl)) {
                this.classes.add(cl);
            }
            this.classesParamsByRank[rank] = params;
            this.classesParams.put(cl, this.classesParamsByRank);
        } else {
            throw new ArrayIndexOutOfBoundsException("Rank " + rank +
                " is out of range while trying to instanciate class " + cl);
        }
    }

    public ArrayList getSpmdClasses() {
        return this.spmdClasses;
    }

    public Hashtable getSpmdClassesParams() {
        return this.spmdClassesParams;
    }

    public ArrayList getClasses() {
        return this.classes;
    }

    public Hashtable getClassesParams() {
        return this.classesParams;
    }
}

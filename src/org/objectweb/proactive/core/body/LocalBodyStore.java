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
package org.objectweb.proactive.core.body;

import org.objectweb.proactive.Body;
import org.objectweb.proactive.core.UniqueID;
import org.objectweb.proactive.core.event.BodyEventListener;
import org.objectweb.proactive.core.event.BodyEventProducerImpl;


/**
 * <i><font size="-1" color="#FF0000">**For internal use only** </font></i><br>
 * <p>
 * This class store all active bodies known in the current JVM. The class is a singleton
 * in a given JVM. It also associates each active thread with its matching body.
 * </p>
 *
 * @author  ProActive Team
 * @version 1.0,  2001/10/23
 * @since   ProActive 0.9
 * @see Body
 * @see UniqueID
 *
 */
public class LocalBodyStore {

  //
  // -- STATIC MEMBERS -----------------------------------------------
  //

  private static LocalBodyStore instance = new LocalBodyStore();
  
  //
  // -- PRIVATE MEMBERS -----------------------------------------------
  //

  /**
   * This table maps all known active Bodies in this JVM with their UniqueID
   * From one UniqueID it is possible to get the corresponding body if
   * it belongs to this JVM
   */
  private BodyMap localBodyMap = new BodyMap();

  /**
   * Static object that manages the registration of listeners and the sending of
   * events
   */
  private BodyEventProducerImpl bodyEventProducer = new BodyEventProducerImpl();

  private ThreadLocal bodyPerThread = new ThreadLocal();
  
  private MetaObjectFactory halfBodyMetaObjectFactory = ProActiveMetaObjectFactory.newInstance();

  //
  // -- CONSTRUCTORS -----------------------------------------------
  //

  /**
   * Creates a new AbstractBody.
   * Used for serialization.
   */
  private LocalBodyStore() {}

  //
  // -- STATIC METHODS -----------------------------------------------
  //

  public static LocalBodyStore getInstance() {
    return instance;
  }
  
  
  //
  // -- PUBLIC METHODS -----------------------------------------------
  //

  public MetaObjectFactory getHalfBodyMetaObjectFactory() {
    return halfBodyMetaObjectFactory;
  }
  
  
  public void setHalfBodyMetaObjectFactory(MetaObjectFactory factory) {
    halfBodyMetaObjectFactory = factory;
  }
   

  /**
   * Returns the body associated with the thread calling the method. If no body is associated with the
   * calling thread, an HalfBody is created to manage the futures.
   * @return the body associated to the active object whose active thread is calling this method.
   */
  public Body getCurrentThreadBody() {
    AbstractBody body = (AbstractBody) bodyPerThread.get();
    if (body == null) {
      // If we cannot find the body from the current thread we assume that the current thread
      // is not the one from an active object. Therefore in this case we create an HalfBody
      // that handle the futures
      body = HalfBody.getHalfBody(halfBodyMetaObjectFactory);
      bodyPerThread.set(body);
      //registerBody(body);
    }
    return body;
  }


  /**
   * Associates the body with the thread calling the method. 
   * @param the body to associate to the active thread that calls this method.
   */
  public void setCurrentThreadBody(Body body) {
    bodyPerThread.set(body);
  }


  /**
   * Returns the body belonging to this JVM whose ID is the one specified.
   * Returns null if a body with such an id is not found in this jvm
   * @param bodyID the ID to look for
   * @return the body with matching id or null
   */
  public Body getLocalBody(UniqueID bodyID) {
    return (Body) localBodyMap.getBody(bodyID);
  }


  /**
   * Returns all local Bodies in a new BodyMap
   * @return all local Bodies in a new BodyMap
   */
  public BodyMap getLocalBodies() {
    return (BodyMap) localBodyMap.clone();
  }


  /**
   * Adds a listener of body events. The listener is notified every time a body
   * (active or not) is registered or unregistered in this JVM.
   * @param listener the listener of body events to add
   */
  public void addBodyEventListener(BodyEventListener listener) {
    bodyEventProducer.addBodyEventListener(listener);
  }


  /**
   * Removes a listener of body events.
   * @param listener the listener of body events to remove
   */
  public void removeBodyEventListener(BodyEventListener listener) {
    bodyEventProducer.removeBodyEventListener(listener);
  }


  //
  // -- FRIENDLY METHODS -----------------------------------------------
  //

  void registerBody(AbstractBody body) {
    localBodyMap.putBody(body.bodyID, body);
    bodyEventProducer.fireBodyCreated(body);
  }

  void unregisterBody(AbstractBody body) {
    localBodyMap.removeBody(body.bodyID);
    bodyEventProducer.fireBodyRemoved(body);
  }

}
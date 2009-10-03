/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.logging.DumpHandler;
import com.tc.net.NodeID;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.session.SessionID;

import java.util.Collection;

public interface ClientLockManager extends TerracottaLocking, ClientHandshakeCallback, DumpHandler {
  /**
  * Called by a Terracotta thread to notify the given thread waiting on the lock.
  */
  public void notified(LockID lock, ThreadID thread);
  
  /**
   * Called by a Terracotta thread to request the return of a greedy lock previously
   * awarded to the client.
   */
  public void recall(LockID lock, ServerLockLevel level, int lease);
  
  /**
   * Called by a Terracotta thread to award a per-thread or greedy lock to the client.
   */
  public void award(NodeID from, SessionID session, LockID lock, ThreadID thread, ServerLockLevel level);
  
  /**
   * Called by a Terracotta thread to indicate that the specified non-blocking try lock attempt
   * has failed at the server.
   */
  public void refuse(NodeID from, SessionID session, LockID lock, ThreadID thread, ServerLockLevel level);
  
  /**
   * Called by a Terracotta thread to return the result of a previous query operation on the RemoteLockManager.
   */
  public void info(ThreadID requestor, Collection<ClientServerExchangeLockContext> contexts);
  
  /**
   * Returns a complete dump (in pseudo-portable format) of the state of all locks.
   */
  public Collection<ClientServerExchangeLockContext> getAllLockContexts();    
}

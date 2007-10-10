/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.lockmanager.impl;

import com.tc.management.L2LockStatsManager;
import com.tc.management.L2LockStatsManagerImpl;
import com.tc.net.groups.ClientID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.tx.WaitInvocation;
import com.tc.objectserver.api.TestSink;
import com.tc.objectserver.lockmanager.api.LockHolder;
import com.tc.objectserver.lockmanager.api.NullChannelManager;

import junit.framework.TestCase;

public class LockStatManagerTest extends TestCase {
  private TestSink           sink;
  private LockManagerImpl    lockManager;
  private L2LockStatsManager lockStatManager;

  protected void setUp() throws Exception {
    super.setUp();
    resetLockManager();
    sink = new TestSink();
  }

  private void resetLockManager() {
    if (lockManager != null) {
      try {
        lockManager.stop();
      } catch (InterruptedException e) {
        fail();
      }
    }

    lockStatManager = new L2LockStatsManagerImpl();
    lockManager = new LockManagerImpl(new NullChannelManager(), lockStatManager);
    lockManager.setLockPolicy(LockManagerImpl.ALTRUISTIC_LOCK_POLICY);
    lockManager.start();
    lockStatManager.start(new NullChannelManager(), lockManager, sink);
  }

  protected void tearDown() throws Exception {
    assertEquals(0, lockManager.getLockCount());
    assertEquals(0, lockManager.getThreadContextCount());
    super.tearDown();
  }
  
  public void testLockHeldDuration() {
    try {
      LockID l1 = new LockID("1");
      final ClientID cid1 = new ClientID(new ChannelID(1));
      ThreadID s1 = new ThreadID(0);
      final ClientID cid2 = new ClientID(new ChannelID(2));
      ThreadID s2 = new ThreadID(1);

      lockManager.requestLock(l1, cid1, s1, LockLevel.WRITE, sink);
      lockManager.requestLock(l1, cid2, s2, LockLevel.READ, sink);

      LockHolder lockHolder1 = lockStatManager.getLockHolder(l1, cid1, s1);
      LockHolder lockHolder2 = lockStatManager.getLockHolder(l1, cid2, s2);
      Thread.sleep(5000);
      lockManager.unlock(l1, cid1, s1);

      assertTrue(lockHolder1.getHeldTimeInMillis() > lockHolder2.getHeldTimeInMillis());
      lockManager.unlock(l1, cid2, s2);
    } catch (InterruptedException e) {
      // ignore
    } finally {
      resetLockManager();
    }
  }
  
  public void testLockStatsManager() {
    veriyLockStatsManagerStatistics();
    
    lockStatManager.disableLockStatistics();
    
    LockID l1 = new LockID("1");
    ThreadID s1 = new ThreadID(0);
    
    final ClientID cid1 = new ClientID(new ChannelID(1));
    
    lockManager.requestLock(l1, cid1, s1, LockLevel.WRITE, sink);
    assertEquals(0, lockStatManager.getNumberOfLockRequested(l1));
    lockManager.unlock(l1, cid1, s1);
    
    lockStatManager.enableLockStatistics();
    
    veriyLockStatsManagerStatistics();
  }

  private void veriyLockStatsManagerStatistics() {
    LockID l1 = new LockID("1");
    ThreadID s1 = new ThreadID(0);
    
    final ClientID cid1 = new ClientID(new ChannelID(1));
    final ClientID cid2 = new ClientID(new ChannelID(2));
    final ClientID cid3 = new ClientID(new ChannelID(3));
    final ClientID cid4 = new ClientID(new ChannelID(4));

    lockManager.requestLock(l1, cid1, s1, LockLevel.WRITE, sink);
    assertEquals(1, lockStatManager.getNumberOfLockRequested(l1));
    assertEquals(0, lockStatManager.getNumberOfPendingRequests(l1));

    lockManager.requestLock(l1, cid2, s1, LockLevel.WRITE, sink); // c2 should pend
    assertEquals(2, lockStatManager.getNumberOfLockRequested(l1));
    assertEquals(1, lockStatManager.getNumberOfPendingRequests(l1));

    lockManager.tryRequestLock(l1, cid3, s1, LockLevel.WRITE, new WaitInvocation(0, 0), sink);
    assertEquals(3, lockStatManager.getNumberOfLockRequested(l1));
    assertEquals(1, lockStatManager.getNumberOfPendingRequests(l1));

    lockManager.unlock(l1, cid1, s1); // it will grant request to c2
    assertEquals(0, lockStatManager.getNumberOfPendingRequests(l1));
    assertEquals(1, lockStatManager.getNumberOfLockReleased(l1));
    assertEquals(1, lockStatManager.getNumberOfLockReleased(l1));

    lockManager.requestLock(l1, cid1, s1, LockLevel.WRITE, sink); // c1 request again
    assertEquals(4, lockStatManager.getNumberOfLockRequested(l1));
    assertEquals(1, lockStatManager.getNumberOfPendingRequests(l1));

    lockManager.unlock(l1, cid2, s1); // grant to c1 again
    assertEquals(1, lockStatManager.getNumberOfLockHopRequests(l1));
    assertEquals(0, lockStatManager.getNumberOfPendingRequests(l1));
    assertEquals(2, lockStatManager.getNumberOfLockReleased(l1));

    lockManager.requestLock(l1, cid3, s1, LockLevel.WRITE, sink);
    lockManager.requestLock(l1, cid4, s1, LockLevel.WRITE, sink);
    assertEquals(6, lockStatManager.getNumberOfLockRequested(l1));
    assertEquals(2, lockStatManager.getNumberOfPendingRequests(l1));

    lockManager.unlock(l1, cid1, s1); // grant to c3
    assertEquals(1, lockStatManager.getNumberOfPendingRequests(l1));
    assertEquals(3, lockStatManager.getNumberOfLockReleased(l1));

    lockManager.unlock(l1, cid3, s1); // grant to c4
    assertEquals(0, lockStatManager.getNumberOfPendingRequests(l1));
    assertEquals(4, lockStatManager.getNumberOfLockReleased(l1));
    lockManager.requestLock(l1, cid3, s1, LockLevel.WRITE, sink);
    assertEquals(7, lockStatManager.getNumberOfLockRequested(l1));
    assertEquals(1, lockStatManager.getNumberOfPendingRequests(l1));

    lockManager.unlock(l1, cid4, s1); // grant to c3 again
    assertEquals(2, lockStatManager.getNumberOfLockHopRequests(l1));
    assertEquals(0, lockStatManager.getNumberOfPendingRequests(l1));
    assertEquals(5, lockStatManager.getNumberOfLockReleased(l1));

    lockManager.unlock(l1, cid3, s1);
    assertEquals(6, lockStatManager.getNumberOfLockReleased(l1));
  }

}

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.async.api.EventContext;
import com.tc.util.Assert;

public class NullTCGroupMemberDiscovery implements TCGroupMemberDiscovery {

  public Node getLocalNode() {
    Assert.fail();
    return null;
  }

  public void setLocalNode(Node local) {
    return;
  }

  public void setTCGroupManager(TCGroupManagerImpl manager) {
    return;
  }

  public void start() {
    return;
  }

  public void stop() {
    return;
  }

  public void discoveryHandler(EventContext context) {
    Assert.fail();
  }

  public void nodeJoined(NodeID nodeID) {
    return;
  }

  public void nodeLeft(NodeID nodeID) {
    return;
  }

}

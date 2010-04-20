/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.net.NodeID;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.session.SessionID;

public interface RemoteServerMapManager extends ClientHandshakeCallback {

  public Object getMappingForKey(ObjectID oid, Object portableKey);

  public void addResponseForKeyValueMapping(SessionID localSessionID, ObjectID mapID, ServerMapRequestID requestID,
                                            Object portableValue, NodeID nodeID);

  public int getSize(ObjectID mapID);
}

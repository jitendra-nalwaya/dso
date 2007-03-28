/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.state;

import com.tc.l2.msg.L2StateMessage;
import com.tc.l2.msg.L2StateMessageFactory;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.GroupMessage;
import com.tc.net.groups.GroupResponse;
import com.tc.net.groups.NodeID;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.State;

import java.util.HashSet;
import java.util.Iterator;

public class ElectionManagerImpl implements ElectionManager {

  private static final TCLogger logger               = TCLogging.getLogger(ElectionManagerImpl.class);

  private static final State    INIT                 = new State("Initial-State");
  private static final State    ELECTION_COMPLETE    = new State("Election-Complete");
  private static final State    ELECTION_IN_PROGRESS = new State("Election-In-Progress");

  private static final long     ELECTION_TIME        = TCPropertiesImpl.getProperties()
                                                         .getLong("l2.ha.electionmanager.electionTimePeriod");

  private final GroupManager    groupManager;
  private State                 state                = INIT;

  // XXX::NOTE:: These variables are not reset until next election
  private HashSet               votes                = new HashSet();
  private Enrollment            myVote               = null;
  private Enrollment            winner;

  public ElectionManagerImpl(GroupManager groupManager) {
    this.groupManager = groupManager;
  }

  public synchronized void handleStartElectionRequest(L2StateMessage msg) {
    Assert.assertEquals(L2StateMessage.START_ELECTION, msg.getType());
    if (state == ELECTION_IN_PROGRESS) {
      // Another node is also joining in the election process
      // Cast its vote and notify my vote
      Assert.assertNotNull(myVote);
      votes.add(msg.getEnrollment());
      if (msg.inResponseTo().isNull()) {
        // This is not a response to this node initiating election. So notify this nodes vote
        GroupMessage response = createElectionStartedMessage(msg, myVote);
        logger.info("Casted vote from " + msg + " My Response : " + response);
        try {
          groupManager.sendTo(msg.messageFrom(), response);
        } catch (GroupException e) {
          logger.error("Error sending Votes to : " + msg.messageFrom(), e);
        }
      } else {
        logger.info("Casted vote from " + msg);
      }
    } else {
      logger.info("Ignoring Start Election Request  : " + msg + " My state = " + state);
    }
  }

  public synchronized void handleElectionAbort(L2StateMessage msg) {
    Assert.assertEquals(L2StateMessage.ABORT_ELECTION, msg.getType());
    if (state == ELECTION_IN_PROGRESS) {
      // An existing ACTIVE Node has forced election to quit
      Assert.assertNotNull(myVote);
      basicAbort(msg);
    } else {
      logger.warn("Ignoring Abort Election Request  : " + msg + " My state = " + state);
    }
  }

  public synchronized void handleElectionResultMessage(L2StateMessage msg) {
    Assert.assertEquals(L2StateMessage.ELECTION_RESULT, msg.getType());
    if (state == ELECTION_COMPLETE && !this.winner.equals(msg.getEnrollment())) {
      // conflict
      GroupMessage resultConflict = L2StateMessageFactory.createResultConflictMessage(msg, this.winner);
      logger.warn("WARNING :: Election result conflict : Winner local = " + this.winner + " :  remote winner = "
                  + msg.getEnrollment());
      try {
        groupManager.sendTo(msg.messageFrom(), resultConflict);
      } catch (GroupException e) {
        logger.error("Error sending Election result conflict message : " + resultConflict);
      }
    }
    if (state == ELECTION_IN_PROGRESS) {
      // Agree to the result and abort the election
      basicAbort(msg);
    }
    GroupMessage resultAgreed = L2StateMessageFactory.createResultAgreedMessage(msg, msg.getEnrollment());
    logger.info("Agreed with Election Result from " + msg.messageFrom() + " : " + resultAgreed);
    try {
      groupManager.sendTo(msg.messageFrom(), resultAgreed);
    } catch (GroupException e) {
      logger.error("Error sending Election result agreed message : " + resultAgreed);
    }
  }

  private void basicAbort(L2StateMessage msg) {
    this.winner = msg.getEnrollment();
    reset();
    logger.info("Aborted Election : Winner is : " + this.winner);
    notifyAll();
  }

  /**
   * This method is called by the winner of the election to announce to the world
   */
  public synchronized void declareWinner(NodeID myNodeId) {
    Assert.assertEquals(winner.getNodeID(), myNodeId);
    GroupMessage msg = createElectionWonMessage(this.winner);
    try {
      this.groupManager.sendAll(msg);
    } catch (GroupException e) {
      logger.error("Error declaring results : ", e);
    }
    logger.info("Declared as Winner: Winner is : " + this.winner);
    reset();
  }

  public synchronized void reset() {
    this.state = INIT;
    this.votes.clear();
    this.myVote = null;
  }

  public NodeID runElection(NodeID myNodeId, boolean isNew) {
    NodeID winnerID = NodeID.NULL_ID;
    int count = 0;
    while (winnerID.isNull()) {
      if (count++ > 1) {
        logger.info("Requesting Re-election !!! count = " + count);
      }
      try {
        winnerID = doElection(myNodeId, isNew);
      } catch (GroupException e1) {
        logger.error("Error during election : ", e1);
        reset();
      }
    }
    return winnerID;
  }

  private synchronized void electionStarted(Enrollment e) {
    if (this.state == ELECTION_IN_PROGRESS) { throw new AssertionError("Election Already in Progress"); }
    this.state = ELECTION_IN_PROGRESS;
    this.myVote = e;
    this.winner = null;
    this.votes.clear();
    this.votes.add(e); // Cast my vote
    logger.info("Election Started : " + e);
  }

  private NodeID doElection(NodeID myNodeId, boolean isNew) throws GroupException {

    // Step 1: publish to cluster NodeID, weight and election start
    Enrollment e = EnrollmentFactory.createEnrollment(myNodeId, isNew);
    electionStarted(e);

    GroupMessage msg = createElectionStartedMessage(e);
    groupManager.sendAll(msg);

    // Step 2: Wait for election completion
    waitTillElectionComplete();

    // Step 3: Compute Winner
    Enrollment lWinner = computeResult();
    if (lWinner != e) {
      logger.info("Election lost : Winner is : " + lWinner);
      Assert.assertNotNull(lWinner);
      return lWinner.getNodeID();
    }
    // Step 4 : local host won the election, so notify world for acceptance
    msg = createElectionResultMessage(e);
    GroupResponse responses = groupManager.sendAllAndWaitForResponse(msg);
    for (Iterator i = responses.getResponses().iterator(); i.hasNext();) {
      L2StateMessage response = (L2StateMessage) i.next();
      Assert.assertEquals(msg.getMessageID(), response.inResponseTo());
      if (response.getType() == L2StateMessage.RESULT_AGREED) {
        Assert.assertEquals(e, response.getEnrollment());
      } else if (response.getType() == L2StateMessage.RESULT_CONFLICT) {
        logger.info("Result Conflict: Local Result : " + e + " From : " + response.messageFrom() + " Result : "
                    + response.getEnrollment());
        return NodeID.NULL_ID;
      } else {
        throw new AssertionError("Node : " + response.messageFrom()
                                 + " responded neither with RESULT_AGREED or RESULT_CONFLICT :" + response);
      }
    }

    // Step 5 : result agreed - I am the winner
    return myNodeId;
  }

  private synchronized Enrollment computeResult() {
    if (state == ELECTION_IN_PROGRESS) {
      state = ELECTION_COMPLETE;
      logger.info("Election Complete : " + votes + " : " + state);
      winner = countVotes();
    }
    return winner;
  }

  private Enrollment countVotes() {
    Enrollment computedWinner = null;
    for (Iterator i = votes.iterator(); i.hasNext();) {
      Enrollment e = (Enrollment) i.next();
      if (computedWinner == null) {
        computedWinner = e;
      } else if (e.wins(computedWinner)) {
        computedWinner = e;
      }
    }
    Assert.assertNotNull(computedWinner);
    return computedWinner;
  }

  private synchronized void waitTillElectionComplete() {
    long start = System.currentTimeMillis();
    long diff = ELECTION_TIME;
    while (state == ELECTION_IN_PROGRESS && diff > 0) {
      try {
        wait(diff);
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
      diff = diff - (System.currentTimeMillis() - start);
    }
  }

  private GroupMessage createElectionStartedMessage(Enrollment e) {
    return L2StateMessageFactory.createElectionStartedMessage(e);
  }

  private GroupMessage createElectionWonMessage(Enrollment e) {
    return L2StateMessageFactory.createElectionWonMessage(e);
  }

  private GroupMessage createElectionResultMessage(Enrollment e) {
    return L2StateMessageFactory.createElectionResultMessage(e);
  }

  private GroupMessage createElectionStartedMessage(L2StateMessage msg, Enrollment e) {
    return L2StateMessageFactory.createElectionStartedMessage(msg, e);
  }

}

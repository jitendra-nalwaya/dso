/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.impl;

import com.tc.exception.ImplementMe;
import com.tc.net.groups.NodeID;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.GCStats;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectManagerStatsListener;
import com.tc.objectserver.context.GCResultContext;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.core.api.Filter;
import com.tc.objectserver.core.api.GarbageCollector;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.impl.ManagedObjectReference;
import com.tc.objectserver.l1.api.TestClientStateManager;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.impl.NullPersistenceTransactionProvider;
import com.tc.statistics.mock.NullStatisticsAgentSubSystem;
import com.tc.text.PrettyPrinterImpl;
import com.tc.util.ObjectIDSet;

import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

public class MarkAndSweepGarbageCollectorTest extends TestCase implements ObjectManager {
  private long                           objectIDCounter     = 0;
  private TestManagedObject              root1;
  private TestManagedObject              root2;
  private Set                            roots               = new HashSet();
  private Map                            managed;                                                       // = new
  // HashMap();
  private GarbageCollector               collector;                                                     // = new
  // MarkAndSweepGarbageCollector();
  private Set                            lookedUp;
  private Set                            released;
  private PersistenceTransactionProvider transactionProvider = new NullPersistenceTransactionProvider();

  private Filter                         filter              = new Filter() {
                                                               public boolean shouldVisit(ObjectID referencedObject) {
                                                                 return true;
                                                               }
                                                             };

  /**
   * Constructor for MarkAndSweepGarbageCollectorTest.
   * 
   * @param arg0
   */
  public MarkAndSweepGarbageCollectorTest(String arg0) {
    super(arg0);
  }

  /*
   * @see TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
    this.managed = new HashMap();

    this.collector = new MarkAndSweepGarbageCollector(this, new TestClientStateManager(), false,
                                                      new NullStatisticsAgentSubSystem());
    this.lookedUp = new HashSet();
    this.released = new HashSet();
    this.root1 = createObject(8);
    this.root2 = createObject(8);
    this.roots = new HashSet();
    roots.add(root1);
    roots.add(root2);
  }

  public Object getLock() {
    return this;
  }

  public Set getRootIDs() {
    HashSet rv = new HashSet();
    for (Iterator i = roots.iterator(); i.hasNext();) {
      ObjectID id = ((TestManagedObject) i.next()).getID();
      rv.add(id);
    }
    return rv;
  }
  
  public void testGCStats() {
      collector.start();
      collector.gc();
      GCStats[] stats = collector.getGarbageCollectorStats();
      for(int i = 0; i < stats.length; i++) {
        System.out.println("stat:" + stats[i]);
      }
      collector.stop();
    
  }

  public void testEmptyRoots() {
    // System.out.println("running: testEmptyRoots");

    Set toDelete = collector.collect(filter, getRootIDs(), new ObjectIDSet(managed.keySet()));
    // System.out.println(toDelete);
    assertTrue(toDelete.size() == 0);
  }

  public void testOneLevelNoGarbage() {
    // System.out.println("running: testOneLevelNoGarbage");
    TestManagedObject tmo = createObject(3);
    root1.setReference(0, tmo.getID());
    Set toDelete = collector.collect(filter, getRootIDs(), new ObjectIDSet(managed.keySet()));
    // System.out.println(toDelete);
    assertTrue(toDelete.size() == 0);
  }

  public void testSharedBetweenRootsNoGarbage() {
    // System.out.println("running: testSharedBetweenRootsNoGarbage");
    TestManagedObject tmo = createObject(3);
    root1.setReference(0, tmo.getID());
    root2.setReference(0, tmo.getID());
    Set toDelete = collector.collect(filter, getRootIDs(), new ObjectIDSet(managed.keySet()));
    // System.out.println(toDelete);
    // System.out.println(managed);
    assertTrue(toDelete.size() == 0);
  }

  public void testObjectCycleNoGarbage() {
    TestManagedObject tmo1 = createObject(3);
    TestManagedObject tmo2 = createObject(3);
    tmo1.setReference(0, tmo2.getID());
    tmo2.setReference(0, tmo1.getID());

    root1.setReference(0, tmo1.getID());

    Set toDelete = collector.collect(filter, getRootIDs(), new ObjectIDSet(managed.keySet()));
    // System.out.println(toDelete);
    assertTrue(toDelete.size() == 0);
  }

  public void testObjectCycleWithGarbage() {
    TestManagedObject tmo1 = createObject(3);
    TestManagedObject tmo2 = createObject(3);
    createObject(3);

    tmo1.setReference(0, tmo2.getID());
    tmo2.setReference(0, tmo1.getID());

    root1.setReference(0, tmo1.getID());

    Set toDelete = collector.collect(filter, getRootIDs(), new ObjectIDSet(managed.keySet()));
    // System.out.println(toDelete);
    assertTrue(toDelete.size() == 1);
  }

  public void testFilter() {
    final TestManagedObject tmo1 = createObject(3);
    final TestManagedObject tmo2 = createObject(3);
    final TestManagedObject tmo3 = createObject(3);

    tmo1.setReference(0, tmo2.getID());
    tmo2.setReference(0, tmo1.getID());
    tmo2.setReference(1, tmo3.getID());

    root1.setReference(0, tmo1.getID());

    Filter testFilter = new Filter() {
      public boolean shouldVisit(ObjectID referencedObject) {
        boolean rv = (!tmo2.getID().equals(referencedObject));
        return rv;
      }
    };

    // make sure that the filter filters out the sub-graph starting at the reference to tmo2.
    collector.collect(testFilter, getRootIDs(), new ObjectIDSet(managed.keySet()));
    assertTrue(this.lookedUp.contains(tmo1.getID()));
    assertFalse(this.lookedUp.contains(tmo2.getID()));
    assertFalse(this.lookedUp.contains(tmo3.getID()));

    // try it with the regular filter to make sure the behavior is actually different.
    collector.collect(filter, getRootIDs(), new ObjectIDSet(managed.keySet()));
    assertTrue(this.lookedUp.contains(tmo1.getID()));
    assertTrue(this.lookedUp.contains(tmo2.getID()));
    assertTrue(this.lookedUp.contains(tmo3.getID()));
  }

  public void testLookupAndReleaseBalanced() {
    final TestManagedObject tmo1 = createObject(3);
    final TestManagedObject tmo2 = createObject(3);
    final TestManagedObject tmo3 = createObject(3);

    tmo1.setReference(0, tmo2.getID());
    tmo2.setReference(0, tmo1.getID());
    tmo2.setReference(1, tmo3.getID());

    root1.setReference(0, tmo1.getID());

    collector.collect(filter, getRootIDs(), new ObjectIDSet(managed.keySet()));
    assertTrue(lookedUp.equals(released));
  }

  public void testIsInGCPause() throws Exception {
    assertFalse(collector.isPausingOrPaused());
    collector.requestGCPause();
    collector.notifyReadyToGC();
    assertTrue(collector.isPausingOrPaused());
    collector.notifyGCComplete();
    assertFalse(collector.isPausingOrPaused());
  }

  private ObjectID nextID() {
    return new ObjectID(objectIDCounter++);
  }

  private TestManagedObject createObject(int refCount) {
    ObjectID[] ids = new ObjectID[refCount];

    Arrays.fill(ids, ObjectID.NULL_ID);

    TestManagedObject tmo = new TestManagedObject(nextID(), ids);
    // System.out.println("Creating Object:" + tmo.getID());
    managed.put(tmo.getID(), tmo.getReference());
    return tmo;
  }

  public ManagedObject getObjectByID(ObjectID id) {
    this.lookedUp.add(id);
    ManagedObjectReference ref = (ManagedObjectReference) managed.get(id);
    return (ref == null) ? null : ref.getObject();
  }

  public void release(PersistenceTransaction tx, ManagedObject object) {
    this.released.add(object.getID());
    return;
  }

  public void releaseAll(PersistenceTransaction tx, Collection c) {
    return;
  }

  public void stop() {
    throw new ImplementMe();
  }

  public boolean lookupObjectsAndSubObjectsFor(NodeID nodeID, ObjectManagerResultsContext responseContext, int maxCount) {
    throw new ImplementMe();
  }

  public boolean lookupObjectsFor(NodeID nodeID, ObjectManagerResultsContext context) {
    throw new ImplementMe();
  }

  public Iterator getRoots() {
    throw new ImplementMe();
  }

  public void createRoot(String name, ObjectID id) {
    throw new ImplementMe();
  }

  public ObjectID lookupRootID(String name) {
    throw new ImplementMe();
  }

  public GarbageCollector getGarbageCollector() {
    throw new ImplementMe();
  }

  public void setGarbageCollector(GarbageCollector gc) {
    throw new ImplementMe();
  }

  public void setStatsListener(ObjectManagerStatsListener listener) {
    throw new ImplementMe();
  }

  public void start() {
    throw new ImplementMe();
  }

  public PrettyPrinterImpl prettyPrint(PrettyPrinterImpl out) {
    throw new ImplementMe();
  }

  public void releaseReadOnly(ManagedObject object) {
    this.released.add(object.getID());
    return;
  }

  public void releaseAllReadOnly(Collection objects) {
    releaseAll(transactionProvider.nullTransaction(), objects);
  }

  public int getCheckedOutCount() {
    return 0;
  }

  public ObjectIDSet getAllObjectIDs() {
    return new ObjectIDSet(managed.keySet());
  }

  public void addFaultedObject(ObjectID oid, ManagedObject mo, boolean removeOnRelease) {
    throw new ImplementMe();
  }

  public void waitUntilReadyToGC() {
    //
  }

  public void flushAndEvict(List objects2Flush) {
    throw new ImplementMe();
  }

  public Map getRootNamesToIDsMap() {
    throw new ImplementMe();
  }

  public void dump(Writer writer) {
    throw new ImplementMe();

  }

  public void dumpToLogger() {
    throw new ImplementMe();

  }

  public String dump() {
    throw new ImplementMe();
  }

  public void preFetchObjectsAndCreate(Set oids, Set newOids) {
    throw new ImplementMe();
  }

  public void createNewObjects(Set ids) {
    throw new ImplementMe();
  }

  public ManagedObject getObjectByIDOrNull(ObjectID id) {
    ManagedObject mo = getObjectByID(id);
    if (mo != null && mo.isNew()) { return null; }
    return mo;
  }

  public void notifyGCComplete(GCResultContext resultContext) {
    throw new ImplementMe();
  }

}

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import com.tc.properties.TCPropertiesConsts;

import java.util.ArrayList;

public abstract class OffHeapActiveActiveTransparentTestBase extends ActiveActiveTransparentTestBase {

  @Override
  protected void setExtraJvmArgs(ArrayList jvmArgs) {
    jvmArgs.add("-XX:MaxDirectMemorySize=" + getMaxDirectMemorySize());

    jvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_OFFHEAP_CACHE_ENABLED + "=true");
    jvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_OFFHEAP_CACHE_MAX_DATASIZE + "=209715200"); // 200m

    jvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_OFFHEAP_OBJECT_CACHE_INITIAL_DATASIZE + "=1048576"); // 1m
    jvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_OFFHEAP_OBJECT_CACHE_TABLESIZE + "=1048576"); // 1m
    jvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_OFFHEAP_OBJECT_CACHE_CONCURRENCY + "=16"); // 2k

    jvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_OFFHEAP_MAP_CACHE_INITIAL_DATASIZE + "=10240"); // 10k
    jvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_OFFHEAP_MAP_CACHE_TABLESIZE + "=1024"); // 1k
  }

  protected String getMaxDirectMemorySize() {
    return "256m";
  }

}
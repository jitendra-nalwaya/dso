/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

public class WildcardAtStartEvent extends SingletonEvent {

  public WildcardAtStartEvent(Object source, String message) {
    super(source, message);
  }

}

/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.dynamic.FileConfigItem;

public interface NewStatisticsConfig {
  FileConfigItem statisticsPath();
}
/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.dna.api;

import com.tc.io.TCDataInput;
import com.tc.io.TCDataOutput;

import java.io.IOException;

public interface IDNAEncoding {

  static final byte        LOGICAL_ACTION_TYPE             = 1;
  static final byte        PHYSICAL_ACTION_TYPE            = 2;
  static final byte        ARRAY_ELEMENT_ACTION_TYPE       = 3;
  static final byte        ENTIRE_ARRAY_ACTION_TYPE        = 4;
  static final byte        LITERAL_VALUE_ACTION_TYPE       = 5;
  static final byte        PHYSICAL_ACTION_TYPE_REF_OBJECT = 6;
  static final byte        SUB_ARRAY_ACTION_TYPE           = 7;

  /**
   * When the policy is set to SERIALIZER then the DNAEncoding.decode() will return the exact Objects that where
   * encoded. For Example if UTF8ByteDataHolder is serialized to a stream, then when it is deserialized, you get an
   * UTF8ByteDataHolder object. Same goes for String or ClassHolder etc.
   * <p>
   * You may want such a policy in TCObjectInputStream, for example.
   */
  public static final byte SERIALIZER                      = 0x00;
  /**
   * When the policy is set to STORAGE then the DNAEncoding.decode() may return Objects that represent the original
   * objects for performance/memory. For Example if String is serialized to a stream, then when it is deserialized, you
   * may get UTF8ByteDataHolder instead.
   * <p>
   * As the name says, you may want such a policy for storage in the L2.
   */
  public static final byte STORAGE                         = 0x01;
  /**
   * When the policy is set to APPLICATOR then the DNAEncoding.decode() will return the original Objects that were
   * encoded in the orinal stream. For Example if UTF8ByteDataHolder is serialized to a stream, then when it is
   * deserialized, you get a String object.
   * <p>
   * You may want such a policy in TCObjectInputStream, for example.
   */
  public static final byte APPLICATOR                      = 0x02;

  public abstract byte getPolicy();

  public abstract void encodeClassLoader(Object value, TCDataOutput output);

  public abstract void encode(Object value, TCDataOutput output);

  public abstract Object decode(TCDataInput input) throws IOException, ClassNotFoundException;

  public abstract void encodeArray(Object value, TCDataOutput output);

  public abstract void encodeArray(Object value, TCDataOutput output, int length);

}
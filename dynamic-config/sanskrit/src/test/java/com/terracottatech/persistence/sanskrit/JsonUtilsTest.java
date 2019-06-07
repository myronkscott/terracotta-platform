/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.persistence.sanskrit;

import com.terracottatech.utilities.Json;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class JsonUtilsTest {
  @Test
  public void parseEmpty() throws Exception {
    String input = "{}";

    SanskritObjectImpl result = new SanskritObjectImpl();
    JsonUtils.parse(Json.copyObjectMapper(), input, result);

    assertNull(result.getString("A"));
  }

  @Test
  public void parseData() throws Exception {
    String input = "{" + System.lineSeparator() +
        "  \"A\" : \"a\"," + System.lineSeparator() +
        "  \"B\" : 1," + System.lineSeparator() +
        "  \"C\" : {" + System.lineSeparator() +
        "    \"E\" : \"e\"" + System.lineSeparator() +
        "  }," + System.lineSeparator() +
        "  \"D\" : null" + System.lineSeparator() +
        "}";

    SanskritObjectImpl result = new SanskritObjectImpl();
    JsonUtils.parse(Json.copyObjectMapper(), input, result);

    assertEquals("a", result.getString("A"));
    assertEquals(1L, (long) result.getLong("B"));
    assertNull(result.getObject("D"));

    SanskritObject child = result.getObject("C");
    assertEquals("e", child.getString("E"));
  }
}

package org.pageseeder.berlioz;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class BerliozOptionTest {

  @ParameterizedTest
  @EnumSource(BerliozOption.class)
  void testDeprecationMatchesAnnotation(BerliozOption option) throws NoSuchFieldException {
    Deprecated annotation = BerliozOption.class.getField(option.name()).getAnnotation(Deprecated.class);
    if (annotation == null) {
      assertFalse(option.isDeprecated(), option + " is not annotated @Deprecated");
      assertNull(option.deprecatedSince());
      assertNull(option.removal());
    } else {
      assertTrue(option.isDeprecated(), option + " is annotated @Deprecated");
      assertEquals(annotation.since(), option.deprecatedSince());
      assertEquals(annotation.forRemoval(), option.removal() != null,
          option + ": removal() must be set if and only if forRemoval is true");
    }
  }

  @ParameterizedTest
  @EnumSource(BerliozOption.class)
  void testDescriptionIsOneLinePlainText(BerliozOption option) {
    String description = option.description();
    assertFalse(description.isBlank(), option + " has no description");
    assertFalse(description.contains("\n"), option + " description must be one line");
    assertFalse(description.endsWith("."), option + " description must not end with a period");
    assertEquals(description.strip(), description);
  }

  @ParameterizedTest
  @EnumSource(BerliozOption.class)
  void testTypeMatchesDefault(BerliozOption option) {
    Object defaultTo = option.defaultTo();
    assertTrue(defaultTo instanceof Boolean || defaultTo instanceof String,
        option + " default must be a Boolean or a String");
    BerliozOption.Type expected = defaultTo instanceof Boolean ? BerliozOption.Type.BOOLEAN : BerliozOption.Type.STRING;
    assertEquals(expected, option.type());
    assertEquals(option.isBoolean(), option.type() == BerliozOption.Type.BOOLEAN);
  }

  @Test
  @SuppressWarnings("removal")
  void testKnownDeprecations() {
    assertEquals("0.14.0", BerliozOption.HTTP_GET_VIA_POST.deprecatedSince());
    assertEquals("1.0", BerliozOption.HTTP_GET_VIA_POST.removal());
    assertFalse(BerliozOption.HTTP_COMPRESSION.isDeprecated());
  }

}

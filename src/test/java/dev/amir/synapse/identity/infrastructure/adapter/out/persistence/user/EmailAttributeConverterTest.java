package dev.amir.synapse.identity.infrastructure.adapter.out.persistence.user;

import static org.assertj.core.api.Assertions.assertThat;

import dev.amir.synapse.identity.domain.value_object.Email;
import org.junit.jupiter.api.Test;

class EmailAttributeConverterTest {

  private final EmailAttributeConverter converter = new EmailAttributeConverter();

  @Test
  void convertsEmailToDatabaseStringAndBack() {
    var email = Email.of("amir@example.com");

    var databaseValue = converter.convertToDatabaseColumn(email);
    var restoredEmail = converter.convertToEntityAttribute(databaseValue);

    assertThat(databaseValue).isEqualTo("amir@example.com");
    assertThat(restoredEmail).isEqualTo(email);
  }

  @Test
  void preservesNullValues() {
    assertThat(converter.convertToDatabaseColumn(null)).isNull();
    assertThat(converter.convertToEntityAttribute(null)).isNull();
  }
}

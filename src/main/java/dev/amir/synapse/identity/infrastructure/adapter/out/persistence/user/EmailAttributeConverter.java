package dev.amir.synapse.identity.infrastructure.adapter.out.persistence.user;

import dev.amir.synapse.identity.domain.value_object.Email;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jspecify.annotations.Nullable;

@Converter
public class EmailAttributeConverter implements AttributeConverter<Email, String> {

  @Override
  public @Nullable String convertToDatabaseColumn(@Nullable Email email) {
    return email == null ? null : email.value();
  }

  @Override
  public @Nullable Email convertToEntityAttribute(@Nullable String value) {
    return value == null ? null : Email.of(value);
  }
}

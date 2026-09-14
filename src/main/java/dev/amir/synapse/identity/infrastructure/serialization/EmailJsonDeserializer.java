package dev.amir.synapse.identity.infrastructure.serialization;

import dev.amir.synapse.identity.domain.value_object.Email;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

public class EmailJsonDeserializer extends StdDeserializer<Email> {

  public EmailJsonDeserializer() {
    super(Email.class);
  }

  @Override
  public @Nullable Email deserialize(JsonParser parser, DeserializationContext context)
      throws JacksonException {
    var value = parser.getValueAsString();
    return value == null || value.isBlank() ? null : Email.of(value);
  }
}

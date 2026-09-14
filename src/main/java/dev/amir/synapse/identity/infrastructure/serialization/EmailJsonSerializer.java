package dev.amir.synapse.identity.infrastructure.serialization;

import dev.amir.synapse.identity.domain.value_object.Email;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class EmailJsonSerializer extends StdSerializer<Email> {

  public EmailJsonSerializer() {
    super(Email.class);
  }

  @Override
  public void serialize(Email email, JsonGenerator generator, SerializationContext context)
      throws JacksonException {
    generator.writeString(email.value());
  }
}

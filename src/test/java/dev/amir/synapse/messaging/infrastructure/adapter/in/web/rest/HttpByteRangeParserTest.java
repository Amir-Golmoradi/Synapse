package dev.amir.synapse.messaging.infrastructure.adapter.in.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.amir.synapse.messaging.domain.exception.InvalidMediaRangeException;
import org.junit.jupiter.api.Test;

class HttpByteRangeParserTest {
  private final HttpByteRangeParser parser = new HttpByteRangeParser();

  @Test
  void parsesClosedOpenAndSuffixRanges() {
    assertThat(parser.parse("bytes=10-19"))
        .satisfies(
            range -> {
              assertThat(range.start()).isEqualTo(10);
              assertThat(range.end()).isEqualTo(19);
              assertThat(range.suffixLength()).isNull();
            });
    assertThat(parser.parse("bytes=10-")).satisfies(range -> assertThat(range.end()).isNull());
    assertThat(parser.parse("bytes=-25"))
        .satisfies(range -> assertThat(range.suffixLength()).isEqualTo(25));
    assertThat(parser.parse(null)).isNull();
  }

  @Test
  void rejectsMultipleMalformedAndNegativeRanges() {
    for (var value : new String[] {"items=0-1", "bytes=", "bytes=2-1", "bytes=0-1,4-5"}) {
      assertThatThrownBy(() -> parser.parse(value)).isInstanceOf(InvalidMediaRangeException.class);
    }
  }
}

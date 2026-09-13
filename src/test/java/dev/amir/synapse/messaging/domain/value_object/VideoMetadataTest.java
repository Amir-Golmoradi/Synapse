package dev.amir.synapse.messaging.domain.value_object;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.amir.synapse.messaging.domain.exception.MessageValidationException;
import dev.amir.synapse.messaging.domain.value_object.VideoMetadata.AudioCodec;
import dev.amir.synapse.messaging.domain.value_object.VideoMetadata.VideoCodec;
import org.junit.jupiter.api.Test;

class VideoMetadataTest {

  @Test
  void acceptsSupportedWebmAndMp4MetadataIncludingSilentVideo() {
    assertThat(new VideoMetadata("video/webm", 42, 1_000, 640, 480, VideoCodec.VP9, null))
        .extracting(VideoMetadata::audioCodec)
        .isNull();
    assertThat(new VideoMetadata("video/mp4", 42, 1_000, 640, 480, VideoCodec.H264, AudioCodec.AAC))
        .extracting(VideoMetadata::contentType)
        .isEqualTo("video/mp4");
  }

  @Test
  void rejectsContainerAndCodecMismatches() {
    assertThatThrownBy(
            () ->
                new VideoMetadata(
                    "video/webm", 42, 1_000, 640, 480, VideoCodec.H264, AudioCodec.OPUS))
        .isInstanceOf(MessageValidationException.class);
    assertThatThrownBy(
            () ->
                new VideoMetadata(
                    "video/mp4", 42, 1_000, 640, 480, VideoCodec.H264, AudioCodec.OPUS))
        .isInstanceOf(MessageValidationException.class);
  }

  @Test
  void rejectsMissingAndNonPositiveMetadata() {
    assertThatThrownBy(
            () -> new VideoMetadata("video/mp4", 0, 1_000, 640, 480, VideoCodec.H264, null))
        .isInstanceOf(MessageValidationException.class);
    assertThatThrownBy(
            () -> new VideoMetadata("video/quicktime", 42, 1_000, 640, 480, VideoCodec.H264, null))
        .isInstanceOf(MessageValidationException.class);
  }
}

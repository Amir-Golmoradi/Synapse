package dev.amir.synapse.messaging.domain.exception;

import dev.amir.synapse.shared.domain.DomainException;
import java.io.Serial;

public final class VideoMessageException extends DomainException {
  @Serial private static final long serialVersionUID = 1L;

  private final String errorCode;
  private final String title;
  private final int httpStatus;

  private VideoMessageException(String errorCode, String title, String message, int httpStatus) {
    super(message);
    this.errorCode = errorCode;
    this.title = title;
    this.httpStatus = httpStatus;
  }

  private VideoMessageException(
      String errorCode, String title, String message, int httpStatus, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
    this.title = title;
    this.httpStatus = httpStatus;
  }

  public static VideoMessageException invalid(String message) {
    return new VideoMessageException("VIDEO_MEDIA_INVALID", "Video media is invalid", message, 422);
  }

  public static VideoMessageException invalid(String message, Throwable cause) {
    return new VideoMessageException(
        "VIDEO_MEDIA_INVALID", "Video media is invalid", message, 422, cause);
  }

  public static VideoMessageException unsupported(String message) {
    return new VideoMessageException(
        "VIDEO_FORMAT_UNSUPPORTED", "Video format is unsupported", message, 415);
  }

  public static VideoMessageException tooLarge() {
    return new VideoMessageException(
        "VIDEO_FILE_TOO_LARGE", "Video file is too large", "Video exceeds the size limit.", 413);
  }

  public static VideoMessageException durationExceeded() {
    return new VideoMessageException(
        "VIDEO_DURATION_EXCEEDED",
        "Video duration is too long",
        "Video exceeds the duration limit.",
        422);
  }

  public static VideoMessageException dimensionsExceeded() {
    return new VideoMessageException(
        "VIDEO_DIMENSIONS_EXCEEDED",
        "Video dimensions are too large",
        "Video exceeds the dimension limit.",
        422);
  }

  public static VideoMessageException unavailable(String message) {
    return new VideoMessageException(
        "VIDEO_MEDIA_UNAVAILABLE", "Video media is unavailable", message, 503);
  }

  public static VideoMessageException unavailable(String message, Throwable cause) {
    return new VideoMessageException(
        "VIDEO_MEDIA_UNAVAILABLE", "Video media is unavailable", message, 503, cause);
  }

  public static VideoMessageException notFound() {
    return new VideoMessageException(
        "MESSAGE_MEDIA_NOT_FOUND",
        "Message media not found",
        "The media was not found or is not accessible.",
        404);
  }

  @Override
  public String getErrorCode() {
    return errorCode;
  }

  @Override
  public String getTitle() {
    return title;
  }

  @Override
  public int getHttpStatus() {
    return httpStatus;
  }
}

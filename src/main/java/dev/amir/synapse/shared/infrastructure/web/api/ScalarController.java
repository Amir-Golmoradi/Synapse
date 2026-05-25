package dev.amir.synapse.shared.infrastructure.web.api;

import com.scalar.maven.core.ScalarHtmlRenderer;
import com.scalar.maven.core.ScalarProperties;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(prefix = "scalar", name = "enabled", havingValue = "true")
public class ScalarController {

  private static final MediaType APPLICATION_JAVASCRIPT =
      MediaType.valueOf("application/javascript");

  private final ScalarProperties scalarProperties;

  public ScalarController(ScalarProperties scalarProperties) {
    this.scalarProperties = scalarProperties;
  }

  @GetMapping("${scalar.path:/scalar}")
  public ResponseEntity<String> getScalarUi() throws IOException {
    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_HTML)
        .body(ScalarHtmlRenderer.render(scalarProperties));
  }

  @GetMapping({"/scalar.js", "${scalar.path:/scalar}/scalar.js"})
  public ResponseEntity<byte[]> getScalarJs() throws IOException {
    return ResponseEntity.ok()
        .contentType(APPLICATION_JAVASCRIPT)
        .body(ScalarHtmlRenderer.getScalarJsContent());
  }
}

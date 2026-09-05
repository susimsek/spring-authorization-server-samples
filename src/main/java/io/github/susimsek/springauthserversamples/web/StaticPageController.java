package io.github.susimsek.springauthserversamples.web;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StaticPageController {

    @GetMapping(value = "/404.html", produces = MediaType.TEXT_HTML_VALUE)
    ResponseEntity<Resource> notFoundPage() {
        Resource notFoundPage = new ClassPathResource("static/404.html");
        if (!notFoundPage.exists()) {
            notFoundPage = new ClassPathResource("static/index.html");
        }
        if (!notFoundPage.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(notFoundPage);
    }
}

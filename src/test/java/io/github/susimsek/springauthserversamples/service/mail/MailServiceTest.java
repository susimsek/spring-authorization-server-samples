package io.github.susimsek.springauthserversamples.service.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.config.ApplicationProperties;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.util.Locale;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

class MailServiceTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final MessageSource messageSource = mock(MessageSource.class);
    private final SpringTemplateEngine templateEngine = mock(SpringTemplateEngine.class);

    @Test
    void skipsDeliveryWhenMailIsDisabled() {
        MailService service =
                new MailService(properties(false), mailSender, messageSource, templateEngine);

        service.sendPasswordReset(
                "user@example.com", "user", Locale.ENGLISH, "https://example/reset");

        verify(mailSender, never()).createMimeMessage();
        verify(templateEngine, never()).process(any(String.class), any(Context.class));
    }

    @Test
    void rendersAndSendsLocalizedVerificationEmail() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("mail/emailVerification"), any(Context.class)))
                .thenReturn("<p>Doğrula</p>");
        when(messageSource.getMessage(
                        "mail.verification.subject", null, Locale.forLanguageTag("tr")))
                .thenReturn("E-posta adresinizi doğrulayın");
        MailService service =
                new MailService(properties(true), mailSender, messageSource, templateEngine);

        service.sendEmailVerification(
                "user@example.com",
                "kullanıcı",
                Locale.forLanguageTag("tr"),
                "https://example/verify?token=secret");

        ArgumentCaptor<Context> context = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("mail/emailVerification"), context.capture());
        assertThat(context.getValue().getLocale()).isEqualTo(Locale.forLanguageTag("tr"));
        assertThat(context.getValue().getVariable("username")).isEqualTo("kullanıcı");
        assertThat(context.getValue().getVariable("actionUrl"))
                .isEqualTo("https://example/verify?token=secret");
        assertThat(context.getValue().getVariable("baseUrl")).isEqualTo("https://example.com");

        mimeMessage.saveChanges();
        assertThat(mimeMessage.getAllRecipients())
                .extracting(Object::toString)
                .containsExactly("user@example.com");
        assertThat(mimeMessage.getSubject()).isEqualTo("E-posta adresinizi doğrulayın");
        assertThat(mimeMessage.getContent()).isEqualTo("<p>Doğrula</p>");
        assertThat(mimeMessage.getContentType()).contains("text/html").contains("charset=UTF-8");
        verify(mailSender).send(mimeMessage);
    }

    private static ApplicationProperties properties(boolean enabled) {
        return new ApplicationProperties(
                new ApplicationProperties.Cache(
                        new ApplicationProperties.Caffeine(Duration.ofHours(1), 10, 100)),
                new ApplicationProperties.Session("0 * * * * *"),
                new ApplicationProperties.AuthorizationServer("https://issuer.example"),
                new ApplicationProperties.Mail(
                        enabled,
                        "Spring Authorization Server <no-reply@example.com>",
                        "https://example.com"));
    }
}

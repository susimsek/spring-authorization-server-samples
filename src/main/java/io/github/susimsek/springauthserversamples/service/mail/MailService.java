package io.github.susimsek.springauthserversamples.service.mail;

import io.github.susimsek.springauthserversamples.config.ApplicationProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/** Sends localized HTML email without coupling delivery to an HTTP request transaction. */
@Service
public class MailService {

    private static final Logger LOG = LoggerFactory.getLogger(MailService.class);
    private static final String VERIFICATION_TEMPLATE = "mail/emailVerification";
    private static final String PASSWORD_RESET_TEMPLATE = "mail/passwordReset";

    private final ApplicationProperties.Mail properties;
    private final JavaMailSender mailSender;
    private final MessageSource messageSource;
    private final SpringTemplateEngine templateEngine;

    public MailService(
            ApplicationProperties applicationProperties,
            JavaMailSender mailSender,
            MessageSource messageSource,
            SpringTemplateEngine templateEngine) {
        this.properties = applicationProperties.mail();
        this.mailSender = mailSender;
        this.messageSource = messageSource;
        this.templateEngine = templateEngine;
    }

    @Async
    public void sendEmailVerification(
            String recipient, String username, Locale locale, String verificationUrl) {
        sendFromTemplate(
                recipient,
                username,
                locale,
                verificationUrl,
                VERIFICATION_TEMPLATE,
                "mail.verification.subject");
    }

    @Async
    public void sendPasswordReset(
            String recipient, String username, Locale locale, String resetUrl) {
        sendFromTemplate(
                recipient,
                username,
                locale,
                resetUrl,
                PASSWORD_RESET_TEMPLATE,
                "mail.password-reset.subject");
    }

    @Async
    public void sendEmail(String recipient, String subject, String content, boolean html) {
        sendEmailSync(recipient, subject, content, html);
    }

    private void sendFromTemplate(
            String recipient,
            String username,
            Locale locale,
            String actionUrl,
            String templateName,
            String subjectKey) {
        if (!properties.enabled()) {
            LOG.debug("Email delivery is disabled; skipping message to '{}'", recipient);
            return;
        }

        Context context = new Context(locale);
        context.setVariable("username", username);
        context.setVariable("actionUrl", actionUrl);
        context.setVariable("baseUrl", properties.baseUrl());

        String content = templateEngine.process(templateName, context);
        String subject = messageSource.getMessage(subjectKey, null, locale);
        sendEmailSync(recipient, subject, content, true);
    }

    private void sendEmailSync(String recipient, String subject, String content, boolean html) {
        if (!properties.enabled()) {
            LOG.debug("Email delivery is disabled; skipping message to '{}'", recipient);
            return;
        }

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper message =
                    new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());
            message.setTo(recipient);
            message.setFrom(properties.from());
            message.setSubject(subject);
            message.setText(content, html);
            mailSender.send(mimeMessage);
            LOG.debug("Email sent to '{}'", recipient);
        } catch (MailException | MessagingException exception) {
            LOG.warn("Email could not be sent to '{}'", recipient, exception);
        }
    }
}

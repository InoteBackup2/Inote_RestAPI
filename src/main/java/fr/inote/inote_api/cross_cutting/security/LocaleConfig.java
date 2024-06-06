package fr.inote.inote_api.cross_cutting.security;

import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/** Locale configuration
 * 
 * @author FYHenry
 * @date 06/05/2024
 * 
 * <p>Implements automatic localization by <em>Accept-Language</em> HTTP header.</p>
 * <p>The messages resources are in the <em>i18n</em> directory.</p>
 */
@Configuration
public class LocaleConfig {
    @Bean
    public AcceptHeaderLocaleResolver getLocaleResolver() {
        final AcceptHeaderLocaleResolver acceptHeaderLocaleResolver = new AcceptHeaderLocaleResolver();
        acceptHeaderLocaleResolver.setDefaultLocale(Locale.ENGLISH);
        return acceptHeaderLocaleResolver;
    }

    @Bean
    public ResourceBundleMessageSource getMessageSource() {
        final ResourceBundleMessageSource resourceBundleMessageSource = new ResourceBundleMessageSource();
        resourceBundleMessageSource.setBasename("i18n/messages");
        resourceBundleMessageSource.setDefaultEncoding("UTF-8");
        return resourceBundleMessageSource;
    }
}

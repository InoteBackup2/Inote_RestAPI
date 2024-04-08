package fr.inote.inoteApi.service.impl;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.assertThat;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;

import fr.inote.inoteApi.crossCutting.exceptions.InoteInvalidEmailException;
import fr.inote.inoteApi.crossCutting.exceptions.InoteUserException;
import fr.inote.inoteApi.entity.User;
import fr.inote.inoteApi.entity.Validation;
import javax.mail.internet.MimeMessage;

import lombok.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@Tag("Service tests")
@ExtendWith(MockitoExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class NotificationServiceImplTest {

        @RegisterExtension
        public
        static final GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
                        .withConfiguration(GreenMailConfiguration.aConfig().withUser("duke", "springboot"))
                        .withPerMethodLifecycle(false);

        /* Dependencies */
        final JavaMailSender javaMailSender;

        @Mock
        private NotificationServiceImpl mockedNotificationService;

        @InjectMocks
        private NotificationServiceImpl notificationService;

        @Autowired
        public NotificationServiceImplTest(NotificationServiceImpl notificationService, JavaMailSender javaMailSender) {
                this.notificationService = notificationService;
                this.javaMailSender = javaMailSender;
        }

        /* Dependencies injections */
        public final String SENDER_EMAIL = "sangoku@kame-house.jp";
        public final String RECEIVER_EMAIL = "kaio@kaio.uni";
        public final String SUBJECT_OF_EMAIL = "Well arrived";
        public final String BODY_OF_EMAIL = "We are arrived";

        @BeforeEach
        void setUp() throws FolderException{
                NotificationServiceImplTest.greenMail.purgeEmailFromAllMailboxes();
        }

        @Test
        @DisplayName("Sending email with good parameters")
        public void sendEmail_ShouldSuccess_withGoodParams() throws NoSuchMethodException, SecurityException {

                /* Access to private method with java reflexion */
                Method privateMethod_sendEmail = getPrivateMethodSendEmail();

                assertThatCode(() -> privateMethod_sendEmail.invoke(this.notificationService,
                                SENDER_EMAIL, RECEIVER_EMAIL, SUBJECT_OF_EMAIL, BODY_OF_EMAIL)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Mocking smtp server with GreenMail")
        void test_shouldSuccess_WhenMockingSmtpServerWithGreenmail() throws NoSuchMethodException, SecurityException {

                /* Access to private method with java reflexion */
                Method privateMethod_sendEmail = getPrivateMethodSendEmail();

                assertThatCode(() -> privateMethod_sendEmail.invoke(this.notificationService,
                                SENDER_EMAIL, RECEIVER_EMAIL, SUBJECT_OF_EMAIL, BODY_OF_EMAIL)).doesNotThrowAnyException();

                await()
                                .atMost(2, SECONDS)
                                .untilAsserted(() -> {
                                        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
                                        assertThat(receivedMessages.length).isEqualTo(1);

                                        MimeMessage receivedMessage = receivedMessages[0];
                                        assertThat(GreenMailUtil.getBody(receivedMessage)).contains(BODY_OF_EMAIL);

                                        assertThat(receivedMessage.getAllRecipients().length).isEqualTo(1);
                                        assertThat(receivedMessage.getAllRecipients()[0].toString())
                                                        .isEqualTo(RECEIVER_EMAIL);
                                });
        }

        @NonNull
        private static Method getPrivateMethodSendEmail() throws NoSuchMethodException {
                Method privateMethod_sendEmail = NotificationServiceImpl.class
                                .getDeclaredMethod("sendEmail",
                                                String.class,
                                                String.class,
                                                String.class,
                                                String.class);

                privateMethod_sendEmail.setAccessible(true);
                return privateMethod_sendEmail;
        }

        @Test
        @DisplayName("Sending email with malFormated address")
        public void sendEmail_ShouldFail_withMalFormatedAddress() throws NoSuchMethodException, SecurityException,
                IllegalArgumentException {

                /* Access to private method with java reflexion */
                Method privateMethod_sendEmail = getPrivateMethodSendEmail();

                // with 2 @
                Throwable thrown = catchThrowable(() -> privateMethod_sendEmail.invoke(this.notificationService,
                                "freezer@@2713487876876767.uni",
                                RECEIVER_EMAIL,
                                SUBJECT_OF_EMAIL,
                                BODY_OF_EMAIL));
                assertThat(thrown)
                                .isInstanceOf(InvocationTargetException.class)
                                .hasCauseInstanceOf(InoteInvalidEmailException.class);

                thrown = catchThrowable(() -> privateMethod_sendEmail.invoke(this.notificationService,
                                SENDER_EMAIL,
                                "freezer@@2713487876876767.uni",
                                SUBJECT_OF_EMAIL,
                                BODY_OF_EMAIL));
                assertThat(thrown)
                                .isInstanceOf(InvocationTargetException.class)
                                .hasCauseInstanceOf(InoteInvalidEmailException.class);

                // With Space
                thrown = catchThrowable(() -> privateMethod_sendEmail.invoke(this.notificationService,
                                SENDER_EMAIL,
                                "maitre kaio@gmail.com",
                                SUBJECT_OF_EMAIL,
                                BODY_OF_EMAIL));

                assertThat(thrown)
                                .isInstanceOf(InvocationTargetException.class)
                                .hasCauseInstanceOf(InoteInvalidEmailException.class);
        }

        @Test
        @DisplayName("Sending activation code by email with good informations")
        public void sendValidation_byEmail_shouldSuccess_whenParametersAreGood() {

                User userTest = User.builder()
                                .email(RECEIVER_EMAIL)
                                .name("Sangoku")
                                .password("1234")
                                .build();

                Validation validationTest = Validation.builder()
                                .activation(null)
                                .code("123456")
                                .creation(Instant.now())
                                .expiration(Instant.now().plus(10, ChronoUnit.MINUTES))
                                .user(userTest)
                                .build();

                assertThatCode(() -> this.notificationService.sendValidation_byEmail(validationTest))
                                .doesNotThrowAnyException();

                await()
                                .atMost(2, SECONDS)
                                .untilAsserted(() -> {
                                        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
                                        assertThat(receivedMessages.length).isEqualTo(1);

                                        MimeMessage receivedMessage = receivedMessages[0];
                                        assertThat(GreenMailUtil.getBody(receivedMessage))
                                                        .contains("Inote notification service");

                                        assertThat(receivedMessage.getAllRecipients().length).isEqualTo(1);
                                        assertThat(receivedMessage.getAllRecipients()[0].toString())
                                                        .isEqualTo(RECEIVER_EMAIL);
                                });
        }

        @Test
        @DisplayName("Sending activation code by email with bad parameters")
        public void sendValidation_byEmail_shouldFail_whenParametersAreBad() {

                User userTest = User.builder()
                                .email("sangoku @ inote.fr")
                                .name("Sangoku")
                                .password("1234")
                                .build();

                Validation validationTest = Validation.builder()
                                .activation(null)
                                .code("123456")
                                .creation(Instant.now())
                                .expiration(Instant.now().plus(10, ChronoUnit.MINUTES))
                                .user(userTest)
                                .build();

                Throwable thrown = catchThrowable(() -> this.notificationService.sendValidation_byEmail(validationTest));

                assertThat(thrown).isInstanceOf(InoteInvalidEmailException.class);
        }

}

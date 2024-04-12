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
import fr.inote.inoteApi.entity.User;
import fr.inote.inoteApi.entity.Validation;
import fr.inote.inoteApi.service.NotificationService;

import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

/*
 * The @ActiveProfiles annotation in Spring is used to declare which active bean
 * definition profiles
 * should be used when loading an ApplicationContext for test classes
 */
@ActiveProfiles("test")

/* Add Mockito functionalities to Junit 5 */
@ExtendWith(MockitoExtension.class)

/* Load entire context app */
@SpringBootTest
public class NotificationServiceImplTest {

        /* JUNIT EXTENSIONS */
        /* ============================================================ */
        @RegisterExtension
        public static final GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
                        .withConfiguration(GreenMailConfiguration.aConfig().withUser("duke", "springboot"))
                        .withPerMethodLifecycle(false);

        /* DEPENDENCIES MOCKING */
        /* ============================================================ */
        /* @Mock create and inject mocked instances of classes */
        /* Dependencies */
        @Mock
        private NotificationServiceImpl mockedNotificationService;

        /* DEPENDENCIES INJECTION */
        /* ============================================================ */
        /*
         * @InjectMocks instance class to be tested and automatically inject mock fields
         * Nota : if service is an interface, instanciate implementation withs mocks in
         * params
         */
        
        private JavaMailSender javaMailSender;

        @InjectMocks
        private NotificationService notificationService = new NotificationServiceImpl(javaMailSender);

        // Constructor injection
        @Autowired
        public NotificationServiceImplTest(NotificationService notificationService, JavaMailSender javaMailSender) {
                this.notificationService = notificationService;
                this.javaMailSender = javaMailSender;
        }

        /* CONSTANTS */
        /* ============================================================ */
        public final String SENDER_EMAIL = "sangoku@kame-house.jp";
        public final String RECEIVER_EMAIL = "kaio@kaio.uni";
        public final String SUBJECT_OF_EMAIL = "Well arrived";
        public final String BODY_OF_EMAIL = "We are arrived";

        /* FIXTURES */
        /* ============================================================ */
        @BeforeEach
        void setUp() throws FolderException {
                NotificationServiceImplTest.greenMail.purgeEmailFromAllMailboxes();
        }

        /* SERVICE UNIT TESTS */
        /* ============================================================ */
        @Test
        @DisplayName("Sending email with good parameters")
        public void sendEmail_ShouldSuccess_withGoodParams() throws NoSuchMethodException, SecurityException {

                /* Arrange */
                // Access to private method with java reflexion
                Method privateMethod_sendEmail = getPrivateMethodSendEmail();

                /* Act & assert */
                assertThatCode(() -> privateMethod_sendEmail.invoke(this.notificationService,
                                SENDER_EMAIL, RECEIVER_EMAIL, SUBJECT_OF_EMAIL, BODY_OF_EMAIL))
                                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Mocking smtp server with GreenMail")
        void test_shouldSuccess_WhenMockingSmtpServerWithGreenmail() throws NoSuchMethodException, SecurityException {

                /* Arrange */
                // Access to private method with java reflexion
                Method privateMethod_sendEmail = getPrivateMethodSendEmail();

                /* Act & assert */
                assertThatCode(() -> privateMethod_sendEmail.invoke(this.notificationService,
                                SENDER_EMAIL, RECEIVER_EMAIL, SUBJECT_OF_EMAIL, BODY_OF_EMAIL))
                                .doesNotThrowAnyException();

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

        @Test
        @DisplayName("Sending email with malFormated address")
        public void sendEmail_ShouldFail_withMalFormatedAddress() throws NoSuchMethodException, SecurityException,
                        IllegalArgumentException {

                /* Arrange */
                Method privateMethod_sendEmail = getPrivateMethodSendEmail();

                /* Act & assert */

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

                /* Arrange */
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

                /* Act & assert */
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
                /* Arrange */
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
                /* Act */
                Throwable thrown = catchThrowable(
                                () -> this.notificationService.sendValidation_byEmail(validationTest));

                /* Assert */
                assertThat(thrown).isInstanceOf(InoteInvalidEmailException.class);
        }

        /* UTILS */
        /* ============================================================ */
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
}

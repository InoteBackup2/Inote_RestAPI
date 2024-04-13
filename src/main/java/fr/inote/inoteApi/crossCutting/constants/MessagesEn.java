package fr.inote.inoteApi.crossCutting.constants;

public class MessagesEn {
    // Auth
    public final static String REGISTER_OK_MAIL_SENDED = "Your Inote account has been created. Before you can use it, you need to activate it via the e-mail we've just sent you.";
    public final static String REGISTER_ERROR_USER_ALREADY_EXISTS = "Account creation impossible. A user with this email address is already registered.";

    // Activation
    public final static String ACTIVATION_NEED_ACTIVATION = """
            Inote Notification Service
                        
            Your request was successful. To complete the operation, please retrieve the activation code sent to your mailbox.
                        
            Wishing you a good day.
            """;

    // Email
    public final static String EMAIL_ERROR_INVALID_EMAIL_FORMAT = "Invalid email format.";
    public final static String EMAIL_ERROR_INVALID_PASSWORD_FORMAT = "The password provided does not comply with security rules.";

    public final static String EMAIL_SUBJECT_ACTIVATION_CODE = "Subject : Activation code";

    // Validations
    public static final String VALIDATION_ERROR_NOT_FOUND = "The Validation was not found in database";
    public static final String VALIDATION_ERROR_VALIDATION_IS_EXPIRED = "The validation is expired";

    // USER
    public static final String USER_ERROR_USER_NOT_FOUND = "User not found in database";
    public static final String ACTIVATION_OF_USER_OK = "User activation succeed";
    public static final String USER_SIGNOUT_SUCCESS = "User deconnection success";

    //ROLES
    public static final String ROLE_ERROR_NOT_FOUND = "The asked role doesn't exists in database";
    public static final String NEW_PASSWORD_SUCCESS = "The new password user affectation has been success";

    // TOKEN
    public static final String TOKEN_ERROR_NOT_FOUND = "Token was not found";
    public static final String TOKEN_ERROR_REFRESH_TOKEN_EXPIRED = "The refresh token is expired";

    // COMMENT
    public static final String COMMENT_ERROR_MESSAGE_IS_EMPTY = "The comment you wish to post contains no message.";
    public static final String EMAIL_ERROR_POSSIBLE_SMTP_SERVEUR_NOT_CONFIGURED = """
            The email could not be sent.
            It's very likely that the smtp server is misconfigured. 
            ==> If you are in dev mode, have you started "
            smtpServer_simul_run.sh" (located in the root folder of the complete project)?
                """;
}

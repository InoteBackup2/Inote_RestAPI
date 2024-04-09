package fr.inote.inoteApi.crossCutting.constants;

public class RegexPatterns {
    public static final String REGEX_EMAIL_PATTERN = "[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";

    /*
     * - at least 8 characters
     * - must contain at least 1 uppercase letter, 1 lowercase letter, and 1 number
     * - Can contain special characters
     */
    public static final String REGEX_PASSWORD_FORMAT = "((?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[\\W]).{8,64})";
}



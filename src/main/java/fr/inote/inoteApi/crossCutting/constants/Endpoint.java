package fr.inote.inoteApi.crossCutting.constants;

public class Endpoint {
    public static final String REGISTER = "/api/auth/register";
    public static final String ACTIVATION = "/api/auth/activation";
    public static final String SIGN_IN = "/api/auth/sign_in";
    public static final String CHANGE_PASSWORD = "/api/auth/change_password";
    public static final String NEW_PASSWORD = "/api/auth/new_password";
    public static final String REFRESH_TOKEN = "/api/auth/refresh_token";
    public static final String SIGN_OUT = "/api/auth/sign_out";
    public static final String CREATE_COMMENT = "/api/comment/create";
    public static final String COMMENT_GET_ALL = "/api/comments";
    public static final String GET_CURRENT_USER = "/api/auth/current-user";
}

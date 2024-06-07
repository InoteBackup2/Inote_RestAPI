package fr.inote.inote_api.cross_cutting.exceptions;

import java.util.Locale;

import org.springframework.context.support.ResourceBundleMessageSource;

public class InoteExistingEmailException extends Exception{
        
    public InoteExistingEmailException(){
        super();
    }

    @Override
    public String getLocalizedMessage(){
        final String source = new ResourceBundleMessageSource().getMessage(
        "auth.REGISTER_ERROR_USER_ALREADY_EXISTS",
            new Object[]{},
            Locale.getDefault()
        );
        return source;
    }

}

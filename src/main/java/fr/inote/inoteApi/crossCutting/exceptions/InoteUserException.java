package fr.inote.inoteApi.crossCutting.exceptions;

public class InoteUserException extends Exception {
    public InoteUserException(String message) {
        super("Inote anomaly detected");
    }
}

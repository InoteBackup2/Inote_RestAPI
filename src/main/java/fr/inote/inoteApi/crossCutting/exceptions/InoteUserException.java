package fr.inote.inoteApi.crossCutting.exceptions;

public class InoteUserException extends Exception {
    public InoteUserException() {
        super("Inote anomaly detected");
    }
}

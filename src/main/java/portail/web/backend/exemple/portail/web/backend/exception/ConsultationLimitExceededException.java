package portail.web.backend.exemple.portail.web.backend.exception;

public class ConsultationLimitExceededException extends RuntimeException {
    public ConsultationLimitExceededException(String message) {
        super(message);
    }
}

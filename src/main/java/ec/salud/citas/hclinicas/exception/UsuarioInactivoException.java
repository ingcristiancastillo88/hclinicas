package ec.salud.citas.hclinicas.exception;

public class UsuarioInactivoException extends RuntimeException {
    public UsuarioInactivoException(String mensaje) { super(mensaje); }
}

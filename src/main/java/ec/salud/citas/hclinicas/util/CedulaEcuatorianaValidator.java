package ec.salud.citas.hclinicas.util;

/**
 * Validador de cédula ecuatoriana.
 * Implementa el algoritmo oficial del Registro Civil del Ecuador.
 * Requisito especial del CU-003 y HU-011.
 *
 * Algoritmo:
 * 1. La cédula tiene 10 dígitos.
 * 2. Los dos primeros dígitos son el código de provincia (01-24).
 * 3. El tercer dígito debe ser menor a 6.
 * 4. Se aplica el algoritmo de módulo 10 sobre los 9 primeros dígitos.
 * 5. El resultado debe coincidir con el dígito verificador (posición 10).
 */
public class CedulaEcuatorianaValidator {

    private CedulaEcuatorianaValidator() {
        // Clase utilitaria, no instanciar
    }

    /**
     * Valida si una cédula ecuatoriana es válida.
     *
     * @param cedula número de cédula (10 dígitos numéricos)
     * @return true si es válida, false en caso contrario
     */
    public static boolean esValida(String cedula) {
        if (cedula == null || cedula.isBlank()) {
            return false;
        }

        // Limpiar espacios
        cedula = cedula.trim();

        // Debe tener exactamente 10 dígitos numéricos
        if (!cedula.matches("\\d{10}")) {
            return false;
        }

        // Validar código de provincia (01-24)
        int provincia = Integer.parseInt(cedula.substring(0, 2));
        if (provincia < 1 || provincia > 24) {
            return false;
        }

        // El tercer dígito debe ser < 6 (persona natural)
        int tercerDigito = Integer.parseInt(String.valueOf(cedula.charAt(2)));
        if (tercerDigito >= 6) {
            return false;
        }

        // Algoritmo módulo 10
        int[] coeficientes = {2, 1, 2, 1, 2, 1, 2, 1, 2};
        int suma = 0;

        for (int i = 0; i < 9; i++) {
            int valor = Integer.parseInt(String.valueOf(cedula.charAt(i))) * coeficientes[i];
            if (valor >= 10) {
                valor -= 9;
            }
            suma += valor;
        }

        int digitoVerificadorCalculado = suma % 10 == 0 ? 0 : 10 - (suma % 10);
        int digitoVerificadorReal = Integer.parseInt(String.valueOf(cedula.charAt(9)));

        return digitoVerificadorCalculado == digitoVerificadorReal;
    }
}

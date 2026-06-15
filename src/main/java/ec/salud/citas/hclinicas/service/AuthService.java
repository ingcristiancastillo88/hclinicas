package ec.salud.citas.hclinicas.service;

import ec.salud.citas.hclinicas.dto.LoginRequest;
import ec.salud.citas.hclinicas.dto.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request, String ipOrigen);
}
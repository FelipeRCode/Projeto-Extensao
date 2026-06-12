package com.erp.controller;

import com.erp.model.Usuario;
import com.erp.repository.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UsuarioRepository usuarioRepository;

    public AuthController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // POST /api/auth/login — valida e-mail e senha no banco
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String senha = body.get("senha");

        if (email == null || senha == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("sucesso", false, "mensagem", "E-mail e senha são obrigatórios."));
        }

        Optional<Usuario> usuario = usuarioRepository.findByEmailAndSenhaAndAtivoTrue(email, senha);

        if (usuario.isPresent()) {
            Usuario u = usuario.get();
            return ResponseEntity.ok(Map.of(
                    "sucesso", true,
                    "id", u.getId(),
                    "nome", u.getNome() != null ? u.getNome() : "",
                    "nivelAcesso", u.getNivelAcesso().name()
            ));
        } else {
            return ResponseEntity.status(401)
                    .body(Map.of("sucesso", false, "mensagem", "E-mail ou senha incorretos."));
        }
    }
}

package com.erp.controller;

import com.erp.model.Notificacao;
import com.erp.model.Usuario;
import com.erp.repository.NotificacaoRepository;
import com.erp.repository.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/senha")
@CrossOrigin(origins = "*")
public class SenhaController {

    private final UsuarioRepository usuarioRepository;
    private final NotificacaoRepository notificacaoRepository;

    public SenhaController(UsuarioRepository usuarioRepository,
                           NotificacaoRepository notificacaoRepository) {
        this.usuarioRepository     = usuarioRepository;
        this.notificacaoRepository = notificacaoRepository;
    }

    // POST /api/senha/esqueci — cria notificação para o super admin redefinir a senha
    @PostMapping("/esqueci")
    public ResponseEntity<Map<String, Object>> esqueci(@RequestBody Map<String, String> body) {
        String email = body.get("email");

        Optional<Usuario> usuarioOpt = (email == null) ? Optional.empty()
                                                        : usuarioRepository.findByEmail(email);
        if (usuarioOpt.isPresent()) {
            Usuario u = usuarioOpt.get();
            if (!notificacaoRepository.existsByUsuario_IdAndResolvidaFalse(u.getId())) {
                String msg = u.getNome() + " (" + u.getEmail() + ") solicitou redefinição de senha.";
                notificacaoRepository.save(new Notificacao(u, msg));
            }
        }

        return ResponseEntity.ok(Map.of(
            "sucesso",  true,
            "mensagem", "Sua solicitação foi enviada ao administrador. Ele irá redefinir sua senha em breve."
        ));
    }
}

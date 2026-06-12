package com.erp.controller;

import com.erp.model.NivelAcesso;
import com.erp.model.Usuario;
import com.erp.repository.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;

    public UsuarioController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // GET /api/usuarios — lista todos os usuários (senha omitida via @JsonIgnore no model)
    @GetMapping
    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    // POST /api/usuarios — cria novo usuário
    @PostMapping
    public ResponseEntity<Map<String, Object>> criar(@RequestBody Map<String, String> body) {
        String nome  = body.get("nome");
        String email = body.get("email");
        String senha = body.get("senha");
        String nivel = body.get("nivelAcesso");

        if (nome == null || nome.isBlank() || email == null || email.isBlank() || senha == null || senha.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("sucesso", false, "mensagem", "Nome, e-mail e senha são obrigatórios."));
        }

        if (usuarioRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("sucesso", false, "mensagem", "E-mail já cadastrado."));
        }

        NivelAcesso nivelAcesso = NivelAcesso.FUNCIONARIO;
        if (nivel != null) {
            try {
                NivelAcesso parsed = NivelAcesso.valueOf(nivel);
                // Não permite criar SUPER_ADMIN via esta rota — rebaixa para FUNCIONARIO
                nivelAcesso = (parsed == NivelAcesso.SUPER_ADMIN) ? NivelAcesso.FUNCIONARIO : parsed;
            } catch (IllegalArgumentException ignored) {}
        }

        Usuario u = new Usuario();
        u.setNome(nome);
        u.setEmail(email);
        u.setSenha(senha);
        u.setAtivo(true);
        u.setNivelAcesso(nivelAcesso);

        usuarioRepository.save(u);
        return ResponseEntity.ok(Map.of("sucesso", true, "mensagem", "Usuário criado com sucesso."));
    }

    // PUT /api/usuarios/{id} — atualiza dados do usuário
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> atualizar(@PathVariable Integer id,
                                                          @RequestBody Map<String, String> body) {
        Optional<Usuario> opt = usuarioRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Usuario u = opt.get();

        if (u.getNivelAcesso() == NivelAcesso.SUPER_ADMIN) {
            return ResponseEntity.badRequest()
                    .body(Map.of("sucesso", false, "mensagem", "Não é possível editar o Super Admin por aqui."));
        }

        if (body.containsKey("nome") && !body.get("nome").isBlank()) {
            u.setNome(body.get("nome"));
        }
        if (body.containsKey("email") && !body.get("email").isBlank()) {
            String novoEmail = body.get("email");
            Optional<Usuario> existente = usuarioRepository.findByEmail(novoEmail);
            if (existente.isPresent() && !existente.get().getId().equals(id)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("sucesso", false, "mensagem", "E-mail já cadastrado para outro usuário."));
            }
            u.setEmail(novoEmail);
        }
        if (body.containsKey("senha") && !body.get("senha").isBlank()) {
            u.setSenha(body.get("senha"));
        }
        if (body.containsKey("nivelAcesso")) {
            try {
                NivelAcesso nivel = NivelAcesso.valueOf(body.get("nivelAcesso"));
                if (nivel != NivelAcesso.SUPER_ADMIN) {
                    u.setNivelAcesso(nivel);
                }
            } catch (IllegalArgumentException ignored) {}
        }

        usuarioRepository.save(u);
        return ResponseEntity.ok(Map.of("sucesso", true, "mensagem", "Usuário atualizado com sucesso."));
    }

    // PUT /api/usuarios/{id}/status — bloqueia ou desbloqueia
    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> alterarStatus(@PathVariable Integer id,
                                                               @RequestBody Map<String, Object> body) {
        Optional<Usuario> opt = usuarioRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Usuario u = opt.get();
        if (u.getNivelAcesso() == NivelAcesso.SUPER_ADMIN) {
            return ResponseEntity.badRequest()
                    .body(Map.of("sucesso", false, "mensagem", "Não é possível bloquear o Super Admin."));
        }

        Boolean ativo = (Boolean) body.get("ativo");
        u.setAtivo(ativo != null ? ativo : true);
        usuarioRepository.save(u);

        String acao = Boolean.TRUE.equals(u.getAtivo()) ? "desbloqueado" : "bloqueado";
        return ResponseEntity.ok(Map.of("sucesso", true, "mensagem", "Usuário " + acao + " com sucesso."));
    }

    // DELETE /api/usuarios/{id} — exclui usuário
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> excluir(@PathVariable Integer id) {
        Optional<Usuario> opt = usuarioRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Usuario u = opt.get();
        if (u.getNivelAcesso() == NivelAcesso.SUPER_ADMIN) {
            return ResponseEntity.badRequest()
                    .body(Map.of("sucesso", false, "mensagem", "Não é possível excluir o Super Admin."));
        }

        usuarioRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("sucesso", true, "mensagem", "Usuário excluído com sucesso."));
    }
}

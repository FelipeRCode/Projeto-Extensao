package com.erp.controller;

// Controller — notificações de redefinição de senha (para o super admin)
import com.erp.model.Notificacao;
import com.erp.model.Usuario;
import com.erp.repository.NotificacaoRepository;
import com.erp.repository.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/notificacoes")
@CrossOrigin(origins = "*")
public class NotificacaoController {

    private final NotificacaoRepository notifRepo;
    private final UsuarioRepository usuarioRepo;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public NotificacaoController(NotificacaoRepository notifRepo, UsuarioRepository usuarioRepo) {
        this.notifRepo   = notifRepo;
        this.usuarioRepo = usuarioRepo;
    }

    // GET /api/notificacoes — lista as solicitações pendentes
    @GetMapping
    public List<Map<String, Object>> listar() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Notificacao n : notifRepo.findByResolvidaFalseOrderByDataDesc()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",        n.getId());
            m.put("usuarioId", n.getUsuario().getId());
            m.put("nome",      n.getUsuario().getNome());
            m.put("email",     n.getUsuario().getEmail());
            m.put("mensagem",  n.getMensagem());
            m.put("data",      n.getData().format(FMT));
            result.add(m);
        }
        return result;
    }

    // GET /api/notificacoes/contagem — quantidade de pendentes (badge do sino)
    @GetMapping("/contagem")
    public Map<String, Object> contagem() {
        return Map.of("pendentes", notifRepo.countByResolvidaFalse());
    }

    // PUT /api/notificacoes/{id}/redefinir — super admin define a nova senha e resolve
    @PutMapping("/{id}/redefinir")
    public ResponseEntity<Map<String, Object>> redefinir(@PathVariable Integer id,
                                                         @RequestBody Map<String, String> body) {
        Optional<Notificacao> opt = notifRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        String novaSenha = body.get("novaSenha");
        if (novaSenha == null || novaSenha.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("sucesso", false, "mensagem", "Informe a nova senha."));
        }

        Notificacao n = opt.get();
        Usuario u = n.getUsuario();
        u.setSenha(novaSenha);
        usuarioRepo.save(u);

        n.setResolvida(true);
        notifRepo.save(n);

        return ResponseEntity.ok(Map.of("sucesso", true,
                "mensagem", "Senha de " + u.getNome() + " redefinida com sucesso."));
    }

    // PUT /api/notificacoes/{id}/ignorar — dispensa a solicitação sem trocar a senha
    @PutMapping("/{id}/ignorar")
    public ResponseEntity<Map<String, Object>> ignorar(@PathVariable Integer id) {
        Optional<Notificacao> opt = notifRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Notificacao n = opt.get();
        n.setResolvida(true);
        notifRepo.save(n);

        return ResponseEntity.ok(Map.of("sucesso", true, "mensagem", "Solicitação dispensada."));
    }
}

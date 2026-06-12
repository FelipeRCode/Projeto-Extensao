package com.erp.controller;

// Controller — CRUD de Perfumes
import com.erp.model.Perfume;
import com.erp.service.PerfumeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/perfumes")
public class PerfumeController {

    private final PerfumeService service;

    public PerfumeController(PerfumeService service) {
        this.service = service;
    }

    // GET /api/perfumes — lista todos ou busca por termo
    @GetMapping
    public List<Perfume> listar(@RequestParam(required = false) String q) {
        return service.buscar(q);
    }

    // GET /api/perfumes/metricas — cards do topo (total, valor, estoque baixo)
    @GetMapping("/metricas")
    public Map<String, Object> metricas() {
        return service.metricas();
    }

    // GET /api/perfumes/estoque-critico — perfumes com estoque abaixo do limite
    @GetMapping("/estoque-critico")
    public List<Perfume> estoqueCritico(@RequestParam(defaultValue = "3") int limite) {
        return service.estoqueCritico(limite);
    }

    // GET /api/perfumes/{id} — busca perfume por ID
    @GetMapping("/{id}")
    public Perfume buscarPorId(@PathVariable Integer id) {
        return service.buscarPorId(id);
    }

    // POST /api/perfumes — cadastra novo perfume
    @PostMapping
    public ResponseEntity<Perfume> cadastrar(@Valid @RequestBody Perfume perfume) {
        return ResponseEntity.ok(service.cadastrar(perfume));
    }

    // PUT /api/perfumes/{id} — atualiza dados do perfume
    @PutMapping("/{id}")
    public ResponseEntity<Perfume> atualizar(@PathVariable Integer id,
                                              @Valid @RequestBody Perfume dados) {
        return ResponseEntity.ok(service.atualizar(id, dados));
    }

    // DELETE /api/perfumes/{id} — exclui perfume (bloqueado se tiver vendas)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Integer id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}

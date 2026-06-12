package com.erp.controller;

// Controller — operações de Vendas
import com.erp.model.Venda;
import com.erp.service.VendaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vendas")
@CrossOrigin(origins = "*")
public class VendaController {

    private final VendaService service;

    public VendaController(VendaService service) {
        this.service = service;
    }

    // POST /api/vendas — registra nova venda
    @PostMapping
    public ResponseEntity<Venda> registrar(@RequestBody VendaService.VendaRequest req) {
        return ResponseEntity.ok(service.registrarVenda(req));
    }

    // GET /api/vendas/ultimas — últimas 20 vendas
    @GetMapping("/ultimas")
    public List<Venda> ultimas() {
        return service.ultimasVendas();
    }

    // GET /api/vendas/metricas — cards do topo (total do dia, ticket médio)
    @GetMapping("/metricas")
    public Map<String, Object> metricas() {
        return service.metricasHoje();
    }

    // GET /api/vendas/relatorio — relatório de lucratividade.
    // Com ?de=&ate= filtra pelo período (function); sem datas usa a view (todas as vendas).
    @GetMapping("/relatorio")
    public List<Map<String, Object>> relatorio(@RequestParam(required = false) String de,
                                               @RequestParam(required = false) String ate) {
        return service.relatorioLucratividade(de, ate);
    }

    // GET /api/vendas/cliente/{id} — histórico e total gasto por cliente
    @GetMapping("/cliente/{id}")
    public ResponseEntity<Map<String, Object>> resumoCliente(@PathVariable Integer id) {
        return ResponseEntity.ok(service.resumoCliente(id));
    }

    // DELETE /api/vendas/{id} — exclui venda e restaura estoque
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Integer id) {
        service.excluirVenda(id);
        return ResponseEntity.noContent().build();
    }

    // PUT /api/vendas/{id} — edita forma de pagamento, quantidade e cliente
    @PutMapping("/{id}")
    public ResponseEntity<Venda> editar(@PathVariable Integer id,
                                        @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(service.editarVenda(id, body));
    }
}

package com.erp.controller;

// Controller — CRUD de Clientes
import com.erp.model.Cliente;
import com.erp.repository.ClienteRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "*")
public class ClienteController {

    private final ClienteRepository clienteRepository;

    public ClienteController(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    // GET /api/clientes — lista todos os clientes
    @GetMapping
    public List<Cliente> listar() {
        return clienteRepository.findAll();
    }

    // GET /api/clientes/{id} — busca cliente por ID
    @GetMapping("/{id}")
    public ResponseEntity<Cliente> buscarPorId(@PathVariable Integer id) {
        return clienteRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/clientes — cadastra novo cliente
    @PostMapping
    public ResponseEntity<Cliente> criar(@Valid @RequestBody Cliente cliente) {
        if (cliente.getNome() == null || cliente.getNome().isBlank())
            return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(clienteRepository.save(cliente));
    }

    // PUT /api/clientes/{id} — atualiza dados do cliente
    @PutMapping("/{id}")
    public ResponseEntity<Cliente> atualizar(@PathVariable Integer id, @Valid @RequestBody Cliente dados) {
        Optional<Cliente> opt = clienteRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Cliente cliente = opt.get();
        cliente.setNome(dados.getNome());
        cliente.setTelefone(dados.getTelefone());
        cliente.setEmail(dados.getEmail());
        return ResponseEntity.ok(clienteRepository.save(cliente));
    }

    // DELETE /api/clientes/{id} — exclui cliente
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Integer id) {
        if (!clienteRepository.existsById(id)) return ResponseEntity.notFound().build();
        clienteRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

package com.erp.repository;

// Repository — operações de banco para Clientes
import com.erp.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    // Busca clientes pelo nome (ignorando maiúsculas/minúsculas)
    List<Cliente> findByNomeContainingIgnoreCase(String nome);
}

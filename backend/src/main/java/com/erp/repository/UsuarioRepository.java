package com.erp.repository;

// Repository — operações de banco para Usuarios
import com.erp.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    // Busca usuário ativo por e-mail e senha (usado no login)
    Optional<Usuario> findByEmailAndSenhaAndAtivoTrue(String email, String senha);

    // Busca usuário por e-mail (usado na recuperação de senha)
    Optional<Usuario> findByEmail(String email);
}

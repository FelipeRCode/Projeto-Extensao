package com.erp.repository;

// Repository — notificações de redefinição de senha
import com.erp.model.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificacaoRepository extends JpaRepository<Notificacao, Integer> {

    // Notificações pendentes (não resolvidas), mais recentes primeiro
    List<Notificacao> findByResolvidaFalseOrderByDataDesc();

    // Quantidade de pendentes (usado no badge do sino)
    long countByResolvidaFalse();

    // Evita criar duas solicitações pendentes para o mesmo usuário
    boolean existsByUsuario_IdAndResolvidaFalse(Integer usuarioId);
}

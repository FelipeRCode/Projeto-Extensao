package com.erp.repository;

// Repository — operações de banco para Vendas
import com.erp.model.Venda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VendaRepository extends JpaRepository<Venda, Integer> {

    // Vendas dentro de um período (usado para métricas do dia)
    @Query("SELECT v FROM Venda v WHERE v.dataVenda >= :inicio AND v.dataVenda <= :fim ORDER BY v.dataVenda DESC")
    List<Venda> findByPeriodo(LocalDateTime inicio, LocalDateTime fim);

    // Últimas vendas com cliente e itens carregados
    @Query("SELECT DISTINCT v FROM Venda v LEFT JOIN FETCH v.cliente LEFT JOIN FETCH v.itens ORDER BY v.dataVenda DESC")
    List<Venda> findUltimasJpql();

    // Relatório de lucratividade via view do banco (todas as vendas)
    @Query(value = "SELECT * FROM v_Lucratividade", nativeQuery = true)
    List<Object[]> obterRelatorioLucratividade();

    // Relatório de lucratividade filtrado por período, via function que retorna tabela
    @Query(value = "SELECT * FROM fc_LucratividadePeriodo(:de, :ate)", nativeQuery = true)
    List<Object[]> obterLucratividadePeriodo(@Param("de") java.time.LocalDateTime de,
                                             @Param("ate") java.time.LocalDateTime ate);

    // Histórico de compras de um cliente específico
    @Query("SELECT v FROM Venda v LEFT JOIN FETCH v.itens i LEFT JOIN FETCH i.perfume WHERE v.cliente.id = :clienteId ORDER BY v.dataVenda DESC")
    List<Venda> findByClienteId(Integer clienteId);
}

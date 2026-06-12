package com.erp.repository;

// Repository — operações de banco para Perfumes
import com.erp.model.Perfume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PerfumeRepository extends JpaRepository<Perfume, Integer> {

    // REQUISITO (5.2): consulta usando a FUNCTION que retorna tabela (fn_PerfumesEstoqueCritico),
    // chamada aqui pelo Java. Retorna os perfumes com estoque abaixo/igual ao limite.
    @Query(value = "SELECT * FROM fc_PerfumesEstoqueCritico(:limite) ORDER BY estoque_atual ASC",
           nativeQuery = true)
    List<Perfume> buscarEstoqueCritico(@Param("limite") Integer limite);

    // Busca por nome ou marca
    @Query("SELECT p FROM Perfume p WHERE LOWER(p.nome) LIKE LOWER(CONCAT('%', :termo, '%')) " +
           "OR LOWER(p.marca) LIKE LOWER(CONCAT('%', :termo, '%'))")
    List<Perfume> buscar(@Param("termo") String termo);

    // Conta quantas vendas estão vinculadas ao perfume (impede exclusão)
    @Query("SELECT COUNT(i) FROM ItemVenda i WHERE i.perfume.id = :perfumeId")
    long countItensVendaPorPerfume(@Param("perfumeId") Integer perfumeId);
}

package com.erp.service;

// Service — regras de negócio para Perfumes
import com.erp.model.Perfume;
import com.erp.repository.PerfumeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PerfumeService {

    private final PerfumeRepository repo;

    public PerfumeService(PerfumeRepository repo) {
        this.repo = repo;
    }

    // Lista todos os perfumes
    public List<Perfume> listarTodos() {
        return repo.findAll();
    }

    // Busca por nome ou marca; sem termo retorna todos
    public List<Perfume> buscar(String termo) {
        if (termo == null || termo.isBlank()) return listarTodos();
        return repo.buscar(termo.trim());
    }

    // Busca perfume por ID — lança erro se não encontrar
    public Perfume buscarPorId(Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Perfume não encontrado: " + id));
    }

    // Cadastra novo perfume
    @Transactional
    public Perfume cadastrar(Perfume perfume) {
        return repo.save(perfume);
    }

    // Atualiza dados de um perfume existente
    @Transactional
    public Perfume atualizar(Integer id, Perfume dados) {
        Perfume p = buscarPorId(id);
        p.setNome(dados.getNome());
        p.setMarca(dados.getMarca());
        p.setVolumeMl(dados.getVolumeMl());
        p.setPrecoCusto(dados.getPrecoCusto());
        p.setPrecoVenda(dados.getPrecoVenda());
        p.setEstoqueAtual(dados.getEstoqueAtual());
        return repo.save(p);
    }

    // Exclui perfume — bloqueado se houver vendas vinculadas
    @Transactional
    public void excluir(Integer id) {
        Perfume perfume = buscarPorId(id);
        long itensVinculados = repo.countItensVendaPorPerfume(id);
        if (itensVinculados > 0) {
            throw new RuntimeException(
                "Não é possível excluir o perfume \"" + perfume.getNome() + "\" pois ele possui " +
                itensVinculados + " venda(s) registrada(s) no histórico. " +
                "Para desativá-lo, zere o estoque."
            );
        }
        repo.deleteById(id);
    }

    // Perfumes com estoque abaixo do limite — usa a FUNCTION fn_PerfumesEstoqueCritico (chamada no banco)
    public List<Perfume> estoqueCritico(int limite) {
        return repo.buscarEstoqueCritico(limite);
    }

    // Métricas para os cards do topo da aba Perfumes
    public Map<String, Object> metricas() {
        List<Perfume> todos = listarTodos();
        double valorEstoque = todos.stream()
                .mapToDouble(p -> p.getPrecoCusto() * p.getEstoqueAtual())
                .sum();
        long baixo = todos.stream().filter(p -> p.getEstoqueAtual() > 0 && p.getEstoqueAtual() <= 3).count();
        long sem   = todos.stream().filter(p -> p.getEstoqueAtual() <= 0).count();

        Map<String, Object> m = new HashMap<>();
        m.put("totalProdutos", todos.size());
        m.put("valorEstoque",  valorEstoque);
        m.put("estoqueBaixo",  baixo);
        m.put("semEstoque",    sem);
        return m;
    }
}

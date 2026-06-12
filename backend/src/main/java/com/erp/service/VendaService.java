package com.erp.service;

// Service — regras de negócio para Vendas
import com.erp.model.Cliente;
import com.erp.model.ItemVenda;
import com.erp.model.Perfume;
import com.erp.model.Venda;
import com.erp.repository.ClienteRepository;
import com.erp.repository.PerfumeRepository;
import com.erp.repository.VendaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class VendaService {

    private final VendaRepository vendaRepo;
    private final PerfumeRepository perfumeRepo;
    private final ClienteRepository clienteRepo;

    // EntityManager para chamar a stored procedure do SQL Server
    @PersistenceContext
    private EntityManager em;

    public VendaService(VendaRepository vendaRepo,
                        PerfumeRepository perfumeRepo,
                        ClienteRepository clienteRepo) {
        this.vendaRepo   = vendaRepo;
        this.perfumeRepo = perfumeRepo;
        this.clienteRepo = clienteRepo;
    }

    // Registra nova venda.
    // REQUISITO (5.2): o cabeçalho da venda é gravado pela PROCEDURE
    // sp_RegistrarVendaPerfumaria, chamada aqui pelo Java. Em seguida, os itens
    // são inseridos em ItensVenda — o que DISPARA o TRIGGER trg_AtualizarEstoquePerfumaria,
    // que é quem desconta o estoque (por isso o Java não mexe mais no estoque).
    @Transactional
    public Venda registrarVenda(VendaRequest req) {
        // Pré-validação amigável (o trigger também barra no banco, mas damos mensagem clara antes)
        for (ItemRequest ir : req.getItens()) {
            Perfume perfume = perfumeRepo.findById(ir.getPerfumeId())
                    .orElseThrow(() -> new RuntimeException("Perfume não encontrado: " + ir.getPerfumeId()));
            if (perfume.getEstoqueAtual() <= 0)
                throw new RuntimeException("O perfume \"" + perfume.getNome() + "\" está sem estoque.");
            if (perfume.getEstoqueAtual() < ir.getQuantidade())
                throw new RuntimeException("Estoque insuficiente para \"" + perfume.getNome() +
                    "\". Disponível: " + perfume.getEstoqueAtual() + " | Solicitado: " + ir.getQuantidade());
        }

        // ---- Chama a PROCEDURE para gravar o cabeçalho e devolver o id gerado ----
        StoredProcedureQuery sp = em.createStoredProcedureQuery("sp_Cad_Venda");
        sp.registerStoredProcedureParameter("valorTotal",     BigDecimal.class, ParameterMode.IN);
        sp.registerStoredProcedureParameter("formaPagamento", String.class,     ParameterMode.IN);
        sp.registerStoredProcedureParameter("clienteId",      Integer.class,    ParameterMode.IN);
        sp.setParameter("valorTotal",     BigDecimal.valueOf(req.getValorTotal()));
        sp.setParameter("formaPagamento", req.getFormaPagamento());
        sp.setParameter("clienteId",      req.getClienteId());
        sp.execute();
        // a procedure devolve o id da venda via SELECT (sem parâmetro OUTPUT)
        Integer vendaId = ((Number) sp.getSingleResult()).intValue();

        // Recarrega a venda criada pela procedure para anexar os itens via JPA
        Venda venda = vendaRepo.findById(vendaId)
                .orElseThrow(() -> new RuntimeException("Falha ao criar a venda (id não retornado pela procedure)."));

        // ---- Insere os itens — isto DISPARA o trigger, que desconta o estoque ----
        for (ItemRequest ir : req.getItens()) {
            Perfume perfume = perfumeRepo.findById(ir.getPerfumeId())
                    .orElseThrow(() -> new RuntimeException("Perfume não encontrado: " + ir.getPerfumeId()));

            ItemVenda iv = new ItemVenda();
            iv.setVenda(venda);
            iv.setPerfume(perfume);
            iv.setQuantidade(ir.getQuantidade());
            iv.setPrecoUnitario(perfume.getPrecoVenda());
            venda.getItens().add(iv);
        }

        return vendaRepo.save(venda);
    }

    // Retorna as últimas 20 vendas com cliente e itens
    public List<Venda> ultimasVendas() {
        List<Venda> todas = vendaRepo.findUltimasJpql();
        return todas.size() > 20 ? todas.subList(0, 20) : todas;
    }

    // Retorna vendas do dia atual
    public List<Venda> vendasHoje() {
        LocalDateTime inicio = LocalDate.now().atStartOfDay();
        LocalDateTime fim    = inicio.plusDays(1).minusNanos(1);
        return vendaRepo.findByPeriodo(inicio, fim);
    }

    // Métricas para os cards do topo da aba Vendas
    public Map<String, Object> metricasHoje() {
        List<Venda> hoje = vendasHoje();
        double totalHoje   = hoje.stream().mapToDouble(Venda::getValorTotal).sum();
        double ticketMedio = hoje.isEmpty() ? 0 : totalHoje / hoje.size();

        // Conta qual forma de pagamento foi mais usada hoje
        Map<String, Long> contagem = new HashMap<>();
        for (Venda v : hoje) contagem.merge(v.getFormaPagamento(), 1L, Long::sum);

        String pgtoMaisUsado = contagem.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("-");

        Map<String, Object> m = new HashMap<>();
        m.put("vendasHoje",    totalHoje);
        m.put("qtdVendas",     hoje.size());
        m.put("ticketMedio",   ticketMedio);
        m.put("pgtoMaisUsado", pgtoMaisUsado);
        return m;
    }

    // Relatório de lucratividade.
    // Sem datas -> usa a VIEW v_Lucratividade (todas as vendas).
    // Com datas -> usa a FUNCTION fc_LucratividadePeriodo (filtra pelo período).
    public List<Map<String, Object>> relatorioLucratividade(String de, String ate) {
        List<Object[]> rows;
        if (de != null && !de.isBlank() && ate != null && !ate.isBlank()) {
            LocalDateTime inicio = LocalDate.parse(de).atStartOfDay();
            LocalDateTime fim    = LocalDate.parse(ate).plusDays(1).atStartOfDay(); // inclui o dia "até" inteiro
            rows = vendaRepo.obterLucratividadePeriodo(inicio, fim);
        } else {
            rows = vendaRepo.obterRelatorioLucratividade();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("Perfume",          row[0]);
            m.put("Marca",            row[1]);
            m.put("Estoque",          row[2]);
            m.put("UnidadesVendidas", row[3]);
            m.put("ReceitaBruta",     row[4]);
            m.put("CustoTotal",       row[5]);
            m.put("LucroLiquido",     row[6]);
            result.add(m);
        }
        return result;
    }

    // Edita forma de pagamento, cliente e quantidade de uma venda existente
    @Transactional
    public Venda editarVenda(Integer vendaId, Map<String, Object> body) {
        Venda venda = vendaRepo.findById(vendaId)
            .orElseThrow(() -> new RuntimeException("Venda não encontrada: " + vendaId));

        // Atualiza forma de pagamento
        if (body.containsKey("formaPagamento"))
            venda.setFormaPagamento((String) body.get("formaPagamento"));

        // Atualiza cliente vinculado
        if (body.containsKey("clienteId")) {
            Object cId = body.get("clienteId");
            if (cId == null) {
                venda.setCliente(null);
            } else {
                clienteRepo.findById(Integer.valueOf(cId.toString())).ifPresent(venda::setCliente);
            }
        }

        // Atualiza quantidade e recalcula valor total
        if (body.containsKey("quantidade") && !venda.getItens().isEmpty()) {
            Integer novaQtd   = Integer.valueOf(body.get("quantidade").toString());
            ItemVenda item    = venda.getItens().get(0);
            Perfume perfume   = item.getPerfume();

            int estoqueAtualizado = perfume.getEstoqueAtual() + item.getQuantidade() - novaQtd;
            if (estoqueAtualizado < 0)
                throw new RuntimeException("Estoque insuficiente para " + perfume.getNome());

            perfume.setEstoqueAtual(estoqueAtualizado);
            perfumeRepo.save(perfume);

            item.setQuantidade(novaQtd);
            venda.setValorTotal(item.getPrecoUnitario() * novaQtd);
        }

        return vendaRepo.save(venda);
    }

    // Exclui venda e restaura o estoque de cada item
    @Transactional
    public void excluirVenda(Integer vendaId) {
        Venda venda = vendaRepo.findById(vendaId)
            .orElseThrow(() -> new RuntimeException("Venda não encontrada: " + vendaId));

        for (ItemVenda item : venda.getItens()) {
            Perfume perfume = item.getPerfume();
            perfume.setEstoqueAtual(perfume.getEstoqueAtual() + item.getQuantidade());
            perfumeRepo.save(perfume);
        }

        vendaRepo.delete(venda);
    }

    // Histórico de compras e total gasto por cliente
    public Map<String, Object> resumoCliente(Integer clienteId) {
        List<Venda> vendas = vendaRepo.findByClienteId(clienteId);
        double total = vendas.stream().mapToDouble(Venda::getValorTotal).sum();

        // Serializa apenas os campos necessários (evita referência circular)
        List<Map<String, Object>> vendasDto = new ArrayList<>();
        for (Venda v : vendas) {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id",             v.getId());
            dto.put("dataVenda",      v.getDataVenda());
            dto.put("valorTotal",     v.getValorTotal());
            dto.put("formaPagamento", v.getFormaPagamento());
            vendasDto.add(dto);
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalGasto", total);
        m.put("qtdCompras", vendas.size());
        m.put("vendas",     vendasDto);
        return m;
    }

    // DTO — dados da venda recebidos do frontend
    public static class VendaRequest {
        private Double valorTotal;
        private String formaPagamento;
        private Integer clienteId;
        private List<ItemRequest> itens = new ArrayList<>();

        public Double getValorTotal() { return valorTotal; }
        public void setValorTotal(Double valorTotal) { this.valorTotal = valorTotal; }
        public String getFormaPagamento() { return formaPagamento; }
        public void setFormaPagamento(String formaPagamento) { this.formaPagamento = formaPagamento; }
        public Integer getClienteId() { return clienteId; }
        public void setClienteId(Integer clienteId) { this.clienteId = clienteId; }
        public List<ItemRequest> getItens() { return itens; }
        public void setItens(List<ItemRequest> itens) { this.itens = itens; }
    }

    // DTO — dados de cada item da venda
    public static class ItemRequest {
        private Integer perfumeId;
        private Integer quantidade;

        public Integer getPerfumeId() { return perfumeId; }
        public void setPerfumeId(Integer perfumeId) { this.perfumeId = perfumeId; }
        public Integer getQuantidade() { return quantidade; }
        public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
    }
}

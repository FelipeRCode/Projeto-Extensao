package com.erp.model;

// Model — representa a tabela Vendas no banco de dados
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Vendas")
public class Venda {

    // Chave primária gerada automaticamente
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Data e hora da venda — preenchida automaticamente
    @Column(name = "data_venda")
    private LocalDateTime dataVenda = LocalDateTime.now();

    // Valor total da venda
    @Column(name = "valor_total", nullable = false)
    private Double valorTotal;

    // Forma de pagamento (PIX, Dinheiro, Cartão)
    @Column(name = "forma_pagamento", length = 30)
    private String formaPagamento;

    // Cliente vinculado à venda — opcional
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id")
    @JsonIgnoreProperties({"vendas"})
    private Cliente cliente;

    // Itens da venda — lista de perfumes vendidos
    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<ItemVenda> itens = new ArrayList<>();

    // Getters
    public Integer getId() { return id; }
    public LocalDateTime getDataVenda() { return dataVenda; }
    public Double getValorTotal() { return valorTotal; }
    public String getFormaPagamento() { return formaPagamento; }
    public Cliente getCliente() { return cliente; }
    public List<ItemVenda> getItens() { return itens; }

    // Setters
    public void setId(Integer id) { this.id = id; }
    public void setDataVenda(LocalDateTime dataVenda) { this.dataVenda = dataVenda; }
    public void setValorTotal(Double valorTotal) { this.valorTotal = valorTotal; }
    public void setFormaPagamento(String formaPagamento) { this.formaPagamento = formaPagamento; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public void setItens(List<ItemVenda> itens) { this.itens = itens; }
}

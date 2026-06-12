package com.erp.model;

// Model — representa a tabela ItensVenda (linhas de cada venda)
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
@Table(name = "ItensVenda")
public class ItemVenda {

    // Chave primária gerada automaticamente
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Venda à qual este item pertence
    @ManyToOne
    @JoinColumn(name = "venda_id", nullable = false)
    @JsonBackReference
    private Venda venda;

    // Perfume vendido neste item
    @ManyToOne
    @JoinColumn(name = "perfume_id", nullable = false)
    private Perfume perfume;

    // Quantidade vendida
    @Column(name = "quantidade", nullable = false)
    private Integer quantidade;

    // Preço unitário no momento da venda
    @Column(name = "preco_unitario", nullable = false)
    private Double precoUnitario;

    // Getters
    public Integer getId() { return id; }
    public Venda getVenda() { return venda; }
    public Perfume getPerfume() { return perfume; }
    public Integer getQuantidade() { return quantidade; }
    public Double getPrecoUnitario() { return precoUnitario; }

    // Setters
    public void setId(Integer id) { this.id = id; }
    public void setVenda(Venda venda) { this.venda = venda; }
    public void setPerfume(Perfume perfume) { this.perfume = perfume; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
    public void setPrecoUnitario(Double precoUnitario) { this.precoUnitario = precoUnitario; }
}

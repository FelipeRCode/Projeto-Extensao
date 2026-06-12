package com.erp.model;

// Model — representa a tabela Perfumes no banco de dados
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "Perfumes")
public class Perfume {

    // Chave primária gerada automaticamente
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Nome obrigatório
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100)
    @Column(name = "nome", nullable = false)
    private String nome;

    // Marca obrigatória
    @NotBlank(message = "Marca é obrigatória")
    @Size(max = 50)
    @Column(name = "marca", nullable = false)
    private String marca;

    // Volume em ml opcional
    @Column(name = "volume_ml")
    private Integer volumeMl;

    // Preço de custo obrigatório
    @NotNull
    @DecimalMin("0.0")
    @Column(name = "preco_custo", nullable = false)
    private Double precoCusto;

    // Preço de venda obrigatório
    @NotNull
    @DecimalMin("0.0")
    @Column(name = "preco_venda", nullable = false)
    private Double precoVenda;

    // Quantidade em estoque — padrão 0
    @Min(0)
    @Column(name = "estoque_atual")
    private Integer estoqueAtual = 0;

    // Construtores
    public Perfume() {}

    public Perfume(String nome, String marca, Integer volumeMl,
                   Double precoCusto, Double precoVenda, Integer estoqueAtual) {
        this.nome = nome;
        this.marca = marca;
        this.volumeMl = volumeMl;
        this.precoCusto = precoCusto;
        this.precoVenda = precoVenda;
        this.estoqueAtual = estoqueAtual;
    }

    // Getters
    public Integer getId() { return id; }
    public String getNome() { return nome; }
    public String getMarca() { return marca; }
    public Integer getVolumeMl() { return volumeMl; }
    public Double getPrecoCusto() { return precoCusto; }
    public Double getPrecoVenda() { return precoVenda; }
    public Integer getEstoqueAtual() { return estoqueAtual; }

    // Setters
    public void setId(Integer id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public void setMarca(String marca) { this.marca = marca; }
    public void setVolumeMl(Integer volumeMl) { this.volumeMl = volumeMl; }
    public void setPrecoCusto(Double precoCusto) { this.precoCusto = precoCusto; }
    public void setPrecoVenda(Double precoVenda) { this.precoVenda = precoVenda; }
    public void setEstoqueAtual(Integer estoqueAtual) { this.estoqueAtual = estoqueAtual; }
}

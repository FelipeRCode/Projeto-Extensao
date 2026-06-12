package com.erp.model;

// Model — tabela MÃE "Pessoas" (HERANÇA, no estilo dos arquivos da professora).
// Clientes e Usuarios HERDAM de Pessoa usando a estratégia JOINED: existe uma
// tabela para cada filha (Clientes, Usuarios), ligadas à mãe (Pessoas) pela MESMA
// chave primária. Aqui ficam os dados comuns a qualquer pessoa — neste caso, o nome.
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "Pessoas")
public abstract class Pessoa {

    // Chave primária gerada automaticamente — compartilhada com as tabelas filhas
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idPessoa")
    private Integer id;

    // Nome obrigatório (comum a clientes e usuários)
    @NotBlank(message = "Nome é obrigatório")
    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    public Integer getId() { return id; }
    public String getNome() { return nome; }

    public void setId(Integer id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
}

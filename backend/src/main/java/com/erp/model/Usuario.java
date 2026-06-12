package com.erp.model;

// Model — tabela Usuarios. HERDA de Pessoa (estratégia JOINED): o id e o nome
// ficam na tabela mãe Pessoas; aqui ficam os dados de login e acesso.
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "Usuarios")
@PrimaryKeyJoinColumn(name = "pessoaId") // mesma PK da tabela mãe Pessoas
public class Usuario extends Pessoa {

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @JsonIgnore
    @Column(nullable = false, length = 255)
    private String senha;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_acesso", nullable = false, length = 20)
    private NivelAcesso nivelAcesso = NivelAcesso.FUNCIONARIO;

    // Getters (id e nome são herdados de Pessoa)
    public String getEmail() { return email; }
    public String getSenha() { return senha; }
    public Boolean getAtivo() { return ativo; }
    public NivelAcesso getNivelAcesso() { return nivelAcesso; }

    // Setters
    public void setSenha(String senha) { this.senha = senha; }
    public void setEmail(String email) { this.email = email; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
    public void setNivelAcesso(NivelAcesso nivelAcesso) { this.nivelAcesso = nivelAcesso; }
}

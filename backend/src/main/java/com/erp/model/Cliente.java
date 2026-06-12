package com.erp.model;

// Model — tabela Clientes. HERDA de Pessoa (estratégia JOINED): o id e o nome
// ficam na tabela mãe Pessoas; aqui ficam só os dados próprios do cliente.
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "Clientes")
@PrimaryKeyJoinColumn(name = "pessoaId") // mesma PK da tabela mãe Pessoas
public class Cliente extends Pessoa {

    // Telefone obrigatório
    @NotBlank(message = "Telefone é obrigatório")
    @Column(nullable = false, length = 20)
    private String telefone;

    // E-mail obrigatório com formato válido
    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    @Column(nullable = false, length = 150)
    private String email;

    // Getters / Setters (nome e id são herdados de Pessoa)
    public String getTelefone() { return telefone; }
    public String getEmail() { return email; }

    public void setTelefone(String telefone) { this.telefone = telefone; }
    public void setEmail(String email) { this.email = email; }
}

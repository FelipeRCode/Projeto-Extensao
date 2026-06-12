package com.erp.model;

// Model — tabela Notificacoes. Quando um gerente/funcionário pede redefinição
// de senha, uma notificação é criada aqui para o super admin resolver no sistema
// (sem envio de e-mail).
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Notificacoes")
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Usuário que solicitou a redefinição de senha
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    @JsonIgnore
    private Usuario usuario;

    @Column(nullable = false, length = 200)
    private String mensagem;

    @Column(nullable = false)
    private LocalDateTime data = LocalDateTime.now();

    // false = pendente; true = já tratada pelo super admin
    @Column(nullable = false)
    private Boolean resolvida = false;

    public Notificacao() {}

    public Notificacao(Usuario usuario, String mensagem) {
        this.usuario   = usuario;
        this.mensagem  = mensagem;
        this.data      = LocalDateTime.now();
        this.resolvida = false;
    }

    public Integer getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getMensagem() { return mensagem; }
    public LocalDateTime getData() { return data; }
    public Boolean getResolvida() { return resolvida; }

    public void setResolvida(Boolean resolvida) { this.resolvida = resolvida; }
}

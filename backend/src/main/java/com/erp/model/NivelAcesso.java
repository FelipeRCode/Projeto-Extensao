package com.erp.model;

// Níveis de acesso do sistema (RBAC)
// SUPER_ADMIN = dono (acesso total + gestão de usuários)
// GERENTE     = gerência (sem acesso à aba Sistema)
// FUNCIONARIO = vendedor (sem relatórios nem dados financeiros)
public enum NivelAcesso {
    SUPER_ADMIN, GERENTE, FUNCIONARIO
}

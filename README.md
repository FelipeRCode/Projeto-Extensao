# Essenza Prime Perfumaria

Sistema ERP para gestão de perfumaria — estoque, vendas, clientes e relatórios.

**Stack:** Java 17 + Spring Boot · SQL Server · HTML/CSS/JS puro · PWA

---

## Como rodar

### 1. Banco de dados

Execute o `banco_completo.sql` no SQL Server Management Studio.

Login padrão: `dono@essenza.com` / `admin123`

### 2. Backend

Edite a senha do banco em `backend/src/main/resources/application.properties`:

```properties
spring.datasource.password=SUA_SENHA_AQUI
```

Depois rode:

```bash
cd backend
./mvnw spring-boot:run
```

O servidor sobe em `http://localhost:8080`.

### 3. Frontend

Abra `frontend/login.html` no navegador (via Live Server ou qualquer servidor estático).

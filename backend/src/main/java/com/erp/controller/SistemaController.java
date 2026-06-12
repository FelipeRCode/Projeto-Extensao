package com.erp.controller;

// Controller — backup e informações do banco de dados
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sistema")
@CrossOrigin(origins = "*")
public class SistemaController {

    private final DataSource dataSource;
    private final EntityManager em;

    // Pasta onde os backups serão salvos (configurável no application.properties)
    @Value("${app.backup.dir:C:\\Backups}")
    private String backupDir;

    public SistemaController(DataSource dataSource, EntityManager em) {
        this.dataSource = dataSource;
        this.em = em;
    }

    // POST /api/sistema/backup — executa BACKUP DATABASE via JDBC direto
    @PostMapping("/backup")
    public ResponseEntity<Map<String, Object>> backup() {
        try {
            // Cria a pasta de backup se não existir
            File dir = new File(backupDir);
            if (!dir.exists()) dir.mkdirs();

            String timestamp   = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String nomeArquivo = "Essenza_Prime_Perfumaria_" + timestamp + ".bak";
            String caminho     = backupDir + File.separator + nomeArquivo;

            String sql = "BACKUP DATABASE [Essenza_Prime_Perfumaria] TO DISK = '" + caminho +
                         "' WITH FORMAT, INIT, NAME = 'Essenza Prime Perfumaria Full Backup', SKIP, NOREWIND, NOUNLOAD, STATS = 10";

            // Usa JDBC direto — o Hibernate não suporta comandos de backup
            try (Connection conn = dataSource.getConnection();
                 Statement stmt  = conn.createStatement()) {
                stmt.setQueryTimeout(120);
                stmt.execute(sql);
            }

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("sucesso",   true);
            resp.put("mensagem",  "Backup realizado com sucesso!");
            resp.put("arquivo",   nomeArquivo);
            resp.put("caminho",   caminho);
            resp.put("tamanhoMB", String.format("%.2f", new File(caminho).length() / 1048576.0));
            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "sucesso",  false,
                "mensagem", "Erro ao realizar backup: " + e.getMessage()
            ));
        }
    }

    // GET /api/sistema/backups — lista os arquivos .bak disponíveis para restauração
    @GetMapping("/backups")
    public ResponseEntity<Map<String, Object>> listarBackups() {
        File dir = new File(backupDir);
        File[] arquivos = dir.exists() ? dir.listFiles((d, nome) -> nome.toLowerCase().endsWith(".bak")) : null;

        java.util.List<String> nomes = new java.util.ArrayList<>();
        if (arquivos != null) {
            java.util.Arrays.sort(arquivos, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            for (File f : arquivos) nomes.add(f.getName());
        }
        return ResponseEntity.ok(Map.of("sucesso", true, "arquivos", nomes, "pasta", backupDir));
    }

    // POST /api/sistema/restore — executa RESTORE DATABASE a partir de um arquivo .bak
    // Se "arquivo" não vier no corpo, usa o backup mais recente da pasta.
    @PostMapping("/restore")
    public ResponseEntity<Map<String, Object>> restore(@RequestBody(required = false) Map<String, String> body) {
        try {
            String arquivo = (body != null) ? body.get("arquivo") : null;

            // Sem nome informado: pega o .bak mais recente
            if (arquivo == null || arquivo.isBlank()) {
                File dir = new File(backupDir);
                File[] baks = dir.exists() ? dir.listFiles((d, nome) -> nome.toLowerCase().endsWith(".bak")) : null;
                if (baks == null || baks.length == 0) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "sucesso", false, "mensagem", "Nenhum arquivo de backup (.bak) encontrado em " + backupDir));
                }
                java.util.Arrays.sort(baks, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                arquivo = baks[0].getName();
            }

            String caminho = backupDir + File.separator + arquivo;
            if (!new File(caminho).exists()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "sucesso", false, "mensagem", "Arquivo não encontrado: " + caminho));
            }

            // O RESTORE precisa rodar a partir de 'master' e sem outras conexões no banco.
            // SINGLE_USER WITH ROLLBACK IMMEDIATE derruba as demais sessões; ao final volta para MULTI_USER.
            String sql =
                "USE master; " +
                "ALTER DATABASE [Essenza_Prime_Perfumaria] SET SINGLE_USER WITH ROLLBACK IMMEDIATE; " +
                "RESTORE DATABASE [Essenza_Prime_Perfumaria] FROM DISK = '" + caminho + "' WITH REPLACE; " +
                "ALTER DATABASE [Essenza_Prime_Perfumaria] SET MULTI_USER; " +
                // volta a conexão para o banco da aplicação (senão ela fica em 'master' no pool)
                "USE [Essenza_Prime_Perfumaria];";

            try (Connection conn = dataSource.getConnection();
                 Statement stmt  = conn.createStatement()) {
                stmt.setQueryTimeout(180);
                stmt.execute(sql);
            }

            return ResponseEntity.ok(Map.of(
                "sucesso",  true,
                "mensagem", "Restauração concluída a partir de " + arquivo + "!",
                "arquivo",  arquivo));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "sucesso",  false,
                "mensagem", "Erro ao restaurar: " + e.getMessage()));
        }
    }

    // GET /api/sistema/info — retorna versão do SQL Server e tamanho do banco
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        try {
            Object versao  = em.createNativeQuery("SELECT @@VERSION").getSingleResult();
            Object tamanho = em.createNativeQuery(
                "SELECT CAST(SUM(size) * 8.0 / 1024 AS DECIMAL(10,2)) FROM sys.database_files"
            ).getSingleResult();

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("sucesso",         true);
            resp.put("versaoSqlServer", versao.toString().split("\n")[0].trim());
            resp.put("tamanhoMB",       tamanho);
            resp.put("pastaBackup",     backupDir);
            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "sucesso",  false,
                "mensagem", e.getMessage()
            ));
        }
    }
}

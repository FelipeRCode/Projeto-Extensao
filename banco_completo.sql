-------------------------------------------------------------
-- Essenza Prime Perfumaria - Script SQL Completo (SQL Server)
-- Recursos: heranca (Pessoas -> Clientes/Usuarios), procedure,
-- trigger, view e function (retorna tabela). Backup/Restore no Java.
-------------------------------------------------------------

-------------------------------------------------------------
-- Criar o banco de dados
-------------------------------------------------------------
create database Essenza_Prime_Perfumaria
go

-------------------------------------------------------------
-- Acessar o banco de dados
-------------------------------------------------------------
use Essenza_Prime_Perfumaria
go

-------------------------------------------------------------
-- Criar a tabela Pessoas (tabela MAE da heranca)
-------------------------------------------------------------
create table Pessoas
(
	idPessoa	int				not null	primary key		identity,
	nome		varchar(100)	not null
)
go

-------------------------------------------------------------
-- Criar a tabela Clientes. Todo cliente e uma Pessoa
-------------------------------------------------------------
create table Clientes
(
	pessoaId	int				not null	primary key		references Pessoas(idPessoa),
	telefone	varchar(20)		not null,
	email		varchar(150)	not null
)
go

-------------------------------------------------------------
-- Criar a tabela Usuarios. Todo usuario e uma Pessoa
-- nivel_acesso: SUPER_ADMIN (dono), GERENTE ou FUNCIONARIO
-------------------------------------------------------------
create table Usuarios
(
	pessoaId		int				not null	primary key		references Pessoas(idPessoa),
	email			varchar(150)	not null	unique,
	senha			varchar(255)	not null,
	ativo			bit				not null	default 1,
	nivel_acesso	varchar(20)		not null	default 'FUNCIONARIO'
					check(nivel_acesso in ('SUPER_ADMIN', 'GERENTE', 'FUNCIONARIO'))
)
go

-------------------------------------------------------------
-- Criar a tabela Perfumes
-------------------------------------------------------------
create table Perfumes
(
	id				int				not null	primary key		identity,
	nome			varchar(100)	not null,
	marca			varchar(50)		not null,
	volume_ml		int					null,
	preco_custo		decimal(10,2)	not null	check(preco_custo >= 0),
	preco_venda		decimal(10,2)	not null	check(preco_venda >= 0),
	estoque_atual	int				not null	default 0	check(estoque_atual >= 0)
)
go

-------------------------------------------------------------
-- Criar a tabela Vendas
-------------------------------------------------------------
create table Vendas
(
	id				int				not null	primary key		identity,
	data_venda		datetime		not null	default GETDATE(),
	valor_total		decimal(10,2)	not null	check(valor_total >= 0),
	forma_pagamento	varchar(30)			null,
	cliente_id		int					null	references Clientes(pessoaId)
)
go

-------------------------------------------------------------
-- Criar a tabela ItensVenda
-------------------------------------------------------------
create table ItensVenda
(
	id				int				not null	primary key		identity,
	venda_id		int				not null	references Vendas(id),
	perfume_id		int				not null	references Perfumes(id),
	quantidade		int				not null	check(quantidade > 0),
	preco_unitario	decimal(10,2)	not null	check(preco_unitario >= 0)
)
go

-------------------------------------------------------------
-- Criar a tabela TokensRedefinicao (recuperacao de senha)
-------------------------------------------------------------
create table TokensRedefinicao
(
	id			int				not null	primary key		identity,
	token		varchar(100)	not null	unique,
	usuario_id	int				not null	references Usuarios(pessoaId),
	expiracao	datetime		not null,
	usado		bit				not null	default 0
)
go

-------------------------------------------------------------
-- Criar a tabela Notificacoes
-- Quando um gerente/funcionario pede redefinicao de senha, gera
-- uma notificacao aqui para o super admin tratar no sistema.
-------------------------------------------------------------
create table Notificacoes
(
	id			int				not null	primary key		identity,
	usuario_id	int				not null	references Usuarios(pessoaId),
	mensagem	varchar(200)	not null,
	data		datetime		not null	default GETDATE(),
	resolvida	bit				not null	default 0
)
go

-------------------------------------------------------------
-- TRIGGER
-- Sempre que um item de venda for inserido (operacao feita pelo
-- sistema Java), o trigger dispara e da baixa no estoque do perfume.
-- TABELA:	ItensVenda
-- EVENTO:	insert
-- TIPO:	After
-------------------------------------------------------------
create trigger tg_AtualizarEstoque
ON		ItensVenda
AFTER	insert
as
begin
	-- da baixa no estoque dos perfumes vendidos
	update P set P.estoque_atual = P.estoque_atual - I.quantidade
	from Perfumes as P inner join inserted as I on P.id = I.perfume_id
end
go

-------------------------------------------------------------
-- PROCEDURE
-- Cadastra o cabecalho de uma venda (transacao programada).
-- E chamada pelo sistema Java e devolve o id gerado (output).
-------------------------------------------------------------
create procedure sp_Cad_Venda
(	-- parametros recebidos
	@valorTotal		decimal(10,2),
	@formaPagamento	varchar(30),
	@clienteId		int
)
as
begin	-- inicio da procedure
	-- evita a mensagem de "linhas afetadas" atrapalhar o retorno do select
	SET NOCOUNT ON;

	begin try
		begin tran		-- inicio da transacao
			-- inserir os dados na tabela de vendas
			insert into Vendas (valor_total, forma_pagamento, cliente_id)
			values (@valorTotal, @formaPagamento, @clienteId)

			-- declarar uma variavel e guardar o id gerado pelo BD
			declare @vendaId int
			set @vendaId = SCOPE_IDENTITY()

			-- tudo certo, confirmar os dados
			commit

			-- devolver o id da venda criada para o sistema Java
			select @vendaId as vendaId
	end try
	begin catch
		-- deu erro, desfazer a operacao
		if @@TRANCOUNT > 0
			rollback;
		THROW;
	end catch
end
go

-------------------------------------------------------------
-- VIEW
-- Consulta complexa de lucratividade (join + agregacoes).
-- E chamada pelo sistema Java na aba Relatorios.
-------------------------------------------------------------
create view v_Lucratividade
as
	select	P.nome	as Perfume,
			P.marca	as Marca,
			P.estoque_atual	as Estoque,
			case when SUM(IV.quantidade) is null
				 then 0 else SUM(IV.quantidade) end							as UnidadesVendidas,
			case when SUM(IV.quantidade * IV.preco_unitario) is null
				 then 0 else SUM(IV.quantidade * IV.preco_unitario) end		as ReceitaBruta,
			case when SUM(IV.quantidade * P.preco_custo) is null
				 then 0 else SUM(IV.quantidade * P.preco_custo) end			as CustoTotal,
			case when SUM(IV.quantidade * (IV.preco_unitario - P.preco_custo)) is null
				 then 0 else SUM(IV.quantidade * (IV.preco_unitario - P.preco_custo)) end	as LucroLiquido
	from Perfumes as P	left join	ItensVenda as IV	ON	IV.perfume_id = P.id
	group by P.id, P.nome, P.marca, P.estoque_atual
go

-------------------------------------------------------------
-- FUNCTION (table-value: retorna uma tabela)
-- Retorna os perfumes com estoque abaixo ou igual ao limite.
-- E usada em uma consulta do sistema Java.
-------------------------------------------------------------
create function fc_PerfumesEstoqueCritico
(	-- parametro recebido
	@limite		int
)
RETURNS		table
as
	return	(
				select id, nome, marca, volume_ml, preco_custo, preco_venda, estoque_atual
				from Perfumes
				where estoque_atual <= @limite
			)
go

-------------------------------------------------------------
-- FUNCTION (table-value) - lucratividade por PERIODO
-- Recebe data inicial e final e retorna a lucratividade de cada
-- perfume considerando apenas as vendas feitas no periodo.
-- Usada no relatorio do sistema Java (botao Filtrar).
-------------------------------------------------------------
create function fc_LucratividadePeriodo
(	-- parametros recebidos
	@dataDe		datetime,
	@dataAte	datetime
)
RETURNS		table
as
	return	(
		select	P.nome	as Perfume,
				P.marca	as Marca,
				P.estoque_atual	as Estoque,
				SUM(case when V.id is not null then IV.quantidade else 0 end)							as UnidadesVendidas,
				SUM(case when V.id is not null then IV.quantidade * IV.preco_unitario else 0 end)		as ReceitaBruta,
				SUM(case when V.id is not null then IV.quantidade * P.preco_custo else 0 end)			as CustoTotal,
				SUM(case when V.id is not null then IV.quantidade * (IV.preco_unitario - P.preco_custo) else 0 end)	as LucroLiquido
		from Perfumes as P
			left join ItensVenda as IV	on IV.perfume_id = P.id
			left join Vendas as V		on V.id = IV.venda_id
										and V.data_venda >= @dataDe
										and V.data_venda <  @dataAte
		group by P.id, P.nome, P.marca, P.estoque_atual
	)
go

-------------------------------------------------------------
-- INJECAO DE DADOS (minimo 10 inserts por tabela)
-------------------------------------------------------------

-- Perfumes (10)
insert into Perfumes (nome, marca, volume_ml, preco_custo, preco_venda, estoque_atual)
values	('Sauvage',				'Dior',					100, 250.00, 450.00, 50),
		('Bleu de Chanel',		'Chanel',				100, 300.00, 520.00, 50),
		('La Vie Est Belle',	'Lancome',				 75, 280.00, 480.00, 50),
		('Black Opium',			'YSL',					 90, 260.00, 470.00, 50),
		('Invictus',			'Paco Rabanne',			100, 220.00, 390.00, 50),
		('Good Girl',			'Carolina Herrera',		 80, 290.00, 510.00, 50),
		('1 Million',			'Paco Rabanne',			100, 230.00, 410.00, 50),
		('Light Blue',			'Dolce & Gabbana',		100, 240.00, 430.00, 50),
		('Acqua di Gio',		'Giorgio Armani',		100, 270.00, 490.00, 50),
		('Scandal',				'Jean Paul Gaultier',	 80, 250.00, 450.00, 50)
go

-- Pessoas (20): a tabela esta vazia, entao os ids gerados serao 1..20.
-- 1..10 serao Clientes, 11..20 serao Usuarios.
insert into Pessoas (nome)
values	('Maria Silva'),
		('Joao Souza'),
		('Ana Oliveira'),
		('Carlos Pereira'),
		('Fernanda Lima'),
		('Rafael Costa'),
		('Juliana Alves'),
		('Bruno Rocha'),
		('Patricia Gomes'),
		('Eduardo Martins'),
		('Arthur Scrignolli'),
		('Carla Mendes'),
		('Paulo Ramos'),
		('Ana Beatriz'),
		('Diego Santos'),
		('Larissa Dias'),
		('Marcelo Nunes'),
		('Tatiane Freitas'),
		('Rodrigo Pinto'),
		('Vanessa Cardoso')
go

-- Clientes (10) -> filhos das Pessoas 1..10
insert into Clientes (pessoaId, telefone, email)
values	( 1, '(11) 98888-0001', 'maria.silva@email.com'),
		( 2, '(11) 98888-0002', 'joao.souza@email.com'),
		( 3, '(11) 98888-0003', 'ana.oliveira@email.com'),
		( 4, '(11) 98888-0004', 'carlos.pereira@email.com'),
		( 5, '(11) 98888-0005', 'fernanda.lima@email.com'),
		( 6, '(11) 98888-0006', 'rafael.costa@email.com'),
		( 7, '(11) 98888-0007', 'juliana.alves@email.com'),
		( 8, '(11) 98888-0008', 'bruno.rocha@email.com'),
		( 9, '(11) 98888-0009', 'patricia.gomes@email.com'),
		(10, '(11) 98888-0010', 'eduardo.martins@email.com')
go

-- Usuarios (10) -> filhos das Pessoas 11..20. Os 3 niveis estao presentes.
insert into Usuarios (pessoaId, email, senha, ativo, nivel_acesso)
values	(11, 'dono@essenza.com',		'admin123',	1, 'SUPER_ADMIN'),
		(12, 'carla@essenza.com',	'ger123',	1, 'GERENTE'),
		(13, 'paulo@essenza.com',	'ger123',	1, 'GERENTE'),
		(14, 'ana@essenza.com',		'func123',	1, 'FUNCIONARIO'),
		(15, 'diego@essenza.com',	'func123',	1, 'FUNCIONARIO'),
		(16, 'larissa@essenza.com',	'func123',	1, 'FUNCIONARIO'),
		(17, 'marcelo@essenza.com',	'func123',	1, 'FUNCIONARIO'),
		(18, 'tatiane@essenza.com',	'func123',	1, 'FUNCIONARIO'),
		(19, 'rodrigo@essenza.com',	'func123',	0, 'FUNCIONARIO'),
		(20, 'vanessa@essenza.com',	'func123',	1, 'FUNCIONARIO')
go

-- Vendas (10) -> uma para cada cliente
insert into Vendas (valor_total, forma_pagamento, cliente_id)
values	( 450.00, 'PIX',		 1),
		( 520.00, 'Credito',	 2),
		( 960.00, 'Debito',		 3),
		( 470.00, 'Dinheiro',	 4),
		(1170.00, 'PIX',		 5),
		( 510.00, 'Credito',	 6),
		( 820.00, 'PIX',		 7),
		( 430.00, 'Debito',		 8),
		( 940.00, 'Credito',	 9),
		(1420.00, 'PIX',		10)
go

-- ItensVenda (12) -> o insert dispara o trigger, que da baixa no estoque
insert into ItensVenda (venda_id, perfume_id, quantidade, preco_unitario)
values	( 1,  1, 1, 450.00),
		( 2,  2, 1, 520.00),
		( 3,  3, 2, 480.00),
		( 4,  4, 1, 470.00),
		( 5,  5, 3, 390.00),
		( 6,  6, 1, 510.00),
		( 7,  7, 2, 410.00),
		( 8,  8, 1, 430.00),
		( 9,  9, 1, 490.00),
		( 9,  1, 1, 450.00),
		(10, 10, 2, 450.00),
		(10,  2, 1, 520.00)
go

-- TokensRedefinicao (10) -> todos ja usados (apenas para popular a tabela)
insert into TokensRedefinicao (token, usuario_id, expiracao, usado)
values	('token-seed-01', 11, '2020-01-01', 1),
		('token-seed-02', 12, '2020-01-01', 1),
		('token-seed-03', 13, '2020-01-01', 1),
		('token-seed-04', 14, '2020-01-01', 1),
		('token-seed-05', 15, '2020-01-01', 1),
		('token-seed-06', 16, '2020-01-01', 1),
		('token-seed-07', 17, '2020-01-01', 1),
		('token-seed-08', 18, '2020-01-01', 1),
		('token-seed-09', 19, '2020-01-01', 1),
		('token-seed-10', 20, '2020-01-01', 1)
go

-- Notificacoes (10) -> historico ja resolvido (resolvida = 1), apenas para popular
insert into Notificacoes (usuario_id, mensagem, data, resolvida)
values	(12, 'Carla Mendes solicitou redefinicao de senha.',		'20260110', 1),
		(13, 'Paulo Ramos solicitou redefinicao de senha.',		'20260112', 1),
		(14, 'Ana Beatriz solicitou redefinicao de senha.',		'20260115', 1),
		(15, 'Diego Santos solicitou redefinicao de senha.',		'20260118', 1),
		(16, 'Larissa Dias solicitou redefinicao de senha.',		'20260120', 1),
		(17, 'Marcelo Nunes solicitou redefinicao de senha.',		'20260122', 1),
		(18, 'Tatiane Freitas solicitou redefinicao de senha.',	'20260125', 1),
		(19, 'Rodrigo Pinto solicitou redefinicao de senha.',		'20260128', 1),
		(20, 'Vanessa Cardoso solicitou redefinicao de senha.',	'20260201', 1),
		(14, 'Ana Beatriz solicitou redefinicao de senha.',		'20260205', 1)
go

-------------------------------------------------------------
-- Conferir os dados cadastrados
-------------------------------------------------------------
select * from Pessoas
go
select * from Clientes
go
select * from Usuarios
go
select * from Perfumes
go
select * from Vendas
go
select * from ItensVenda
go

-------------------------------------------------------------
-- Para recriar o banco do zero, descomente as linhas abaixo
-------------------------------------------------------------
-- use master
-- go
-- drop database Essenza_Prime_Perfumaria
-- go

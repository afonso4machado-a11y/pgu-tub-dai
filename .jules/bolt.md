## 2024-05-08 - [Otimização de Performance no Dashboard]
**Learning:** Foi identificado que a geração de dados do dashboard era dispendiosa na base de dados, pelo que foi implementado um sistema de cache de 30 segundos.
**Action:** Na próxima vez que dados em tempo real pesados forem requeridos pela interface, ponderar se um cache em memória de curta duração pode ser implementado antes de bater na base de dados.

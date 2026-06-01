package pt.uminho.dai.pgu.api;

import pt.uminho.dai.pgu.business.acessos_configuracao.*;
import pt.uminho.dai.pgu.business.operacao_tempo_real.*;
import pt.uminho.dai.pgu.business.analitica_historico.*;
import pt.uminho.dai.pgu.data.*;
import pt.uminho.dai.pgu.data.acessos_configuracao.*;
import pt.uminho.dai.pgu.data.operacao_tempo_real.*;
import pt.uminho.dai.pgu.data.analitica_historico.*;
import pt.uminho.dai.pgu.api.acessos_configuracao.*;
import pt.uminho.dai.pgu.api.operacao_tempo_real.*;
import pt.uminho.dai.pgu.api.analitica_historico.*;

import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração central do Spring Boot.
 * Regista o bean Sistema (Fachada do domínio) e configura CORS para
 * permitir comunicação entre o frontend Vue.js e o backend.
 */
@Configuration
public class SpringConfig implements WebMvcConfigurer {

 @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:80,http://localhost,https://afonso4machado-a11y.github.io,https://pgu-tub.is-a.dev}")
 private String corsAllowedOrigins;

 @Bean
 public Sistema sistema() {
 Sistema sistema = new Sistema();

 // Frota Realista TUB 
 sistema.registarAutocarro("TUB-101", 80, "23-AB-45", "Mercedes", "Citaro");
 sistema.registarAutocarro("TUB-102", 60, "67-CD-89", "Volvo", "7900");
 sistema.registarAutocarro("TUB-103", 80, "10-EF-23", "Mercedes", "eCitaro");
 sistema.registarAutocarro("TUB-104", 50, "45-GH-67", "MAN", "Lion's City");
 sistema.registarAutocarro("TUB-105", 70, "89-IJ-01", "Volvo", "7900E");
 sistema.registarAutocarro("TUB-106", 80, "34-KL-56", "Mercedes", "Citaro G");
 sistema.registarAutocarro("TUB-107", 65, "12-MN-34", "IVECO", "Crossway");
 sistema.registarAutocarro("TUB-108", 60, "56-OP-78", "Volvo", "7900H");
 sistema.registarAutocarro("TUB-109", 75, "90-QR-12", "Mercedes", "Citaro");
 sistema.registarAutocarro("TUB-110", 70, "23-ST-45", "MAN", "Lion's City");

 // Linhas TUB 
 sistema.registarLinha("L2", "Ponte de Prado — Bom Jesus");
 sistema.registarLinha("L3", "Avenida Central — Ruães");
 sistema.registarLinha("L5", "Dume — Quinta da Capela");
 sistema.registarLinha("L6", "Av. Gen. Norton de Matos — Gondizalves/Semelhe");
 sistema.registarLinha("L7", "Celeirós — S. Vítor");
 sistema.registarLinha("L8", "Rua 25 de Abril — Sete Fontes");
 sistema.registarLinha("L9", "Ruães — Nogueira (Barral)");
 sistema.registarLinha("L12", "Av. da Liberdade — Lageosa/Pedralva via Gualtar");
 sistema.registarLinha("L13", "Av. Gen. Norton de Matos — Lageosa/Pedralva");
 sistema.registarLinha("L14", "Praça Conde de Agrolongo — Priscos");
 sistema.registarLinha("L18", "Rua do Raio — Pinheiro do Bicho via Esporões");
 sistema.registarLinha("L19", "Areal — Boavista");
 sistema.registarLinha("L20", "Av. da Liberdade — Escudeiros via Ponte Nova");
 sistema.registarLinha("L40", "Gualtar — Real");
 sistema.registarLinha("L43", "Estação — Universidade");

 // Associar autocarros às linhas 
 sistema.associarAutocarroALinha("TUB-101", "L7");
 sistema.associarAutocarroALinha("TUB-102", "L43");
 sistema.associarAutocarroALinha("TUB-103", "L7");
 sistema.associarAutocarroALinha("TUB-104", "L40");
 sistema.associarAutocarroALinha("TUB-105", "L43");
 sistema.associarAutocarroALinha("TUB-106", "L40");
 sistema.associarAutocarroALinha("TUB-107", "L2");
 sistema.associarAutocarroALinha("TUB-108", "L3");
 sistema.associarAutocarroALinha("TUB-109", "L12");
 sistema.associarAutocarroALinha("TUB-110", "L19");

 System.out.println("[PGU] Sistema inicializado com 10 autocarros e 15 linhas TUB.");
 return sistema;
 }

 @Override
 public void addCorsMappings(CorsRegistry registry) {
 // Prioridade: CORS_ALLOWED_ORIGINS (env) → @Value do application.properties
 String origins = System.getenv("CORS_ALLOWED_ORIGINS");
 if (origins == null || origins.isBlank()) {
 origins = corsAllowedOrigins;
 }
 
 registry.addMapping("/api/**")
 .allowedOrigins(origins.split(","))
 .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
 .allowedHeaders("Content-Type", "Authorization", "X-Admin-Email")
 .exposedHeaders("Authorization", "X-Admin-Email")
 .allowCredentials(true)
 .maxAge(3600);
 }
}
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

 // Linhas TUB 
 sistema.registarLinha("L7", "Celeirós — S. Vítor");
 sistema.registarLinha("L40", "Gualtar — Real");
 sistema.registarLinha("L43", "Estação — Universidade");

 // Associar autocarros às linhas 
 sistema.associarAutocarroALinha("TUB-101", "L7");
 sistema.associarAutocarroALinha("TUB-102", "L43");
 sistema.associarAutocarroALinha("TUB-103", "L7");
 sistema.associarAutocarroALinha("TUB-104", "L40");
 sistema.associarAutocarroALinha("TUB-105", "L43");
 sistema.associarAutocarroALinha("TUB-106", "L40");

 System.out.println("[PGU] Sistema inicializado com 6 autocarros e 3 linhas TUB.");
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
 .allowedHeaders("Content-Type", "Authorization")
 .exposedHeaders("Authorization")
 .allowCredentials(true)
 .maxAge(3600);
 }
}
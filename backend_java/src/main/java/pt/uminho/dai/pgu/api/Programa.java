package pt.uminho.dai.pgu.api;

import pt.uminho.dai.pgu.p2_businesslogic.acessos_configuracao.*;
import pt.uminho.dai.pgu.p2_businesslogic.operacao_tempo_real.*;
import pt.uminho.dai.pgu.p2_businesslogic.analitica_historico.*;
import pt.uminho.dai.pgu.p7_data.*;
import pt.uminho.dai.pgu.p7_data.acessos_configuracao.*;
import pt.uminho.dai.pgu.p7_data.operacao_tempo_real.*;
import pt.uminho.dai.pgu.p7_data.analitica_historico.*;
import pt.uminho.dai.pgu.api.acessos_configuracao.*;
import pt.uminho.dai.pgu.api.operacao_tempo_real.*;
import pt.uminho.dai.pgu.api.analitica_historico.*;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "pt.uminho.dai.pgu")
public class Programa {
    public static void main(String[] args) {
        SpringApplication.run(Programa.class, args);
        System.out.println("====== SERVIDOR SPRING BOOT TUB INICIADO NA PORTA 8080 ======");
    }
}
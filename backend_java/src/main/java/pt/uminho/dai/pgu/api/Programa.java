package pt.uminho.dai.pgu.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "pt.uminho.dai.pgu")
public class Programa {
    public static void main(String[] args) {
        SpringApplication.run(Programa.class, args);
        System.out.println("====== SERVIDOR SPRING BOOT TUB INICIADO NA PORTA 8080 ======");
    }
}
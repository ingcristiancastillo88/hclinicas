package ec.salud.citas.hclinicas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import tools.jackson.databind.ObjectMapper;

@EnableAsync
@SpringBootApplication
public class HclinicasApplication {

	public static void main(String[] args) {
		SpringApplication.run(HclinicasApplication.class, args);
	}

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

}

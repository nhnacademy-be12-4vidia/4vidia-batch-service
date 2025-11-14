package init.data.DataParser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class DataParserApplication {

	public static void main(String[] args) {
		SpringApplication.run(DataParserApplication.class, args);
	}

}

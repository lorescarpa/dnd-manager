package it.lorenzoscarpa.dnd;

import org.springframework.boot.SpringApplication;

public class TestDndApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(DndApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

package com.dawproject.springboot.beer.app;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class Beer4LifeApplication implements CommandLineRunner{

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;
	
//	@Autowired
//	private IUploadFileService uploadFileService;
	

	public static void main(String[] args) {
		SpringApplication.run(Beer4LifeApplication.class, args);
	}
	
	@Override
	public void run(String... args) throws Exception {
//		uploadFileService.deleteAll();
//		uploadFileService.init();
		
		String password="admin";
		
		for(int i = 0 ; i<2 ; i++) {
			String bcryptPassword = passwordEncoder.encode(password);
			System.out.println(bcryptPassword);
		}
	}

}

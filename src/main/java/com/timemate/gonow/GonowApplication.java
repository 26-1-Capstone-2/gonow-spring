package com.timemate.gonow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class}) // 시큐리티가 더 이상 비밀번호를 자동으로 만들지 않음
public class GonowApplication {
	public static void main(String[] args) {
		SpringApplication.run(GonowApplication.class, args);
	}
}

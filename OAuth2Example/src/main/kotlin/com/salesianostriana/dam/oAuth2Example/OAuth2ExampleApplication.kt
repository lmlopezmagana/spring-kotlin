package com.salesianostriana.dam.oAuth2Example

import com.salesianostriana.dam.oAuth2Example.security.users.User
import com.salesianostriana.dam.oAuth2Example.security.users.UserService
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class OAuth2ExampleApplication {
	private val logger = LoggerFactory.getLogger(javaClass)

	@Bean
	fun setupInitialData(
			userService: UserService
	) = CommandLineRunner {
		_ ->
		run {
			var user1 = User(username = "luismi", password = "12345678", fullName = "Luis Miguel Lopez", roles = mutableSetOf("USER"))
			var user2 = User(username = "miguel", password = "12345678", fullName = "Miguel Campos Rivera", roles = mutableSetOf("ADMIN"))


			user1 = userService.create(user1).orElseThrow()
			user2 = userService.create(user2).orElseThrow()

			logger.info(user1.toString())
			logger.info(user2.toString())


		}
	}
}

fun main(args: Array<String>) {
	runApplication<OAuth2ExampleApplication>(*args)
}

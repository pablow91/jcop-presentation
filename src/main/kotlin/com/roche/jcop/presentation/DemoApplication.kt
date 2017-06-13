package com.roche.jcop.presentation

import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*
import springfox.documentation.swagger2.annotations.EnableSwagger2
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@SpringBootApplication
@EnableSwagger2
class DemoApplication {
    @Bean
    fun hello(): Queue {
        return Queue("bonus")
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(DemoApplication::class.java, *args)
}

@RestController
@RequestMapping("/api")
class BankController(
        val userRepository: UserRepository,
        val queue: Queue,
        val template: RabbitTemplate
) {

    @GetMapping
    fun getAllUsers(): List<UserEntity> {
        return userRepository.findAll()
    }

    @PostMapping("/{name}/{int}/")
    fun addUser(@PathVariable("name") name: String, @PathVariable("int") value: Int): Unit {
        userRepository.save(UserEntity(name, value))
    }

    @PutMapping("/{int}/")
    fun giveBonus(@PathVariable("int") value: Int) {
        template.convertAndSend(queue.name, value.toString())
    }

}

@Entity
data class UserEntity(
        val name: String,
        val value: Int,
        @Id @GeneratedValue val id: Int? = null
)

interface UserRepository : JpaRepository<UserEntity, Int>

@Component
@RabbitListener(queues = arrayOf("bonus"))
class BonusReceiver(
        val userRepository: UserRepository
) {

    @RabbitHandler
    fun receive(param: String) {
        val addList = userRepository.findAll()
                .map { it.copy(value = it.value + param.toInt()) }
        userRepository.save(addList)
        userRepository.flush()
    }
}

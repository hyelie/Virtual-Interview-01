package com.newsfeed.demo.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

  @Bean
  public Queue fanoutQueue() {
    return new Queue(RabbitMQConstants.FANOUT_QUEUE, true);
  }

  @Bean
  public DirectExchange fanoutExchange() {
    return new DirectExchange(RabbitMQConstants.FANOUT_EXCHANGE);
  }

  @Bean
  public Binding fanoutBinding(Queue fanoutQueue, DirectExchange fanoutExchange) {
    return BindingBuilder.bind(fanoutQueue).to(fanoutExchange)
        .with(RabbitMQConstants.FANOUT_ROUTING_KEY);
  }

  @Bean
  public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(jsonMessageConverter());
    return rabbitTemplate;
  }
}

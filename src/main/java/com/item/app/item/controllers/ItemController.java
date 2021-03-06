package com.item.app.item.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.item.app.item.models.Item;

import com.products.app.productscommons.models.entity.Product;
import com.item.app.item.models.service.ItemService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;

@RefreshScope
@RestController
public class ItemController {
    
    private final Logger logger = LoggerFactory.getLogger(ItemController.class);

    @Autowired
    private Environment environment;

    @Autowired
    @SuppressWarnings("rawtypes")
    private CircuitBreakerFactory cBreakerFactory;

    @Autowired
    @Qualifier("restTemplate-service")
    private ItemService itemService;

    @Value("${text.config}") private String text;

    @GetMapping("/config")
    public ResponseEntity<?> getConfig(@Value("${server.port}") String port){
        Map<String, String> json = new HashMap<>();
        json.put("text", text);
        json.put("port", port);
        if(environment.getActiveProfiles().length > 0 && environment.getActiveProfiles()[0].equals("development")){
            json.put("autor", environment.getProperty("config.autor.name"));
            json.put("email", environment.getProperty("config.autor.email"));
        }
        return new ResponseEntity<Map<String, String>>(json, HttpStatus.OK);
    }

    @GetMapping("/list")
    public List<Item> list(@RequestParam(name = "name", required = false) String name, @RequestHeader(name = "token-request", required = false) String token){
        System.out.println(name);
        System.out.println(token);
        return itemService.findAll();
    }
    @PostMapping("/save")
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@RequestBody Product product){
        return itemService.save(product);
    }
    @PutMapping("/save/{id}")
    @ResponseStatus(HttpStatus.CREATED)
    public Product update(@RequestBody Product product, @PathVariable Long id){
        return itemService.update(product, id);
    }
    @DeleteMapping("/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id){
        itemService.delete(id);
    }

    //Factory
    @GetMapping("/detail.v2/{id}/cantidad/{cantidad}")
    public Item detail(@PathVariable Long id,@PathVariable Integer cantidad){
        return cBreakerFactory.create("items")
        .run( () -> itemService.findById(id, cantidad), e -> alternativeMethod(id, cantidad, e));
    }

    //CircuitBreaker
    @CircuitBreaker(name = "items", fallbackMethod = "alternativeMethod")
    @TimeLimiter(name = "items")
    @GetMapping("/detail.v3/{id}/cantidad/{cantidad}")
    public Item detailv2(@PathVariable Long id,@PathVariable Integer cantidad){
        return itemService.findById(id, cantidad);
    }

    //Normal
    @CircuitBreaker(name = "items", fallbackMethod = "alternativeMethod2")
    @TimeLimiter(name = "items")
    @GetMapping("/detail/{id}/cantidad/{cantidad}")
    public CompletableFuture<Item> detailv3(@PathVariable Long id,@PathVariable Integer cantidad){
        return CompletableFuture.supplyAsync(() -> itemService.findById(id, cantidad));
    }

    public Item alternativeMethod(Long id, Integer cantidad, Throwable e){
        logger.info(e.getMessage());
        Item item = new Item();
        Product product = new Product();
        
        item.setCantidad(cantidad);
        product.setId(id);
        product.setName("Camara Somy");
        product.setPrice(500.00);
        item.setProduct(product);
        return item;
    }
    //TimeLimiter
    public CompletableFuture<Item> alternativeMethod2(Long id, Integer cantidad, Throwable e){
        logger.info(e.getMessage());
        Item item = new Item();
        Product product = new Product();
        
        item.setCantidad(cantidad);
        product.setId(id);
        product.setName("Camara Somy");
        product.setPrice(500.00);
        item.setProduct(product);
        return CompletableFuture.supplyAsync(() -> item);
    }
    
}

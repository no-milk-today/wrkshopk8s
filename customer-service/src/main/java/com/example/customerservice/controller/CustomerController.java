package com.example.customerservice.controller;

import com.example.clients.order.OrderClient;
import com.example.clients.order.OrderResponse;
import com.example.customerservice.model.Customer;
import com.example.customerservice.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/customers") 
public class CustomerController {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderClient orderClient;

    
    @GetMapping
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("TEST!");
    }

    
    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable Long id) {
        Optional<Customer> customer = customerRepository.findById(id);
        
        return customer.map(ResponseEntity::ok)
                       .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/orders")
    public List<OrderResponse> getOrdersForCustomer(@PathVariable Long id) {
        return orderClient.findByCustomerId(id);
    }

    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) 
    public Customer createCustomer(@RequestBody Customer customer) {
        
        
        return customerRepository.save(customer);
    }

    
}
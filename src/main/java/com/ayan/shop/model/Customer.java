package com.ayan.shop.model;

import jakarta.persistence.*;

@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public String name;

    @Column(nullable = false, unique = true)
    public String email;

    @Column(nullable = false)
    public String password;

    @Column(nullable = false)
    public String role = "CUSTOMER";

    public String address;
    public Boolean active = Boolean.TRUE;

    public Customer() {}

    public Customer(String n, String e, String p, String r) {
        name = n;
        email = e;
        password = p;
        role = r;
    }
}

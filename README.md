# Dynamic create table with JPA

* Use Javassist lib to generate Java class file
* Jpa will auto create these entity table
* In this demo project, the class dynamic created
```java
package com.example;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class NewClass {
    @Column
    @Id
    private Long id;
    private String name;

    public NewClass() {
        System.out.println("INIT");
    }

    public void printInfo() {
        System.out.println("info!");
    }
}
```



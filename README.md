# Loading classes at runtime to create Custom Tables

This demo meets the requirement of dynamic loading of classes and creating custom tables(from users).

In traditional software process Tables have been defined as lunching the application((entity and repository files). 

But in case of custom tables there are no defined entity and repository classes. The reason for this is, custom tables can have user defined variables which cannot be predetermined. So to store those variables in database we need to create entity classes and repository classes when we receive them from user, i.e.  at run time.

## Technical highlights

* Use Javassist lib to create entity class
* Jpa will create the mapping of entity and table at run time
* To load/create classes at runtime we should avoid springboot fat jar and go with a lib which contains all dependent jars and the application jar as well.

the entity class create in this demo
```java
package com.example;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(
    name = "new_class"
)
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

Hibernate ddl corresponding
```mysql
create table new_class (
   id bigint not null,
    name varchar(255),
    primary key (id)
) engine=InnoDB
```



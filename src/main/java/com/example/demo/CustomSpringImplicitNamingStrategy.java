package com.example.demo;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitForeignKeyNameSource;
import org.hibernate.boot.model.naming.NamingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;

import java.util.List;

import static java.util.stream.Collectors.joining;

public class CustomSpringImplicitNamingStrategy extends SpringImplicitNamingStrategy {

    final static Logger log = LoggerFactory.getLogger(CustomSpringImplicitNamingStrategy.class);
    @Override
    public Identifier determineForeignKeyName(ImplicitForeignKeyNameSource source) {
        String hashedFkName = NamingHelper.INSTANCE.generateHashedFkName("FK", source.getTableName(), source.getReferencedTableName(), source.getColumnNames());
        String sourceColumnNames = joinIdentifierNames(source.getColumnNames());
        String referencedColumnNames = joinIdentifierNames(source.getReferencedColumnNames());

        StringBuilder foreignKeyName = new StringBuilder(hashedFkName);
        appendIfNotTooLong(foreignKeyName, source.getTableName().getText());
        appendIfNotTooLong(foreignKeyName, "_");
        appendIfNotTooLong(foreignKeyName, sourceColumnNames);
        appendIfNotTooLong(foreignKeyName, "_to_");
        appendIfNotTooLong(foreignKeyName, source.getReferencedTableName().getText());
        appendIfNotTooLong(foreignKeyName, "_");
        appendIfNotTooLong(foreignKeyName, referencedColumnNames);

        Identifier identifier = this.toIdentifier(foreignKeyName.toString(), source.getBuildingContext());
        log.debug("Generated values: foreign key name: {}, identifier: {}", foreignKeyName.toString(), identifier.getText());
        return identifier;
    }

    private static void appendIfNotTooLong(StringBuilder sb, String append) {
        if (sb.length() + append.length() < 64) {
            sb.append(append);
        }
    }

    private static String joinIdentifierNames(List<? extends Identifier> identifiers) {
        return identifiers.stream()
                .map(Identifier::getText)
                .map(String::toLowerCase)
                .collect(joining("_"));
    }
}

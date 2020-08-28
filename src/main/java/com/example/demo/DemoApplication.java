package com.example.demo;

import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.StringMemberValue;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.MySQL5InnoDBDialect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.support.ClasspathScanningPersistenceUnitPostProcessor;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.annotation.PostConstruct;
import javax.persistence.*;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * @author wzdbsss@hotmail.com
 */
@SpringBootApplication
public class DemoApplication {

    @Autowired
    DataSource dataSource;

    private static final String TABLE_NAME = "new_class";

    private static final String PACKAGE_TO_SCAN = "com.example";

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @PostConstruct
    public void dynamicCreateTable() throws ClassNotFoundException, IOException, CannotCompileException {
        Map<String, Class> classHashMap = create();
        buildEntityManagerFactory(classHashMap, "update");
    }

    private Map<String, Class> create() throws CannotCompileException, IOException, ClassNotFoundException {

        ClassPool classPool = ClassPool.getDefault();
        String customClassPath = new File("").getAbsolutePath();
        addClasspath(classPool, customClassPath);

        CtClass ctClass = classPool.makeClass(PACKAGE_TO_SCAN + ".NewClass");
        final ClassFile ccFile = ctClass.getClassFile();

        addConstructor(ctClass);
        addEntityAnnotation(ccFile);
        addFieldId(ctClass);
        addFieldName(ctClass);
        addMethodPrintInfo(ctClass);
        writeClassFile(customClassPath, ctClass);

        Class<?> newClass = classPool.getClassLoader().loadClass(ctClass.getName());
        HashMap<String, Class> classHashMap = new HashMap<>();
        classHashMap.put(newClass.getName(), newClass);
        return classHashMap;

    }

    private void addClasspath(ClassPool classPool, String customClassPath) {
        URLClassLoader classLoader = (URLClassLoader) classPool.getClassLoader();
        try {
            Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            method.invoke(classLoader, new File(customClassPath).toURI().toURL());
        } catch (NoSuchMethodException | IllegalAccessException | MalformedURLException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void addConstructor(CtClass ctClass) throws CannotCompileException {
        CtConstructor ctConstructor = new CtConstructor(new CtClass[]{}, ctClass);
        StringBuffer buffer = new StringBuffer();
        buffer.append("{\n")
                .append("System.out.println(\"INIT\");\n")
                .append("\n}");
        ctConstructor.setBody(buffer.toString());
        ctClass.addConstructor(ctConstructor);
    }

    private void addEntityAnnotation(ClassFile ccFile) {
        final AnnotationsAttribute entityAttr = new AnnotationsAttribute(ccFile.getConstPool(), AnnotationsAttribute.visibleTag);

        final Annotation entityAnnotation = new Annotation(Entity.class.getName(), ccFile.getConstPool());
        entityAttr.addAnnotation(entityAnnotation);

        final Annotation tableAnnotation = new Annotation(Table.class.getName(), ccFile.getConstPool());
        tableAnnotation.addMemberValue("name", new StringMemberValue(TABLE_NAME, ccFile.getConstPool()));
        entityAttr.addAnnotation(tableAnnotation);

        ccFile.addAttribute(entityAttr);
    }

    private void addFieldName(CtClass ctClass) throws CannotCompileException {
        ctClass.addField(CtField.make("private String name;", ctClass));
    }

    private void addFieldId(CtClass ctClass) throws CannotCompileException {
        CtField privateLongId = CtField.make("private Long id;", ctClass);
        final AnnotationsAttribute fieldAttr = new AnnotationsAttribute(ctClass.getClassFile().getConstPool(), AnnotationsAttribute.visibleTag);
        final Annotation columnAnnotation = new Annotation(Column.class.getName(), ctClass.getClassFile().getConstPool());
        final Annotation idAnnotation = new Annotation(Id.class.getName(), ctClass.getClassFile().getConstPool());
        fieldAttr.addAnnotation(columnAnnotation);
        fieldAttr.addAnnotation(idAnnotation);
        privateLongId.getFieldInfo().addAttribute(fieldAttr);

        ctClass.addField(privateLongId);
    }

    private void writeClassFile(String customClassPath, CtClass ctClass) throws CannotCompileException, IOException {
        ctClass.writeFile(customClassPath);
    }

    private void addMethodPrintInfo(CtClass ctClass) throws CannotCompileException {
        CtMethod ctMethod = new CtMethod(CtClass.voidType, "printInfo", new CtClass[]{}, ctClass);
        ctMethod.setModifiers(Modifier.PUBLIC);
        StringBuffer buffer2 = new StringBuffer();
        buffer2.append("{\nSystem.out.println(\"info!\");\n")
                .append("}");
        ctMethod.setBody(buffer2.toString());
        ctClass.addMethod(ctMethod);
    }

    private EntityManagerFactory buildEntityManagerFactory(final Map<String, Class> entityClasses, final String ddlMode) {
        final LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactoryBean.setDataSource(dataSource);
        entityManagerFactoryBean.setPackagesToScan(PACKAGE_TO_SCAN);
        entityManagerFactoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        entityManagerFactoryBean.setJpaProperties(createProperties(ddlMode));
        entityManagerFactoryBean.setPersistenceUnitPostProcessors(new ClasspathScanningPersistenceUnitPostProcessor(PACKAGE_TO_SCAN),
                pui ->
                {
                    final List<String> managedClassNames = pui.getManagedClassNames();
                    final List<String> classesShouldBeManaged = entityClasses.values().stream().map(Class::getName).collect(Collectors.toList());
                    managedClassNames.removeIf(s -> !classesShouldBeManaged.contains(s));
                }
        );
        //executeSchemaUpdate
        entityManagerFactoryBean.afterPropertiesSet();

        return entityManagerFactoryBean.getObject();
    }

    private Properties createProperties(final String ddlMode) {
        final Properties properties = new Properties();
        properties.put(AvailableSettings.HBM2DDL_AUTO, ddlMode);
        properties.put(AvailableSettings.FORMAT_SQL, true);
        properties.put(AvailableSettings.DIALECT, MySQL5InnoDBDialect.class.getName());
        properties.put(AvailableSettings.SHOW_SQL, true);
        properties.put(AvailableSettings.PHYSICAL_NAMING_STRATEGY, PhysicalNamingStrategyStandardImpl.class.getName());
        properties.put(AvailableSettings.IMPLICIT_NAMING_STRATEGY, CustomSpringImplicitNamingStrategy.class.getName());
        properties.put(AvailableSettings.JDBC_TIME_ZONE, "UTC");
        return properties;
    }

}

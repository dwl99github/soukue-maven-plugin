package com.soukue.mybatisplusgen;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.baomidou.mybatisplus.generator.keywords.MySqlKeyWordsHandler;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

@Mojo(name = "generator")
public class GeneratorMojo extends AbstractMojo {

    @Parameter(property = "classFolderPath")
    private String classFolderPath;
    @Parameter(property = "application")
    private String application;
    @Parameter(property = "configurationFile", required = true)
    private File configurationFile;
    @Parameter(property = "javaFileDir", required = true)
    private File javaFileDir;
    @Parameter(property = "resourceFileDir", required = true)
    private File resourceFileDir;
    @Parameter(property = "apiFileDir")
    private File apiFileDir;
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("开始生成MyBatis-Plus代码");
        if (this.configurationFile == null || !(this.configurationFile.exists())) {
            throw new MojoExecutionException("configurationFile 读取失败");
        }
        getLog().info("configurationFile: " + this.configurationFile.getAbsolutePath());
        getLog().info("javaFileDir: " + this.javaFileDir.getAbsolutePath());
        getLog().info("resourceFileDir: " + this.resourceFileDir.getAbsolutePath());

        Properties prop = new Properties();
        PrintStream out;
        try {
            prop.load(Files.newInputStream(this.configurationFile.toPath()));
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            out = new PrintStream(bout);
            prop.list(out);
            getLog().info("输入参数: ");
            getLog().info(bout.toString());
        } catch (IOException e) {
            getLog().error(e.getMessage(), e);
            throw new MojoExecutionException(e.getMessage());
        }

        Set<String> nameSet = prop.stringPropertyNames();
        getLog().info("nameSet: " + nameSet);
        for (String name : nameSet){
            if ((name.startsWith("table.generate.")) && (name.endsWith(".package"))) {
                String tableName = name.substring("table.generate.".length(), name.lastIndexOf(46));
                getLog().info("tableName: " + tableName);
                generate(prop, tableName);
            }
        }
    }

    private void generate(Properties prop, String tableName) {
        String packageName = prop.getProperty("table.generate." + tableName + ".package");
        getLog().info("packageName: " + packageName);
        String xmlPrefixPath = packageName.replace('.', '/');
        String mapperXmlPath = this.resourceFileDir.getAbsolutePath() + "/" + xmlPrefixPath;
        getLog().info("mapperXmlPath: " + mapperXmlPath);
        getLog().info("jdbc.url: " + prop.getProperty("jdbc.url"));
        getLog().info("schema: " + prop.getProperty("jdbc.schema"));
        boolean isOverwrite;
        String override = prop.getProperty("global.override");
        if (override != null){
            isOverwrite = Boolean.parseBoolean(override);
        } else {
            isOverwrite = false;
        }

        try{
            DataSourceConfig.Builder builder1 = new DataSourceConfig.Builder(prop.getProperty("jdbc.url") + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC",
                    prop.getProperty("jdbc.username"), prop.getProperty("jdbc.password"));
            DataSourceConfig.Builder dataSourceConfig = builder1
                    .schema(prop.getProperty("jdbc.schema"))
                    .keyWordsHandler(new MySqlKeyWordsHandler());
            FastAutoGenerator.create(dataSourceConfig)
                    .globalConfig(builder -> builder
                                    .author("soukue") // 设置作者名
                                    .outputDir(this.javaFileDir.getAbsolutePath()) // 设置输出目录
                                    .disableOpenDir() //禁止自动打开输出目录
//                              .enableSwagger() // 开启 Swagger 模式
                                    .dateType(DateType.ONLY_DATE) // 设置时间类型策略
                                    .commentDate("yyyy-MM-dd") // 设置注释日期格式
                    )
                    .packageConfig(builder -> builder
                            .parent(packageName) // 设置父包名
                            .entity("entity") // 设置 Entity 包名
                            .mapper("mapper")
                            .service("service")
                            .serviceImpl("service.impl")
                            .xml("mappers")
                            .pathInfo(Collections.singletonMap(OutputFile.xml, mapperXmlPath)) // 设置路径配置信息
                    )
                    .strategyConfig(builder -> {
                        builder.addInclude(tableName) // 设置需要生成的表名
                                .entityBuilder().formatFileName("%sEntity")
                                .enableLombok() // 启用 Lombok
                                .enableTableFieldAnnotation() // 启用字段注解
                                .enableFileOverride()
                                .controllerBuilder().disable(); // 不生成controller
//                            .enableRestStyle(); // 启用 REST 风格
                        ;
                        if(isOverwrite){
                            builder.mapperBuilder().enableFileOverride() // 允许mapper覆盖
                                    .serviceBuilder().enableFileOverride(); // 允许service覆盖
                        }
                    })
                    .templateEngine(new FreemarkerTemplateEngine())
                    .execute();
        } catch (Exception e){
            getLog().error(e.getMessage(), e);
            throw e;
        }
    }
}
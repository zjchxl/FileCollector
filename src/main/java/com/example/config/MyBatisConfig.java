package com.example.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

@Configuration
@MapperScan(basePackages = {
            "com.example.fileMonitor.dao",
            "com.example.ftp.dao"
        }, //扫描的包路径
        sqlSessionFactoryRef = "sqlSessionFactory" //指定使用的SqlSessionFactor
)
public class MyBatisConfig {
    @Autowired
    private DataSource dataSource;
    @Value("${mybatis.mapper-locations}")
    private String mapperLocations;
    @Value("${mybatis.type-aliases-package}")
    private String typeAliasesPackage;

    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);

        // 关键：设置Mapper XML文件的位置
        // 使用通配符匹配所有Mapper XML文件
        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources(mapperLocations);

        System.out.println("加载的Mapper XML文件：");
        for (Resource resource : resources) {
            System.out.println("  - " + resource.getFilename());
        }
        factoryBean.setMapperLocations(resources);

        // 设置实体类别名包，这样在XML中可以直接使用类名
        factoryBean.setTypeAliasesPackage(typeAliasesPackage);

        // 配置MyBatis
        org.apache.ibatis.session.Configuration configuration =
                new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);  // 驼峰命名转换
        configuration.setCacheEnabled(true);              // 开启二级缓存
        configuration.setLogImpl(org.apache.ibatis.logging.stdout.StdOutImpl.class); // SQL日志

        factoryBean.setConfiguration(configuration);

        return factoryBean.getObject();
    }
}

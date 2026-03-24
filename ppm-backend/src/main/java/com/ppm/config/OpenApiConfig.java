package com.ppm.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PPM 数据分析系统 API")
                        .description("各基地各供应商 PPM 计算、统计、查询与导入导出")
                        .version("0.0.1"));
    }
}

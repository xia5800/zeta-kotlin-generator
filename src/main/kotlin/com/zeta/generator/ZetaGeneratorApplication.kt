package com.zeta.generator

import com.zeta.generator.engine.CodeGeneratorConfig
import com.zeta.generator.engine.Generator

/**
 * Main方法
 *
 * @author gcc
 */
fun main(args: Array<String>) {
    // 生成代码
    Generator.run(buildConfig())
}

/**
 * 获取配置
 * @return CodeGeneratorConfig
 */
private fun buildConfig(): CodeGeneratorConfig  {
    // 要生成代码的表
    val tableInclude = mutableListOf(
        "sys_user"
    )
    // 需要去除的表前缀
    val tablePrefix = mutableListOf("sys_")

    return CodeGeneratorConfig.build("zeta-kotlin", "demo", "gcc").apply {
        this.tableInclude = tableInclude
        this.tablePrefix = tablePrefix
        // 项目包名
        this.packageName = "com.zeta"
        // 代码输出目录，不要带项目名，生成代码的时候会自动拼接项目名
        this.outputDir = "D://codeGen/"

        // entity父类路径（默认包含，id、创建人创建时间、修改人修改时间）。 如不需要修改人修改时间，请使用SuperEntity。不需要继承父类请设置空""值
        this.entitySuperClass = "org.zetaframework.base.entity.Entity"

        // 数据库配置
        this.dbUrl = "jdbc:mysql://localhost:3306/zeta_kotlin?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=GMT%2B8"
        this.dbUsername = "root"
        this.dbPassword = "root"
    }
}

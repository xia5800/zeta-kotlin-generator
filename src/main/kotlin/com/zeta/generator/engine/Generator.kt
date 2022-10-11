package com.zeta.generator.engine

import cn.hutool.core.util.StrUtil
import cn.hutool.json.JSONUtil
import cn.hutool.log.Log
import cn.hutool.log.LogFactory
import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.generator.FastAutoGenerator
import com.baomidou.mybatisplus.generator.config.*
import com.baomidou.mybatisplus.generator.config.po.TableField
import com.baomidou.mybatisplus.generator.config.rules.DateType
import com.baomidou.mybatisplus.generator.engine.BeetlTemplateEngine
import com.zeta.generator.enums.EntityTypeEnum
import java.util.function.Consumer

/**
 * 生成器
 * @author gcc
 */
object Generator {
    private val log: Log = LogFactory.get();

    /** 空字符串，用于join时多拼接一个'/' */
    private const val EMPTY_STR = ""

    /** Kotlin 文件存放路径 */
    private const val KOTLIN_SOURCE_PATH = "src/main/kotlin"

    /** 资源 文件存放路径 */
    private const val RESOURCES_SOURCE_PATH = "src/main/resources"

    /** controller 文件存放路径 */
    private const val CONTROLLER_PATH = "controller"

    /** service 文件存放路径 */
    private const val SERVICE_PATH = "service"

    /** service实现类 文件存放路径 */
    private const val SERVICE_IMPL_PATH = "$SERVICE_PATH/impl"

    /** mapper 文件存放路径 */
    private const val MAPPER_PATH = "dao"

    /** xml 文件存放路径 */
    private const val MAPPER_XML_PATH = "mapper"

    /** entity 文件存放路径 */
    private const val ENTITY_PATH = "model/entity"

    /** entityDTO 文件存放路径 */
    private const val ENTITY_DTO_PATH = "model/dto"

    /** entityQueryParam 文件存放路径 */
    private const val ENTITY_QUERY_PARAM_PATH = "model/param"


    /**
     * 代码生成
     *
     * 说明：
     * 利用mybatis-plus-generator生成代码
     * 文档:https://baomidou.com/pages/981406/
     * @param config CodeGeneratorConfig
     */
    fun run(config: CodeGeneratorConfig) {
        FastAutoGenerator.create(config.dbUrl, config.dbUsername, config.dbPassword)
            // 全局配置
            .globalConfig(globalConfigBuild(config))
            // 包配置
            .packageConfig(packageConfigBuild(config))
            // 策略配置
            .strategyConfig(strategyConfigBuild(config))
            // 模板配置
            .templateConfig(templateConfigBuild())
            // 模板引擎配置
            .templateEngine(BeetlTemplateEngine())
            // 注入自定义配置参数
            .injectionConfig(injectionConfigBuild(config))
            .execute()
    }

    /**
     * 全局配置 构造器
     * @param config CodeGeneratorConfig  代码生成器配置参数
     * @return Consumer<GlobalConfig.Builder>
     */
    private fun globalConfigBuild(config: CodeGeneratorConfig): Consumer<GlobalConfig.Builder> =
        Consumer {
            // 代码输出目录 D:/codeGen/zeta-kotlin
            it.outputDir("${config.outputDir}/${config.projectName}")
            it.author(config.author)
            if (!config.openDir) {
                // 禁止打开输出目录
                it.disableOpenDir()
            }
            // 开启 kotlin 模式
            it.enableKotlin()
            // 开启 swagger 模式
            it.enableSwagger()
            // 覆盖已有文件
            it.fileOverride()
            // 使用 java8 新时间类型： LocalDateTime、LocalDate、LocalTime
            it.dateType(DateType.TIME_PACK)
            // 指定注释日期格式化
            it.commentDate("yyyy-MM-dd HH:mm:ss")
        }

    /**
     * 包配置 构造器
     * @param config CodeGeneratorConfig  代码生成器配置参数
     * @return Consumer<PackageConfig.Builder>
     */
    private fun packageConfigBuild(config: CodeGeneratorConfig): Consumer<PackageConfig.Builder> = Consumer {
        // 父包名
        it.parent(config.packageName)
        // 父包模块名
        it.moduleName(config.moduleName)
        // mapper包名
        it.mapper("dao")
        // entity包名
        it.entity("model.entity")

        // 配置什么文件生成在什么地方
        val basePath = getBasePath(config)
        val pathInfos: MutableMap<OutputFile, String> = mutableMapOf(
            OutputFile.controller to getControllerPath(basePath),
            OutputFile.entity to getEntityPath(basePath),
            OutputFile.mapper to getMapperPath(basePath),
            OutputFile.mapperXml to getXmlPath(config.outputDir, config.projectName, config.moduleName),
            OutputFile.service to getServicePath(basePath),
            OutputFile.serviceImpl to getServiceImplPath(basePath),
            OutputFile.other to basePath,
        )

        // 配置xml的生成路径在resource目录下
        it.pathInfo(pathInfos)
    }

    /**
     * 策略配置 构造器
     * @param config CodeGeneratorConfig  代码生成器配置参数
     * @return Consumer<StrategyConfig.Builder>
     */
    private fun strategyConfigBuild(config: CodeGeneratorConfig): Consumer<StrategyConfig.Builder> = Consumer {
        it.addInclude(config.tableInclude!!)
        it.addTablePrefix(*config.tablePrefix!!.toTypedArray())
        // Entity 策略配置
        it.entityBuilder().apply {
            // 如果需要继承父类
            if(!config.superEntity.eq(EntityTypeEnum.NONE)) {
                this.enableActiveRecord()
                this.superClass(config.superEntity.path)
                this.addSuperEntityColumns(*config.superEntity.columns)
            }
        }
            .enableRemoveIsPrefix()
            .enableTableFieldAnnotation()
            .logicDeleteColumnName("deleted")
            .logicDeletePropertyName("deleted")
            .idType(IdType.INPUT)
            .build()

        // Controller 策略配置
        it.controllerBuilder()
            .enableRestStyle()
            .build()

        // Mapper 策略配置
        it.mapperBuilder()
            .enableBaseResultMap()
            .enableBaseColumnList()
            .build()
    }

    /**
     * 模板配置 构造器
     * @return Consumer<TemplateConfig.Builder>
     */
    private fun templateConfigBuild(): Consumer<TemplateConfig.Builder> = Consumer {
        // 配置模板路径
        it.controller("/templates/controller.kt")
        it.service("/templates/service.kt")
        it.serviceImpl("/templates/serviceImpl.kt")
        it.entity("/templates/entity.kt")
        it.mapper("/templates/mapper.kt")
        it.mapperXml("/templates/mapper.xml")
    }

    /**
     * 注入自定义配置参数  构造器
     * @return Consumer<InjectionConfig.Builder>
     */
    private fun injectionConfigBuild(config: CodeGeneratorConfig): Consumer<InjectionConfig.Builder> = Consumer { injectionConfigBuilder ->
        // 输出文件之前处理
        injectionConfigBuilder.beforeOutputFile { tableInfo, objectMap ->
            // 修改表注释，去掉表注释“xxx表”中的`表`字
            tableInfo.comment = updateTableComment(tableInfo.comment)

            // 获取权限码  表名：sys_user -> sys:user
            objectMap["authorityCode"] = genAuthorityCode(tableInfo.name)

            // 特有的字段处理 TreeEntity、StateEntity特有字段处理
            objectMap["uniqueColumns"] = uniqueFieldHandler(config, tableInfo.commonFields)

            // 自定义dto类名、包路径
            objectMap["customPackage"] = customPackage(tableInfo.entityName, config.packageName, config.moduleName)

            // 配置自定义模板的位置和生成出来的文件名
            customFile(injectionConfigBuilder, tableInfo.entityName)

            log.info(JSONUtil.toJsonStr(tableInfo))
            log.info(JSONUtil.toJsonStr(objectMap))
        }

        // 在这里配置自定义的模板参数 。可以直接在模板中使用${packageName}取值
        injectionConfigBuilder.customMap(mutableMapOf<String, Any>(
            "packageName" to config.packageName,
            "timePackage" to "java.time.*",
            "repositoryAnnotation" to config.enableRepository
        ))
    }

    /**
     * 【扩展函数】首字母小写
     *
     * 说明：这是一个String类的扩展函数
     * @return String
     */
    private fun String.lowCaseKeyFirstChar(): String {
        return if (Character.isLowerCase(this[0])) {
            this
        } else {
            StringBuilder().append(Character.toLowerCase(this[0])).append(this.substring(1)).toString()
        }
    }

    /**
     * 获取项目基础路径
     *
     * @param config 代码生成器配置参数
     * @return 项目绝对路径 eg: d://codeGen/zeta-boot/src/main/kotlin/com/zeta/system
     */
    private fun getBasePath(config: CodeGeneratorConfig): String {
        val packagePath = "${config.packageName.split(".").joinToString("/")}/${config.moduleName}"
        // basePath: 【d://codeGen】/【zeta-boot】/src/main/kotlin/【com/zeta/system】
        return listOf(config.outputDir, config.projectName, KOTLIN_SOURCE_PATH, packagePath).joinToString("/")
    }

    /**
     * 获取Controller文件的存放路径
     *
     * @param basePath 项目绝对路径 eg: d://codeGen/zeta-boot/src/main/kotlin/com/zeta/system
     * @return Controller文件的绝对路径 eg: d://codeGen/zeta-boot/src/main/kotlin/com/zeta/system/controller/
     */
    private fun getControllerPath(basePath: String): String {
        return listOf(basePath, CONTROLLER_PATH, EMPTY_STR).joinToString("/")
    }

    /**
     * 获取Entity文件的存放路径
     *
     * @param basePath 项目绝对路径 eg: d://codeGen/zeta-boot/src/main/kotlin/com/zeta/system
     * @return Entity文件的绝对路径 eg: d://codeGen/zeta-boot/src/main/kotlin/com/zeta/system/model/entity/
     */
    private fun getEntityPath(basePath: String): String {
        return listOf(basePath, ENTITY_PATH, EMPTY_STR).joinToString("/")
    }

    /**
     * 获取Service文件的存放路径
     *
     * @param basePath 项目绝对路径 eg: d://codeGen/zeta-boot/src/main/kotlin/com/zeta/system
     * @return Service文件的绝对路径 eg: d://codeGen/zeta-boot/src/main/kotlin/com/zeta/system/service/
     */
    private fun getServicePath(basePath: String): String {
        return listOf(basePath, SERVICE_PATH, EMPTY_STR).joinToString("/")
    }

    /**
     * 获取ServiceImpl文件的存放路径
     *
     * @param basePath 项目绝对路径 eg: d://codeGen/zeta-boot/src/main/kotlin/com/zeta/system
     * @return ServiceImpl文件的绝对路径 eg: d://codeGen/zeta-boot/src/main/kotlin/com/zeta/system/service/impl/
     */
    private fun getServiceImplPath(basePath: String): String {
        return listOf(basePath, SERVICE_IMPL_PATH, EMPTY_STR).joinToString("/")
    }

    /**
     * 获取Mapper(Dao)文件的存放路径
     *
     * @param basePath 项目绝对路径 eg: d://codeGen/zeta-boot/src/main/kotlin/com/zeta/system
     * @return Mapper(Dao)文件的绝对路径 eg: d://codeGen/zeta-boot/src/main/kotlin/com/zeta/system/dao/
     */
    private fun getMapperPath(basePath: String): String {
        return listOf(basePath, MAPPER_PATH, EMPTY_STR).joinToString("/")
    }

    /**
     * 获取Xml文件的存放路径
     *
     * @param outputDir  输出目录 不包含项目名。 eg: D://project
     * @param projectName 项目名 eg: zeta-boot
     * @param moduleName 模块名 eg: system
     * @return Xml文件的绝对路径 eg: d://codeGen/zeta-boot/src/main/resources/mapper/system
     */
    private fun getXmlPath(outputDir: String, projectName: String, moduleName: String): String {
        return listOf(outputDir, projectName, RESOURCES_SOURCE_PATH, MAPPER_XML_PATH, moduleName).joinToString("/")
    }

    /**
     * 获取 EntityDTO 名称
     *
     * @param entityName 实体类名 eg: SysUser
     * @return DTO名 eg:SysUserDTO
     */
    private fun getEntityDTOName(entityName: String): String {
        return "${entityName}DTO"
    }

    /**
     * 获取 SaveDTO 名称
     *
     * @param entityName 实体类名 eg: SysUser
     * @return SaveDTO名 eg:SysUserSaveDTO
     */
    private fun getEntitySaveDTOName(entityName: String): String {
        return "${entityName}SaveDTO"
    }

    /**
     * 获取 UpdateDTO 名称
     *
     * @param entityName 实体类名 eg: SysUser
     * @return UpdateDTO名 eg:SysUserUpdateDTO
     */
    private fun getEntityUpdateDTOName(entityName: String): String {
        return "${entityName}UpdateDTO"
    }

    /**
     * 获取 QueryParam 名称
     *
     * @param entityName 实体类名 eg: SysUser
     * @return QueryParam名 eg:SysUserQueryParam
     */
    private fun getEntityQueryParamName(entityName: String): String {
        return "${entityName}QueryParam"
    }

    /**
     * 修改表注释
     *
     * 说明：去掉表注释“xxx表”中的`表`字
     *
     * @param tableComment 表注释
     */
    private fun updateTableComment(tableComment: String): String {
        return if (StrUtil.isNotBlank(tableComment)) StrUtil.removeSuffix(tableComment, "表") else ""
    }

    /**
     * 生成权限码
     *
     * 例如： 表名：sys_user -> sys:user
     *
     * @param tableName 表名
     * @return
     */
    private fun genAuthorityCode(tableName: String): String {
        return tableName.split("_").joinToString(":")
    }

    /**
     * 特有的字段处理
     *
     * 说明：
     * 像TreeEntity、StateEntity这样的类，有一些特有的字段（除了id、create_time...之外的字段）。例如：'label'、'parent_id'、'state'
     * 在生成SaveDTO、UpdateDTO的时候不会生成进来。所以需要特别处理
     *
     * @param config 代码生成器配置参数
     * @param tableInfo  表信息，关联到当前字段信息
     */
    private fun uniqueFieldHandler(config: CodeGeneratorConfig, commonFields: List<TableField>): List<TableField> {
        var uniqueColumns: List<TableField> = mutableListOf()

        when(config.superEntity) {
            EntityTypeEnum.TREE_ENTITY -> {
                uniqueColumns = commonFields.filter {
                    // 筛选出字段名在特有字段里面的
                    it.columnName in EntityTypeEnum.TREE_ENTITY.uniqueColumns!!.toList()
                }
            }
            EntityTypeEnum.STATE_ENTITY -> {
                uniqueColumns = commonFields.filter {
                    // 筛选出字段名在特有字段里面的
                    it.columnName in EntityTypeEnum.STATE_ENTITY.uniqueColumns!!.toList()
                }
            }
            else -> {}
        }

        return uniqueColumns
    }

    /**
     * 自定义dto类名、包路径
     *
     * 说明：
     * 1.生成SaveDTO、UpdateDTO文件的名字
     * 2.自定义dto、param等文件的包路径
     *
     * @param entityName 实体类名称 eg: SysUser
     * @param packageName 项目包路径 eg: com.zeta
     * @param moduleName  模块名  eg: system
     * @return map eg: {
     *  "entityDTO": "SysUserDTO",           // DTO文件的名字
     *  "entitySaveDTO": "SysUserSaveDTO",   // SaveDTO文件的名字
     *  ...
     *  "dtoPackagePath": "com.zeta.system.model.dto.sysUser"  // DTO文件的包路径
     *  "paramBasePackagePath": "com.zeta.system.model.param"  // QueryParam文件的包路径
     * }
     */
    private fun customPackage(entityName: String, packageName: String, moduleName: String): MutableMap<String, String> {
        // 表名首字母小写
        val lowFirstCharEntityName = entityName.lowCaseKeyFirstChar()
        // dtoBasePackagePath：com.zeta.system.model.dto.sysUser
        val dtoBasePackagePath = "${packageName}.${moduleName}.model.dto.${lowFirstCharEntityName}"
        // paramBasePackagePath: com.zeta.system.model.param
        val paramBasePackagePath = "${packageName}.${moduleName}.model.param"

        // 构造返回值map
        return mutableMapOf(
            // DTO类、QueryParam类类名
            "entityDTO" to getEntityDTOName(entityName),
            "entitySaveDTO" to getEntitySaveDTOName(entityName),
            "entityUpdateDTO" to getEntityUpdateDTOName(entityName),
            "entityQueryParam" to getEntityQueryParamName(entityName),
            // DTO类、QueryParam类包路径
            "dtoPackagePath" to dtoBasePackagePath,
            "paramBasePackagePath" to paramBasePackagePath,
        )
    }

    /**
     * 配置自定义模板的位置和生成出来的文件名
     *
     * @param injectionConfigBuilder
     * @param entityName 实体类名称 eg: SysUser
     */
    private fun customFile(injectionConfigBuilder: InjectionConfig.Builder, entityName: String) {
        // entityDTOPath: model/dto/tableName
        val entityDTOPath = "${ENTITY_DTO_PATH}/${entityName.lowCaseKeyFirstChar()}"

        /**
         * entityDTO :          【d://codeGen】/【zeta-boot】/src/main/kotlin/【com/zeta/system/】/model/dto/【tableName】
         * entitySaveDTO :      【d://codeGen】/【zeta-boot】/src/main/kotlin/【com/zeta/system/】/model/dto/【tableName】SaveDTO
         * entityUpdateDTO :    【d://codeGen】/【zeta-boot】/src/main/kotlin/【com/zeta/system/】/model/dto/【tableName】UpdateDTO
         * entityQueryParam ：  【d://codeGen】/【zeta-boot】/src/main/kotlin/【com/zeta/system/】/model/param
         */
        // 在这里配置自定义模板的位置和生成出来的文件名
        injectionConfigBuilder.customFile(mutableMapOf<String, String>(
            // ps：由于上面配置了other目录的pathInfo。所以这里的key（即文件名）要配一个路径。为什么要`../`相对路径是因为不这样会创建一个和表同名的文件夹
            "../${entityDTOPath}/${getEntityDTOName(entityName)}.kt" to "/templates/entityDTO.kt.btl",
            "../${entityDTOPath}/${getEntitySaveDTOName(entityName)}.kt" to "/templates/entitySaveDTO.kt.btl",
            "../${entityDTOPath}/${getEntityUpdateDTOName(entityName)}.kt" to "/templates/entityUpdateDTO.kt.btl",
            "../${ENTITY_QUERY_PARAM_PATH}/${getEntityQueryParamName(entityName)}.kt" to "/templates/param.kt.btl",
        ))
    }

}

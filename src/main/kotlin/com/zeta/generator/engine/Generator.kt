package com.zeta.generator.engine

import cn.hutool.core.util.StrUtil
import cn.hutool.json.JSONUtil
import cn.hutool.log.Log
import cn.hutool.log.LogFactory
import com.baomidou.mybatisplus.annotation.FieldFill
import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.generator.FastAutoGenerator
import com.baomidou.mybatisplus.generator.config.*
import com.baomidou.mybatisplus.generator.config.rules.DateType
import com.baomidou.mybatisplus.generator.engine.BeetlTemplateEngine
import com.baomidou.mybatisplus.generator.fill.Property
import java.util.function.Consumer

/**
 * 生成器
 * @author gcc
 */
object Generator {
    private val log: Log = LogFactory.get();

    /** 空字符串，用于join时多拼接一个'/' */
    private val EMPTY_STR = ""

    /** Kotlin 文件存放路径 */
    private val KOTLIN_SOURCE_PATH = "src/main/kotlin"

    /** 资源 文件存放路径 */
    private val RESOURCES_SOURCE_PATH = "src/main/resources"

    /** controller 文件存放路径 */
    private val CONTROLLER_PATH = "controller"

    /** service 文件存放路径 */
    private val SERVICE_PATH = "service"

    /** service实现类 文件存放路径 */
    private val SERVICE_IMPL_PATH = "$SERVICE_PATH/impl"

    /** mapper 文件存放路径 */
    private val MAPPER_PATH = "dao"

    /** xml 文件存放路径 */
    private val MAPPER_XML_PATH = "mapper_"

    /** entity 文件存放路径 */
    private val ENTITY_PATH = "model/entity"

    /** entityDTO 文件存放路径 */
    private val ENTITY_DTO_PATH = "model/dto"

    /** entityQueryParam 文件存放路径 */
    private val ENTITY_QUERY_PARAM_PATH = "model/param"


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
        // 包路径  例如：com/zeta/system
        val packagePath = "${config.packageName.split(".").joinToString("/")}/${config.moduleName}"
        // basePath: 【d://codeGen】/【zeta-boot】/src/main/kotlin/【com/zeta/system】
        val basePath = listOf(config.outputDir, config.projectName, KOTLIN_SOURCE_PATH, packagePath).joinToString("/")
        // controller: 【d://codeGen】/【zeta-boot】/src/main/kotlin/【com/zeta/system/】controller/
        val controllerPath = listOf(basePath, CONTROLLER_PATH, EMPTY_STR).joinToString("/")
        // entity: 【d://codeGen】/【zeta-boot】/src/main/kotlin/【com/zeta/system】/model/entity/
        val entityPath = listOf(basePath, ENTITY_PATH, EMPTY_STR).joinToString("/")
        // service: 【d://codeGen】/【zeta-boot】/src/main/kotlin/【com/zeta/system/】service/
        val servicePath = listOf(basePath, SERVICE_PATH, EMPTY_STR).joinToString("/")
        // serviceImpl: 【d://codeGen】/【zeta-boot】/src/main/kotlin/【com/zeta/system/】service/impl/
        val serviceImplPath = listOf(basePath, SERVICE_IMPL_PATH, EMPTY_STR).joinToString("/")
        // dao: 【d://codeGen】/【zeta-boot】/src/main/kotlin/【com/zeta/system/】dao/
        val mapperPath = listOf(basePath, MAPPER_PATH, EMPTY_STR).joinToString("/")
        // mapper.xml: 【d://codeGen】/【zeta-boot】/src/main/resources/mapper_【system】
        val xmlPath = listOf(config.outputDir, config.projectName, RESOURCES_SOURCE_PATH, MAPPER_XML_PATH).joinToString("/")

        val pathInfos: MutableMap<OutputFile, String> = mutableMapOf(
            OutputFile.controller to controllerPath,
            OutputFile.entity to entityPath,
            OutputFile.mapper to mapperPath,
            OutputFile.mapperXml to xmlPath + config.moduleName + "/",
            OutputFile.service to servicePath,
            OutputFile.serviceImpl to serviceImplPath,
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
            if(StrUtil.isNotBlank(config.entitySuperClass)) {
                this.enableActiveRecord()
                this.superClass(config.entitySuperClass)
                this.addSuperEntityColumns("id", "created_by", "create_time", "updated_by", "update_time")
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
    private fun injectionConfigBuild(config: CodeGeneratorConfig): Consumer<InjectionConfig.Builder> = Consumer {
        // 输出文件之前处理
        it.beforeOutputFile { tableInfo, objectMap ->
            log.info(JSONUtil.toJsonStr(tableInfo))
            log.info(JSONUtil.toJsonStr(objectMap))

            // 修改表名，去掉表名xxx表中的`表`字
            val comment = StrUtil.removeSuffix(tableInfo.comment, "表")
            tableInfo.comment = comment

            // 这里也可以配置自定义模板的参数
            val entityName = tableInfo.entityName
            val entityDTOName = entityName + "DTO"
            val entitySaveDTOName = entityName + "SaveDTO"
            val entityUpdateDTOName = entityName + "UpdateDTO"
            val entityQueryParamName = entityName + "QueryParam"
            val lowFirstCharEntityName = entityName.lowCaseKeyFirstChar()
            // dtoBasePackagePath：com.zeta.system.model.dto.sysUser
            val dtoBasePackagePath = "${config.packageName}.${config.moduleName}.model.dto.${lowFirstCharEntityName}"
            // paramBasePackagePath: com.zeta.system.model.param
            val paramBasePackagePath = "${config.packageName}.${config.moduleName}.model.param"
            val customTemplateParam: MutableMap<String, String> = mutableMapOf(
                // DTO类、QueryParam类类名
                "entityDTO" to entityDTOName,
                "entitySaveDTO" to entitySaveDTOName,
                "entityUpdateDTO" to entityUpdateDTOName,
                "entityQueryParam" to entityQueryParamName,
                // DTO类、QueryParam类包路径
                "dtoPackagePath" to dtoBasePackagePath,
                "paramBasePackagePath" to paramBasePackagePath,
            )
            objectMap["customPackage"] = customTemplateParam

            // 获取权限码  表名：sys_user -> sys:user
            val authorityCode = tableInfo.name.split("_").joinToString(":")
            objectMap["authorityCode"] = authorityCode


            /**
             * entityDTO :          【d://codeGen】/【zeta-boot】/src/main/kotlin/【com/zeta/system/】/model/dto/【tableName】
             * entitySaveDTO :      【d://codeGen】/【zeta-boot】/src/main/kotlin/【com/zeta/system/】/model/dto/【tableName】
             * entityUpdateDTO :    【d://codeGen】/【zeta-boot】/src/main/kotlin/【com/zeta/system/】/model/dto/【tableName】
             * entityQueryParam ：  【d://codeGen】/【zeta-boot】/src/main/kotlin/【com/zeta/system/】/model/param
             */
            // entityDTOPath: model/dto/tableName
            val entityDTOPath = "${ENTITY_DTO_PATH}/${lowFirstCharEntityName}"
            // 在这里配置自定义模板的位置和生成出来的文件名
            it.customFile(mutableMapOf<String, String>(
                // ps：由于上面配置了other目录的pathInfo。所以这里的key（即文件名）要配一个路径。为什么要`../`相对路径是因为不这样会创建一个和表同名的文件夹
                "../${entityDTOPath}/${entityDTOName}.kt" to "/templates/entityDTO.kt.btl",
                "../${entityDTOPath}/${entitySaveDTOName}.kt" to "/templates/entitySaveDTO.kt.btl",
                "../${entityDTOPath}/${entityUpdateDTOName}.kt" to "/templates/entityUpdateDTO.kt.btl",
                "../${ENTITY_QUERY_PARAM_PATH}/${entityQueryParamName}.kt" to "/templates/param.kt.btl",
            ))
        }

        // 在这里配置自定义的模板参数 。可以直接在模板中使用${packageName}取值
        it.customMap(mutableMapOf<String, Any>(
            "packageName" to config.packageName,
            "repositoryAnnotation" to config.enableRepository
        ))
    }

    /**
     * 首字母小写
     *
     * 说明：这是一个String类的扩展函数
     * @param key String?
     * @return String?
     */
    private fun String.lowCaseKeyFirstChar(): String {
        return if (Character.isLowerCase(this[0])) {
            this
        } else {
            StringBuilder().append(Character.toLowerCase(this[0])).append(this.substring(1)).toString()
        }
    }

}

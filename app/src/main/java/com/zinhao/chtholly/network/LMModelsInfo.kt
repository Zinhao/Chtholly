package com.zinhao.chtholly.network


/**
 * 根响应对象
 */
data class ModelsResponse(
    val models: List<Model>
)

/**
 * 模型基础信息（涵盖 llm / embedding 两种类型）
 */
data class Model(
    val type: String,
    val publisher: String,
    val key: String,
    val display_name: String,
    val architecture: String? = null,          // embedding 模型无此字段
    val quantization: Quantization,
    val size_bytes: Long,
    val params_string: String? = null,
    val loaded_instances: List<LoadedInstance>,
    val max_context_length: Int,
    val format: String,
    val capabilities: Capabilities? = null,    // embedding 模型无此字段
    val description: String? = null
){
    override fun toString(): String {
        return key
    }
}

/**
 * 量化信息
 */
data class Quantization(
    val name: String,
    val bits_per_weight: Int
)

/**
 * 已加载的模型实例
 */
data class LoadedInstance(
    val id: String,
    val config: InstanceConfig? = null         // embedding 模型 loaded_instances 为空，config 可能不存在
)

/**
 * 模型实例运行配置
 */
data class InstanceConfig(
    val context_length: Int,
    val eval_batch_size: Int,
    val flash_attention: Boolean,
    val num_experts: Int,
    val offload_kv_cache_to_gpu: Boolean
)

/**
 * 模型能力声明
 */
data class Capabilities(
    val vision: Boolean,
    val trained_for_tool_use: Boolean
)
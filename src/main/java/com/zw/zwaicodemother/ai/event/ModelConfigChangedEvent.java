package com.zw.zwaicodemother.ai.event;

import com.zw.zwaicodemother.model.enums.ModelCapabilityEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 模型配置变更事件。
 * <p>
 * 用于在配置保存后通知各级缓存失效，避免 ModelConfigService 与模型消费方产生循环依赖。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 * @since 2026-09-17
 */
@Getter
@AllArgsConstructor
public class ModelConfigChangedEvent {

    /**
     * 变更的能力，为空表示全部失效
     */
    private final ModelCapabilityEnum capability;
}

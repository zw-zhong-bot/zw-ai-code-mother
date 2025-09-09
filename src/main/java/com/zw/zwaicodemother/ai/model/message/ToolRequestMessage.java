package com.zw.zwaicodemother.ai.model.message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ToolRequestMessage extends StreamMessage{
    private String id;
    private   String name;
    private  String arguments;

//    public ToolRequestMessage(String id, String name,  String arguments) {
//        super(StreamMessageTypeEnum.TOOL_REQUEST.getValue());
//        this.id = toolExecutionRequest.id;
//        this.name = toolExecutionRequest.name;
//        this.arguments = toolExecutionRequest.arguments;
//    }
}

package com.zinhao.chtholly;

import java.util.Map;

public interface CallAble {
    boolean call(Map<String,Object> argMap,String callId);
}
